package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Соседние королевства: столицы с правителем и гарнизоном, отношения, торговля, вассалитет, война и походы армий.
 * Армии «идут» абстрактно: когда приходит время, они появляются у края города игрока со стороны своей столицы.
 */
public final class Realms {
    private Realms() {}

    public static final String RULER_TAG = "regnum_ruler";
    private static final int WORLD_REALMS = 4;
    private static final String[][] NAMES = {
            {"Северное княжество Ладога", "Княжество Белозёрье", "Княжество Гардарика"},
            {"Аврелийская марка", "Империя Ромула", "Консульство Сервиан"},
            {"Королевство Вальдмарк", "Герцогство Грэнхольм", "Королевство Эскарн"},
            {"Орда Белого Коня", "Каганат Тэмуджин", "Кочевье Хонгор"},
            {"Султанат Аль-Хазир", "Эмират Дюн", "Султанат Сахрем"},
            {"Клан Дун-Кэрн", "Кланы Аннеллон", "Союз Глен-Моргах"}};
    private static final String[][] RULERS = {
            {"князь Ярополк", "князь Святогор", "княгиня Ольга"},
            {"император Гай Север", "консул Марк", "императрица Юлия"},
            {"король Эдвин", "герцог Гийом", "королева Матильда"},
            {"каган Бату", "хан Орхон", "ханша Сарнай"},
            {"султан Касим", "эмир Зафар", "султанша Лейла"},
            {"вождь Брайан", "вождь Кормак", "вождь Мэйв"}};

    // ------------------------------------------------------------------ запросы

    public static Realm realmOf(Entity e, UUID id) {
        if (id == null || !(e.level() instanceof ServerLevel sl)) return null;
        return KingdomData.get(sl.getServer()).realm(id);
    }

    /** Должен ли солдат считать цель врагом из-за войны между королевствами. */
    public static boolean hostile(SoldierEntity s, LivingEntity e) {
        if (!e.isAlive()) return false;
        Realm mine = realmOf(s, s.realmId());
        if (mine != null) {
            if (mine.state != Realm.WAR) return false;
            if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
            if (e instanceof SoldierEntity o) return o.realmId() == null;
            return e instanceof Villager;
        }
        if (e instanceof SoldierEntity o && o.realmId() != null) {
            Realm r = realmOf(s, o.realmId());
            return r != null && r.state == Realm.WAR;
        }
        if (e instanceof SiegeTowerEntity tw && tw.realmId() != null) {
            Realm r = realmOf(s, tw.realmId());
            return r != null && r.state == Realm.WAR;
        }
        if (e instanceof CatapultEntity cat && cat.realmId() != null) {
            Realm r = realmOf(s, cat.realmId());
            return r != null && r.state == Realm.WAR;
        }
        return false;
    }

    public static int playerPower(KingdomData data, UUID owner) {
        int n = 0;
        for (City c : data.ownedBy(owner)) n += c.soldiers.size();
        return n;
    }

    /** Дневной доход от торговых договоров и дани вассалов. */
    public static int dailyBonus(KingdomData data, City c) {
        List<City> mine = data.ownedBy(c.owner);
        if (mine.isEmpty() || mine.get(0) != c) return 0;
        int sum = 0;
        for (Realm r : data.realms()) {
            if (r.state == Realm.VASSAL && c.owner.equals(r.liege)) sum += 4 + r.strength / 4;
            else if (r.trade && r.state == Realm.PEACE) sum += 2 + Math.max(0, r.relation) / 20;
        }
        return sum;
    }

