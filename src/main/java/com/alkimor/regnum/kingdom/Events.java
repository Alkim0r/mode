package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.alkimor.regnum.survival.kit.WildfireFlaskEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Мировые события.
 * <ul>
 * <li><b>Армия ночи</b>: когда королевство достаточно сильно, на севере пробуждается армия мёртвых. До её прихода 30 игровых дней
 * (знамения, разведчики). Сталь почти не берёт Ходоков Ночи — только огонь, лучше всего «Дикий огонь».</li>
 * <li><b>Беженцы</b>: малое событие. Люди просятся в город; среди них могут быть шпионы или больные.
 * Проверка разведкой решает, кого впускать.</li>
 * </ul>
 */
public final class Events {
    private Events() {}

    public static final String NIGHT_TAG = "regnum_nightwalker";
    public static final String REFUGEE_TAG = "regnum_refugee";
    public static final int NIGHT_PREP_DAYS = 30, NIGHT_WAVES = 5;

    // ------------------------------------------------------------------ Армия ночи

    /** Урон по Ходокам: обычное оружие почти бессильно, огонь работает вдвое сильнее. */
    public static float nightDamage(LivingEntity victim, DamageSource src, float amount) {
        if (!victim.getTags().contains(NIGHT_TAG)) return amount;
        if (src.is(DamageTypeTags.IS_FIRE)) return amount * 2f;
        return Math.max(0.5f, amount * 0.12f);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        event.setAmount(nightDamage(event.getEntity(), event.getSource(), event.getAmount()));
    }

