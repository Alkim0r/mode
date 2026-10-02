package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Вассальные деревни: берёшь под защиту ванильную деревню — она платит дань, но привлекает разбойников. */
public final class Villages {
    private Villages() {}

    public static final int CLAIM_COST = 30, MAX = 3;
    private static final String[] NAMES = {"Ключи", "Зарное", "Липовка", "Дубровка", "Мостки", "Заречье", "Берёзовка", "Каменка", "Лужки", "Ольшаны"};
    private static long now = 0;

    public static int dailyBonus(KingdomData data, City c) {
        List<City> mine = data.ownedBy(c.owner);
        if (mine.isEmpty() || mine.get(0) != c) return 0;
        int sum = 0;
        for (VassalVillage v : data.villages()) {
            if (!c.owner.equals(v.owner) || now < v.unrestUntil) continue;
            if (v.loyalty < 20) continue;
            double taxMul = v.tax == 0 ? 0.6 : v.tax == 2 ? 1.6 : 1.0;
            sum += (int) Math.round((3 + Math.min(20, v.villagers) / 2) * taxMul * (0.5 + v.loyalty / 100.0));
            if (v.purpose == 1) sum += 4 + Math.min(20, v.villagers) / 3;
            sum -= v.garrison; // жалованье гарнизона
        }
        return sum;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 200 != 0) return;
        ServerLevel ow = server.overworld();
        boolean newDay = DayClock.newDay(server, "villages");
        now = ow.getGameTime();
        for (VassalVillage v : KingdomData.get(server).villages()) {
            if (!ow.isLoaded(v.pos())) continue;
            v.villagers = ow.getEntitiesOfClass(Villager.class, new AABB(v.pos()).inflate(48, 24, 48)).size();
        }
        KingdomData data = KingdomData.get(server);
        if (newDay) daily(server, ow, data);
        levy(ow, data);
    }

    /** Раз в сутки: верность, голод, уход мятежных деревень. */
    public static void daily(MinecraftServer server, ServerLevel ow, KingdomData data) {
        for (VassalVillage v : new ArrayList<>(data.villages())) {
            ServerPlayer owner = server.getPlayerList().getPlayer(v.owner);
            int d = 0;
            d += v.garrison >= 2 ? 2 : v.garrison == 0 ? -1 : 0;
            d += v.tax == 0 ? 2 : v.tax == 2 ? -3 : 0;
            d += v.palisade;
            if (v.villagers > 0 && v.villagers < 3) d -= 2;
            if (v.famineDays > 0) {
                d -= 3;
                v.famineDays--;
            } else if (ow.random.nextInt(100) < 5) {
                v.famineDays = 3;
                if (owner != null) Text.bad(owner, "В «" + v.name + "» голодно: урожай погиб. /regnum village relief " + indexOf(data, v) + " (20 монет) успокоит людей.");
            }
            if (v.purpose != 0) d += 1;
            purposeDaily(server, data, v);
            v.loyalty = Math.max(0, Math.min(100, v.loyalty + d));
            if (v.loyalty < 8) {
                data.removeVillage(v);
                if (owner != null) Text.bad(owner, "Жители «" + v.name + "» подняли бунт и прогнали ваших сборщиков: деревня ушла из-под вашей руки.");
            } else if (v.loyalty < 20 && owner != null) {
                Text.bad(owner, "«" + v.name + "» бунтует (верность " + v.loyalty + "%): дани нет. Снизьте налог, поставьте гарнизон или частокол.");
            }
        }
        data.setDirty();
    }

    /** Ежедневный эффект назначения аванпоста. */
    private static void purposeDaily(MinecraftServer server, KingdomData data, VassalVillage v) {
        if (v.purpose == 0 || v.loyalty < 20) return;
        City c = null;
        for (City o : data.ownedBy(v.owner)) if (c == null || o.level > c.level) c = o;
        if (c == null) return;
        switch (v.purpose) {
            case 2 -> {
                c.glory += 2;
                Science sc = Science.get(server);
                if (sc.of(v.owner).current != null) sc.addPoints(v.owner, 2);
            }
            case 3 -> c.deposit(Resource.IRON, 2 + v.garrison / 2);
            case 4 -> {
                if (c.plagueDays > 0) c.plagueDays = Math.max(0, c.plagueDays - 1);
                c.deposit(Resource.HERBS, 3);
            }
            default -> { }
        }
    }

    /** Просьбы независимых деревень: вовремя взявший их под защиту получает скидку и высокую верность. */
    public record Plea(BlockPos pos, long expires, String reason) {}
    public static final java.util.Map<UUID, Plea> PLEAS = new java.util.HashMap<>();
    private static final String[] REASONS = {"их грабят разбойники", "они голодают после неурожая", "соседний рудник оспаривают чужаки", "им грозит набег"};

    public static boolean plea(ServerLevel ow, ServerPlayer p) {
        KingdomData data = KingdomData.get(ow.getServer());
        BlockPos v = ow.findNearestMapStructure(StructureTags.VILLAGE, p.blockPosition(), 16, false);
        if (v == null) return false;
        double d = Math.hypot(v.getX() - p.getX(), v.getZ() - p.getZ());
        if (d < 30 || d > 320) return false;
        for (VassalVillage o : data.villages()) if (o.pos().distSqr(v) < 80 * 80) return false;
        String why = REASONS[ow.random.nextInt(REASONS.length)];
        PLEAS.put(p.getUUID(), new Plea(v, ow.getGameTime() + 24000L * 3, why));
        Text.gold(p, "✉ Старейшины деревни у X " + v.getX() + " Z " + v.getZ() + " (~" + (int) d + " блоков) просят защиты: " + why
                + ". Придите за 3 дня и возьмите их под руку (/regnum village claim): цена вдвое ниже, верность выше.");
        return true;
    }

    /** Когда город в осаде, лояльные деревни присылают ополчение (раз за войну). */
    private static void levy(ServerLevel ow, KingdomData data) {
        for (City c : data.all()) {
            if (!c.war) { c.levied = false; continue; }
            if (c.levied || !ow.isLoaded(c.hall)) continue;
            int sent = 0;
            for (VassalVillage v : data.villages()) {
                if (!c.owner.equals(v.owner) || v.loyalty < 55) continue;
                if (v.pos().distSqr(c.hall) > 600 * 600) continue;
                int n = Math.min(v.purpose == 3 ? 10 : 6, 2 + v.garrison + (v.purpose == 3 ? 3 : 0));
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2 * i / n;
                    int px = c.hall.getX() + (int) (Math.cos(a) * 9), pz = c.hall.getZ() + (int) (Math.sin(a) * 9);
                    ow.getChunk(px >> 4, pz >> 4);
                    int py = ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
                    SoldierEntity s = KingdomModule.SOLDIER.get().create(ow);
                    if (s == null) continue;
                    s.setup(i % 3 == 2 ? SoldierType.ARCHER : SoldierType.SPEARMAN, c.owner, null, 4);
                    s.setCulture(Culture.byId(c.culture));
                    s.moveTo(px + 0.5, py, pz + 0.5);
                    s.command(Order.HOLD, s.position(), 0f, Formation.LOOSE, i, n, null);
                    s.addTag("regnum_levy");
                    ow.addFreshEntity(s);
                    sent++;
                }
                v.levyCalled = true;
            }
            c.levied = true;
            if (sent > 0) {
                ServerPlayer p = ow.getServer().getPlayerList().getPlayer(c.owner);
                if (p != null) Text.gold(p, "Деревни прислали ополчение в «" + c.name + "»: " + sent + " бойцов.");
            }
        }
    }

    private static int indexOf(KingdomData data, VassalVillage v) {
        int i = 1;
        for (VassalVillage o : data.villages()) {
            if (o == v) return i;
            if (v.owner.equals(o.owner)) i++;
        }
        return i;
    }

    private static VassalVillage nth(KingdomData data, ServerPlayer p, int n) {
        int i = 1;
        for (VassalVillage v : data.villages()) {
            if (!p.getUUID().equals(v.owner)) continue;
            if (i++ == n) return v;
        }
        return null;
    }

    /** Ближайшая деревня игрока (вассальная), для событий. */
    public static VassalVillage nearest(KingdomData data, ServerPlayer p) {
        VassalVillage best = null;
        double bd = 260 * 260;
        for (VassalVillage v : data.villages()) {
            if (!p.getUUID().equals(v.owner)) continue;
            double d = v.pos().distSqr(p.blockPosition());
            if (d < bd && d > 30 * 30) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    public static void onRaidOutcome(KingdomData data, BlockPos pos, boolean won, ServerPlayer p) {
        for (VassalVillage v : data.villages()) {
            if (v.pos().distSqr(pos) > 20 * 20) continue;
            if (won) { v.raidsRepelled++; v.loyalty = Math.min(100, v.loyalty + 5); }
            else {
                v.raidsLost++;
                v.loyalty = Math.max(0, v.loyalty - 12);
                v.unrestUntil = now + 24000L * 3;
                if (p != null) p.sendSystemMessage(Text.of("«" + v.name + "» разграблена: три дня без дани.", ChatFormatting.RED));
            }
            data.setDirty();
        }
    }

    private static VassalVillage at(KingdomData data, ServerPlayer p) {
        for (VassalVillage v : data.villages()) if (v.pos().distSqr(p.blockPosition()) < 80 * 80) return v;
        return null;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("village")
                .then(Commands.literal("claim").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    ServerLevel ow = p.serverLevel();
                    List<City> cities = data.ownedBy(p.getUUID());
                    if (cities.isEmpty()) { Text.bad(p, "Сначала основайте город."); return 0; }
                    List<VassalVillage> mine = new ArrayList<>();
                    for (VassalVillage v : data.villages()) if (p.getUUID().equals(v.owner)) mine.add(v);
                    if (mine.size() >= MAX) { Text.bad(p, "Больше " + MAX + " деревень под защиту не взять."); return 0; }
                    if (at(data, p) != null) { Text.bad(p, "Эта деревня уже чья-то."); return 0; }
                    BlockPos v = ow.findNearestMapStructure(StructureTags.VILLAGE, p.blockPosition(), 3, false);
                    if (v == null || Math.hypot(v.getX() - p.getX(), v.getZ() - p.getZ()) > 70) {
                        Text.bad(p, "Рядом нет деревни. Встаньте в её центре.");
                        return 0;
                    }
                    City c = cities.get(0);
                    Plea pl = PLEAS.get(p.getUUID());
                    boolean pleaOk = pl != null && ow.getGameTime() < pl.expires() && pl.pos().distSqr(v) < 90 * 90;
                    int cost = pleaOk ? CLAIM_COST / 2 : CLAIM_COST;
                    if (c.treasury < cost) { Text.bad(p, "Нужно " + cost + " монет в казне «" + c.name + "»."); return 0; }
                    c.treasury -= cost;
                    String name = NAMES[ow.random.nextInt(NAMES.length)];
                    VassalVillage vv = new VassalVillage(UUID.randomUUID(), name, v, p.getUUID());
                    if (pleaOk) { vv.loyalty = 75; PLEAS.remove(p.getUUID()); }
                    data.addVillage(vv);
                    Text.good(p, "Деревня «" + name + "» принята под вашу руку. Она платит дань, но разбойники теперь придут и к ней.");
                    return 1;
                }))
                .then(Commands.literal("list").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    int i = 1;
                    Text.gold(p, "— Ваши деревни —");
                    for (VassalVillage v : data.villages()) {
                        if (!p.getUUID().equals(v.owner)) continue;
                        Text.info(p, "  " + i++ + ". " + v.name + " (X " + v.x + " Z " + v.z + "), жителей ~" + v.villagers + ", верность " + v.loyalty + "%, налог " + (v.tax == 0 ? "щадящий" : v.tax == 2 ? "тяжёлый" : "обычный")
                                + ", " + VassalVillage.PURPOSES[v.purpose] + ", гарнизон " + v.garrison + ", частокол " + v.palisade + ", набегов отбито " + v.raidsRepelled
                                + ", потеряно " + v.raidsLost + (now < v.unrestUntil ? ", ограблена" : "") + (v.loyalty < 20 ? ", БУНТ" : "") + (v.famineDays > 0 ? ", голод" : ""));
                    }
                    if (i == 1) Text.info(p, "  Пока нет. Встаньте в центре деревни и введите /regnum village claim.");
                    return 1;
                }))
                .then(Commands.literal("tax").then(Commands.argument("n", IntegerArgumentType.integer(1, 20))
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 2)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            KingdomData data = KingdomData.get(p.server);
                            VassalVillage v = nth(data, p, IntegerArgumentType.getInteger(ctx, "n"));
                            if (v == null) return 0;
                            v.tax = IntegerArgumentType.getInteger(ctx, "level");
                            data.setDirty();
                            Text.good(p, "Налог «" + v.name + "»: " + (v.tax == 0 ? "щадящий (дань ×0.6, верность растёт)" : v.tax == 2 ? "тяжёлый (дань ×1.6, верность падает)" : "обычный") + ".");
                            return 1;
                        }))))
                .then(Commands.literal("purpose").then(Commands.argument("n", IntegerArgumentType.integer(1, 20))
                        .then(Commands.argument("kind", IntegerArgumentType.integer(0, 4)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            KingdomData data = KingdomData.get(p.server);
                            VassalVillage v = nth(data, p, IntegerArgumentType.getInteger(ctx, "n"));
                            List<City> cs = data.ownedBy(p.getUUID());
                            if (v == null || cs.isEmpty()) return 0;
                            int k = IntegerArgumentType.getInteger(ctx, "kind");
                            if (k == v.purpose) return 1;
                            if (k != 0 && cs.get(0).treasury < 50) { Text.bad(p, "Нужно 50 монет в казне на обустройство."); return 0; }
                            if (k != 0) cs.get(0).treasury -= 50;
                            v.purpose = k;
                            data.setDirty();
                            Text.good(p, "«" + v.name + "»: " + VassalVillage.PURPOSES[k] + ". (1 торговый: дань, 2 разведка: слава и знания, 3 военный: железо и гарнизон до 10, 4 лазарет: лечит эпидемию, травы)");
                            return 1;
                        }))))
                .then(Commands.literal("muster").then(Commands.argument("n", IntegerArgumentType.integer(1, 20))
                        .then(Commands.argument("count", IntegerArgumentType.integer(0, 10)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            KingdomData data = KingdomData.get(p.server);
                            VassalVillage v = nth(data, p, IntegerArgumentType.getInteger(ctx, "n"));
                            if (v == null) return 0;
                            int want = Math.min(IntegerArgumentType.getInteger(ctx, "count"), v.purpose == 3 ? 10 : 6);
                            int extra = Math.max(0, want - v.garrison);
                            List<City> cs = data.ownedBy(p.getUUID());
                            if (!cs.isEmpty() && cs.get(0).treasury < extra * 4) {
                                Text.bad(p, "Нужно " + extra * 4 + " монет в казне на оружие и обучение.");
                                return 0;
                            }
                            if (!cs.isEmpty()) cs.get(0).treasury -= extra * 4;
                            v.garrison = want;
                            data.setDirty();
                            Text.good(p, "Гарнизон «" + v.name + "»: " + want + " ополченцев (жалованье " + want + "/день). Они защитят деревню от набегов.");
                            return 1;
                        }))))
                .then(Commands.literal("fortify").then(Commands.argument("n", IntegerArgumentType.integer(1, 20)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    VassalVillage v = nth(data, p, IntegerArgumentType.getInteger(ctx, "n"));
                    if (v == null) return 0;
                    if (v.palisade >= 2) { Text.info(p, "«" + v.name + "» укреплена полностью."); return 1; }
                    int cost = v.palisade == 0 ? 40 : 90;
                    List<City> cs = data.ownedBy(p.getUUID());
                    if (cs.isEmpty() || cs.get(0).treasury < cost) { Text.bad(p, "Нужно " + cost + " монет в казне."); return 0; }
                    cs.get(0).treasury -= cost;
                    v.palisade++;
                    data.setDirty();
                    Text.good(p, "«" + v.name + "»: " + (v.palisade == 1 ? "вырос частокол" : "выкопан ров и поставлены башни") + ". Разбойники придут меньшими силами, жители спокойнее.");
                    return 1;
                })))
                .then(Commands.literal("relief").then(Commands.argument("n", IntegerArgumentType.integer(1, 20)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    VassalVillage v = nth(data, p, IntegerArgumentType.getInteger(ctx, "n"));
                    List<City> cs = data.ownedBy(p.getUUID());
                    if (v == null || cs.isEmpty()) return 0;
                    if (cs.get(0).treasury < 20) { Text.bad(p, "Нужно 20 монет на зерно."); return 0; }
                    cs.get(0).treasury -= 20;
                    v.famineDays = 0;
                    v.loyalty = Math.min(100, v.loyalty + 10);
                    data.setDirty();
                    Text.good(p, "Зерно роздано: верность «" + v.name + "» " + v.loyalty + "%.");
                    return 1;
                })))
                .then(Commands.literal("abandon").then(Commands.argument("n", IntegerArgumentType.integer(1, 20)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    int n = IntegerArgumentType.getInteger(ctx, "n"), i = 1;
                    for (VassalVillage v : new ArrayList<>(data.villages())) {
                        if (!p.getUUID().equals(v.owner)) continue;
                        if (i++ == n) {
                            data.removeVillage(v);
                            Text.info(p, "Вы отпустили «" + v.name + "».");
                            return 1;
                        }
                    }
                    return 0;
                })))));
    }
}
