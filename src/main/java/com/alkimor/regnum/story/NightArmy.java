package com.alkimor.regnum.story;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.City;
import com.alkimor.regnum.kingdom.KingdomData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Армия ночи — большое событие поздней стадии. После второй арки у героя есть 10 игровых дней: знаки и слухи, потом
 * в сумерки к его городу приходят три волны «ночных». Обычное оружие почти не берёт их (25% урона), огонь — вдвое сильнее.
 * Победа даёт славу, казну и флаг «night_defeated». Это прототип события: дикий огонь как ремесло — отдельная работа.
 */
public final class NightArmy {
    private NightArmy() {}

    public static final String TAG = "regnum_night";
    private static final String KEY = "regnum_night";
    public static final int WARN_DAYS = 10, WAVES = 3;
    private static final int[] SIGNS = {10, 7, 3, 1};
    private static final String[] SIGN_TEXT = {
            "Путники рассказывают: над северными холмами горит бледная комета.",
            "Скот боится выходить в поле, у колодцев белый иней — а на дворе осень.",
            "С севера тянет мертвенным холодом. Дозорные видят огни, которые не греют.",
            "Они идут. Сегодня на закате. Запритесь за стенами и готовьте огонь!"};

    /** Состояние: stage 0 нет, 1 предупреждение, 2 бой, 3 побеждена. */
    private static CompoundTag st(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    private static long today(ServerLevel sl) {
        return sl.getDayTime() / 24000L;
    }

    public static void begin(ServerPlayer p, int days) {
        CompoundTag s = st(p);
        s.putInt("stage", 1);
        s.putLong("attackDay", today(p.serverLevel()) + days);
        s.putInt("signSent", 0);
        s.putInt("wave", 0);
        p.sendSystemMessage(Text.of("☾ Печать названа — и Безымянный ответил. Через " + days + " дней придёт армия ночи. Её не берёт обычное оружие: ищите огонь.", ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int tc = server.getTickCount();
        if (tc % 100 != 61) return;
        ServerLevel ow = server.overworld();
        for (ServerPlayer p : ow.players()) {
            CompoundTag s = st(p);
            int stage = s.getInt("stage");
            if (stage == 0) {
                if (Quests.flag(p, "arc2_done") && !Quests.flag(p, "night_defeated")) begin(p, WARN_DAYS);
                continue;
            }
            if (stage == 3) continue;
            long day = today(ow);
            long attack = s.getLong("attackDay");
            if (stage == 1) {
                long left = attack - day;
                for (int i = s.getInt("signSent"); i < SIGNS.length; i++) {
                    if (left <= SIGNS[i]) {
                        p.sendSystemMessage(Text.of("☾ " + SIGN_TEXT[i], ChatFormatting.DARK_PURPLE));
                        s.putInt("signSent", i + 1);
                    }
                }
                if (day >= attack && ow.getDayTime() % 24000L >= 13000L) {
                    City c = home(server, p);
                    if (c == null || !ow.isLoaded(c.hall)) continue;
                    s.putInt("stage", 2);
                    s.putInt("wave", 0);
                    s.putLong("nextWave", ow.getGameTime());
                    p.sendSystemMessage(Text.of("☾ Армия ночи у ворот «" + c.name + "»!", ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                }
            } else if (stage == 2) {
                fightTick(ow, p, s);
            }
        }
    }

    private static City home(MinecraftServer server, ServerPlayer p) {
        City best = null;
        for (City c : KingdomData.get(server).ownedBy(p.getUUID())) if (best == null || c.level > best.level) best = c;
        return best;
    }

    private static List<Monster> alive(ServerLevel ow, ServerPlayer p) {
        List<Monster> out = new ArrayList<>();
        City c = home(ow.getServer(), p);
        if (c == null) return out;
        int r = c.radius() + 160;
        for (Monster m : ow.getEntitiesOfClass(Monster.class, new net.minecraft.world.phys.AABB(c.hall).inflate(r, 128, r), x -> x.getTags().contains(TAG) && x.isAlive())) out.add(m);
        return out;
    }

    private static void fightTick(ServerLevel ow, ServerPlayer p, CompoundTag s) {
        City c = home(ow.getServer(), p);
        if (c == null) return;
        int wave = s.getInt("wave");
        long now = ow.getGameTime();
        List<Monster> live = alive(ow, p);
        necroTick(ow, p, live);
        if (wave < WAVES && now >= s.getLong("nextWave") && live.size() < 6 + wave * 6) {
            spawnWave(ow, c, wave + 1);
            s.putInt("wave", wave + 1);
            s.putLong("nextWave", now + 1200);
            p.sendSystemMessage(Text.of("☾ Волна " + (wave + 1) + " из " + WAVES + "!", ChatFormatting.RED));
        } else if (wave >= WAVES && live.isEmpty()) {
            win(ow, p, c, s);
        } else if (ow.getDayTime() % 24000L < 1000L && wave >= WAVES) {
            // рассвет: остатки рассыпаются, победа засчитана
            for (Monster m : live) m.discard();
            win(ow, p, c, s);
        }
    }

    /** Некромант (идея Skeleton Rising): раз в 8 секунд поднимает мёртвых; погиб — поднятые рассыпаются. */
    private static void necroTick(ServerLevel ow, ServerPlayer p, List<Monster> live) {
        if (ow.getGameTime() % 160 != 0) return;
        for (Monster m : live) {
            if (!m.getTags().contains("regnum_necro") || live.size() >= 30) continue;
            for (int i = 0; i < 3; i++) {
                Monster z = (Monster) (i == 2 ? EntityType.SKELETON.create(ow) : EntityType.ZOMBIE.create(ow));
                if (z == null) continue;
                double a = ow.random.nextDouble() * Math.PI * 2;
                z.moveTo(m.getX() + Math.cos(a) * 2.5, m.getY(), m.getZ() + Math.sin(a) * 2.5, ow.random.nextFloat() * 360f, 0f);
                z.finalizeSpawn(ow, ow.getCurrentDifficultyAt(z.blockPosition()), MobSpawnType.EVENT, null);
                z.addTag(TAG); z.addTag("regnum_raised");
                z.setPersistenceRequired();
                z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                z.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64);
                ow.addFreshEntity(z);
                ow.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, z.getX(), z.getY() + 1, z.getZ(), 10, 0.3, 0.6, 0.3, 0.03);
            }
            p.displayClientMessage(Text.of("Некромант поднимает павших! Убейте его — поднятые рассыплются.", ChatFormatting.DARK_PURPLE), true);
        }
    }

    private static void win(ServerLevel ow, ServerPlayer p, City c, CompoundTag s) {
        s.putInt("stage", 3);
        c.glory += 50;
        c.treasury += 500;
        KingdomData.get(ow.getServer()).setDirty();
        Quests.setFlag(p, "night_defeated");
        p.sendSystemMessage(Text.of("☀ Армия ночи отступила с рассветом. Слава «" + c.name + "» +50, в казне +500.", ChatFormatting.GOLD, ChatFormatting.BOLD));
        com.alkimor.regnum.survival.Skills.chronicle(p, "Отбита армия ночи у «" + c.name + "»");
    }

    private static void spawnWave(ServerLevel ow, City c, int wave) {
        int n = 10 + 4 * wave;
        int dist = c.radius() + 36;
        for (int i = 0; i < n; i++) {
            double a = ow.random.nextDouble() * Math.PI * 2;
            int x = c.hall.getX() + (int) (Math.cos(a) * dist), z = c.hall.getZ() + (int) (Math.sin(a) * dist);
            if (!ow.hasChunkAt(new BlockPos(x, 64, z))) continue;
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            boolean breaker = wave >= 2 && i == 0;
            boolean necro = wave >= 2 && i == 1;
            Monster m = (Monster) (breaker ? EntityType.RAVAGER.create(ow) : necro ? EntityType.WITHER_SKELETON.create(ow) : i % 5 == 4 ? EntityType.STRAY.create(ow) : EntityType.ZOMBIE.create(ow));
            if (m == null) continue;
            m.moveTo(x + 0.5, y, z + 0.5, ow.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(ow, ow.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
            m.addTag(TAG);
            m.setPersistenceRequired();
            if (necro) m.addTag("regnum_necro");
            m.setCustomName(Text.of(breaker ? "Ломающий строй" : necro ? "Некромант" : "Ночной", ChatFormatting.DARK_PURPLE));
            m.setCustomNameVisible(false);
            if (necro) m.setCustomNameVisible(true);
            if (!breaker && !necro) {
                m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)); // не горят на солнце
                m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(i % 3 == 0 ? Items.IRON_SWORD : Items.STONE_AXE));
            }
            var hp = m.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) { hp.setBaseValue(breaker ? 160 : necro ? 90 : 24 + 4 * wave); m.setHealth(m.getMaxHealth()); }
            m.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64);
            ow.addFreshEntity(m);
            ow.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, m.getX(), m.getY() + 1, m.getZ(), 12, 0.4, 0.8, 0.4, 0.02);
        }
    }

    /** Обычное оружие берёт ночных на 25%; огонь — вдвое сильнее. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        if (!v.getTags().contains(TAG) || v.getTags().contains("regnum_necro")) return; // некромант — слабое место
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (e.getSource().is(DamageTypeTags.IS_FIRE)) e.setAmount(e.getAmount() * 2f);
        else e.setAmount(e.getAmount() * 0.25f);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (!v.getTags().contains("regnum_necro") || !(v.level() instanceof ServerLevel ow)) return;
        for (Monster m : ow.getEntitiesOfClass(Monster.class, v.getBoundingBox().inflate(120), x -> x.getTags().contains("regnum_raised") && x.isAlive())) {
            m.kill();
            ow.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, m.getX(), m.getY() + 1, m.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("night")
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CompoundTag s = st(p);
                    int stage = s.getInt("stage");
                    String t = switch (stage) {
                        case 1 -> "Предупреждение: армия ночи придёт через " + Math.max(0, s.getLong("attackDay") - today(p.serverLevel())) + " дн.";
                        case 2 -> "Бой: волна " + s.getInt("wave") + " из " + WAVES + ", ночных в окрестности: " + alive(p.serverLevel(), p).size();
                        case 3 -> "Армия ночи побеждена.";
                        default -> "Армия ночи пока спит. Её будит завершение второй арки сюжета.";
                    };
                    Text.gold(p, t);
                    return 1;
                }))
                .then(Commands.literal("start").requires(src -> src.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    begin(p, 0);
                    return 1;
                }))
                .then(Commands.literal("reset").requires(src -> src.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    p.getPersistentData().remove(KEY);
                    return 1;
                }))));
    }
}
