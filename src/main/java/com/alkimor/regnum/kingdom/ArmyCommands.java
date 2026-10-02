package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.InvUtil;
import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.core.network.ArmyInfoPayload;
import com.alkimor.regnum.core.network.ArmyOrderPayload;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Приказы армии: жезл командира и командный экран. */
public final class ArmyCommands {
    private ArmyCommands() {}

    private static final Map<UUID, Long> LAST_ORDER_XP = new HashMap<>();
    public static final int MILITIA_COST = 4;

    public static List<SoldierEntity> squad(ServerPlayer king, int squad) {
        List<SoldierEntity> list = king.serverLevel().getEntitiesOfClass(SoldierEntity.class, king.getBoundingBox().inflate(96),
                s -> s.isAlive() && s.isOwnedBy(king) && (squad == 0 || s.getSquad() == squad));
        list.sort(Comparator.comparingInt(Entity::getId));
        return list;
    }

    public static int selectedSquad(ItemStack baton) {
        return baton.getOrDefault(KingdomModule.BATON_SQUAD.get(), 0);
    }

    public static Formation selectedFormation(ItemStack baton) {
        return Formation.byId(baton.getOrDefault(KingdomModule.BATON_FORMATION.get(), 0));
    }

    public static String squadName(int squad) {
        return squad == 0 ? "Вся армия" : "Отряд " + squad;
    }

    // ------------------------------------------------------------------ отдача приказа

    public static void issue(ServerPlayer king, int squad, Order order, Vec3 anchor, float facing, Formation f, @Nullable LivingEntity target) {
        List<SoldierEntity> list = squad(king, squad);
        if (list.isEmpty()) {
            Text.bar(king, squadName(squad) + ": рядом нет ваших солдат", ChatFormatting.GRAY);
            return;
        }
        int n = list.size();
        // порядок в строю по ролям: щитники и копья впереди, пехота за ними, стрелки сзади; конница — на флангах
        List<SoldierEntity> body = new java.util.ArrayList<>(), wings = new java.util.ArrayList<>();
        for (SoldierEntity s : list) (s.getSoldierType().role == SoldierType.Role.CAVALRY ? wings : body).add(s);
        body.sort(Comparator.comparingInt(s -> rowPriority(s.getSoldierType())));
        list = new java.util.ArrayList<>(body);
        list.addAll(wings);
        for (int i = 0; i < n; i++) {
            SoldierEntity s = list.get(i);
            int slot = i < body.size() ? i : -(i - body.size()) - 1;
            Vec3 post = switch (order) {
                case HOLD, CHARGE -> s.position();
                default -> f.worldPos(anchor, facing, slot, n, 0);
            };
            s.command(order, post, facing, f, slot, n, target == null ? null : target.getUUID());
            if (target != null) s.setTarget(target);
        }
        String extra = target != null ? ": " + target.getName().getString() : (order == Order.MOVE || order == Order.FOLLOW ? " (" + f.title + ")" : "");
        Text.bar(king, squadName(squad) + " [" + n + "] — " + order.title + extra, ChatFormatting.GOLD);
        king.serverLevel().playSound(null, king.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 0.8f, 0.7f);

        long now = king.serverLevel().getGameTime();
        Long last = LAST_ORDER_XP.get(king.getUUID());
        if (last == null || now - last > 200) {
            LAST_ORDER_XP.put(king.getUUID(), now);
            Skills.addXp(king, Skill.LEADERSHIP, 2);
        }
    }

    /** Чем меньше — тем ближе к врагу в строю. */
    public static int rowPriority(SoldierType t) {
        return switch (t.role) {
            case SHIELD -> 0;
            case SPEAR -> 1;
            case HEAVY -> 2;
            case INFANTRY -> t == SoldierType.KNIGHT ? 2 : t == SoldierType.MILITIA ? 3 : 2;
            case ARCHER -> 5;
            case HORSE_ARCHER -> 6;
            case GUNNER -> 5;
            default -> 4;
        };
    }

