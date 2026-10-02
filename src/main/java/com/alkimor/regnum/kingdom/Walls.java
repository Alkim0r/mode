package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Строительство стен рабочими. Игрок размечает «Планом строителя» точки стены и ворота,
 * строитель города (житель в мастерской) возводит её слой за слоем за счёт казны.
 */
public final class Walls {
    private Walls() {}

    public static final String BUILDER_TAG = "regnum_builder";
    private static final int MAX_POINTS = 64, SPAWN_COST = 15;

    public static final class Draft {
        public final List<BlockPos> pts = new ArrayList<>();
        public final List<Boolean> gates = new ArrayList<>();
        public int style = WallStyle.WOOD.ordinal();
    }

    private static final Map<UUID, Draft> DRAFTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_PLACE = new HashMap<>();

    public static Draft draft(net.minecraft.world.entity.player.Player p) {
        return DRAFTS.computeIfAbsent(p.getUUID(), k -> new Draft());
    }

    // ------------------------------------------------------------------ работа с планом

    public static void addPoint(ServerPlayer p, BlockPos pos, boolean gate) {
        City c = Territory.cityAt(p.level(), pos);
        if (c == null || !Council.can(c, p.getUUID(), Council.Role.BUILD)) {
            Text.bad(p, "Стены размечаются только на земле вашего города.");
            return;
        }
        Draft d = draft(p);
        if (d.pts.size() >= MAX_POINTS) {
            Text.bad(p, "Слишком много точек. Подтвердите план или очистите его.");
            return;
        }
        d.pts.add(pos.immutable());
        d.gates.add(gate);
        p.displayClientMessage(Text.of((gate ? "Ворота" : "Точка") + " " + d.pts.size() + " · "
                + WallStyle.byOrdinal(d.style).title, ChatFormatting.GOLD), true);
        p.level().playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    public static void cycleStyle(ServerPlayer p) {
        Draft d = draft(p);
        WallStyle s;
        int guard = 0;
        do {
            d.style = (d.style + 1) % WallStyle.values().length;
            s = WallStyle.byOrdinal(d.style);
        } while (s.tech() != null && !Science.has(p.server, p.getUUID(), s.tech()) && ++guard < 8);
        p.sendSystemMessage(Text.of("Стена: " + s.title + ", высота " + s.height + ". ", ChatFormatting.GOLD).append(Text.of(s.desc, ChatFormatting.GRAY)));
    }

    public static void clearDraft(ServerPlayer p) {
        draft(p).pts.clear();
        draft(p).gates.clear();
        Text.info(p, "План стены очищен.");
    }

    public static void confirm(ServerPlayer p) {
        Draft d = draft(p);
        if (d.pts.size() < 2) {
            Text.info(p, "Отметьте хотя бы две точки (ПКМ по земле). С Shift точка становится воротами. Shift + ПКМ в воздух — сменить вид стены.");
            return;
        }
        KingdomData data = KingdomData.get(p.server);
        City c = Territory.cityAt(p.level(), d.pts.get(0));
        if (c == null || !Council.can(c, p.getUUID(), Council.Role.BUILD)) {
            Text.bad(p, "Стена должна начинаться на земле вашего города.");
            return;
        }
        if (c.count(BuildingType.BUILDER_HUT) == 0) {
            Text.bad(p, "Нужна «Мастерская строителя» в городе: без неё некому строить.");
            return;
        }
        if (c.wall != null) {
            Text.bad(p, "Строители уже заняты стеной (" + progress(c.wall) + "%). Отмена: /regnum wall cancel");
            return;
        }
        WallStyle st = WallStyle.byOrdinal(d.style);
        if (st.tech() != null && !Science.has(p.server, p.getUUID(), st.tech())) {
            Text.bad(p, st.title + " требует технологию «" + st.tech().title + "» (/regnum science).");
            return;
        }
        WallJob job;
        int blocks;
        if (st == WallStyle.CASTLE) {
            WallKit kit = WallKit.of(c.culture);
            List<int[]> ip = new ArrayList<>();
            for (BlockPos q : d.pts) ip.add(new int[]{q.getX(), q.getZ()});
            BlockPos f0 = d.pts.get(0), fl = d.pts.get(d.pts.size() - 1);
            boolean closed = d.pts.size() > 2 && Math.abs(f0.getX() - fl.getX()) + Math.abs(f0.getZ() - fl.getZ()) <= 8;
            if (closed) {
                ip.remove(ip.size() - 1);
                d.gates.remove(d.gates.size() - 1);
            }
            List<WallKit.Op> ops = kit.plan(ip, d.gates, closed);
            job = WallJob.planPieces(kit, st.ordinal(), ops);
            blocks = 0;
            for (WallKit.Op o : ops) blocks += Prefab.blocks(p.serverLevel(), o.id());
        } else {
            job = WallJob.plan(d.pts, d.gates, st);
            blocks = job.total() * st.height;
        }
        int coins = blocks / st.blocksPerCoin;
        c.wall = job;
        data.setDirty();
        d.pts.clear();
        d.gates.clear();
        p.sendSystemMessage(Text.of("⚒ Строители взялись за работу: " + st.title + ", " + job.total() + (st == WallStyle.CASTLE ? " кусков, около " : " клеток, около ")
                + blocks + " блоков. Расход казны: ~" + coins + " (в казне " + c.treasury + ").", ChatFormatting.GOLD));
    }

    public static int progress(WallJob j) {
        return j.total() == 0 ? 100 : 100 * j.cur() / j.total();
    }

    // ------------------------------------------------------------------ строительство

    public enum Step {PLACED, SKIPPED, WAIT, DONE}

    private static boolean empty(BlockState s) {
        return s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty();
    }

    /** Один шаг стройки: кладёт (или пропускает) следующий блок клетки. Без привязки к строителю — удобно проверять. */
    public static Step step(ServerLevel sl, City c, WallJob j) {
        if (j.done()) return Step.DONE;
        WallStyle st = WallStyle.byOrdinal(j.style);
        long cell = j.cells[j.cell];
        int x = BlockPos.getX(cell), z = BlockPos.getZ(cell);
        if (!sl.hasChunk(x >> 4, z >> 4)) return Step.WAIT;
        if (j.base == Integer.MIN_VALUE) {
            j.base = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            int depth = 0;
            if (!j.gate[j.cell]) {
                for (int d = 1; d <= 8; d++) {
                    if (empty(sl.getBlockState(new BlockPos(x, j.base - d, z)))) depth = d;
                    else break;
                }
            }
            j.layer = -depth;
        }
        int layer = j.layer;
        boolean gate = j.gate[j.cell];
        Block want;
        if (gate) {
            want = layer == 4 && st.height >= 5 ? Blocks.SPRUCE_PLANKS : null;
        } else if (layer < 0) {
            want = st == WallStyle.PALISADE || st == WallStyle.WOOD ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS;
        } else {
            want = st.block(c.culture, layer, x, z, j.cell);
        }
        BlockPos pos = new BlockPos(x, j.base + layer, z);
        boolean placed = false;
        if (want != null) {
            BlockState cur = sl.getBlockState(pos);
            boolean tree = cur.is(BlockTags.LEAVES) || cur.is(BlockTags.LOGS);
            if (cur.is(want)) {
                // уже стоит
            } else if (empty(cur) || tree) {
                if (j.coinCounter + 1 >= st.blocksPerCoin && c.treasury <= 0) return Step.WAIT;
                if (++j.coinCounter >= st.blocksPerCoin) {
                    j.coinCounter = 0;
                    c.treasury = Math.max(0, c.treasury - 1);
                }
                if (tree) sl.destroyBlock(pos, false);
                sl.setBlock(pos, want.defaultBlockState(), 3);
                placed = true;
            }
        } else if (gate && layer >= 0 && layer < 4) {
            BlockState cur = sl.getBlockState(pos);
            if (cur.is(BlockTags.LEAVES) || cur.is(BlockTags.LOGS) || cur.canBeReplaced()) sl.destroyBlock(pos, false);
        }
        // дальше
        j.layer++;
        if (j.layer >= st.height) {
            j.cell++;
            j.base = Integer.MIN_VALUE;
        }
        return placed ? Step.PLACED : Step.SKIPPED;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 2 != 0) return;
        ServerLevel ow = server.overworld();
        KingdomData data = KingdomData.get(server);
        for (City c : data.all()) {
            WallJob j = c.wall;
            if (j == null) continue;
            BlockPos hut = firstHut(c);
            if (hut == null) continue;
            if (!ow.isLoaded(hut)) continue;
            Villager b = builder(ow, c, j, hut);
            if (b == null) continue;
            if (j.done()) {
                finish(ow, data, c, j, b);
                continue;
            }
            work(ow, data, c, j, b);
        }
    }

