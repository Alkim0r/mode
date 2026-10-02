package com.alkimor.regnum.combat;

import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.dungeon.boss.BossRules;
import com.alkimor.regnum.kingdom.BanditEntity;
import com.alkimor.regnum.kingdom.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Умные враги: дерутся «как игроки».
 * <ul>
 *   <li>Ранги: обычный, бывалый, ветеран, элита (распределение зависит от пресета сложности и возраста мира).</li>
 *   <li>Дистанция: пока ваш удар заряжен — враг держит дистанцию и уходит вбок/назад; сразу после вашего замаха — рвётся вперёд.</li>
 *   <li>«Ударил — отошёл»: после удачного удара враг кружит вокруг цели.</li>
 *   <li>Прыжок с критом, уход от летящих стрел, расхождение по флангам.</li>
 *   <li>Парирование слабых ударов (против «закликивания»), отступление и лечение у разумных врагов, ярость у нежити.</li>
 * </ul>
 * Работает только для врагов рядом с игроками и с целью — дёшево по производительности.
 */
public final class Tactics {
    private Tactics() {}

    public static final String RANK_KEY = "regnum_rank";
    private static final ResourceLocation ELITE_HP = ResourceLocation.fromNamespaceAndPath("regnum", "rank_hp");
    private static final ResourceLocation ELITE_DMG = ResourceLocation.fromNamespaceAndPath("regnum", "rank_dmg");
    public static final String[] RANK_NAMES = {"обычный", "бывалый", "ветеран", "элита"};

    /** Состояние «мозга» (не сохраняется — восстанавливается само). */
    static final class Brain {
        int dodgeCd, parryCd, rushTicks, jumpCd, fleeTicks, lastArrowId = -1;
        long lastHit = -100;
        int strafeDir = 1;
        boolean healed, critNext;
    }

    private static final Map<Mob, Brain> BRAINS = new WeakHashMap<>();
    /** Сила последнего удара игрока (до сброса шкалы) — для парирования. */
    private static final Map<UUID, Float> LAST_STRENGTH = new HashMap<>();
    private static final Map<UUID, Long> HINT_COOLDOWN = new HashMap<>();

    // ------------------------------------------------------------------ кто умный

    public static boolean tactical(Entity e) {
        if (!(e instanceof Mob m) || BossRules.isBoss(e) || e instanceof SoldierEntity) return false;
        return m instanceof Zombie || m instanceof AbstractSkeleton || m instanceof AbstractIllager
                || m instanceof AbstractPiglin || m instanceof BanditEntity || m instanceof Witch;
    }

    static boolean ranged(Mob m) {
        if (m instanceof WitherSkeleton) return false;
        ItemStack main = m.getMainHandItem();
        return main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem
                || m instanceof Pillager || m instanceof Witch;
    }

    static boolean smart(Mob m) {
        return m instanceof AbstractIllager || m instanceof AbstractPiglin || m instanceof BanditEntity || m instanceof Witch;
    }

    public static int rank(Mob m) {
        return m.getPersistentData().getInt(RANK_KEY);
    }