    /** ПКМ жезлом «в даль»: цель — враг, точка — выдвинуться, небо — следовать. */
    public static void pointOrder(ServerPlayer king, ItemStack baton) {
        int squad = selectedSquad(baton);
        Formation f = selectedFormation(baton);
        Vec3 eye = king.getEyePosition();
        Vec3 look = king.getViewVector(1f);
        Vec3 end = eye.add(look.scale(48));
        AABB box = king.getBoundingBox().expandTowards(look.scale(48)).inflate(1.0);
        EntityHitResult ehr = ProjectileUtil.getEntityHitResult(king, eye, end, box,
                e -> e instanceof LivingEntity && !(e instanceof Player) && !e.isSpectator() && e.isPickable()
                        && !(e instanceof SoldierEntity s && s.isOwnedBy(king)), 48 * 48);
        HitResult bhr = king.pick(64, 1f, false);

        boolean entityCloser = ehr != null && (bhr.getType() == HitResult.Type.MISS
                || ehr.getLocation().distanceToSqr(eye) < bhr.getLocation().distanceToSqr(eye));
        if (entityCloser && ehr.getEntity() instanceof LivingEntity le) {
            issue(king, squad, Order.ATTACK_TARGET, le.position(), king.getYRot(), f, le);
            king.serverLevel().sendParticles(ParticleTypes.ANGRY_VILLAGER, le.getX(), le.getY() + le.getBbHeight() + 0.3, le.getZ(), 3, 0.2, 0.1, 0.2, 0);
        } else if (bhr.getType() == HitResult.Type.BLOCK) {
            moveTo(king, baton, ((BlockHitResult) bhr).getBlockPos());
        } else {
            issue(king, squad, Order.FOLLOW, king.position(), king.getYRot(), f, null);
        }
    }