    /** Чинит стену по сохранённому чертежу. Возвращает false, если чинить нечего или идёт другая стройка. */
    public static boolean startRepair(KingdomData data, City c) {
        if (c.wallPlan == null || c.wall != null) return false;
        c.wall = WallJob.load(c.wallPlan.save());
        c.wall.resetProgress();
        c.wall.repair = true;
        data.setDirty();
        return true;
    }

    private static BlockPos firstHut(City c) {
        for (var e : c.buildings.entrySet()) if (e.getValue() == BuildingType.BUILDER_HUT) return BlockPos.of(e.getKey());
        return null;
    }

    private static void tell(ServerLevel ow, City c, WallJob j, String msg) {
        long now = ow.getGameTime();
        if (now - j.lastMsg < 1200) return;
        j.lastMsg = now;
        ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
        if (owner != null) Text.info(owner, "⚒ " + c.name + ": " + msg);
    }

    private static Villager builder(ServerLevel ow, City c, WallJob j, BlockPos hut) {
        if (j.builder != null && ow.getEntity(j.builder) instanceof Villager v && v.isAlive()) return v;
        Villager pick = null;
        double bd = Double.MAX_VALUE;
        for (Villager v : ow.getEntitiesOfClass(Villager.class, new AABB(hut).inflate(40), v -> v.isAlive() && !v.isBaby())) {
            if (v.getTags().contains(BUILDER_TAG)) continue;
            double d = v.distanceToSqr(Vec3.atCenterOf(hut));
            if (d < bd) {
                bd = d;
                pick = v;
            }
        }
        if (pick == null) {
            if (c.treasury < SPAWN_COST) {
                tell(ow, c, j, "для строителя нужен житель или " + SPAWN_COST + " в казне, чтобы нанять.");
                return null;
            }
            pick = EntityType.VILLAGER.create(ow);
            if (pick == null) return null;
            c.treasury -= SPAWN_COST;
            pick.moveTo(hut.getX() + 0.5, hut.getY() + 1, hut.getZ() + 0.5);
            ow.addFreshEntity(pick);
        }
        pick.setVillagerData(pick.getVillagerData().setProfession(VillagerProfession.MASON).setLevel(2));
        pick.setNoAi(true);
        pick.addTag(BUILDER_TAG);
        pick.setCustomName(net.minecraft.network.chat.Component.literal("Строитель"));
        pick.setCustomNameVisible(true);
        j.builder = pick.getUUID();
        return pick;
    }

