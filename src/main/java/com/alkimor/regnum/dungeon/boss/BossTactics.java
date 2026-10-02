package com.alkimor.regnum.dungeon.boss;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.Commissions;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.kingdom.SoldierType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Тактика против боссов. Босс получает полный урон, только когда вокруг него сомкнут строй: герои (до 4), щитовики
 * держат фронт, стрелки и орудия бьют издали, копейщики/двуручники гасят натиск, конница заходит на фланги.
 * Каждый недостающий элемент режет урон; у «военных» боссов (regnum_warboss) пол — 20%, у обычных — 65%.
 * Идеальный строй даёт бонус ×1.25. Военачальники делят авторитет за победу.
 */
public final class BossTactics {
    private BossTactics() {}

    public static final String WARBOSS = "regnum_warboss";
    public static final int NEED_UNITS = 6, NEED_PLAYERS = 4;
    private static final ResourceLocation WAR_HP = ResourceLocation.fromNamespaceAndPath("regnum", "warboss_hp");

    public record Formation(int players, int shields, int ranged, int pikes, int cavalry, float coord) {
        public String line() {
            return "Герои " + players + "/" + NEED_PLAYERS + " · щиты " + shields + "/" + NEED_UNITS + " · стрелки " + ranged + "/" + NEED_UNITS
                    + " · копья " + pikes + "/" + NEED_UNITS + " · конница " + cavalry + "/" + NEED_UNITS;
        }
    }

    private record Cache(long tick, Formation f) {}

    private static final Map<UUID, Cache> CACHE = new HashMap<>();

    public static boolean warboss(Entity boss) {
        return boss.getPersistentData().getBoolean(WARBOSS);
    }

    /** Состав сил вокруг босса (кэш на секунду). */
    public static Formation formation(ServerLevel sl, Mob boss) {
        Cache c = CACHE.get(boss.getUUID());
        if (c != null && sl.getGameTime() - c.tick < 20) return c.f;
        AABB box = boss.getBoundingBox().inflate(40);
        int players = 0, shields = 0, ranged = 0, pikes = 0, cav = 0;
        for (Player p : sl.getEntitiesOfClass(Player.class, box, p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) players++;
        for (SoldierEntity s : sl.getEntitiesOfClass(SoldierEntity.class, box, s -> s.isAlive() && s.getOwnerPlayer() != null || s.isAlive() && s.getCommander() != null)) {
            switch (s.getSoldierType().role) {
                case SHIELD -> shields++;
                case ARCHER, GUNNER, HORSE_ARCHER -> ranged++;
                case SPEAR, HEAVY -> pikes++;
                case CAVALRY -> cav++;
                default -> {
                }
            }
            if (s.getSoldierType() == SoldierType.HORSE_ARCHER) cav++;
        }
        float coord = 0.2f * Math.min(1f, players / (float) NEED_PLAYERS)
                + 0.2f * Math.min(1f, shields / (float) NEED_UNITS)
                + 0.2f * Math.min(1f, ranged / (float) NEED_UNITS)
                + 0.2f * Math.min(1f, pikes / (float) NEED_UNITS)
                + 0.2f * Math.min(1f, cav / (float) NEED_UNITS);
        Formation f = new Formation(players, shields, ranged, pikes, cav, coord);
        CACHE.put(boss.getUUID(), new Cache(sl.getGameTime(), f));
        return f;
    }

    /** Множитель урона по боссу от строя. */
    public static float multiplier(ServerLevel sl, Mob boss) {
        float floor = warboss(boss) ? 0.2f : 0.65f;
        float m = floor + (1.25f - floor) * formation(sl, boss).coord();
        return warboss(boss) ? m * 0.4f : m; // исполин: броня легенды (здоровье упирается в потолок 1024)
    }

    /** Сделать босса «военным»: максимум здоровья, пол урона 20% и броня ×0.4 — нужны герои и войска. */
    public static void empower(Mob boss) {
        boss.getPersistentData().putBoolean(WARBOSS, true);
        AttributeInstance hp = boss.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.addOrReplacePermanentModifier(new AttributeModifier(WAR_HP, 5.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        boss.setHealth(boss.getMaxHealth());
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 11) return;
        for (ServerLevel sl : event.getServer().getAllLevels()) {
            for (ServerPlayer p : sl.players()) {
                List<Mob> bosses = sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(40), b -> b.isAlive() && BossRules.isBoss(b));
                if (bosses.isEmpty()) continue;
                Mob boss = bosses.get(0);
                Formation f = formation(sl, boss);
                float m = multiplier(sl, boss);
                ChatFormatting col = m >= 1.1f ? ChatFormatting.GREEN : m >= 0.7f ? ChatFormatting.YELLOW : ChatFormatting.RED;
                p.displayClientMessage(Component.literal((warboss(boss) ? "☠ Исполин: " : "Строй: ") + f.line() + "  → урон ×" + String.format("%.2f", m)).withStyle(col), true);
            }
        }
        if (event.getServer().getTickCount() % 1200 == 11) CACHE.keySet().removeIf(id -> {
            for (ServerLevel sl : event.getServer().getAllLevels()) if (sl.getEntity(id) != null) return false;
            return true;
        });
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob boss) || !BossRules.isBoss(boss) || !(boss.level() instanceof ServerLevel sl)) return;
        Commissions cm = Commissions.get(sl.getServer());
        int bonus = warboss(boss) ? 60 : 20;
        for (Commissions.Commission c : cm.all()) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(c.commander);
            if (p == null || p.level() != sl || p.distanceToSqr(boss) > 70 * 70) continue;
            int n = sl.getEntitiesOfClass(SoldierEntity.class, boss.getBoundingBox().inflate(50), s -> c.commander.equals(s.getCommander())).size();
            if (n == 0) continue;
            cm.addRep(c.commander, bonus + n);
            c.victories++;
            Text.gold(p, "Ваш отряд (" + n + ") участвовал в победе над боссом: авторитет +" + (bonus + n) + " (" + c.rank() + ").");
        }
        CACHE.remove(boss.getUUID());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("warboss").requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ServerLevel sl = p.serverLevel();
                    var boss = com.alkimor.regnum.dungeon.RegionsModule.SCARAB_QUEEN.get().create(sl);
                    if (boss == null) return 0;
                    boss.moveTo(p.getX() + 6, p.getY(), p.getZ(), 0f, 0f);
                    sl.addFreshEntity(boss);
                    empower(boss);
                    Text.gold(p, "Призван исполин: нужны ≥4 героя и по 6+ бойцов каждого рода (щиты, стрелки, копья, конница).");
                    return 1;
                })));
    }
}
