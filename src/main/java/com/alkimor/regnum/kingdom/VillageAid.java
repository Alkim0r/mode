package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Малое событие «Деревня в беде»: разбойники грабят ближайшую деревню, игрок может прийти на помощь. */
public final class VillageAid {
    private VillageAid() {}

    public static final String TAG = "regnum_village_raider";

    private static final class Raid {
        final List<UUID> ids = new ArrayList<>();
        BlockPos village;
        long expires;
    }

    private static final Map<UUID, Raid> RAIDS = new HashMap<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 100 != 0) return;
        ServerLevel ow = server.overworld();
        long now = ow.getGameTime();
        for (ServerPlayer p : ow.players()) {
            Raid raid = RAIDS.get(p.getUUID());
            if (raid != null) {
                check(ow, p, raid, now);
                continue;
            }
            if (p.isCreative() || p.isSpectator() || now < 24000L * 3) continue;
            long next = NEXT.computeIfAbsent(p.getUUID(), k -> now + 24000L + ow.random.nextInt(24000));
            if (now < next) continue;
            NEXT.put(p.getUUID(), now + 6000);
            if (start(ow, p, now)) NEXT.put(p.getUUID(), now + 24000L * (2 + ow.random.nextInt(3)));
        }
    }

    public static boolean start(ServerLevel ow, ServerPlayer p, long now) {
        VassalVillage mine = Villages.nearest(KingdomData.get(ow.getServer()), p);
        BlockPos v = mine != null && ow.random.nextFloat() < 0.7f ? mine.pos() : ow.findNearestMapStructure(StructureTags.VILLAGE, p.blockPosition(), 14, false);
        if (v == null) return false;
        double dist = Math.hypot(v.getX() - p.getX(), v.getZ() - p.getZ());
        if (dist < 40 || dist > 260) return false;
        Raid raid = new Raid();
        raid.village = v;
        raid.expires = now + 20L * 60 * 8;
        ow.getChunk(v.getX() >> 4, v.getZ() >> 4);
        int n = 4 + ow.random.nextInt(3);
        if (mine != null && mine.pos().equals(v)) {
            n = Math.max(2, n - mine.palisade);
            // гарнизон выходит навстречу: ополченцы деревни
            for (int i = 0; i < mine.garrison; i++) {
                int gx = v.getX() + ow.random.nextInt(7) - 3, gz = v.getZ() + ow.random.nextInt(7) - 3;
                int gy = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz);
                SoldierEntity g = KingdomModule.SOLDIER.get().create(ow);
                if (g == null) continue;
                g.setup(SoldierType.MILITIA, mine.owner, null, 4);
                g.moveTo(gx + 0.5, gy, gz + 0.5);
                g.command(Order.HOLD, g.position(), 0f, Formation.LOOSE, i, mine.garrison, null);
                g.addTag("regnum_levy");
                ow.addFreshEntity(g);
            }
        }
        for (int i = 0; i <= n; i++) {
            int x = v.getX() + ow.random.nextInt(11) - 5, z = v.getZ() + ow.random.nextInt(11) - 5;
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BanditEntity b = KingdomModule.BANDIT.get().create(ow);
            if (b == null) continue;
            b.moveTo(x + 0.5, y, z + 0.5, ow.random.nextFloat() * 360f, 0f);
            b.finalizeSpawn(ow, ow.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null);
            b.setup(i == 0 ? BanditEntity.CAPTAIN : ow.random.nextFloat() < 0.4f ? BanditEntity.ARCHER : BanditEntity.THUG);
            b.addTag(TAG);
            b.setPersistenceRequired();
            b.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 120, 0));
            ow.addFreshEntity(b);
            raid.ids.add(b.getUUID());
        }
        if (raid.ids.isEmpty()) return false;
        RAIDS.put(p.getUUID(), raid);
        double dx = v.getX() - p.getX(), dz = v.getZ() - p.getZ();
        String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "востоке" : "западе") : (dz > 0 ? "юге" : "севере");
        p.sendSystemMessage(Text.of("⚑ Гонец: разбойники грабят деревню на " + dir + " (~" + (int) dist + " блоков, X " + v.getX() + " Z " + v.getZ()
                + "). Успейте за 8 минут!", ChatFormatting.GOLD, ChatFormatting.BOLD));
        return true;
    }

    private static void check(ServerLevel ow, ServerPlayer p, Raid raid, long now) {
        int alive = 0;
        for (UUID id : raid.ids) {
            Entity e = ow.getEntity(id);
            if (e != null && e.isAlive()) alive++;
        }
        if (alive == 0) {
            RAIDS.remove(p.getUUID());
            int gold = 3 + raid.ids.size() * 2;
            p.getInventory().add(new ItemStack(Items.GOLD_INGOT, gold));
            p.getInventory().add(new ItemStack(Items.EMERALD, 2 + ow.random.nextInt(4)));
            KingdomData data = KingdomData.get(ow.getServer());
            for (City c : data.ownedBy(p.getUUID())) {
                c.glory += 15;
                c.treasury += 20;
                break;
            }
            data.setDirty();
            Villages.onRaidOutcome(data, raid.village, true, p);
            p.sendSystemMessage(Text.of("Деревня спасена! Крестьяне отдали вам " + gold + " золота и изумруды. Слава города растёт.", ChatFormatting.GREEN));
        } else if (now > raid.expires) {
            RAIDS.remove(p.getUUID());
            for (UUID id : raid.ids) {
                Entity e = ow.getEntity(id);
                if (e != null) e.discard();
            }
            Villages.onRaidOutcome(KingdomData.get(ow.getServer()), raid.village, false, p);
            p.sendSystemMessage(Text.of("Вы опоздали: разбойники разграбили деревню и ушли.", ChatFormatting.RED));
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("event")
                .then(Commands.literal("village").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    RAIDS.remove(p.getUUID());
                    boolean ok = start(p.serverLevel(), p, p.serverLevel().getGameTime());
                    if (!ok) Text.bad(p, "Подходящая деревня в 40–260 блоках не найдена.");
                    return ok ? 1 : 0;
                }))));
    }
}
