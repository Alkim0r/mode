package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Эпидемии и лазарет. Болезнь приходит в большие города (голод и скученность повышают риск), косит жителей, ослабляет
 * бойцов и игроков. Лазарет лечит бойцов и жителей, травы на складе ускоряют выздоровление, карантин сдерживает заразу.
 */
public final class Health {
    private Health() {}

    public static float startChance(MinecraftServer server, City c) {
        if (c.postPlagueDays > 0) return 0f;
        float p = 0.012f + 0.004f * c.level + 0.002f * (c.soldiers.size() / 10f) + (c.hungerDays > 0 ? 0.03f : 0f);
        int inf = Math.min(2, c.count(BuildingType.INFIRMARY));
        p *= 1f - 0.35f * inf;
        if (Science.has(server, c.owner, Science.Tech.MEDICINE)) p *= 0.8f;
        if (c.quarantine) p *= 0.5f;
        return Math.min(0.12f, Math.max(0f, p));
    }

    public static void start(ServerLevel ow, City c, int level) {
        c.plagueDays = 4 + ow.random.nextInt(4);
        c.plagueLevel = Math.max(1, Math.min(3, level));
        c.healthyDays = 0;
        c.plagueSource = c.hungerDays > 0 ? 1 : c.soldiers.size() >= 30 ? 2 : 0;
        ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
        if (owner != null) {
            owner.sendSystemMessage(Text.of("☣ В «" + c.name + "» вспыхнула эпидемия (сила " + c.plagueLevel + ")! "
                    + (c.count(BuildingType.INFIRMARY) == 0 ? "Постройте Лазарет и запасите травы. " : "Лазарет борется с болезнью. ")
                    + "Карантин: /regnum plague quarantine", ChatFormatting.RED, ChatFormatting.BOLD));
        }
    }

    /** Раз в сутки. Возвращает короткую строку для сводки. */
    public static String daily(ServerLevel ow, KingdomData data, City c) {
        int inf = c.count(BuildingType.INFIRMARY);
        if (c.postPlagueDays > 0) c.postPlagueDays--;
        if (c.plagueDays <= 0) {
            c.healthyDays++;
            if (c.healthyDays >= 4 && c.population >= 3 && ow.random.nextFloat() < startChance(ow.getServer(), c)) start(ow, c, 1 + (c.hungerDays > 0 ? 1 : 0) + ow.random.nextInt(2));
            return c.plagueDays > 0 ? "эпидемия!" : "";
        }
        // течение болезни
        int herbs = 0;
        if (inf > 0 && c.take(Resource.HERBS, 2 * c.plagueLevel)) herbs = 1;
        int cure = 1 + Math.min(2, inf) + herbs;
        if (inf == 0 && !c.quarantine && ow.random.nextFloat() < 0.2f && c.plagueLevel < 3) c.plagueLevel++;
        c.plagueDays -= cure;
        // потери среди жителей
        int dead = 0;
        float lethal = (inf > 0 ? 0.08f : 0.22f) * c.plagueLevel * (herbs > 0 ? 0.5f : 1f);
        if (inf > 0 && patients(c) > bedCapacity(c)) lethal *= 1.5f; // лазарет переполнен
        if (ow.isLoaded(c.hall)) {
            int r = c.radius();
            for (Villager v : ow.getEntitiesOfClass(Villager.class, new AABB(c.hall).inflate(r, 32, r), v -> c.contains(v.blockPosition()))) {
                if (ow.random.nextFloat() < lethal * 0.35f) {
                    v.kill();
                    dead++;
                }
            }
        }
        if (c.quarantine) c.treasury -= Math.min(c.treasury, Math.max(1, c.dailyIncome(2) / 3));
        else c.treasury -= Math.min(c.treasury, Math.max(1, c.dailyIncome(2) / 5 * c.plagueLevel));
        if (c.plagueDays <= 0) {
            c.plagueDays = 0;
            c.plagueLevel = 0;
            c.healthyDays = 0;
            c.postPlagueDays = 3;
            ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
            if (owner != null) Text.good(owner, "☣ Эпидемия в «" + c.name + "» закончилась.");
            return "эпидемия отступила";
        }
        return "эпидемия: " + c.plagueDays + " дн., погибло " + dead;
    }