    /** Горение дикого огня и поведение Ходоков. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity e) || e.level().isClientSide || e.tickCount % 10 != 0) return;
        long until = e.getPersistentData().getLong(WildfireFlaskEntity.BURN_KEY);
        if (until > e.level().getGameTime()) {
            e.hurt(e.level().damageSources().inFire(), 3f);
            if (e.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, e.getX(), e.getY() + 1, e.getZ(), 3, 0.2, 0.4, 0.2, 0.01);
        }
        if (!(e instanceof Mob m) || !e.getTags().contains(NIGHT_TAG)) return;
        if (m.getRemainingFireTicks() > 0 && until <= e.level().getGameTime() && !e.isOnFire()) m.setRemainingFireTicks(0);
        if (m.tickCount % 20 == 0 && m.getTarget() == null) {
            SoldierEntity near = null;
            double bd = 26 * 26;
            for (SoldierEntity s : m.level().getEntitiesOfClass(SoldierEntity.class, m.getBoundingBox().inflate(26), s -> s.realmId() == null && s.isAlive())) {
                double d = s.distanceToSqr(m);
                if (d < bd) {
                    bd = d;
                    near = s;
                }
            }
            if (near != null) m.setTarget(near);
        }
        if (m.tickCount % 60 == 0 && m.getTarget() == null && m.level() instanceof ServerLevel sl) {
            KingdomData d = KingdomData.get(sl.getServer());
            City c = d.byId(d.nightCity);
            if (c != null) m.getNavigation().moveTo(c.hall.getX() + 0.5, c.hall.getY(), c.hall.getZ() + 0.5, 1.0);
        }
    }

    public static Mob spawnWalker(ServerLevel ow, BlockPos pos, boolean archer) {
        Mob m;
        if (archer) {
            Skeleton s = EntityType.SKELETON.create(ow);
            if (s == null) return null;
            s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            s.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            m = s;
        } else {
            Zombie z = EntityType.ZOMBIE.create(ow);
            if (z == null) return null;
            z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ow.random.nextInt(3) == 0 ? Items.IRON_SWORD : Items.STONE_SWORD));
            m = z;
        }
        m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ow.random.nextFloat() * 360f, 0f);
        m.addTag(NIGHT_TAG);
        m.setPersistenceRequired();
        m.setCustomName(Component.literal(archer ? "Ночной лучник" : "Ходок Ночи"));
        m.setCustomNameVisible(false);
        var hp = m.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(archer ? 30 : 50);
        m.setHealth(m.getMaxHealth());
        var atk = m.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null && !archer) atk.setBaseValue(7);
        var kb = m.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(0.6);
        for (EquipmentSlot s : EquipmentSlot.values()) m.setDropChance(s, 0f);
        m.finalizeSpawn(ow, ow.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        ow.addFreshEntity(m);
        return m;
    }

    private static int countWalkers(ServerLevel ow) {
        int n = 0;
        for (var e : ow.getAllEntities()) if (e instanceof LivingEntity le && le.isAlive() && le.getTags().contains(NIGHT_TAG)) n++;
        return n;
    }

    private static boolean ready(KingdomData data) {
        int top = 0, soldiers = 0;
        for (City c : data.all()) {
            top = Math.max(top, c.level);
            soldiers += c.soldiers.size();
        }
        return top >= 4 && soldiers >= 20;
    }

    private static City owner(KingdomData data) {
        City best = null;
        for (City c : data.all()) if (best == null || c.level > best.level) best = c;
        return best;
    }

    private static void broadcast(MinecraftServer server, String msg, ChatFormatting f) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(Text.of(msg, f, ChatFormatting.BOLD));
    }

    public static void startNight(ServerLevel ow, KingdomData data, City c, long day) {
        data.nightStage = 1;
        data.nightArrival = day + NIGHT_PREP_DAYS;
        data.nightWave = 0;
        data.nightCity = c.id;
        data.setDirty();
        broadcast(ow.getServer(), "❄ С севера пришла страшная весть: пробудились мёртвые. Армия Ночи будет у вас через " + NIGHT_PREP_DAYS
                + " дней. Сталь их почти не берёт — ищите огонь, который не гаснет (Дикий огонь: склянка, порох, пламенный порошок и гнилой корень из топей).", ChatFormatting.AQUA);
    }

    private static void nightTick(ServerLevel ow, KingdomData data, long day, long tod, int tc) {
        if (data.nightStage == 0) {
            if (tc % 1200 == 0 && day >= 8 && ready(data)) {
                City c = owner(data);
                if (c != null && ow.getServer().getPlayerList().getPlayerCount() > 0) startNight(ow, data, c, day);
            }
            return;
        }
        City c = data.byId(data.nightCity);
        if (c == null) {
            data.nightStage = 0;
            data.setDirty();
            return;
        }
        if (data.nightStage == 1) {
            long left = data.nightArrival - day;
            if (tc % 600 == 0) {
                if (left == 20 || left == 10 || left == 5 || left == 1) {
                    String[] omens = {"Птицы улетают на юг, а в северных холмах не горят костры.", "Дозорные видят на горизонте синий свет.",
                            "Выпал странный иней. Лошади не идут на север.", "Ночью слышен далёкий звон. Завтра они будут здесь."};
                    int i = left == 20 ? 0 : left == 10 ? 1 : left == 5 ? 2 : 3;
                    broadcast(ow.getServer(), "❄ До прихода Армии Ночи " + left + " дн. " + omens[i], ChatFormatting.AQUA);
                }
            }
            // последние дни — разведчики мёртвых
            if (left <= 10 && left > 0 && tod >= 14000 && tod < 14100 && tc % 100 == 0 && ow.isLoaded(c.hall)) {
                for (int i = 0; i < 3; i++) spawnRing(ow, c, i % 2 == 0);
            }
            if (day >= data.nightArrival && tod >= 13000 && tod < 23000) {
                data.nightStage = 2;
                data.nightWave = 0;
                data.setDirty();
                broadcast(ow.getServer(), "❄ Армия Ночи пришла! Ходоки идут с севера к городу «" + c.name + "».", ChatFormatting.DARK_AQUA);
            }
            return;
        }
        if (data.nightStage == 2) {
            if (!ow.isLoaded(c.hall)) return;
            if (data.nightWave < NIGHT_WAVES && tc % 1800 == 0) {
                data.nightWave++;
                int n = 8 + 4 * data.nightWave;
                for (int i = 0; i < n; i++) spawnRing(ow, c, data.nightWave >= 2 && i % 4 == 0);
                data.setDirty();
                broadcast(ow.getServer(), "❄ Волна " + data.nightWave + " из " + NIGHT_WAVES + ": " + n + " Ходоков.", ChatFormatting.AQUA);
            }
            if (data.nightWave >= NIGHT_WAVES && tc % 100 == 0 && countWalkers(ow) == 0) {
                data.nightStage = 3;
                c.glory += 100;
                c.treasury += 200;
                data.setDirty();
                broadcast(ow.getServer(), "☼ Армия Ночи сокрушена. Рассвет над «" + c.name + "»: слава +100, из трофеев в казну +200.", ChatFormatting.GOLD);
                ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
                if (owner != null) Skills.chronicle(owner, "Отбита Армия Ночи у стен «" + c.name + "»");
            }
        }
    }

    private static void spawnRing(ServerLevel ow, City c, boolean archer) {
        double a = -Math.PI / 2 + (ow.random.nextDouble() - 0.5) * 1.4; // северная сторона
        double d = c.radius() + 30 + ow.random.nextInt(14);
        int x = c.hall.getX() + (int) (Math.cos(a) * d), z = c.hall.getZ() + (int) (Math.sin(a) * d);
        if (!ow.isLoaded(new BlockPos(x, 64, z))) return;
        int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        spawnWalker(ow, new BlockPos(x, y, z), archer);
    }

    // ------------------------------------------------------------------ беженцы

    private static final class Pending {
        final UUID city;
        final int kind; // 0 честные, 1 шпионы, 2 больные
        final List<UUID> ids = new ArrayList<>();
        boolean checked = false, revealed = false;
        long expires;

        Pending(UUID city, int kind, long expires) {
            this.city = city;
            this.kind = kind;
            this.expires = expires;
        }
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private static void refugeeTick(ServerLevel ow, KingdomData data, long now) {
        for (City c : data.all()) {
            ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
            Pending p = PENDING.get(c.id);
            if (p != null) {
                if (now > p.expires) finishRefugees(ow, c, p, "Беженцы не дождались ответа и ушли дальше.");
                else steer(ow, c, p);
                continue;
            }
            if (owner == null || !ow.isLoaded(c.hall) || !owner.blockPosition().closerThan(c.hall, c.radius() + 96)) continue;
            Long next = NEXT.get(c.id);
            if (next == null) {
                NEXT.put(c.id, now + 24000L * 2 + ow.random.nextInt(24000 * 2));
                continue;
            }
            if (now < next) continue;
            NEXT.put(c.id, now + 24000L * 4 + ow.random.nextInt(24000 * 3));
            spawnRefugees(ow, c, owner, now);
        }
    }

    public static void spawnRefugees(ServerLevel ow, City c, ServerPlayer owner, long now) {
        float r = ow.random.nextFloat();
        int kind = r < 0.65f ? 0 : r < 0.85f ? 1 : 2;
        Pending p = new Pending(c.id, kind, now + 20 * 60 * 5);
        double a = ow.random.nextDouble() * Math.PI * 2;
        double d = c.radius() + 18;
        int bx = c.hall.getX() + (int) (Math.cos(a) * d), bz = c.hall.getZ() + (int) (Math.sin(a) * d);
        int n = 3 + ow.random.nextInt(3);
        for (int i = 0; i < n; i++) {
            Villager v = EntityType.VILLAGER.create(ow);
            if (v == null) continue;
            int x = bx + i, z = bz + (i % 2);
            v.moveTo(x + 0.5, ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5);
            v.addTag(REFUGEE_TAG);
            v.setCustomName(Component.literal("Беженец"));
            v.setPersistenceRequired();
            ow.addFreshEntity(v);
            p.ids.add(v.getUUID());
        }
        if (p.ids.isEmpty()) return;
        PENDING.put(c.id, p);
        owner.sendSystemMessage(Text.of("⚑ К городу «" + c.name + "» подходят беженцы: " + p.ids.size() + " человек. Просят убежища.", ChatFormatting.GOLD));
        var line = Component.empty();
        line.append(Text.button("Принять", "/regnum event refugees accept", "Впустить в город"));
        line.append(Component.literal("  "));
        line.append(Text.button("Проверить", "/regnum event refugees check", "Разведка и дозор присмотрятся к ним (5 из казны)"));
        line.append(Component.literal("  "));
        line.append(Text.button("Отказать", "/regnum event refugees refuse", "Прогнать"));
        owner.sendSystemMessage(line);
    }

    private static void steer(ServerLevel ow, City c, Pending p) {
        for (UUID id : p.ids) {
            if (ow.getEntity(id) instanceof Villager v && v.isAlive() && v.tickCount % 40 == 0 && v.distanceToSqr(c.hall.getX(), c.hall.getY(), c.hall.getZ()) > 25 * 25) {
                // ждут у границы: стоят, пока игрок решает
                v.getNavigation().stop();
            }
        }
    }

    private static void finishRefugees(ServerLevel ow, City c, Pending p, String msg) {
        PENDING.remove(c.id);
        for (UUID id : p.ids) {
            if (ow.getEntity(id) instanceof Villager v) {
                v.removeTag(REFUGEE_TAG);
                v.setCustomName(null);
                if (msg.startsWith("Беженцы не")) v.discard();
            }
        }
        ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
        if (owner != null) Text.info(owner, msg);
    }

    private static void refugeeAction(ServerPlayer p, String act) {
        ServerLevel ow = p.serverLevel();
        KingdomData data = KingdomData.get(p.server);
        City c = null;
        Pending pend = null;
        for (City x : data.ownedBy(p.getUUID())) {
            if (PENDING.containsKey(x.id)) {
                c = x;
                pend = PENDING.get(x.id);
            }
        }
        if (c == null) {
            Text.info(p, "Сейчас никто не просится в город.");
            return;
        }
        switch (act) {
            case "check" -> {
                if (pend.checked) {
                    Text.info(p, "Вы уже присматривались к ним.");
                    return;
                }
                if (c.treasury < 5) {
                    Text.bad(p, "На проверку нужно 5 в казне.");
                    return;
                }
                c.treasury -= 5;
                pend.checked = true;
                double chance = 0.35 + (Skills.level(p, Skill.SCOUTING) + Skills.level(p, Skill.ROGUERY)) / 200.0 + (c.count(BuildingType.WATCHTOWER) > 0 ? 0.15 : 0);
                boolean ok = ow.random.nextDouble() < Math.min(0.95, chance);
                Skills.addXp(p, Skill.SCOUTING, 6);
                if (ok && pend.kind == 1) {
                    pend.revealed = true;
                    Text.gold(p, "Дозорные раскусили их: среди беженцев шпионы! Впускать их не стоит.");
                } else if (ok && pend.kind == 2) {
                    pend.revealed = true;
                    Text.gold(p, "Дозорные заметили: люди больны, это зараза. Лучше не впускать.");
                } else if (ok) {
                    pend.revealed = true;
                    Text.good(p, "Дозорные уверены: это честные люди, бежавшие от беды.");
                } else {
                    Text.info(p, "Ничего подозрительного дозорные не заметили. Но уверенности нет.");
                }
                data.setDirty();
            }
            case "refuse" -> {
                if (pend.kind == 0 && pend.revealed) c.glory = Math.max(0, c.glory - 2);
                finishRefugees(ow, c, pend, "Беженцам отказали. Они ушли.");
                for (UUID id : pend.ids) if (ow.getEntity(id) instanceof Villager v) v.discard();
            }
            case "accept" -> {
                for (UUID id : pend.ids) {
                    if (ow.getEntity(id) instanceof Villager v) v.getNavigation().moveTo(c.hall.getX() + 0.5, c.hall.getY(), c.hall.getZ() + 0.5, 0.7);
                }
                if (pend.kind == 0) {
                    c.glory += 3;
                    Text.good(p, "Беженцы поселились в «" + c.name + "». Население растёт, слава +3.");
                    finishRefugees(ow, c, pend, "Люди обживаются.");
                } else if (pend.kind == 1) {
                    long loss = Math.max(10, c.treasury / 4);
                    c.treasury = (int) Math.max(0, c.treasury - loss);
                    Text.bad(p, "Среди них были шпионы: они ночью вскрыли казну («" + c.name + "»: −" + loss + ") и растворились в темноте.");
                    finishRefugees(ow, c, pend, "Шпионы скрылись.");
                    for (UUID id : pend.ids) if (ow.getEntity(id) instanceof Villager v && ow.random.nextInt(3) == 0) v.discard();
                } else {
                    for (ServerPlayer sp : ow.players()) {
                        if (sp.blockPosition().closerThan(c.hall, c.radius())) {
                            sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.alkimor.regnum.survival.SurvivalModule.INFECTION, 20 * 120, 0));
                        }
                    }
                    Text.bad(p, "Беженцы были больны. В городе вспыхнула зараза: нужны отвары и лекарь.");
                    finishRefugees(ow, c, pend, "Больные размещены в городе.");
                }
                data.setDirty();
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ тик и команды

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tc = server.getTickCount();
        if (tc % 20 != 0) return;
        ServerLevel ow = server.overworld();
        KingdomData data = KingdomData.get(server);
        long time = ow.getDayTime();
        nightTick(ow, data, time / 24000L, time % 24000L, tc);
        if (tc % 100 == 0) refugeeTick(ow, data, ow.getGameTime());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("event")
                .then(Commands.literal("refugees").then(Commands.argument("act", StringArgumentType.word()).executes(ctx -> {
                    refugeeAction(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "act"));
                    return 1;
                })))
                .then(Commands.literal("night").requires(s -> s.hasPermission(2)).then(Commands.argument("act", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ServerLevel ow = p.server.overworld();
                    KingdomData data = KingdomData.get(p.server);
                    City c = data.nearestOwned(p.getUUID(), p.blockPosition());
                    String act = StringArgumentType.getString(ctx, "act");
                    long day = ow.getDayTime() / 24000L;
                    switch (act) {
                        case "start" -> {
                            if (c == null) return 0;
                            startNight(ow, data, c, day);
                        }
                        case "now" -> {
                            if (c == null) return 0;
                            data.nightCity = c.id;
                            data.nightStage = 2;
                            data.nightWave = 0;
                            data.setDirty();
                        }
                        case "reset" -> {
                            data.nightStage = 0;
                            data.setDirty();
                        }
                        case "refugees" -> {
                            if (c != null) spawnRefugees(ow, c, p, ow.getGameTime());
                        }
                        default -> Text.info(p, "start | now | reset | refugees");
                    }
                    return 1;
                })))));
    }

    public static AABB box(BlockPos p, int r) {
        return new AABB(p).inflate(r);
    }
}