    // ------------------------------------------------------------------ тик

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tc = server.getTickCount();
        if (tc % 20 != 0) return;
        ServerLevel ow = server.overworld();
        KingdomData data = KingdomData.get(server);
        if (data.realms().isEmpty() && tc > 200) generate(ow, data);
        long now = ow.getGameTime();
        for (Realm r : new ArrayList<>(data.realms())) {
            if (!r.built) {
                tryBuild(ow, data, r);
                continue;
            }
            if (r.known < 2 && tc % 100 == 0) discover(ow, data, r, now);
            if (r.trade && r.state == Realm.PEACE && tc % 6000 == 0) sendCaravan(ow, data, r);
            if (r.ally && tc % 100 == 0) allyAid(ow, data, r);
            if (r.state == Realm.WAR && tc % 100 == 0) warTick(ow, data, r, now);
            if (r.state != Realm.WAR && r.relation != 0 && tc % 1200 == 0) {
                r.relation += r.relation > 0 ? -1 : 1; // со временем чувства остывают
                data.setDirty();
            }
        }
    }

    /** Караван от торгового партнёра к ближайшему городу игрока. */
    public static boolean sendCaravan(ServerLevel ow, KingdomData data, Realm r) {
        for (ServerPlayer p : ow.players()) {
            City c = data.nearestOwned(p.getUUID(), r.capital());
            if (c == null || !ow.isLoaded(c.hall)) continue;
            double dx = r.x - c.hall.getX(), dz = r.z - c.hall.getZ();
            double len = Math.max(1, Math.hypot(dx, dz));
            double dist = c.radius() + 40;
            int sx = c.hall.getX() + (int) (dx / len * dist), sz = c.hall.getZ() + (int) (dz / len * dist);
            int sy = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
            CaravanEntity cv = KingdomModule.CARAVAN.get().create(ow);
            if (cv == null) return false;
            cv.setRoute(r.id, c.id);
            cv.moveTo(sx + 0.5, sy, sz + 0.5, 0f, 0f);
            cv.setCustomName(Text.of("Караван «" + r.name + "»", ChatFormatting.GOLD));
            ow.addFreshEntity(cv);
            Text.info(p, "К городу «" + c.name + "» движется караван «" + r.name + "».");
            return true;
        }
        return false;
    }

    /** Союзник присылает подмогу, когда на город игрока идёт чужая армия (раз за войну). */
    private static void allyAid(ServerLevel ow, KingdomData data, Realm ally) {
        allyAid(ow, data, ally, null);
    }

    private static int allyAid(ServerLevel ow, KingdomData data, Realm ally, City only) {
        int sent = 0;
        for (City c : data.all()) {
            if (only != null && c != only) continue;
            if (!c.war || c.aided) continue;
            ServerPlayer p = ow.getServer().getPlayerList().getPlayer(c.owner);
            com.alkimor.regnum.Regnum.LOGGER.info("[allyAid] city {} owner online {} selftest {}", c.name, p != null, Boolean.getBoolean("regnum.selftest"));
            if ((p == null && !Boolean.getBoolean("regnum.selftest")) || !ow.isLoaded(c.hall)) continue;
            c.aided = true;
            data.setDirty();
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                int px = c.hall.getX() + (int) (Math.cos(a) * 8), pz = c.hall.getZ() + (int) (Math.sin(a) * 8);
                ow.getChunk(px >> 4, pz >> 4);
                int py = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
                SoldierEntity s = spawn(ow, ally, Doctrine.field(ally.culture, i), new BlockPos(px, py, pz), 3);
                com.alkimor.regnum.Regnum.LOGGER.info("[allyAid] spawn {} -> {}", i, s);
                if (s != null) {
                    s.command(Order.HOLD, c.hall.getCenter(), s.getYRot(), Formation.LOOSE, i, 6, null);
                    sent++;
                }
            }
            if (p != null) Text.gold(p, "Союзник «" + ally.name + "» прислал подмогу в «" + c.name + "»!");
            return sent;
        }
        return sent;
    }

    /** Для проверок: подмога без ожидания игрока рядом. */
    public static int allyAidForTest(ServerLevel ow, KingdomData data, Realm ally, City only) {
        return allyAid(ow, data, ally, only);
    }

    /** Игрок подошёл близко — королевство открыто; раз в сутки по одному доходит слух. */
    private static void discover(ServerLevel ow, KingdomData data, Realm r, long now) {
        for (ServerPlayer p : ow.players()) {
            if (Math.hypot(p.getX() - r.x, p.getZ() - r.z) < 260) {
                r.known = 2;
                data.setDirty();
                Text.gold(p, "Вы открыли королевство «" + r.name + "».");
                return;
            }
        }
        if (r.known == 0 && now % 24000 < 100 && ow.random.nextInt(3) == 0 && !ow.players().isEmpty()) {
            r.known = 1;
            data.setDirty();
            for (ServerPlayer p : ow.players()) {
                double dx = r.x - p.getX(), dz = r.z - p.getZ();
                String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "востоке" : "западе") : (dz > 0 ? "юге" : "севере");
                Text.info(p, "Купцы шепчутся: далеко на " + dir + " есть королевство «" + r.name + "».");
            }
        }
    }

    private static void generate(ServerLevel ow, KingdomData data) {
        BlockPos spawn = ow.getSharedSpawnPos();
        double a0 = ow.random.nextDouble() * Math.PI * 2;
        int[] cultures = new int[Culture.values().length];
        for (int i = 0; i < WORLD_REALMS; i++) {
            double a = a0 + i * Math.PI * 2 / WORLD_REALMS + ow.random.nextDouble() * 0.5;
            double d = 850 + ow.random.nextDouble() * 450;
            int x = spawn.getX() + (int) (Math.cos(a) * d), z = spawn.getZ() + (int) (Math.sin(a) * d);
            int cu = ow.random.nextInt(Culture.values().length);
            for (int k = 0; k < 6 && cultures[cu] > 0; k++) cu = ow.random.nextInt(Culture.values().length);
            cultures[cu]++;
            int ni = ow.random.nextInt(NAMES[cu].length);
            data.addRealm(new Realm(UUID.randomUUID(), NAMES[cu][ni], RULERS[cu][ni], cu, x, z));
        }
        com.alkimor.regnum.Regnum.LOGGER.info("[Realms] создано королевств: {}", data.realms().size());
    }

    /** Строим столицу, когда рядом оказывается игрок (до этого мир там не загружен). */
    public static boolean tryBuild(ServerLevel ow, KingdomData data, Realm r) {
        ServerPlayer near = null;
        for (ServerPlayer p : ow.players()) {
            double dx = p.getX() - r.x, dz = p.getZ() - r.z;
            if (dx * dx + dz * dz < 140 * 140) near = p;
        }
        if (near == null) return false;
        return build(ow, data, r);
    }

    private static void load(ServerLevel ow, int cx, int cz, int chunks) {
        for (int dx = -chunks; dx <= chunks; dx++) {
            for (int dz = -chunks; dz <= chunks; dz++) ow.getChunk((cx >> 4) + dx, (cz >> 4) + dz);
        }
    }

    /** Подходит ли место для столицы: сухо, не слишком высоко и достаточно ровно на всей площадке. Возвращает уровень земли или 0. */
    private static int suitable(ServerLevel ow, int x, int z) {
        int sea = ow.getSeaLevel();
        int lo = 9999, hi = -9999, sum = 0, n = 0;
        for (int dx = -40; dx <= 40; dx += 20) {
            for (int dz = -40; dz <= 40; dz += 20) {
                int px = x + dx, pz = z + dz;
                ow.getChunk(px >> 4, pz >> 4);
                int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
                if (y <= sea || y > sea + 110 || !ow.getFluidState(new BlockPos(px, y - 1, pz)).isEmpty()) return 0;
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
                if (hi - lo > 9) return 0;
                sum += y;
                n++;
            }
        }
        return Math.round(sum / (float) n);
    }

    /** Спиральный поиск ровного сухого участка вокруг точки (горы и океаны новых биомов не помеха). */
    private static BlockPos findSite(ServerLevel ow, int tx, int tz) {
        for (int ring = 0; ring <= 12; ring++) {
            int pts = ring == 0 ? 1 : ring * 6;
            for (int i = 0; i < pts; i++) {
                double a = Math.PI * 2 * i / pts + ring * 0.7;
                int x = tx + (int) (Math.cos(a) * ring * 32), z = tz + (int) (Math.sin(a) * ring * 32);
                int g = suitable(ow, x, z);
                if (g > 0) return new BlockPos(x, g - 1, z);
            }
        }
        return null;
    }

    public static boolean build(ServerLevel ow, KingdomData data, Realm r) {
        load(ow, r.x, r.z, 2);
        BlockPos site = findSite(ow, r.x, r.z);
        if (site == null) {
            // совсем неудачный край света: двигаем цель и пробуем позже
            r.x += ow.random.nextInt(601) - 300;
            r.z += ow.random.nextInt(601) - 300;
            data.setDirty();
            return false;
        }
        r.x = site.getX();
        r.z = site.getZ();
        r.culture = Culture.forBiome(ow.getBiome(site)).ordinal();
        Capital.terraform(ow, r.x, r.z, site.getY());
        r.y = site.getY() + 1;
        buildCapital(ow, r, site.getY());
        r.built = true;
        data.setDirty();
        return true;
    }

    /** Для проверок: строит столицу в заданной точке без поиска места (земля на уровне g). */
    public static void buildAt(ServerLevel ow, Realm r, int x, int g, int z) {
        r.x = x;
        r.z = z;
        r.y = g + 1;
        Capital.terraform(ow, x, z, g);
        buildCapital(ow, r, g);
        r.built = true;
    }

    private static void buildCapital(ServerLevel ow, Realm r, int g) {
        int cx = r.x, cz = r.z;
        BlockPos gate = Capital.build(ow, r, cx, g, cz);
        BlockPos hall = new BlockPos(gate.getX(), g + 1, gate.getZ());
        SoldierEntity ruler = spawn(ow, r, SoldierType.KNIGHT, hall, 0);
        if (ruler != null) {
            ruler.addTag(RULER_TAG);
            ruler.setInvulnerable(true);
            ruler.setCustomName(net.minecraft.network.chat.Component.literal(r.ruler.substring(0, 1).toUpperCase() + r.ruler.substring(1) + " · " + r.name));
            ruler.setCustomNameVisible(true);
        }
        for (int i = 0; i < r.strength; i++) {
            SoldierType t = Doctrine.garrison(r.culture, i);
            double a = Math.PI * 2 * i / r.strength;
            int px = cx + (int) (Math.cos(a) * 22), pz = cz + (int) (Math.sin(a) * 22);
            spawn(ow, r, t, new BlockPos(px, g + 1, pz), 1);
        }
    }

    public static SoldierEntity spawn(ServerLevel ow, Realm r, SoldierType t, BlockPos pos, int squad) {
        SoldierEntity s = KingdomModule.SOLDIER.get().create(ow);
        if (s == null) return null;
        s.setup(t, r.id, null, Math.max(1, squad));
        s.setRealm(r.id);
        s.setCulture(Culture.byId(r.culture));
        s.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ow.random.nextFloat() * 360f, 0f);
        s.command(Order.HOLD, s.position(), s.getYRot(), Formation.LOOSE, 0, 1, null);
        ow.addFreshEntity(s);
        return s;
    }

    // ------------------------------------------------------------------ война

    public static final String SAPPER_TAG = "regnum_sapper";
    public static final String RAM_TAG = "regnum_ram";

    /** Удар по стене на пути к цели. Возвращает true, если блок разрушен. Работает только на земле города, где идёт война. */
    public static boolean breach(SoldierEntity s, net.minecraft.world.phys.Vec3 toward) {
        if (!(s.level() instanceof ServerLevel sl)) return false;
        net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(toward.x - s.getX(), 0, toward.z - s.getZ());
        if (dir.lengthSqr() < 0.01) return false;
        dir = dir.normalize();
        BlockPos feet = s.blockPosition();
        for (int step = 1; step <= 2; step++) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos p = feet.offset((int) Math.round(dir.x * step), dy, (int) Math.round(dir.z * step));
                BlockState st = sl.getBlockState(p);
                if (st.isAir() || !st.getFluidState().isEmpty() || st.getDestroySpeed(sl, p) < 0) continue;
                City c = Territory.cityAt(sl, p);
                if (c == null || !c.war) return false;
                s.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                sl.playSound(null, p, st.getSoundType().getHitSound(), net.minecraft.sounds.SoundSource.HOSTILE, 0.8f, 0.8f);
                float dmg = s.getTags().contains(RAM_TAG) ? 16f : s.getTags().contains(SAPPER_TAG) ? 6f : 2f;
                if (Territory.damage(sl, p, dmg)) {
                    sl.destroyBlock(p, false);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private static List<SoldierEntity> army(ServerLevel ow, Realm r) {
        List<SoldierEntity> list = new ArrayList<>();
        for (Entity e : ow.getAllEntities()) {
            if (e instanceof SoldierEntity s && r.id.equals(s.realmId()) && s.getSquad() == 2 && s.isAlive()) list.add(s);
        }
        return list;
    }

    private static void warTick(ServerLevel ow, KingdomData data, Realm r, long now) {
        List<SoldierEntity> army = army(ow, r);
        City target = data.nearestOwned(r.warOwner, r.capital());
        if (target == null) {
            endWar(ow, data, r, "Нет города, который можно было бы атаковать.");
            return;
        }
        if (!ow.isLoaded(target.hall)) return; // чанки города выгружены: армию не считаем разбитой
        if (!army.isEmpty() && r.armyStart > 0 && now - r.armyStart > 36000) {
            for (SoldierEntity s : army) s.discard(); // застрявшие остатки: поход считается проваленным
            removeCamp(ow, r);
            army = new ArrayList<>();
        }
        if (!army.isEmpty()) {
            target.war = true;
            data.setDirty();
            campTick(ow, data, r, army);
            if (checkSack(ow, data, r, target, army)) return;
            // построились — в атаку (через ~15 секунд после появления или когда строй задели)
            for (SoldierEntity s : army) {
                if (s.getOrder() == Order.MOVE && (s.tickCount > 300 || s.getTarget() != null)) {
                    s.command(Order.CHARGE, target.hall.getCenter(), s.getYRot(), s.getFormation(), 0, army.size(), null);
                }
            }
            return;
        }
        if (target.war) {
            // армия разбита
            removeCamp(ow, r);
            target.war = false;
            target.aided = false;
            r.armiesLost++;
            r.armyTimer = now + 12000;
            data.setDirty();
            ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(r.warOwner);
            if (owner != null) Text.good(owner, "Армия «" + r.name + "» разбита у «" + target.name + "».");
            if (r.armiesLost >= 3) victory(ow, data, r, target);
            return;
        }
        if (now < r.armyTimer) return;
        ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(r.warOwner);
        if (owner == null || !owner.blockPosition().closerThan(target.hall, target.radius() + 128) || !ow.isLoaded(target.hall)) {
            r.armyTimer = now + 600; // ждём, пока король рядом
            return;
        }
        sendArmy(ow, data, r, target, owner);
    }

    public static int sendArmy(ServerLevel ow, KingdomData data, Realm r, City target, ServerPlayer owner) {
        double dx = r.x - target.hall.getX(), dz = r.z - target.hall.getZ();
        double len = Math.max(1.0, Math.hypot(dx, dz));
        double dist = target.radius() + 22;
        int sx = target.hall.getX() + (int) (dx / len * dist), sz = target.hall.getZ() + (int) (dz / len * dist);
        int mine = playerPower(data, target.owner);
        int n = Math.min(36, Math.max(5 + r.strength / 3 + 2 * r.armiesSent, Math.min(30, (int) (mine * 0.7))));
        int spawned = 0;
        // состав: доктрина культуры, поздние армии получают порох
        java.util.List<SoldierType> comp = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            SoldierType t = n >= 8 || i % 3 != 0 ? Doctrine.field(r.culture, i) : SoldierType.MILITIA;
            if (t == SoldierType.CROSSBOW && r.armiesSent >= 3) t = SoldierType.MUSKETEER;
            if (i % 12 == 11 && r.armiesSent >= 5) t = SoldierType.BOMBARDIER;
            comp.add(t);
        }
        // строй: щиты и копья впереди, стрелки сзади, конница на флангах; армия встаёт перед стеной и выжидает
        java.util.List<SoldierType> body = new ArrayList<>(), wings = new ArrayList<>();
        for (SoldierType t : comp) (t.role == SoldierType.Role.CAVALRY ? wings : body).add(t);
        body.sort(java.util.Comparator.comparingInt(ArmyCommands::rowPriority));
        Formation form = (r.culture == 1 || r.culture == 0) ? Formation.SHIELD_WALL : r.culture == 3 ? Formation.WEDGE : r.culture == 5 ? Formation.PHALANX : Formation.LINE;
        float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
        net.minecraft.world.phys.Vec3 anchor = new net.minecraft.world.phys.Vec3(sx, 0, sz);
        for (int i = 0; i < n; i++) {
            boolean wing = i >= body.size();
            SoldierType t = wing ? wings.get(i - body.size()) : body.get(i);
            int slot = wing ? -(i - body.size()) - 1 : i;
            net.minecraft.world.phys.Vec3 w = form.worldPos(anchor, yaw, slot, n, 0);
            int px = (int) Math.floor(w.x), pz = (int) Math.floor(w.z);
            if (!ow.hasChunkAt(new BlockPos(px, 64, pz))) continue;
            int py = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
            SoldierEntity s = spawn(ow, r, t, new BlockPos(px, py, pz), 2);
            if (s != null) {
                if (r.bribed) s.addMorale(-30f);
                s.command(Order.MOVE, new net.minecraft.world.phys.Vec3(px + 0.5, py, pz + 0.5), yaw, form, slot, n, null);
                if (n >= 8 && i % 9 == 8 && !wing && !t.ranged()) {
                    s.addTag(RAM_TAG);
                    s.setCustomName(Text.of("Таран", ChatFormatting.DARK_RED));
                    s.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE));
                    var hp = s.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                    if (hp != null) { hp.setBaseValue(hp.getBaseValue() + 40); s.setHealth(s.getMaxHealth()); }
                } else if (i % 4 == 3 && !wing && !t.ranged()) {
                    s.addTag(SAPPER_TAG);
                    s.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
                }
                spawned++;
            }
        }
        // осадная башня впереди армии: везёт воинов к стене
        if (r.bribed) {
            r.bribed = false;
            data.setDirty();
        }
        if (n >= 14 && !r.siegeSabotaged) {
            double fwd = 14;
            int tx = sx - (int) (dx / len * 8), tz = sz - (int) (dz / len * 8);
            int ty = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz);
            SiegeTowerEntity tw = KingdomModule.SIEGE_TOWER.get().create(ow);
            if (tw != null) {
                tw.setRealm(r.id);
                tw.setCarried(4 + Math.min(4, n / 8));
                tw.moveTo(tx + 0.5, ty, tz + 0.5, (float) Math.toDegrees(Math.atan2(dx, -dz)), 0f);
                tw.setCustomName(Text.of("Осадная башня «" + r.name + "»", ChatFormatting.DARK_RED));
                ow.addFreshEntity(tw);
            }
        }
        // осадные катапульты позади армии
        int cats = r.siegeSabotaged ? 0 : n >= 16 ? 2 : n >= 10 ? 1 : 0;
        if (r.siegeSabotaged) { r.siegeSabotaged = false; data.setDirty(); }
        for (int k = 0; k < cats; k++) {
            double back = 12 + k * 3;
            double side = (k == 0 ? -1 : 1) * 6;
            int cx = sx + (int) (dx / len * back - dz / len * side), cz = sz + (int) (dz / len * back + dx / len * side);
            int cy = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            CatapultEntity cat = KingdomModule.CATAPULT.get().create(ow);
            if (cat != null) {
                cat.setRealm(r.id);
                cat.moveTo(cx + 0.5, cy, cz + 0.5, (float) Math.toDegrees(Math.atan2(dx, -dz)), 0f);
                cat.setCustomName(Text.of("Катапульта «" + r.name + "»", ChatFormatting.DARK_RED));
                ow.addFreshEntity(cat);
            }
        }
        if (n >= 10) buildCamp(ow, r, sx + (int) (dx / len * 20), sz + (int) (dz / len * 20));
        r.armiesSent++;
        r.armyStart = ow.getGameTime();
        r.armyTimer = ow.getGameTime() + 24000;
        target.war = true;
        data.setDirty();
        owner.sendSystemMessage(Text.of("⚔ Армия «" + r.name + "» (" + spawned + " воинов) подошла к городу «" + target.name + "» со стороны своей земли!",
                ChatFormatting.RED, ChatFormatting.BOLD));
        return spawned;
    }

    /** Лагерь осаждающих: костёр с запасами за линией армии. Разорите его — армия потеряет боевой дух. */
    /** Блоки лагеря хранятся в самом королевстве (переживают перезапуск). */
    private static java.util.List<BlockPos> campBlocks(Realm r) {
        java.util.List<BlockPos> out = new ArrayList<>();
        for (long l : r.camp) out.add(BlockPos.of(l));
        return out;
    }

    private static void buildCamp(ServerLevel ow, Realm r, int x, int z) {
        if (!ow.hasChunkAt(new BlockPos(x, 64, z))) return;
        java.util.List<BlockPos> blocks = new ArrayList<>();
        int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos fire = new BlockPos(x, y, z);
        if (!ow.getBlockState(fire).canBeReplaced() || !ow.getBlockState(fire.below()).isSolid()) return;
        ow.setBlockAndUpdate(fire, net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState());
        blocks.add(fire);
        int[][] side = {{2, 0}, {-2, 0}, {0, 2}};
        for (int[] d : side) {
            BlockPos b = new BlockPos(x + d[0], ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + d[0], z + d[1]), z + d[1]);
            if (ow.getBlockState(b).canBeReplaced() && ow.getBlockState(b.below()).isSolid()) {
                ow.setBlockAndUpdate(b, net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState());
                blocks.add(b);
            }
        }
        r.camp.clear();
        for (BlockPos b : blocks) r.camp.add(b.asLong());
    }

    /** Где стоит костёр вражеского лагеря (null — лагеря нет). */
    public static BlockPos campPos(Realm r) {
        return r.camp.isEmpty() ? null : BlockPos.of(r.camp.get(0));
    }

    private static void removeCamp(ServerLevel ow, Realm r) {
        java.util.List<BlockPos> blocks = campBlocks(r);
        r.camp.clear();
        if (blocks.isEmpty()) return;
        for (BlockPos b : blocks) {
            if (!ow.hasChunkAt(b)) continue; // выгруженный лагерь не подгружаем ради сноса
            var st = ow.getBlockState(b);
            if (st.is(net.minecraft.world.level.block.Blocks.CAMPFIRE) || st.is(net.minecraft.world.level.block.Blocks.HAY_BLOCK)) ow.removeBlock(b, false);
        }
    }

    /** Раз в проверку: бойцы у костра лечатся; если костёр разорён — армия теряет дух. */
    private static void campTick(ServerLevel ow, KingdomData data, Realm r, List<SoldierEntity> army) {
        java.util.List<BlockPos> blocks = campBlocks(r);
        if (blocks.isEmpty()) return;
        BlockPos fire = blocks.get(0);
        if (!ow.isLoaded(fire)) return;
        if (!ow.getBlockState(fire).is(net.minecraft.world.level.block.Blocks.CAMPFIRE)) {
            removeCamp(ow, r);
            for (SoldierEntity s : army) s.addMorale(-35f);
            City target = data.nearestOwned(r.warOwner, r.capital());
            if (target != null) target.glory += 3;
            ServerPlayer owner = r.warOwner == null ? null : ow.getServer().getPlayerList().getPlayer(r.warOwner);
            if (owner != null) Text.good(owner, "Обоз «" + r.name + "» разорён! Боевой дух вражеской армии упал, слава города +3.");
            data.setDirty();
            return;
        }
        for (SoldierEntity s : army) if (s.blockPosition().closerThan(fire, 8)) s.heal(2f);
    }

    private static final java.util.Map<UUID, Integer> SACK = new java.util.HashMap<>();

    /** Враг у ратуши три проверки подряд (~15 с) — город разграблен: потери казны и склада, слава падает. */
    private static boolean checkSack(ServerLevel ow, KingdomData data, Realm r, City target, List<SoldierEntity> army) {
        int near = 0;
        for (SoldierEntity s : army) if (s.blockPosition().closerThan(target.hall, 7)) near++;
        if (near < 4) { SACK.remove(r.id); return false; }
        int n = SACK.merge(r.id, 1, Integer::sum);
        if (n < 3) return false;
        SACK.remove(r.id);
        int lost = target.treasury * 40 / 100;
        target.treasury -= lost;
        StringBuilder sb = new StringBuilder();
        for (Resource res : Resource.values()) {
            int have = target.stock(res);
            int take = have * 30 / 100;
            if (take > 0) { target.stock.put(res, have - take); sb.append(res.title).append(" -").append(take).append("; "); }
        }
        target.glory = Math.max(0, target.glory - 10);
        removeCamp(ow, r);
        java.util.List<String> ruinedNames = target.ruinSome(ow.random, 2);
        if (!ruinedNames.isEmpty()) sb.append("разрушено: ").append(String.join(", ", ruinedNames)).append(" (починка: /regnum repair); ");
        target.war = false;
        target.aided = false;
        r.armiesLost = 0;
        for (SoldierEntity s : army) s.discard();
        r.armyTimer = ow.getGameTime() + 24000;
        ServerPlayer owner = r.warOwner == null ? null : ow.getServer().getPlayerList().getPlayer(r.warOwner);
        if (owner != null) {
            Text.bad(owner, "⚔ Ратуша «" + target.name + "» пала! Враг разграбил город: казна −" + lost + ", " + sb + "слава −10. Новая армия придёт позже — укрепитесь.");
            com.alkimor.regnum.survival.Skills.chronicle(owner, "Город «" + target.name + "» разграблен войсками «" + r.name + "»");
        }
        data.setDirty();
        return true;
    }

    /** Победа: три разбитых войска. Итог зависит от выбранной цели войны. */
    private static void victory(ServerLevel ow, KingdomData data, Realm r, City target) {
        int goal = r.warGoal;
        UUID owner = r.warOwner;
        ServerPlayer p = owner == null ? null : ow.getServer().getPlayerList().getPlayer(owner);
        String result;
        if (goal == 1 && owner != null && playerPower(data, owner) >= r.strength) {
            endWar(ow, data, r, "Правитель «" + r.name + "» склоняет голову: королевство присягает вам.");
            r.state = Realm.VASSAL;
            r.liege = owner;
            r.relation = -5;
            result = "вассалитет";
        } else if (goal == 2) {
            r.relation = -20;
            target.glory += 25;
            if (p != null) com.alkimor.regnum.survival.Skills.addHonor(p, 10, "победа над «" + r.name + "»");
            endWar(ow, data, r, "«" + r.name + "» просит мира. Слава вашего города растёт (+25), трофеи — в летописи.");
            result = "слава";
        } else {
            int tribute = 120 + r.strength * 15;
            target.treasury += tribute;
            r.relation = -20;
            endWar(ow, data, r, "«" + r.name + "» платит откуп: +" + tribute + " монет в казну «" + target.name + "».");
            result = "дань";
        }
        if (p != null) com.alkimor.regnum.survival.Skills.chronicle(p, "Победа над «" + r.name + "» (" + result + ")");
        data.setDirty();
    }

    private static void declareWithGoal(ServerPlayer p, KingdomData data, Realm r, int goal) {
        r.warGoal = goal;
        r.armiesLost = 0;
        Diplomacy.declare(p.serverLevel(), data, r, p);
        if (r.state == Realm.WAR) {
            Text.gold(p, "Цель войны: " + Realm.GOALS[goal] + ". Победа — разбить три войска «" + r.name + "». Мир: /regnum realm peace <id> рядом с их правителем.");
            if (goal == 1) Text.info(p, "Для вассалитета нужна армия не меньше " + r.strength + " бойцов в момент победы; иначе вы получите дань.");
        }
    }

    private static void endWar(ServerLevel ow, KingdomData data, Realm r, String msg) {
        r.state = Realm.PEACE;
        r.warGoal = 0;
        r.armiesLost = 0;
        Diplomacy.setTruce(ow, r, 3);
        r.armyTimer = 0;
        removeCamp(ow, r);
        for (SoldierEntity s : army(ow, r)) s.discard();
        for (City c : data.all()) {
            if (r.warOwner != null && c.owner.equals(r.warOwner)) {
                c.war = false;
                c.aided = false;
                Walls.startRepair(data, c);
            }
        }
        data.setDirty();
        ServerPlayer owner = r.warOwner == null ? null : ow.getServer().getPlayerList().getPlayer(r.warOwner);
        if (owner != null) Text.gold(owner, msg);
    }

    /** Игрок убил солдата королевства в мирное время — отношения портятся. */
    public static void onRealmSoldierKilled(ServerLevel sl, Realm r, Entity killer) {
        if (!(killer instanceof ServerPlayer p) || r.state != Realm.PEACE) return;
        r.relation = Math.max(-100, r.relation - 15);
        r.note("вы убили их солдата в мирное время");
        KingdomData data = KingdomData.get(sl.getServer());
        data.setDirty();
        if (r.relation <= -60) declareWar(sl, data, r, p);
        else Text.bad(p, "«" + r.name + "» не прощает убийства своих людей. Отношения: " + r.relation);
    }

    public static void declareWar(ServerLevel ow, KingdomData data, Realm r, ServerPlayer p) {
        r.offer = 0;
        r.ally = false;
        r.state = Realm.WAR;
        r.relation = -100;
        r.warOwner = p.getUUID();
        r.liege = null;
        r.armyTimer = ow.getGameTime() + 3600;
        data.setDirty();
        p.sendSystemMessage(Text.of("⚔ «" + r.name + "» объявляет вам войну! Первое войско придёт через несколько минут.", ChatFormatting.RED, ChatFormatting.BOLD));
    }

    // ------------------------------------------------------------------ диалог и дипломатия

    public static void talk(ServerPlayer p, SoldierEntity ruler) {
        Realm r = realmOf(ruler, ruler.realmId());
        if (r == null) return;
        if (r.known < 2) { r.known = 2; KingdomData.get(p.server).setDirty(); }
        int idx = indexOf(KingdomData.get(p.server), r);
        p.sendSystemMessage(Text.of("— " + ruler.getName().getString() + " —", ChatFormatting.GOLD));
        p.sendSystemMessage(Text.of("  Отношения: " + r.relation + " (" + r.stateTitle() + "). Гарнизон: " + r.strength + ". Договор о торговле: "
                + (r.trade ? "есть" : "нет") + ".", ChatFormatting.GRAY));
        var line = Component0.empty();
        line.append(Text.button("Подарок 20", "/regnum realm gift " + idx + " 20", "Из казны ближайшего города; +5 к отношениям"));
        line.append(Component0.sp());
        line.append(Text.button("Торговый договор", "/regnum realm treaty " + idx, "Нужны отношения от 25 и 30 в казне"));
        line.append(Component0.sp());
        line.append(Text.button("Вассалитет", "/regnum realm vassal " + idx, "Нужны отношения от 50 и армия не слабее их гарнизона"));
        p.sendSystemMessage(line);
        var line2 = Component0.empty();
        line2.append(Text.button("Объявить войну", "/regnum realm war " + idx, "Они пошлют войско на ваш город"));
        line2.append(Component0.sp());
        line2.append(Text.button("Просить мира", "/regnum realm peace " + idx, "Мир после поражений или за 50 из казны"));
        p.sendSystemMessage(line2);
        var line3 = Component0.empty();
        line3.append(Text.button("Тайная служба", "/regnum spy list", "Агенты, разведка и диверсии против этого и других королевств"));
        p.sendSystemMessage(line3);
    }

    private static final class Component0 {
        static net.minecraft.network.chat.MutableComponent empty() {
            return net.minecraft.network.chat.Component.empty();
        }

        static net.minecraft.network.chat.Component sp() {
            return net.minecraft.network.chat.Component.literal("  ");
        }
    }

    static int indexOf(KingdomData data, Realm r) {
        int i = 1;
        for (Realm o : data.realms()) {
            if (o == r) return i;
            i++;
        }
        return 0;
    }

    static Realm byIndex(KingdomData data, int idx) {
        int i = 1;
        for (Realm o : data.realms()) if (i++ == idx) return o;
        return null;
    }

    private static boolean rulerNear(ServerPlayer p, Realm r) {
        for (SoldierEntity s : p.serverLevel().getEntitiesOfClass(SoldierEntity.class, p.getBoundingBox().inflate(12),
                s -> s.getTags().contains(RULER_TAG) && r.id.equals(s.realmId()))) return true;
        Text.bad(p, "Для этого нужно быть рядом с правителем «" + r.name + "».");
        return false;
    }

    static City cityOf(ServerPlayer p, KingdomData data) {
        City c = data.nearestOwned(p.getUUID(), p.blockPosition());
        if (c == null) Text.bad(p, "У вас нет города, казна которого могла бы участвовать.");
        return c;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("realm")
                .then(Commands.literal("list").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Text.gold(p, "— Соседние королевства —");
                    int i = 1;
                    for (Realm r : data.realms()) {
                        if (r.known == 0 && !p.hasPermissions(2)) { i++; continue; }
                        double dx = r.x - p.getX(), dz = r.z - p.getZ();
                        String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "восток" : "запад") : (dz > 0 ? "юг" : "север");
                        if (r.known == 1 && !p.hasPermissions(2)) {
                            Text.info(p, "  " + i++ + ". " + r.name + " — лишь слухи, на " + dir + ". Пошлите разведчика: /regnum realm scout <номер>");
                            continue;
                        }
                        Text.info(p, "  " + i++ + ". " + r.name + " (" + Culture.byId(r.culture).title + ", " + (r.known >= 2 ? r.temper() : "характер неизвестен") + ") — " + r.stateTitle() + ", отношения " + r.relation
                                + ", ~" + (int) Math.hypot(dx, dz) + " блоков на " + dir);
                    }
                    return 1;
                }))
                .then(Commands.literal("sortie").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    for (Realm r : data.realms()) {
                        if (r.state != Realm.WAR || r.warOwner == null || !r.warOwner.equals(p.getUUID())) continue;
                        BlockPos camp = campPos(r);
                        if (camp == null) continue;
                        double dx = camp.getX() - p.getX(), dz = camp.getZ() - p.getZ();
                        float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
                        ArmyCommands.issue(p, 0, Order.MOVE, net.minecraft.world.phys.Vec3.atBottomCenterOf(camp), yaw, Formation.WEDGE, null);
                        Text.gold(p, "Вылазка к обозу «" + r.name + "» (" + camp.getX() + ", " + camp.getZ() + "): разорите костёр! Отозвать: /regnum rally.");
                        return 1;
                    }
                    Text.info(p, "Вражеского лагеря сейчас нет: он ставится вместе с большой армией (10+ бойцов).");
                    return 0;
                }))
                .then(Commands.literal("why").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Realm r = byIndex(KingdomData.get(p.server), IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null || r.known == 0) return 0;
                    Text.gold(p, "«" + r.name + "»: отношения " + r.relation + ", правитель " + r.temper() + ".");
                    long day = p.serverLevel().getDayTime() / 24000L;
                    if (r.lastSpyDay >= 0) {
                        long age = Math.max(0, day - r.lastSpyDay);
                        Text.info(p, "  Сведения о силе королевства получены " + (age == 0 ? "сегодня" : age + " дн. назад") + (age > 5 ? " — они устарели, пошлите агента снова." : "."));
                    } else if (r.known >= 2) {
                        Text.info(p, "  Подробной разведки не было: оценки приблизительны.");
                    }
                    if (r.history.isEmpty()) Text.info(p, "  Ничего заметного между вами не было.");
                    for (String h : r.history) Text.info(p, "  • " + h);
                    return 1;
                })))
                .then(Commands.literal("scout").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    City c = cityOf(p, data);
                    if (r == null || c == null || r.known == 0) return 0;
                    if (r.known >= 2) { Text.info(p, "«" + r.name + "» вам уже известно."); return 1; }
                    if (c.treasury < 40) { Text.bad(p, "Разведчикам нужно 40 монет."); return 0; }
                    c.treasury -= 40;
                    r.known = 2;
                    data.setDirty();
                    Text.good(p, "Разведчики вернулись: «" + r.name + "» — " + Culture.byId(r.culture).title + ", правитель " + r.ruler + ", гарнизон ~" + r.strength + ".");
                    return 1;
                })))
                .then(Commands.literal("gift").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null || !rulerNear(p, r) || r.state == Realm.WAR) return 0;
                    City c = cityOf(p, data);
                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                    if (c == null) return 0;
                    if (c.treasury < amount) {
                        Text.bad(p, "В казне «" + c.name + "» только " + c.treasury + ".");
                        return 0;
                    }
                    c.treasury -= amount;
                    r.relation = Math.min(100, r.relation + Math.max(1, amount / 4));
                    r.note("вы отправили дар " + amount);
                    data.setDirty();
                    Text.good(p, "«" + r.name + "» принимает дар. Отношения: " + r.relation);
                    return 1;
                }))))
                .then(Commands.literal("treaty").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null || !rulerNear(p, r)) return 0;
                    City c = cityOf(p, data);
                    if (c == null) return 0;
                    if (r.state != Realm.PEACE || r.relation < 25) Text.bad(p, "Они не хотят торговать: нужны отношения от 25 и мир.");
                    else if (r.trade) Text.info(p, "Договор уже заключён.");
                    else if (c.treasury < 30) Text.bad(p, "Для договора нужно 30 в казне.");
                    else {
                        c.treasury -= 30;
                        r.trade = true;
                        data.setDirty();
                        Text.good(p, "Торговый договор с «" + r.name + "» заключён: ежедневный доход растёт.");
                    }
                    return 1;
                })))
                .then(Commands.literal("alliance").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null || !rulerNear(p, r)) return 0;
                    City c = cityOf(p, data);
                    if (c == null) return 0;
                    if (r.state != Realm.PEACE || !r.trade || r.relation < 70) Text.bad(p, "Союз возможен при мире, торговом договоре и отношениях от 70.");
                    else if (r.ally) Text.info(p, "Вы уже союзники.");
                    else if (c.treasury < 120) Text.bad(p, "Для союза нужно 120 в казне.");
                    else {
                        c.treasury -= 120;
                        r.ally = true;
                        data.setDirty();
                        Text.gold(p, "«" + r.name + "» стал вашим союзником: пришлёт подмогу, когда на вас пойдут войной.");
                    }
                    return 1;
                })))
                .then(Commands.literal("vassal").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null || !rulerNear(p, r)) return 0;
                    if (r.state != Realm.PEACE) Text.bad(p, "Сейчас вассалитет невозможен.");
                    else if (r.relation < 50) Text.bad(p, "Они не доверяют вам: нужны отношения от 50.");
                    else if (playerPower(data, p.getUUID()) < r.strength) Text.bad(p, "У вас слабая армия: нужно не меньше " + r.strength + " солдат.");
                    else {
                        r.state = Realm.VASSAL;
                        r.liege = p.getUUID();
                        data.setDirty();
                        Text.gold(p, "«" + r.name + "» присягает вам. Они будут платить дань.");
                    }
                    return 1;
                })))
                .then(Commands.literal("war").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null) return 0;
                    if (r.state == Realm.WAR) Text.info(p, "Война уже идёт.");
                    else declareWithGoal(p, data, r, 0);
                    return 1;
                }).then(Commands.argument("goal", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null) return 0;
                    String g = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "goal").toLowerCase();
                    int goal = g.startsWith("vas") || g.startsWith("вас") ? 1 : g.startsWith("gl") || g.startsWith("сл") ? 2 : 0;
                    if (r.state == Realm.WAR) Text.info(p, "Война уже идёт.");
                    else declareWithGoal(p, data, r, goal);
                    return 1;
                }))))
                .then(Commands.literal("peace").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null || !rulerNear(p, r)) return 0;
                    if (r.state != Realm.WAR) {
                        Text.info(p, "Войны нет.");
                        return 1;
                    }
                    City c = cityOf(p, data);
                    if (c == null) return 0;
                    if (r.armiesLost < 1) {
                        if (c.treasury < 50) {
                            Text.bad(p, "Они ещё не проиграли ни одного боя: мир стоит 50 в казне.");
                            return 0;
                        }
                        c.treasury -= 50;
                    }
                    r.relation = -10;
                    endWar(p.serverLevel(), data, r, "Мир с «" + r.name + "» заключён.");
                    return 1;
                })))
                .then(Commands.literal("send").requires(s -> s.hasPermission(2)).then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    City c = data.nearestOwned(p.getUUID(), p.blockPosition());
                    if (r == null || c == null) return 0;
                    r.state = Realm.WAR;
                    r.warOwner = p.getUUID();
                    sendArmy(p.serverLevel(), data, r, c, p);
                    return 1;
                })))));
    }
}