    public static void moveTo(ServerPlayer king, ItemStack baton, BlockPos clicked) {
        Vec3 anchor = Vec3.atBottomCenterOf(clicked.above());
        double dx = anchor.x - king.getX(), dz = anchor.z - king.getZ();
        float facing = (float) Math.toDegrees(Math.atan2(-dx, dz));
        issue(king, selectedSquad(baton), Order.MOVE, anchor, facing, selectedFormation(baton), null);
        king.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, anchor.x, anchor.y + 0.2, anchor.z, 8, 0.6, 0.1, 0.6, 0);
    }

    // ------------------------------------------------------------------ командный экран

    public static void sendInfo(ServerPlayer king, ItemStack baton) {
        int[] counts = new int[5];
        int[] orders = {-1, -1, -1, -1, -1};
        for (SoldierEntity s : squad(king, 0)) {
            counts[0]++;
            counts[s.getSquad()]++;
            orders[s.getSquad()] = s.getOrder().ordinal();
        }
        PacketDistributor.sendToPlayer(king, new ArmyInfoPayload(counts, orders, selectedSquad(baton), selectedFormation(baton).ordinal()));
    }

    public static void handle(ServerPlayer king, ArmyOrderPayload p) {
        ItemStack baton = king.getMainHandItem().getItem() instanceof CommanderBatonItem ? king.getMainHandItem()
                : king.getOffhandItem().getItem() instanceof CommanderBatonItem ? king.getOffhandItem() : ItemStack.EMPTY;
        if (baton.isEmpty()) return;
        int squad = Math.max(0, Math.min(4, p.squad()));
        Formation f = Formation.byId(p.formation());
        baton.set(KingdomModule.BATON_SQUAD.get(), squad);
        baton.set(KingdomModule.BATON_FORMATION.get(), f.ordinal());
        if (p.order() < 0) return;

        Order order = Order.byId(p.order());
        switch (order) {
            case FOLLOW -> issue(king, squad, Order.FOLLOW, king.position(), king.getYRot(), f, null);
            case HOLD -> issue(king, squad, Order.HOLD, king.position(), king.getYRot(), f, null);
            case CHARGE -> issue(king, squad, Order.CHARGE, king.position(), king.getYRot(), f, null);
            case RETREAT -> {
                City c = KingdomData.get(king.server).nearestOwned(king.getUUID(), king.blockPosition());
                if (c == null) {
                    Text.bad(king, "У вас нет города, куда можно отступить.");
                    return;
                }
                issue(king, squad, Order.RETREAT, Vec3.atBottomCenterOf(c.hall.above()).add(0, 0, 3), 0f, f, null);
            }
            case PATROL, GUARD -> {
                City c = KingdomData.get(king.server).nearestOwned(king.getUUID(), king.blockPosition());
                if (c == null) {
                    Text.bad(king, "Патруль и охрана возможны только для города.");
                    return;
                }
                for (SoldierEntity s : squad(king, squad)) s.setCityRadius(c.radius());
                issue(king, squad, order, Vec3.atBottomCenterOf(c.hall.above()).add(0, 0, 3), 0f, f, null);
            }
            case MOVE -> {
                HitResult bhr = king.pick(64, 1f, false);
                if (bhr instanceof BlockHitResult b && bhr.getType() == HitResult.Type.BLOCK) moveTo(king, baton, b.getBlockPos());
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ работа с отдельными бойцами

    public static void cycleSquad(ServerPlayer king, SoldierEntity s) {
        int next = s.getSquad() % 4 + 1;
        s.setSquad(next);
        Text.bar(king, s.getSoldierType().title + " переведён в отряд " + next, ChatFormatting.AQUA);
    }

    /** Вербовка жителя в ополчение (как в Mount & Blade). */
    public static void recruitVillager(ServerPlayer king, Villager v) {
        if (!RegnumConfig.KINGDOM_ENABLED.get()) return;
        if (v.isBaby()) {
            Text.bad(king, "Дети в ополчение не идут.");
            return;
        }
        if (v.getVillagerXp() > 0) {
            Text.bad(king, "Опытный мастер не бросит ремесло ради войны.");
            return;
        }
        KingdomData data = KingdomData.get(king.server);
        City c = data.nearestOwned(king.getUUID(), king.blockPosition());
        if (c != null && c.soldiers.size() >= c.armyCap(Skills.armyBonus(king))) {
            Text.bad(king, "Армия города «" + c.name + "» достигла предела.");
            return;
        }
        if (!king.getAbilities().instabuild && InvUtil.count(king, Items.EMERALD) < MILITIA_COST) {
            Text.bad(king, "Нужно " + MILITIA_COST + " изумруда, чтобы снарядить ополченца.");
            return;
        }
        InvUtil.take(king, Items.EMERALD, MILITIA_COST);
        ServerLevel level = king.serverLevel();
        SoldierEntity s = KingdomModule.SOLDIER.get().create(level);
        if (s == null) return;
        s.moveTo(v.getX(), v.getY(), v.getZ(), v.getYRot(), 0f);
        s.finalizeSpawn(level, level.getCurrentDifficultyAt(v.blockPosition()), MobSpawnType.CONVERSION, null);
        s.setup(SoldierType.MILITIA, king.getUUID(), c == null ? null : c.id, c == null ? 1 : c.recruitSquad);
        s.command(Order.FOLLOW, king.position(), king.getYRot(), Formation.LINE, 0, 1, null);
        v.discard();
        level.addFreshEntity(s);
        if (c != null) {
            c.soldiers.put(s.getUUID(), SoldierType.MILITIA.ordinal());
            data.setDirty();
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, s.getX(), s.getY() + 1, s.getZ(), 10, 0.3, 0.5, 0.3, 0);
        Text.good(king, "Житель вступил в ополчение и следует за вами.");
        Skills.addXp(king, Skill.LEADERSHIP, 4);
    }
}