    private static void release(ServerLevel ow, WallJob j) {
        if (j.builder != null && ow.getEntity(j.builder) instanceof Villager v) {
            v.setNoAi(false);
            v.removeTag(BUILDER_TAG);
            v.setCustomName(null);
            v.setCustomNameVisible(false);
        }
    }

    private static void finish(ServerLevel ow, KingdomData data, City c, WallJob j, Villager b) {
        release(ow, j);
        c.wallPlan = WallJob.load(j.save());
        c.wallPlan.resetProgress();
        c.wallPlan.repair = false;
        c.wall = null;
        data.setDirty();
        ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
        if (owner != null) {
            owner.sendSystemMessage(Text.of("⚒ Стена города «" + c.name + "» достроена.", ChatFormatting.GREEN));
            ow.playSound(null, owner.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.0f);
        }
    }

    private static void work(ServerLevel ow, KingdomData data, City c, WallJob j, Villager b) {
        long cell = j.pieces() ? 0 : j.cells[j.cell];
        int x = j.pieces() ? j.ox[j.op] : BlockPos.getX(cell), z = j.pieces() ? j.oz[j.op] : BlockPos.getZ(cell);
        // встать рядом, со стороны города
        double hx = c.hall.getX() - x, hz = c.hall.getZ() - z;
        double hl = Math.max(1.0, Math.hypot(hx, hz));
        double tx = x + 0.5 + hx / hl * 2.5, tz = z + 0.5 + hz / hl * 2.5;
        double ty = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(tx), (int) Math.floor(tz));
        Vec3 pos = b.position();
        double dx = tx - pos.x, dz = tz - pos.z;
        double d = Math.hypot(dx, dz);
        if (d > 48) {
            b.teleportTo(tx, ty, tz);
        } else if (d > 0.5) {
            double stepLen = Math.min(0.45, d);
            double nx = pos.x + dx / d * stepLen, nz = pos.z + dz / d * stepLen;
            double ny = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(nx), (int) Math.floor(nz));
            b.setPos(nx, ny, nz);
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(-(x + 0.5 - b.getX()), z + 0.5 - b.getZ())));
        b.setYRot(yaw);
        b.setYHeadRot(yaw);
        b.yBodyRot = yaw;
        if (d > (j.pieces() ? 9 : 4.5)) return;
        if (j.pieces()) {
            workPiece(ow, data, c, j, b);
            return;
        }
        long now = ow.getGameTime();
        int huts = Math.max(1, c.count(BuildingType.BUILDER_HUT));
        int interval = Math.max(2, 14 - 2 * c.level - 2 * (huts - 1) - c.ownerSteward / 25); // навык управления ускоряет стройку
        Long last = LAST_PLACE.get(c.id);
        if (last != null && now - last < interval) return;
        j.paused = false;
        for (int i = 0; i < 24; i++) {
            Step s = step(ow, c, j);
            if (s == Step.PLACED) {
                LAST_PLACE.put(c.id, now);
                b.swing(InteractionHand.MAIN_HAND);
                ow.playSound(null, b.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.NEUTRAL, 0.5f, 0.9f + ow.random.nextFloat() * 0.2f);
                ow.sendParticles(ParticleTypes.CLOUD, x + 0.5, ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 0.2, z + 0.5, 1, 0.2, 0.1, 0.2, 0.01);
                data.setDirty();
                return;
            }
            if (s == Step.WAIT) {
                j.paused = true;
                tell(ow, c, j, "работа стоит: в казне нет средств или участок не загружен.");
                return;
            }
            if (s == Step.DONE) return;
            if (j.base == Integer.MIN_VALUE) {
                data.setDirty();
                return; // перешли к следующей клетке — идём к ней
            }
        }
    }

    /** Постройка готового куска: по паре слоёв за шаг, оплата кусочком при начале. */
    private static void workPiece(ServerLevel ow, KingdomData data, City c, WallJob j, Villager b) {
        long now = ow.getGameTime();
        int huts = Math.max(1, c.count(BuildingType.BUILDER_HUT));
        int interval = Math.max(2, 10 - c.level - (huts - 1) - c.ownerSteward / 40);
        Long last = LAST_PLACE.get(c.id);
        if (last != null && now - last < interval) return;
        String id = j.oid[j.op];
        if (!Prefab.exists(ow, id)) {
            j.op++;
            j.opLayer = 0;
            data.setDirty();
            return;
        }
        int x = j.ox[j.op], z = j.oz[j.op];
        if (j.opLayer == 0) {
            int cost = Math.max(1, Prefab.blocks(ow, id) / (j.repair ? 400 : WallStyle.CASTLE.blocksPerCoin));
            if (c.treasury < cost) {
                j.paused = true;
                tell(ow, c, j, "для следующего куска нужно " + cost + " в казне.");
                return;
            }
            c.treasury -= cost;
            var size = Prefab.size(ow, id, net.minecraft.world.level.block.Rotation.NONE);
            int hx = Math.max(1, size.getX() / 2), hz = Math.max(1, size.getZ() / 2);
            if (j.oq[j.op] % 2 != 0) {
                int t = hx;
                hx = hz;
                hz = t;
            }
            j.ogy[j.op] = Prefab.groundAt(ow, x, z, hx, hz);
        }
        j.paused = false;
        int gy = j.ogy[j.op];
        int total = Prefab.height(ow, id);
        int to = Math.min(total, j.opLayer + 2);
        Prefab.place(ow, id, x, gy, z, Prefab.rotFor(j.oq[j.op]), WallKit.of(c.culture).foundation, j.opLayer, to);
        j.opLayer = to;
        LAST_PLACE.put(c.id, now);
        b.swing(InteractionHand.MAIN_HAND);
        ow.playSound(null, b.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.NEUTRAL, 0.7f, 0.8f + ow.random.nextFloat() * 0.2f);
        ow.sendParticles(ParticleTypes.CLOUD, x + 0.5, gy + to + 0.5, z + 0.5, 6, 2.5, 0.3, 2.5, 0.01);
        if (to >= total) {
            j.op++;
            j.opLayer = 0;
        }
        data.setDirty();
    }

    // ------------------------------------------------------------------ предпросмотр и команды

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        if (!p.getMainHandItem().is(com.alkimor.regnum.kingdom.KingdomModule.BUILDER_PLAN.get())) return;
        Draft d = DRAFTS.get(p.getUUID());
        if (d == null || d.pts.isEmpty() || !(p.level() instanceof ServerLevel sl)) return;
        int budget = 160;
        for (int i = 0; i < d.pts.size() && budget > 0; i++) {
            BlockPos a = d.pts.get(i);
            sl.sendParticles(p, d.gates.get(i) ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD, true, a.getX() + 0.5, a.getY() + 1.3, a.getZ() + 0.5, 3, 0.1, 0.3, 0.1, 0);
            if (i + 1 < d.pts.size()) {
                BlockPos b = d.pts.get(i + 1);
                int n = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
                for (int k = 1; k < n && budget > 0; k += 2) {
                    double t = (double) k / n;
                    double px = a.getX() + (b.getX() - a.getX()) * t, pz = a.getZ() + (b.getZ() - a.getZ()) * t;
                    int py = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) px, (int) pz);
                    sl.sendParticles(p, ParticleTypes.CRIT, true, px + 0.5, py + 0.4, pz + 0.5, 1, 0, 0, 0, 0);
                    budget--;
                }
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("wall")
                .then(Commands.literal("cancel").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    City c = data.nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null || c.wall == null) {
                        clearDraft(p);
                        return 1;
                    }
                    release(p.serverLevel(), c.wall);
                    c.wall = null;
                    data.setDirty();
                    clearDraft(p);
                    Text.good(p, "Стройка стены отменена. Уже возведённое остаётся.");
                    return 1;
                }))
                .then(Commands.literal("repair").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    City c = data.nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null || !startRepair(data, c)) {
                        Text.bad(p, "Чинить нечего: у города нет построенной стены или стройка уже идёт.");
                        return 0;
                    }
                    Text.good(p, "Строитель осмотрит стену и залатает бреши за счёт казны.");
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null || c.wall == null) Text.info(p, "Сейчас ничего не строится.");
                    else Text.info(p, "Стена «" + WallStyle.byOrdinal(c.wall.style).title + "»: " + progress(c.wall) + "%"
                            + (c.wall.paused ? " (работа стоит)" : ""));
                    return 1;
                }))));
    }
}
