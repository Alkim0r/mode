package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Ежедневная экономика городов и набеги. */
public final class KingdomManager {
    private KingdomManager() {}

    private static final Map<UUID, ServerBossEvent> RAID_BARS = new HashMap<>();
    private static final int WAVE_TIMEOUT = 20 * 60 * 4;

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0 || !RegnumConfig.KINGDOM_ENABLED.get()) return;
        ServerLevel ow = server.overworld();
        KingdomData data = KingdomData.get(server);
        long time = ow.getDayTime();
        long day = time / 24000L;
        long tod = time % 24000L;

        boolean trainingTick = server.getTickCount() % 600 == 0;
        for (City c : new ArrayList<>(data.all())) {
            if (trainingTick) train(ow, data, c);
            if (c.lastEconomyDay < 0) c.lastEconomyDay = day;
            if (c.nextRaidDay < 0) c.nextRaidDay = day + 2;
            if (day > c.lastEconomyDay) {
                c.lastEconomyDay = day;
                var rulerOn = server.getPlayerList().getPlayer(c.owner);
                if (rulerOn != null) {
                    if (c.absentDays >= 5) com.alkimor.regnum.core.Text.info(rulerOn, "Вас не было " + c.absentDays + " сут. " + (c.hasRegent() || !c.governor.isEmpty() ? "Город держался под управлением." : "Без регента и наместника доход города падал."));
                    c.absentDays = 0;
                } else if (c.absentDays++ == 4 && !c.hasRegent() && c.governor.isEmpty()) {
                    for (var e : c.roles.keySet()) { var m = server.getPlayerList().getPlayer(e); if (m != null) com.alkimor.regnum.core.Text.info(m, "Правитель «" + c.name + "» давно отсутствует. Назначьте регента: /regnum council grant <игрок> казна."); }
                }
                runEconomy(ow, data, c);
                String spoiled = Spoilage.daily(ow, c);
                if (!spoiled.isEmpty()) {
                    var owner = server.getPlayerList().getPlayer(c.owner);
                    if (owner != null) com.alkimor.regnum.core.Text.info(owner, "Порча запасов в «" + c.name + "»: " + spoiled + (c.count(BuildingType.WAREHOUSE) == 0 ? ". Склад замедлил бы порчу." : "."));
                }
                data.setDirty();
            }
            if (c.raidActive) {
                tickRaid(ow, data, c);
            } else if (RegnumConfig.RAIDS_ENABLED.get() && day >= c.nextRaidDay && tod >= 13000 && tod < 14000) {
                tryStartRaid(ow, data, c, day);
            }
        }
        // убрать полосы набегов у исчезнувших городов
        RAID_BARS.entrySet().removeIf(e -> {
            City c = data.byId(e.getKey());
            if (c == null || !c.raidActive) {
                e.getValue().removeAllPlayers();
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------ учебный плац

    private static void train(ServerLevel ow, KingdomData data, City c) {
        int trained = 0;
        for (var e : c.buildings.entrySet()) {
            if (e.getValue() != BuildingType.TRAINING_GROUND) continue;
            BlockPos pos = BlockPos.of(e.getKey());
            if (!ow.isLoaded(pos)) continue;
            for (SoldierEntity s : ow.getEntitiesOfClass(SoldierEntity.class, new AABB(pos).inflate(12),
                    s -> s.isAlive() && c.id.equals(s.getCityId()) && s.getOrder() == Order.HOLD && !s.isSparring())) {
                if (c.treasury <= 0) break;
                Technique t;
                float r = ow.random.nextFloat();
                if (s.isArcher()) t = r < 0.6f ? Technique.KITING : r < 0.8f ? Technique.FOCUS_FIRE : Technique.RETREAT_HEAL;
                else if (s.getSoldierType() == SoldierType.KNIGHT && r < 0.4f) t = Technique.SHIELD_BLOCK;
                else {
                    Technique[] melee = {Technique.JUMP_CRIT, Technique.W_TAP, Technique.STRAFE, Technique.FOCUS_FIRE, Technique.RETREAT_HEAL, Technique.SHIELD_BLOCK};
                    t = melee[ow.random.nextInt(melee.length)];
                }
                s.trainTech(t, 1);
                ow.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, s.getX(), s.getY() + 1.8, s.getZ(), 3, 0.2, 0.1, 0.2, 0);
                trained++;
            }
        }
        if (trained > 0) {
            int cost = (trained + 1) / 2;
            c.treasury = Math.max(0, c.treasury - cost);
            data.setDirty();
        }
    }

    // ------------------------------------------------------------------ экономика

    private static void runEconomy(ServerLevel ow, KingdomData data, City c) {
        if (ow.isLoaded(c.hall)) {
            int r = c.radius();
            c.population = ow.getEntitiesOfClass(Villager.class, new AABB(c.hall).inflate(r, 32, r), v -> c.contains(v.blockPosition())).size();
            if (c.perk(City.P_GROWTH)) c.population = Math.round(c.population * 1.15f);
        }
        ServerPlayer ownerNow = ow.getServer().getPlayerList().getPlayer(c.owner);
        if (ownerNow != null) {
            c.refreshOwner(ownerNow);
            com.alkimor.regnum.survival.Skills.addXp(ownerNow, com.alkimor.regnum.survival.Skill.STEWARD, 3 + c.level);
        }
        settleBonds(ow, c);
        int income = c.dailyIncome(RegnumConfig.TAX_PER_VILLAGER.get()) + Realms.dailyBonus(data, c) + Villages.dailyBonus(data, c);
        c.treasury += income;
        int upkeep = RegnumConfig.UPKEEP_ENABLED.get() ? c.dailyUpkeep() : 0;
        int deserted = 0;
        if (upkeep > c.treasury) {
            // не хватает — часть армии уходит
            List<UUID> ids = new ArrayList<>(c.soldiers.keySet());
            while (c.dailyUpkeep() > c.treasury && !ids.isEmpty()) {
                UUID u = ids.remove(ow.random.nextInt(ids.size()));
                c.soldiers.remove(u);
                deserted++;
            }
            upkeep = c.dailyUpkeep();
        }
        c.treasury -= upkeep;
        String stockLine = Industry.daily(ow, data, c);

        ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
        if (owner != null) {
            owner.sendSystemMessage(Text.of("☼ Новый день в «" + c.name + "»: ", ChatFormatting.GOLD)
                    .append(Text.of("налоги +" + income + ", жалованье −" + upkeep + ", казна " + c.treasury
                            + " изумр., жителей " + c.population, ChatFormatting.GRAY)));
            owner.sendSystemMessage(Text.of("   запасы: " + stockLine, ChatFormatting.DARK_GRAY));
            if (deserted > 0) Text.bad(owner, "Казна пуста! Армию покинули солдаты: " + deserted + ".");
        }
    }

    /** Раз в 7 дней облигации приносят доход или убыток в зависимости от стратегии. */
    private static void settleBonds(ServerLevel ow, City c) {
        long day = ow.getDayTime() / 24000L;
        if (c.bonds <= 0 || c.lastBondDay < 0 || day - c.lastBondDay < 7) return;
        c.lastBondDay = day;
        double r = switch (c.bondStrategy) {
            case 1 -> -0.06 + ow.random.nextDouble() * 0.20;
            case 2 -> -0.30 + ow.random.nextDouble() * 0.70;
            default -> 0.03 + ow.random.nextDouble() * 0.04;
        };
        int before = c.bonds;
        c.bonds = Math.max(0, (int) Math.round(c.bonds * (1 + r)));
        ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
        if (owner != null) {
            int delta = c.bonds - before;
            owner.sendSystemMessage(Text.of("§ Казначей «" + c.name + "»: облигации " + (delta >= 0 ? "+" : "") + delta
                    + " (" + Math.round(r * 100) + "%), теперь " + c.bonds + " изумр.", delta >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }

    // ------------------------------------------------------------------ набеги

    private static void tryStartRaid(ServerLevel ow, KingdomData data, City c, long day) {
        ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
        boolean ownerNear = owner != null && owner.level() == ow && owner.blockPosition().closerThan(c.hall, c.radius() + 128);
        if (!ownerNear || ow.getDifficulty() == Difficulty.PEACEFUL) {
            c.nextRaidDay = day + 1; // набег откладывается, пока короля нет рядом
            data.setDirty();
            return;
        }
        c.raidActive = true;
        c.raidWave = 0;
        c.raidWavesTotal = 1 + (c.level + 1) / 2;
        c.raidDelay = c.count(BuildingType.WATCHTOWER) > 0 ? 30 : 10;
        c.raidStolen = 0;
        c.raiders.clear();
        data.setDirty();

        String dir = c.count(BuildingType.WATCHTOWER) > 0 ? " Дозорные заметили их заранее!" : "";
        broadcastNear(ow, c, Text.of("⚔ На город «" + c.name + "» идёт набег разбойников! Волн: " + c.raidWavesTotal + "." + dir, ChatFormatting.RED, ChatFormatting.BOLD));
        ow.playSound(null, c.hall, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 64f, 1.0f);
    }

    private static void tickRaid(ServerLevel ow, KingdomData data, City c) {
        ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
        if (owner == null) {
            // король вышел — разбойники отступают, набег переносится
            despawnRaiders(ow, c);
            c.raidActive = false;
            c.nextRaidDay = ow.getDayTime() / 24000L + 1;
            data.setDirty();
            return;
        }

        // подсчёт живых
        int alive = 0;
        for (Iterator<UUID> it = c.raiders.iterator(); it.hasNext(); ) {
            Entity e = ow.getEntity(it.next());
            if (e != null && e.isAlive()) alive++;
            else it.remove();
        }
        updateBar(ow, c, alive);

        if (alive > 0) {
            c.raidTimer++;
            if (c.raidTimer > WAVE_TIMEOUT / 20) {
                broadcastNear(ow, c, Text.of("Разбойники отступают с награбленным...", ChatFormatting.GRAY));
                despawnRaiders(ow, c);
                finishRaid(ow, data, c, false);
            }
            return;
        }

        if (c.raidWave >= c.raidWavesTotal) {
            finishRaid(ow, data, c, true);
            return;
        }
        if (c.raidDelay > 0) {
            c.raidDelay--;
            return;
        }
        spawnWave(ow, c);
        data.setDirty();
    }

    private static void spawnWave(ServerLevel ow, City c) {
        c.raidWave++;
        c.raidTimer = 0;
        c.raidDelay = 10;
        boolean last = c.raidWave == c.raidWavesTotal;
        int size = 3 + c.level * 2 + c.raidWave + (int) Math.min(6, c.glory / 50);
        c.raidWaveSize = size;

        double angle = ow.random.nextDouble() * Math.PI * 2;
        int dist = c.radius() + 20;
        int bx = c.hall.getX() + (int) (Math.cos(angle) * dist);
        int bz = c.hall.getZ() + (int) (Math.sin(angle) * dist);
        boolean towers = c.count(BuildingType.WATCHTOWER) > 0;

        for (int i = 0; i < size + (last ? 1 : 0); i++) {
            int x = bx + ow.random.nextInt(9) - 4;
            int z = bz + ow.random.nextInt(9) - 4;
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            BanditEntity b = KingdomModule.BANDIT.get().create(ow);
            if (b == null) continue;
            b.moveTo(x + 0.5, y, z + 0.5, ow.random.nextFloat() * 360f, 0f);
            b.finalizeSpawn(ow, ow.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
            int variant = (last && i == size) ? BanditEntity.CAPTAIN : (ow.random.nextFloat() < 0.4f ? BanditEntity.ARCHER : BanditEntity.THUG);
            b.setup(variant);
            b.joinRaid(c.id, c.hall);
            if (towers) b.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 90, 0));
            if (c.perk(City.P_WALLS)) {
                var hpA = b.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                if (hpA != null) hpA.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        com.alkimor.regnum.Regnum.id("walls"), -0.2, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                b.setHealth(b.getMaxHealth());
            }
            ow.addFreshEntity(b);
            c.raiders.add(b.getUUID());
        }
        String side = sideName(c.hall, bx, bz);
        broadcastNear(ow, c, Text.of("Волна " + c.raidWave + "/" + c.raidWavesTotal + ": разбойники идут " + side
                + (last ? ", их ведёт атаман!" : "!"), ChatFormatting.RED));
    }

    private static void finishRaid(ServerLevel ow, KingdomData data, City c, boolean victory) {
        c.raidActive = false;
        c.raiders.clear();
        c.nextRaidDay = ow.getDayTime() / 24000L + RegnumConfig.RAID_INTERVAL_DAYS.get();
        ServerBossEvent bar = RAID_BARS.remove(c.id);
        if (bar != null) bar.removeAllPlayers();
        ServerPlayer owner = ow.getServer().getPlayerList().getPlayer(c.owner);
        if (victory) {
            int reward = 8 * c.level + 4 * c.raidWavesTotal;
            int glory = 15 * c.raidWavesTotal;
            c.treasury += reward;
            c.glory += glory;
            broadcastNear(ow, c, Text.of("🏆 Набег отбит! Казна +" + reward + ", слава +" + glory
                    + (c.raidStolen > 0 ? " (разграблено: " + c.raidStolen + ")" : ""), ChatFormatting.GOLD, ChatFormatting.BOLD));
            if (owner != null) {
                Skills.addXp(owner, Skill.LEADERSHIP, 30 * c.raidWavesTotal);
                Skills.addXp(owner, Skill.TACTICS, 40 * c.raidWavesTotal);
                Skills.addHonor(owner, 5, "город защищён");
                Skills.chronicle(owner, "Отбит набег на «" + c.name + "» (" + c.raidWavesTotal + " волн)");
            }
        } else if (owner != null) {
            Text.bad(owner, "Набег на «" + c.name + "» закончился поражением. Разграблено: " + c.raidStolen + " изумр.");
        }
        data.setDirty();
    }

    private static void despawnRaiders(ServerLevel ow, City c) {
        for (UUID u : c.raiders) {
            Entity e = ow.getEntity(u);
            if (e != null) e.discard();
        }
        c.raiders.clear();
    }

    private static void updateBar(ServerLevel ow, City c, int alive) {
        ServerBossEvent bar = RAID_BARS.computeIfAbsent(c.id, k -> new ServerBossEvent(
                Component.literal("Набег на «" + c.name + "»"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10));
        bar.setName(Component.literal("Набег на «" + c.name + "» — волна " + Math.max(1, c.raidWave) + "/" + c.raidWavesTotal));
        bar.setProgress(c.raidWaveSize > 0 ? Math.min(1f, alive / (float) c.raidWaveSize) : 1f);
        double r = c.radius() + 64;
        for (ServerPlayer p : ow.players()) {
            if (p.blockPosition().closerThan(c.hall, r)) bar.addPlayer(p);
            else bar.removePlayer(p);
        }
    }

    /** Разбойник у ратуши грабит казну. */
    public static void plunder(MinecraftServer server, UUID cityId, BanditEntity bandit) {
        KingdomData data = KingdomData.get(server);
        City c = data.byId(cityId);
        if (c == null || !c.raidActive) return;
        int take = Math.min(c.treasury, c.perk(City.P_VAULT) ? (bandit.getRandom().nextBoolean() ? 1 : 2) : 3);
        if (take <= 0) return;
        c.treasury -= take;
        c.raidStolen += take;
        data.setDirty();
        ServerPlayer owner = server.getPlayerList().getPlayer(c.owner);
        if (owner != null) Text.bar(owner, "Разбойники грабят казну! −" + take + " изумр.", ChatFormatting.RED);
    }

    /** Отладка и команда: начать набег немедленно. */
    public static boolean forceRaid(ServerLevel ow, City c) {
        if (c.raidActive) return false;
        c.nextRaidDay = 0;
        tryStartRaid(ow, KingdomData.get(ow.getServer()), c, ow.getDayTime() / 24000L);
        return c.raidActive;
    }

    private static void broadcastNear(ServerLevel ow, City c, Component msg) {
        double r = c.radius() + 128;
        for (ServerPlayer p : ow.players()) {
            if (p.getUUID().equals(c.owner) || p.blockPosition().closerThan(c.hall, r)) p.sendSystemMessage(msg);
        }
    }

    private static String sideName(BlockPos hall, int x, int z) {
        double dx = x - hall.getX(), dz = z - hall.getZ();
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? "с востока" : "с запада";
        return dz > 0 ? "с юга" : "с севера";
    }
}