    // ------------------------------------------------------------------ лазарет и болезнь в реальном времени

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 100 != 7) return;
        ServerLevel ow = server.overworld();
        KingdomData data = KingdomData.get(server);
        for (City c : data.all()) {
            if (!ow.isLoaded(c.hall)) continue;
            // лазарет лечит
            for (var e : c.buildings.entrySet()) {
                if (e.getValue() != BuildingType.INFIRMARY) continue;
                BlockPos pos = BlockPos.of(e.getKey());
                if (!ow.isLoaded(pos)) continue;
                AABB box = new AABB(pos).inflate(10, 5, 10);
                for (SoldierEntity s : ow.getEntitiesOfClass(SoldierEntity.class, box, s -> s.isAlive() && c.id.equals(s.getCityId()))) {
                    s.heal(1.5f);
                    s.removeEffect(MobEffects.WEAKNESS);
                    s.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                }
                for (ServerPlayer p : ow.getEntitiesOfClass(ServerPlayer.class, box, p -> p.isAlive() && (p.getUUID().equals(c.owner) || c.trusted.contains(p.getUUID())))) {
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
                    p.removeEffect(MobEffects.WEAKNESS);
                }
            }
            // болезнь ослабляет
            if (c.plagueDays > 0) {
                int amp = Math.max(0, c.plagueLevel - 2);
                AABB box = new AABB(c.hall).inflate(c.radius(), 40, c.radius());
                boolean cover = c.count(BuildingType.INFIRMARY) > 0;
                for (SoldierEntity s : ow.getEntitiesOfClass(SoldierEntity.class, box, s -> s.isAlive() && c.id.equals(s.getCityId()))) {
                    if (s.getRandom().nextInt(cover ? 4 : 2) == 0) s.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 220, amp, true, false));
                }
                for (ServerPlayer p : ow.getEntitiesOfClass(ServerPlayer.class, box, p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && c.contains(p.blockPosition()))) {
                    if (p.getRandom().nextInt(cover ? 4 : 2) == 0) {
                        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, amp, false, true));
                        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0, false, true));
                    }
                }
            }
        }
    }

    /** Сколько больных принимает лазарет: 8 коек на здание, травы и персонал не считаем. */
    public static int bedCapacity(City c) {
        return 8 * c.count(BuildingType.INFIRMARY);
    }

    /** Сколько больных в городе при текущей эпидемии. */
    public static int patients(City c) {
        if (c.plagueDays <= 0) return 0;
        return Math.max(1, (c.population + c.soldiers.size() / 3) * c.plagueLevel / 4);
    }

    public static final String[] SOURCES = {"скученность жителей", "голод в городе", "переполненные казармы"};

    /** Объяснимый диагноз: что происходит, почему, чем рискуем и что делать. */
    public static java.util.List<String> diagnose(MinecraftServer server, City c) {
        java.util.List<String> out = new java.util.ArrayList<>();
        int inf = c.count(BuildingType.INFIRMARY);
        if (c.plagueDays <= 0) {
            out.add("Эпидемии нет. Риск новой вспышки в сутки: " + Math.round(startChance(server, c) * 1000) / 10.0 + "%.");
            if (c.postPlagueDays > 0) out.add("Город передыхает после болезни ещё " + c.postPlagueDays + " дн.: новой вспышки не будет.");
            if (c.hungerDays > 0) out.add("Повышает риск: голод " + c.hungerDays + " дн.");
            if (c.soldiers.size() >= 30) out.add("Повышает риск: тесные казармы (" + c.soldiers.size() + " бойцов).");
            if (inf == 0) out.add("Совет: Лазарет снижает риск на треть.");
            return out;
        }
        String[] sym = {"", "кашель и слабость", "жар, слабость, голод у заражённых", "тяжёлая горячка, мор среди жителей"};
        out.add("Эпидемия, сила " + c.plagueLevel + ": " + sym[Math.max(1, Math.min(3, c.plagueLevel))] + ".");
        out.add("Причина вспышки: " + SOURCES[Math.max(0, Math.min(2, c.plagueSource))] + ".");
        out.add("Осталось примерно " + c.plagueDays + " дн. " + (inf > 0 ? "Лазарет ускоряет выздоровление." : "Лазарета нет — болезнь может усилиться."));
        out.add("Койки лазарета: " + bedCapacity(c) + ", больных примерно " + patients(c) + (inf > 0 && patients(c) > bedCapacity(c) ? " — лазарет переполнен, смертность выше. Постройте ещё один." : "."));
        out.add("Травы на складе: " + c.stock(Resource.HERBS) + " (лазарет тратит " + 2 * c.plagueLevel + " в день).");
        out.add(c.quarantine ? "Карантин объявлен: заразы вдвое меньше, но торговля страдает." : "Карантин: /regnum plague quarantine.");
        return out;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("plague")
                .then(Commands.literal("quarantine").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) return 0;
                    c.quarantine = !c.quarantine;
                    KingdomData.get(p.server).setDirty();
                    Text.info(p, c.quarantine ? "Карантин объявлен: вдвое меньше риск заразы, но торговля стоит (доход падает)." : "Карантин снят.");
                    return 1;
                }))
                .then(Commands.literal("diag").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    Text.gold(p, "Лазарет «" + c.name + "»: диагноз");
                    for (String l : diagnose(p.server, c)) Text.info(p, "  " + l);
                    return 1;
                }))
                .then(Commands.literal("start").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) return 0;
                    start(p.serverLevel(), c, 2);
                    KingdomData.get(p.server).setDirty();
                    return 1;
                }))
                .then(Commands.literal("cure").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) return 0;
                    c.plagueDays = 0;
                    c.plagueLevel = 0;
                    KingdomData.get(p.server).setDirty();
                    return 1;
                }))));
    }
}
