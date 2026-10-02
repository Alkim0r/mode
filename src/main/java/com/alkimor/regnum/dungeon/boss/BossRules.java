package com.alkimor.regnum.dungeon.boss;

import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.dungeon.CryptLordEntity;
import com.alkimor.regnum.kingdom.SoldierEntity;
import net.minecraft.core.particles.ParticleOptions;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * Общие правила боссов: масштаб по числу игроков и пресету сложности,
 * армия не решает исход боя (солдаты наносят боссу малый урон, а получают двойной).
 */
public final class BossRules {
    private BossRules() {}

    private static final ResourceLocation HP_ID = ResourceLocation.fromNamespaceAndPath("regnum", "boss_scale_hp");
    private static final ResourceLocation DMG_ID = ResourceLocation.fromNamespaceAndPath("regnum", "boss_scale_dmg");
    private static final float[] PRESET_HP = {1.0f, 1.5f, 2.2f};
    private static final float[] PRESET_DMG = {1.0f, 1.25f, 1.6f};

    public static boolean isBoss(Entity e) {
        return e instanceof CryptLordEntity || e instanceof MireMotherEntity || e instanceof ForgemasterEntity || e instanceof ScarabQueenEntity;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob boss) || !isBoss(boss)) return;
        if (boss.getPersistentData().getBoolean("regnum_scaled")) return;
        boss.getPersistentData().putBoolean("regnum_scaled", true);
        int players = 0;
        for (Player p : event.getLevel().players()) {
            if (!p.isSpectator() && p.distanceToSqr(boss) < 64 * 64) players++;
        }
        players = Math.max(1, players);
        int tier = RegnumConfig.tier();
        double hpMult = PRESET_HP[tier] * (1 + RegnumConfig.BOSS_HP_PER_EXTRA_PLAYER.get() * (players - 1));
        double dmgMult = PRESET_DMG[tier] * (1 + 0.12 * (players - 1));
        AttributeInstance hp = boss.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hpMult != 1.0) {
            hp.addOrReplacePermanentModifier(new AttributeModifier(HP_ID, hpMult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        AttributeInstance dmg = boss.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmgMult != 1.0) {
            dmg.addOrReplacePermanentModifier(new AttributeModifier(DMG_ID, dmgMult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        boss.getPersistentData().putFloat("regnum_dmg_scale", (float) dmgMult);
        boss.setHealth(boss.getMaxHealth());
        if (players > 1 && event.getLevel() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(boss) < 64 * 64) {
                    p.displayClientMessage(Component.literal("Босс почуял " + players + " героев и набрался сил: здоровье ×"
                            + String.format("%.1f", hpMult)).withStyle(net.minecraft.ChatFormatting.DARK_RED), false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Entity src = event.getSource().getEntity();
        if (isBoss(victim) && victim instanceof Mob bm && victim.level() instanceof ServerLevel bsl && (src instanceof Player || src instanceof SoldierEntity)) {
            event.setAmount(event.getAmount() * BossTactics.multiplier(bsl, bm));
        }
        if (isBoss(victim) && src instanceof SoldierEntity) {
            event.setAmount((float) (event.getAmount() * RegnumConfig.SOLDIER_DAMAGE_TO_BOSSES.get()));
        } else if (victim instanceof SoldierEntity && isBoss(src)) {
            event.setAmount(event.getAmount() * 2.0f);
        }
    }

    /**
     * «Жатва»: если в бою много солдат — босс бьёт по ним площадными ударами.
     * Возвращает true, если удар начат.
     */
    public static boolean reapArmy(ServerLevel sl, Mob boss, Telegraph tele, ParticleOptions warn, ParticleOptions burst, float damage, String text) {
        List<SoldierEntity> army = sl.getEntitiesOfClass(SoldierEntity.class, boss.getBoundingBox().inflate(20), LivingEntity::isAlive);
        if (army.size() < 3) return false;
        int n = 0;
        for (SoldierEntity s : army) {
            if (n++ >= 6) break;
            tele.cast(sl, Telegraph.circle(Telegraph.lead(s, 8), 2.6, 22, damage).soldiers(1.5f).warn(warn).burst(burst)
                    .text(n == 1 ? text : null));
        }
        return true;
    }
}
