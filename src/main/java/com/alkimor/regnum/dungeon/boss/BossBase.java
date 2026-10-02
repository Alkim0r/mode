package com.alkimor.regnum.dungeon.boss;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Общая основа боссов Regnum: полоса здоровья, три фазы, неуязвимость при смене фазы,
 * реплики, честь и летопись за победу. Конкретный босс реализует {@link #abilities}.
 * Работает с любым PathfinderMob через композицию — см. {@link Controller}.
 */
public final class BossBase {
    private BossBase() {}

    /** Состояние босса, которое держит сама сущность. */
    public static final class Controller {
        private final PathfinderMob mob;
        private final String name;
        private final String trophyLine;
        public final ServerBossEvent bar;
        public int phase = 1;
        public int invul = 0;
        private final int[] cooldowns = new int[8];

        public Controller(PathfinderMob mob, String name, BossEvent.BossBarColor color, String trophyLine) {
            this.mob = mob;
            this.name = name;
            this.trophyLine = trophyLine;
            this.bar = new ServerBossEvent(mob.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        }

        /** Вызывать из customServerAiStep. Возвращает новую фазу, если она сменилась, иначе 0. */
        public int tick() {
            bar.setProgress(mob.getHealth() / mob.getMaxHealth());
            if (invul > 0) invul--;
            for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;
            float hp = mob.getHealth() / mob.getMaxHealth();
            int p = hp > 0.66f ? 1 : hp > 0.33f ? 2 : 3;
            if (p > phase) {
                phase = p;
                invul = 40;
                if (mob.level() instanceof ServerLevel sl) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) {
                        bolt.moveTo(mob.getX(), mob.getY(), mob.getZ());
                        bolt.setVisualOnly(true);
                        sl.addFreshEntity(bolt);
                    }
                }
                if (p == 3) bar.setDarkenScreen(true);
                return p;
            }
            return 0;
        }

        /** Готова ли способность slot; если да — ставит перезарядку. */
        public boolean ready(int slot, int cooldown) {
            if (cooldowns[slot] > 0) return false;
            cooldowns[slot] = cooldown;
            return true;
        }

        public void delay(int slot, int ticks) {
            cooldowns[slot] = Math.max(cooldowns[slot], ticks);
        }

        public void say(String line) {
            if (!(mob.level() instanceof ServerLevel sl)) return;
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(mob) < 48 * 48) {
                    p.sendSystemMessage(Text.of(name + ": ", ChatFormatting.DARK_RED, ChatFormatting.BOLD).append(Text.of(line, ChatFormatting.GOLD)));
                }
            }
        }

        public List<LivingEntity> foesAround(double r) {
            return mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(r),
                    e -> e != mob && e.isAlive() && (e instanceof Player pl && !pl.isCreative() && !pl.isSpectator() || e instanceof SoldierEntity));
        }

        public void onDeath(DamageSource source, String deathLine) {
            if (!(mob.level() instanceof ServerLevel sl)) return;
            say(deathLine);
            if (source.getEntity() instanceof ServerPlayer killer) {
                Skills.addHonor(killer, 20, "повержен " + name);
                Skills.chronicle(killer, "Повержен босс: " + name);
                sl.getServer().getPlayerList().broadcastSystemMessage(
                        Text.of("⚔ " + killer.getName().getString() + " одолел: " + name + "! " + trophyLine, ChatFormatting.GOLD), false);
            }
        }

        public void save(CompoundTag tag) {
            tag.putInt("BossPhase", phase);
        }

        public void load(CompoundTag tag) {
            phase = Math.max(1, tag.getInt("BossPhase"));
            bar.setName(mob.getDisplayName());
        }

        public boolean blocksDamage() {
            return invul > 0;
        }
    }

    /** Стандартный набор целей босса. */
    public static void standardGoals(PathfinderMob mob, GoalSelector goals, GoalSelector targets, net.minecraft.world.entity.ai.goal.Goal attackGoal) {
        goals.addGoal(0, new FloatGoal(mob));
        goals.addGoal(2, attackGoal);
        goals.addGoal(5, new MoveTowardsRestrictionGoal(mob, 1.0));
        goals.addGoal(6, new WaterAvoidingRandomStrollGoal(mob, 0.7));
        goals.addGoal(7, new LookAtPlayerGoal(mob, Player.class, 16f));
        goals.addGoal(8, new RandomLookAroundGoal(mob));
        targets.addGoal(1, new HurtByTargetGoal(mob, net.minecraft.world.entity.monster.Monster.class));
        targets.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
        targets.addGoal(3, new NearestAttackableTargetGoal<>(mob, SoldierEntity.class, true));
    }
}