    // ------------------------------------------------------------------ ранги при появлении

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !tactical(event.getEntity())) return;
        Mob m = (Mob) event.getEntity();
        if (m.getPersistentData().contains(RANK_KEY)) return;
        RandomSource r = m.getRandom();
        long day = event.getLevel().getDayTime() / 24000L;
        int[] w = switch (RegnumConfig.tier()) {
            case 0 -> new int[]{55, 32, 11, 2};
            case 2 -> new int[]{5, 30, 42, 23};
            default -> new int[]{22, 40, 28, 10};
        };
        // мир взрослеет: каждые 10 дней враги опытнее
        int shift = (int) Math.min(15, day / 10 * 3);
        w[0] = Math.max(0, w[0] - shift);
        w[3] += shift;
        int roll = r.nextInt(w[0] + w[1] + w[2] + w[3]), rank = 0;
        for (int acc = w[0]; roll >= acc && rank < 3; acc += w[++rank]) { }
        if (m instanceof BanditEntity b && b.getVariant() == BanditEntity.CAPTAIN) rank = 3;
        m.getPersistentData().putInt(RANK_KEY, rank);
        if (rank >= 2) {
            AttributeInstance hp = m.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.addOrReplacePermanentModifier(new AttributeModifier(ELITE_HP, rank == 3 ? 0.5 : 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                m.setHealth(m.getMaxHealth());
            }
            AttributeInstance dmg = m.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmg != null && rank == 3) dmg.addOrReplacePermanentModifier(new AttributeModifier(ELITE_DMG, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        // элита заметна по снаряжению: шлем и щит у пехоты
        if (rank == 3 && !(m instanceof BanditEntity)) {
            if (m.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && !(m instanceof AbstractPiglin)) {
                m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(r.nextBoolean() ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET));
                m.setDropChance(EquipmentSlot.HEAD, 0.02f);
            }
            if (!ranged(m) && m.getOffhandItem().isEmpty() && (m instanceof Zombie || m instanceof AbstractIllager)) {
                m.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                m.setDropChance(EquipmentSlot.OFFHAND, 0.02f);
            }
        }
    }

    // ------------------------------------------------------------------ сила удара игрока

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide()) LAST_STRENGTH.put(p.getUUID(), p.getAttackStrengthScale(0.5f));
    }

    // ------------------------------------------------------------------ тик

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity ent = event.getEntity();
        if (ent.level().isClientSide() || (ent.tickCount & 1) != 0 || !tactical(ent) || !RegnumConfig.SMART_MOBS.get()) return;
        Mob mob = (Mob) ent;
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive() || mob.isPassenger() || mob.isInWater()) return;
        int range = RegnumConfig.TACTICS_RANGE.get();
        if (mob.distanceToSqr(t) > (double) range * range) return;
        int rank = rank(mob);
        Brain b = BRAINS.computeIfAbsent(mob, k -> new Brain());
        if (b.dodgeCd > 0) b.dodgeCd -= 2;
        if (b.parryCd > 0) b.parryCd -= 2;
        if (b.jumpCd > 0) b.jumpCd -= 2;
        ServerLevel sl = (ServerLevel) mob.level();
        float hp = mob.getHealth() / mob.getMaxHealth();

        // отступление и лечение / ярость
        if (rank >= 2 && !b.healed && hp < 0.3f) {
            b.healed = true;
            if (smart(mob) && mob instanceof PathfinderMob pm) {
                b.fleeTicks = 50;
            } else {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1));
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0));
                sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getEyeY() + 0.4, mob.getZ(), 4, 0.3, 0.2, 0.3, 0);
            }
        }
        if (b.fleeTicks > 0) {
            b.fleeTicks -= 2;
            if (mob instanceof PathfinderMob pm && (b.fleeTicks % 10 == 0)) {
                Vec3 away = DefaultRandomPos.getPosAway(pm, 12, 5, t.position());
                if (away != null) pm.getNavigation().moveTo(away.x, away.y, away.z, 1.35);
            }
            if (b.fleeTicks <= 0) {
                mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 2));
                sl.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 1f, 1f);
                sl.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getEyeY() + 0.3, mob.getZ(), 3, 0.3, 0.2, 0.3, 0);
            }
            return;
        }
        if (rank == 0) return;

        dodgeArrows(sl, mob, b, rank);
        if (ranged(mob)) rangedTactics(sl, mob, t, b, rank);
        else meleeTactics(sl, mob, t, b, rank);
        spread(mob, t);
    }

    private static void meleeTactics(ServerLevel sl, Mob mob, LivingEntity t, Brain b, int rank) {
        double d = mob.distanceTo(t);
        Player p = t instanceof Player pl ? pl : null;
        float charge = p != null ? p.getAttackStrengthScale(0.5f) : 1f;
        // 1. Держать дистанцию, пока ваш удар заряжен
        if (p != null && b.dodgeCd <= 0 && d < 3.4 && charge > 0.92f && mob.onGround()) {
            float chance = rank == 1 ? 0.10f : rank == 2 ? 0.22f : 0.34f;
            if (mob.getRandom().nextFloat() < chance) {
                boolean back = mob.getRandom().nextFloat() < 0.45f;
                dodge(sl, mob, t, back);
                b.dodgeCd = 34 - rank * 6;
                return;
            }
        }
        // 2. Вы промахнулись или только что ударили — враг рвётся вперёд
        if (p != null && charge < 0.35f && d < 5.0 && d > 1.4) b.rushTicks = Math.max(b.rushTicks, 6 + rank * 2);
        if (b.rushTicks > 0) {
            b.rushTicks -= 2;
            if (mob.onGround()) push(mob, dirTo(mob, t).scale(0.11 + 0.02 * rank));
        }
        // 3. Ударил — отошёл: кружит вокруг цели
        long now = mob.level().getGameTime();
        if (now - b.lastHit < 18 && d < 4.5 && mob.onGround()) {
            Vec3 to = dirTo(mob, t);
            Vec3 side = new Vec3(-to.z, 0, to.x).scale(b.strafeDir * 0.075);
            push(mob, side.add(to.scale(-0.04)));
        }
        // 4. Прыжок с критом
        if (rank >= 2 && b.jumpCd <= 0 && d > 2.4 && d < 4.6 && mob.onGround() && mob.getRandom().nextFloat() < 0.18f) {
            Vec3 to = dirTo(mob, t);
            mob.setDeltaMovement(to.x * 0.42, 0.42, to.z * 0.42);
            mob.hasImpulse = true;
            b.critNext = true;
            b.jumpCd = 70 - rank * 10;
        }
        // 5. Щит: поднимает, когда вы натягиваете лук
        if (p != null && mob.getOffhandItem().is(Items.SHIELD) && p.isUsingItem() && p.getUseItem().getItem() instanceof BowItem
                && !mob.isUsingItem() && mob.getRandom().nextFloat() < 0.25f) {
            mob.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
        } else if (mob.isUsingItem() && mob.getUseItem().is(Items.SHIELD) && (p == null || !p.isUsingItem() || d < 2.5)) {
            mob.stopUsingItem();
        }
    }

    private static void rangedTactics(ServerLevel sl, Mob mob, LivingEntity t, Brain b, int rank) {
        double d = mob.distanceTo(t);
        // слишком близко — отскок назад
        if (d < 4.5 && b.dodgeCd <= 0 && mob.onGround()) {
            Vec3 away = dirTo(mob, t).scale(-1);
            if (safe(mob, away)) {
                mob.setDeltaMovement(away.x * 0.55, 0.32, away.z * 0.55);
                mob.hasImpulse = true;
                b.dodgeCd = 50 - rank * 8;
                return;
            }
        }
        // вы целитесь — уходит в сторону
        if (t instanceof Player p && p.isUsingItem() && (p.getUseItem().getItem() instanceof BowItem || p.getUseItem().getItem() instanceof CrossbowItem)
                && b.dodgeCd <= 0 && mob.onGround() && mob.getRandom().nextFloat() < 0.05f * rank) {
            dodge(sl, mob, t, false);
            b.dodgeCd = 40;
        }
    }

    /** Увернуться от летящей стрелы игрока. */
    private static void dodgeArrows(ServerLevel sl, Mob mob, Brain b, int rank) {
        if (rank < 2 || (mob.tickCount & 3) != 0 || !mob.onGround()) return;
        for (AbstractArrow a : sl.getEntitiesOfClass(AbstractArrow.class, mob.getBoundingBox().inflate(10),
                a -> a.getOwner() instanceof Player && a.getDeltaMovement().lengthSqr() > 0.25)) {
            if (a.getId() == b.lastArrowId) continue;
            Vec3 v = a.getDeltaMovement();
            Vec3 rel = mob.position().add(0, mob.getBbHeight() / 2, 0).subtract(a.position());
            double tt = rel.dot(v) / v.lengthSqr();
            if (tt <= 0 || tt > 12) continue;
            double miss = rel.subtract(v.scale(tt)).length();
            if (miss > 1.1) continue;
            b.lastArrowId = a.getId();
            if (mob.getRandom().nextFloat() > (rank == 2 ? 0.35f : 0.6f)) return;
            Vec3 side = new Vec3(-v.z, 0, v.x).normalize();
            if (mob.getRandom().nextBoolean()) side = side.scale(-1);
            if (!safe(mob, side)) side = side.scale(-1);
            if (!safe(mob, side)) return;
            mob.setDeltaMovement(side.x * 0.6, 0.25, side.z * 0.6);
            mob.hasImpulse = true;
            return;
        }
    }

    /** Не толпиться: союзники рядом расходятся по флангам. */
    private static void spread(Mob mob, LivingEntity t) {
        if ((mob.tickCount & 7) != 0) return;
        for (Mob o : mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(1.4),
                o -> o != mob && o.getTarget() == t && tactical(o))) {
            Vec3 to = dirTo(mob, t);
            Vec3 side = new Vec3(-to.z, 0, to.x);
            Vec3 rel = mob.position().subtract(o.position());
            double s = Math.signum(rel.dot(side));
            if (s == 0) s = mob.getId() % 2 == 0 ? 1 : -1;
            push(mob, side.scale(0.16 * s));
            BRAINS.computeIfAbsent(mob, k -> new Brain()).strafeDir = (int) s;
            break;
        }
    }

    private static void dodge(ServerLevel sl, Mob mob, LivingEntity t, boolean back) {
        Vec3 to = dirTo(mob, t);
        Vec3 dir = back ? to.scale(-1) : new Vec3(-to.z, 0, to.x).scale(mob.getRandom().nextBoolean() ? 1 : -1);
        if (!safe(mob, dir)) dir = dir.scale(-1);
        if (!safe(mob, dir)) return;
        mob.setDeltaMovement(dir.x * 0.55, 0.28, dir.z * 0.55);
        mob.hasImpulse = true;
        sl.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.1, mob.getZ(), 3, 0.2, 0.05, 0.2, 0.01);
    }

    private static Vec3 dirTo(Entity from, Entity to) {
        Vec3 d = to.position().subtract(from.position());
        Vec3 h = new Vec3(d.x, 0, d.z);
        return h.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : h.normalize();
    }

    private static void push(Mob mob, Vec3 v) {
        mob.setDeltaMovement(mob.getDeltaMovement().add(v.x, 0, v.z));
        mob.hasImpulse = true;
    }

    /** Безопасно ли сместиться в направлении dir: под ногами есть опора, нет лавы и обрыва. */
    static boolean safe(Mob mob, Vec3 dir) {
        BlockPos p = BlockPos.containing(mob.position().add(dir.scale(1.8)));
        var lvl = mob.level();
        if (!lvl.getBlockState(p).getCollisionShape(lvl, p).isEmpty() && !lvl.getBlockState(p.above()).getCollisionShape(lvl, p.above()).isEmpty()) return false;
        for (int i = 1; i <= 3; i++) {
            BlockState s = lvl.getBlockState(p.below(i));
            if (s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) || s.is(net.minecraft.world.level.block.Blocks.FIRE)) return false;
            if (!s.getCollisionShape(lvl, p.below(i)).isEmpty()) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ урон: парирование и криты

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        Entity src = event.getSource().getEntity();

        // крит после прыжка
        if (src instanceof Mob attacker && tactical(attacker) && event.getSource().getDirectEntity() == attacker) {
            Brain b = BRAINS.get(attacker);
            if (b != null) {
                b.lastHit = attacker.level().getGameTime();
                if (attacker.getRandom().nextFloat() < 0.5f) b.strafeDir = -b.strafeDir;
                if (b.critNext) {
                    b.critNext = false;
                    event.setAmount(event.getAmount() * 1.4f);
                    ((ServerLevel) victim.level()).sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }

        // парирование
        if (!RegnumConfig.MOB_PARRY.get() || !(victim instanceof Mob mob) || !tactical(mob) || !(src instanceof ServerPlayer p)
                || event.getSource().getDirectEntity() != p) return;
        int rank = rank(mob);
        if (rank == 0) return;
        Brain b = BRAINS.computeIfAbsent(mob, k -> new Brain());
        if (b.parryCd > 0) return;
        // враг должен смотреть на атакующего
        Vec3 look = mob.getViewVector(1f);
        Vec3 to = p.position().subtract(mob.position()).normalize();
        if (look.x * to.x + look.z * to.z < 0.35) return;
        float strength = LAST_STRENGTH.getOrDefault(p.getUUID(), 1f);
        boolean weak = strength < 0.7f;
        float chance = switch (rank) {
            case 1 -> weak ? 0.30f : 0f;
            case 2 -> weak ? 0.45f : 0.10f;
            default -> weak ? 0.60f : 0.18f;
        };
        if (mob.getOffhandItem().is(Items.SHIELD)) chance += 0.15f;
        if (mob.getRandom().nextFloat() >= chance) return;
        event.setCanceled(true);
        b.parryCd = 30;
        b.rushTicks = 8;
        ServerLevel sl = (ServerLevel) mob.level();
        sl.playSound(null, mob.blockPosition(), mob.getOffhandItem().is(Items.SHIELD) ? SoundEvents.SHIELD_BLOCK : SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 0.8f, 1.7f);
        sl.sendParticles(ParticleTypes.ENCHANTED_HIT, mob.getX(), mob.getY(0.7), mob.getZ(), 8, 0.25, 0.25, 0.25, 0.15);
        p.knockback(0.35, mob.getX() - p.getX(), mob.getZ() - p.getZ());
        p.hurtMarked = true;
        long now = sl.getGameTime();
        if (now - HINT_COOLDOWN.getOrDefault(p.getUUID(), -10000L) > 20 * 60) {
            HINT_COOLDOWN.put(p.getUUID(), now);
            p.displayClientMessage(Component.literal("Удар парирован (" + RANK_NAMES[rank] + ")! Слабые удары легко отбить — бейте, когда оружие заряжено.")
                    .withStyle(net.minecraft.ChatFormatting.GOLD), true);
        } else {
            p.displayClientMessage(Component.literal("Парировано!").withStyle(net.minecraft.ChatFormatting.GOLD), true);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        // если враг получил урон — иногда меняет сторону обхода, чтобы не быть предсказуемым
        if (event.getEntity() instanceof Mob m && tactical(m)) {
            Brain b = BRAINS.get(m);
            if (b != null && m.getRandom().nextFloat() < 0.3f) b.strafeDir = -b.strafeDir;
        }
    }
}
