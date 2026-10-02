package com.alkimor.regnum.survival;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.kingdom.BanditEntity;
import com.alkimor.regnum.kingdom.SoldierEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Эффекты перков и черт в бою и в мире, а также опыт навыков за действия.
 * Атрибутные бонусы (скорость, здоровье, дальность) — в {@link PlayerStats}.
 */
public final class PerkEvents {
    private PerkEvents() {}

    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_SCOUT_POS = new HashMap<>();

    private static boolean cd(Player p, String key, long ticks) {
        UUID id = new UUID(p.getUUID().getMostSignificantBits(), p.getUUID().getLeastSignificantBits() ^ key.hashCode());
        long now = p.level().getGameTime();
        if (now - COOLDOWN.getOrDefault(id, -1_000_000L) < ticks) return false;
        COOLDOWN.put(id, now);
        return true;
    }

    private static SurvivorData d(Player p) {
        return Skills.data(p);
    }

    static boolean isBeast(LivingEntity e) {
        if (e instanceof Animal) return true;
        return e instanceof Monster && !(e instanceof AbstractIllager) && !(e instanceof Zombie) && !(e instanceof AbstractSkeleton)
                && !(e instanceof BanditEntity);
    }

    private static boolean dark(Player p) {
        return p.level().getMaxLocalRawBrightness(p.blockPosition()) < 6;
    }

    // ================================================================== урон

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        Entity srcEnt = event.getSource().getEntity();
        float amount = event.getAmount();

        // ---------- атакует игрок
        if (srcEnt instanceof ServerPlayer p && victim != p) {
            Weapons.Kind kind = Weapons.of(event.getSource(), p);
            if (kind != null) {
                SurvivorData d = d(p);
                Skill sk = Weapons.skill(kind);
                int lvl = d.level(sk);
                float mult = 1f + (kind == Weapons.Kind.THROWING ? 0.0025f : 0.002f) * lvl;
                float flat = 0f;
                double dist = p.distanceTo(victim);
                float hpPart = victim.getHealth() / victim.getMaxHealth();
                switch (kind) {
                    case ONE_HANDED -> {
                        if (d.has(Perk.OH_HEAVY)) mult *= 1.10f;
                        if (d.has(Perk.OH_DUEL) && victim.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(6),
                                m -> m != victim && m instanceof Enemy && m.isAlive()).isEmpty()) mult *= 1.2f;
                        if (d.has(Perk.OH_CRIT) && p.getRandom().nextFloat() < 0.12f) {
                            mult *= 1.6f;
                            ((ServerLevel) p.level()).sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 14, 0.3, 0.3, 0.3, 0.25);
                        }
                    }
                    case TWO_HANDED -> {
                        if (d.has(Perk.TH_POWER)) mult *= 1.10f;
                        if (d.has(Perk.TH_PIERCE)) flat += victim.getArmorValue() * 0.25f;
                        if (d.has(Perk.TH_BERSERK)) mult *= 1f + 0.35f * (1f - p.getHealth() / p.getMaxHealth());
                        if (d.has(Perk.TH_EXECUTE) && hpPart < 0.25f) mult *= 1.5f;
                    }
                    case POLEARM -> {
                        if (d.has(Perk.PA_BRACE)) mult *= 1.12f;
                        if (d.has(Perk.PA_CHARGE) && p.isSprinting()) mult *= 1.3f;
                        if (d.has(Perk.PA_IMPALE)) flat += victim.getArmorValue() * 0.25f;
                        if (d.has(Perk.PA_DRAGON)) mult *= 1.1f;
                        if (d.has(Perk.PA_GIANT) && (victim.isVehicle() || victim.getBbWidth() > 1.2f || victim.getBbHeight() > 2.6f)) mult *= 1.8f;
                    }
                    case BOW -> {
                        if (d.has(Perk.BW_STEADY)) mult *= 1.10f;
                        if (d.has(Perk.BW_RANGE) && dist > 16) mult *= 1.15f;
                        if (d.has(Perk.BW_HUNTER) && isBeast(victim)) mult *= 1.25f;
                        if (d.has(Perk.BW_PIERCE)) flat += victim.getArmorValue() * 0.25f;
                        if (d.has(Perk.BW_HEADSHOT) && event.getSource().getDirectEntity() != null
                                && event.getSource().getDirectEntity().getY() >= victim.getEyeY() - 0.35) {
                            mult *= 1.5f;
                            p.displayClientMessage(Component.literal("В голову!").withStyle(ChatFormatting.GOLD), true);
                        }
                    }
                    case CROSSBOW -> {
                        if (d.has(Perk.CB_POWER)) mult *= 1.10f;
                        if (d.has(Perk.CB_ARMOR) && victim.getArmorValue() >= 10) mult *= 1.2f;
                        if (d.has(Perk.CB_CROUCH) && p.isCrouching()) mult *= 1.15f;
                        if (d.has(Perk.CB_SIEGE)) mult *= 1.35f;
                    }
                    case THROWING -> {
                        if (d.has(Perk.TW_POWER)) mult *= 1.15f;
                        if (d.has(Perk.TW_PIERCE)) flat += victim.getArmorValue() * 0.25f;
                        if (d.has(Perk.TW_FINISH) && hpPart < 0.5f) mult *= 1.5f;
                        if (d.has(Perk.TW_MASTER)) mult *= 1.3f;
                    }
                }
                if (Weapons.ranged(kind)) {
                    if (d.has(Trait.MARKSMAN)) mult *= 1.1f;
                    if (d.has(Trait.EAGLE_EYE) && dist > 16) mult *= 1.12f;
                    if (d.has(Trait.SHORT_SIGHTED) && dist > 16) mult *= 0.8f;
                    if (d.has(Trait.NIGHT_BLIND) && (p.level().isNight() || dark(p))) mult *= 0.9f;
                }
                if (p.isPassenger()) {
                    if (!Weapons.ranged(kind) && d.has(Perk.RD_MELEE)) mult *= 1.1f;
                    if (kind == Weapons.Kind.BOW && d.has(Perk.RD_ARCHER)) mult *= 1.15f;
                    if (d.has(Perk.RD_CENTAUR)) mult *= 1.15f;
                }
                if (!Weapons.ranged(kind) && d.has(Perk.RG_BACKSTAB) && victim instanceof Mob m && m.getTarget() != p) {
                    Vec3 look = victim.getViewVector(1f);
                    Vec3 to = p.position().subtract(victim.position()).normalize();
                    if (look.x * to.x + look.z * to.z < -0.2) {
                        mult *= 1.8f;
                        ((ServerLevel) p.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, victim.getX(), victim.getY(0.7), victim.getZ(), 6, 0.2, 0.2, 0.2, 0.1);
                        p.displayClientMessage(Component.literal("Удар в спину!").withStyle(ChatFormatting.DARK_RED), true);
                        Skills.addXp(p, Skill.ROGUERY, 4);
                    }
                }
                if (d.has(Perk.EN_CONSTRUCT) && (victim instanceof AbstractGolem)) mult *= 1.5f;
                amount = amount * mult + flat;
            }
        }

        // ---------- солдаты игрока атакуют / получают урон
        if (srcEnt instanceof SoldierEntity s && s.getOwnerPlayer() instanceof ServerPlayer king) {
            SurvivorData d = d(king);
            float m = 1f + 0.001f * d.level(Skill.TACTICS);
            if (d.has(Perk.TC_AMBUSH) && s.level().isNight()) m *= 1.15f;
            if (d.has(Perk.TC_FLANK) && s.level().getEntitiesOfClass(SoldierEntity.class, victim.getBoundingBox().inflate(2.5),
                    o -> o != s && o.getTarget() == victim).size() >= 1) m *= 1.15f;
            if (d.has(Perk.TC_GENERAL)) m *= 1.10f;
            boolean near = king.distanceToSqr(s) < 16 * 16;
            if (near && d.has(Perk.CH_LEGEND)) m *= 1.05f;
            if (near && d.has(Perk.LD_BANNER)) m *= 1.10f;
            amount *= m;
        }
        if (victim instanceof SoldierEntity s && s.getOwnerPlayer() instanceof ServerPlayer king && d(king).has(Perk.TC_STEADFAST)) {
            amount *= 0.85f;
        }
        if (victim instanceof AbstractHorse h && h.getControllingPassenger() instanceof ServerPlayer rider && d(rider).has(Perk.RD_ARMOR)) {
            amount *= 0.7f;
        }

        // ---------- получает урон игрок
        if (victim instanceof ServerPlayer p) {
            SurvivorData d = d(p);
            boolean physical = !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR) && !event.getSource().is(DamageTypeTags.IS_FIRE)
                    && !event.getSource().is(DamageTypeTags.IS_FALL);
            if (physical && d.has(Trait.THICK_SKIN)) amount *= 0.9f;
            if (d.has(Perk.OH_SHIELD) && p.getOffhandItem().getItem() instanceof ShieldItem) amount *= 0.85f;
            if (d.has(Perk.PA_WALL) && srcEnt != null && Weapons.melee(p.getMainHandItem()) == Weapons.Kind.POLEARM) {
                Vec3 look = p.getViewVector(1f);
                Vec3 to = srcEnt.position().subtract(p.position()).normalize();
                if (look.x * to.x + look.z * to.z > 0.5) amount *= 0.8f;
            }
            if (d.has(Perk.EN_SAPPER) && event.getSource().is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.6f;
            if (d.has(Perk.OH_PARRY) && srcEnt instanceof LivingEntity && event.getSource().getDirectEntity() == srcEnt
                    && Weapons.melee(p.getMainHandItem()) == Weapons.Kind.ONE_HANDED && !p.getMainHandItem().isEmpty()
                    && p.getRandom().nextFloat() < 0.10f) {
                event.setCanceled(true);
                p.level().playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 1.8f);
                ((ServerLevel) p.level()).sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY(1.0), p.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                p.displayClientMessage(Component.literal("Парировано!").withStyle(ChatFormatting.GOLD), true);
                return;
            }
            // Чудо-лекарь: раз в 20 минут спасает от смерти
            if (d.has(Perk.MD_MIRACLE) && amount >= p.getHealth() && cd(p, "miracle", 20 * 60 * 20)) {
                amount = Math.max(0f, p.getHealth() - 1f);
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2));
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
                ((ServerLevel) p.level()).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY(1), p.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
                p.sendSystemMessage(Component.literal("Чудо-лекарь: вы чудом устояли на ногах!").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onShield(LivingShieldBlockEvent event) {
        if (event.getDamageSource().getDirectEntity() instanceof AbstractArrow a && a.getOwner() instanceof ServerPlayer p
                && d(p).has(Perk.CB_SHIELD) && a.getWeaponItem() != null && a.getWeaponItem().getItem() instanceof CrossbowItem) {
            event.setBlocked(false);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        float dmg = event.getNewDamage();
        if (dmg <= 0 || !(event.getSource().getEntity() instanceof ServerPlayer p) || victim == p
                || victim instanceof net.minecraft.world.entity.decoration.ArmorStand) return;
        Weapons.Kind kind = Weapons.of(event.getSource(), p);
        if (kind == null) return;
        SurvivorData d = d(p);
        // опыт
        Skill sk = Weapons.skill(kind);
        if (Weapons.ranged(kind)) {
            int distanceBonus = (int) (p.distanceTo(victim) / 8);
            Skills.addXp(p, sk, Math.max(1, Math.min(12, Math.round(dmg) + distanceBonus)));
        } else {
            Skills.addXp(p, sk, Math.max(1, Math.min(8, Math.round(dmg))));
        }
        if (p.isPassenger()) Skills.addXp(p, Skill.RIDING, 1);
        // эффекты
        switch (kind) {
            case ONE_HANDED -> {
                if (d.has(Perk.OH_LEECH)) p.heal(dmg * 0.08f);
                if (d.has(Perk.OH_BLEED) && p.getRandom().nextFloat() < 0.15f)
                    victim.addEffect(new MobEffectInstance(SurvivalModule.BLEEDING, 20 * 8, 0), p);
            }
            case TWO_HANDED -> {
                if (d.has(Perk.TH_STUN) && p.getRandom().nextFloat() < 0.15f) stun(victim, p, 30);
            }
            case POLEARM -> {
                if (d.has(Perk.PA_PUSH)) victim.knockback(1.0, p.getX() - victim.getX(), p.getZ() - victim.getZ());
            }
            case BOW -> {
                if (d.has(Perk.BW_POISON)) victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), p);
            }
            case CROSSBOW -> {
                if (d.has(Perk.CB_KNOCK)) victim.knockback(1.2, p.getX() - victim.getX(), p.getZ() - victim.getZ());
                if (d.has(Perk.CB_SLOW)) victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), p);
            }
            case THROWING -> {
                if (d.has(Perk.TW_TRIP)) victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), p);
                if (d.has(Perk.TW_STUN)) stun(victim, p, 20);
            }
        }
        if (p.isPassenger() && !Weapons.ranged(kind) && d.has(Perk.RD_TRAMPLE)) {
            victim.knockback(1.1, p.getX() - victim.getX(), p.getZ() - victim.getZ());
        }
    }

    private static void stun(LivingEntity victim, Player p, int ticks) {
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 4), p);
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1), p);
        if (victim instanceof Mob m) m.getNavigation().stop();
        ((ServerLevel) victim.level()).sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getEyeY() + 0.3, victim.getZ(), 6, 0.3, 0.1, 0.3, 0.05);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && event.getSource().getDirectEntity() == p
                && d(p).has(Perk.TH_WHIRL) && Weapons.melee(p.getMainHandItem()) == Weapons.Kind.TWO_HANDED) {
            p.heal(4f);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof BanditEntity b && event.getSource().getEntity() instanceof ServerPlayer p) {
            Skills.addXp(p, Skill.ROGUERY, 2);
            if (d(p).has(Perk.RG_POCKET)) {
                ItemStack extra = p.getRandom().nextFloat() < 0.5f ? new ItemStack(Items.EMERALD, 1 + p.getRandom().nextInt(2))
                        : new ItemStack(Items.IRON_NUGGET, 2 + p.getRandom().nextInt(5));
                event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(b.level(), b.getX(), b.getY(), b.getZ(), extra));
            }
        }
    }

    // ================================================================== стрелы

    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow a)
                || !(a.getOwner() instanceof ServerPlayer p) || a.getTags().contains("regnum_extra")) return;
        SurvivorData d = d(p);
        boolean crossbow = a.getWeaponItem() != null && a.getWeaponItem().getItem() instanceof CrossbowItem;
        if (!(a instanceof Arrow)) return;
        if (!crossbow && d.has(Perk.BW_SAVE) && !p.isCreative() && a.pickup == AbstractArrow.Pickup.ALLOWED && p.getRandom().nextFloat() < 0.25f) {
            a.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            if (!p.getInventory().add(new ItemStack(Items.ARROW))) p.drop(new ItemStack(Items.ARROW), false);
        }
        float volley = crossbow ? (d.has(Perk.CB_VOLLEY) ? 0.2f : 0f) : (d.has(Perk.BW_VOLLEY) ? 0.15f : 0f);
        if (volley > 0 && p.getRandom().nextFloat() < volley) {
            Arrow extra = new Arrow(p.level(), p, new ItemStack(Items.ARROW), a.getWeaponItem());
            extra.addTag("regnum_extra");
            extra.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            extra.setPos(a.getX(), a.getY(), a.getZ());
            Vec3 v = a.getDeltaMovement();
            extra.shoot(v.x, v.y, v.z, (float) v.length(), 4f);
            extra.setBaseDamage(a.getBaseDamage());
            p.level().addFreshEntity(extra);
        }
    }

    // ================================================================== скрытность

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide()) return;
        SurvivorData d = d(p);
        double v = 1.0;
        if (p.isCrouching()) {
            if (d.has(Perk.RG_QUIET)) v *= 0.7;
            if (dark(p)) v *= 0.75;
        }
        if (d.has(Trait.QUIET)) v *= 0.8;
        if (d.has(Trait.NOISY)) v *= 1.3;
        if (d.has(Perk.SC_GHOST)) v *= 0.6;
        v *= 1.0 - Math.min(0.3, 0.003 * d.level(Skill.ROGUERY));
        if (p.getArmorValue() > 12) v *= 1.15;
        event.modifyVisibility(v);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof BanditEntity b && event.getNewAboutToBeSetTarget() instanceof ServerPlayer p
                && p.isCrouching() && d(p).has(Perk.RG_BLEND) && b.getLastHurtByMob() != p) {
            event.setCanceled(true);
        }
    }

    // ================================================================== каждую секунду

    private static final net.minecraft.resources.ResourceLocation PATHS = Regnum.id("hero_paths");

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || p.isSpectator()) return;
        SurvivorData d = d(p);
        ServerLevel sl = p.serverLevel();
        float hp = p.getHealth() / p.getMaxHealth();

        if (d.has(Perk.AT_WIND) && hp < 0.3f && !p.hasEffect(MobEffects.REGENERATION)) p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
        if (d.has(Perk.MD_HEALER) && p.tickCount - p.getLastHurtByMobTimestamp() > 200 && p.getHealth() < p.getMaxHealth() && p.tickCount % 60 == 0) p.heal(1f);
        if (d.has(Trait.COWARD) && hp < 0.3f) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
        if (d.has(Trait.GLUTTON)) p.causeFoodExhaustion(0.03f);
        boolean darkHere = dark(p);
        if ((d.has(Perk.SC_NIGHT) && p.isCrouching() && darkHere) || (d.has(Trait.CAT_EYES) && darkHere && p.getY() < 50)) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
        }
        if (d.has(Perk.RG_SHADOW) && p.isCrouching() && darkHere && cd(p, "shadow", 20 * 60)) {
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, true, false));
            p.displayClientMessage(Component.literal("Вы растворились в тени").withStyle(ChatFormatting.DARK_GRAY), true);
        }
        if (d.has(Perk.SC_EAGLE) && p.isCrouching()) {
            for (Mob m : sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(20), m -> m instanceof Enemy)) {
                m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
            }
        }
        if (d.has(Perk.SC_SENSE) && cd(p, "sense_check", 60)) {
            Vec3 look = p.getViewVector(1f);
            for (Mob m : sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(10), m -> m.getTarget() == p)) {
                Vec3 to = m.position().subtract(p.position()).normalize();
                if (look.x * to.x + look.z * to.z < -0.3) {
                    p.displayClientMessage(Component.literal("⚠ Сзади враг!").withStyle(ChatFormatting.RED), true);
                    break;
                }
            }
        }
        // тропы: быстрее по траве, песку, снегу
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            BlockState below = sl.getBlockState(p.blockPosition().below());
            boolean wild = below.is(BlockTags.DIRT) || below.is(BlockTags.SAND) || below.is(BlockTags.SNOW);
            boolean want = d.has(Perk.SC_PATHS) && wild;
            if (want && speed.getModifier(PATHS) == null) speed.addTransientModifier(new AttributeModifier(PATHS, 0.06, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else if (!want && speed.getModifier(PATHS) != null) speed.removeModifier(PATHS);
        }
        // конь
        if (p.getVehicle() instanceof AbstractHorse h) {
            if (h.getDeltaMovement().horizontalDistanceSqr() > 0.01 && p.tickCount % 40 == 0) Skills.addXp(p, Skill.RIDING, 1);
            int r = d.level(Skill.RIDING);
            if (d.has(Perk.RD_SPEED) || r >= 50) h.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false));
            if (d.has(Perk.RD_LEAP)) h.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, true, false));
            if (d.has(Perk.RD_TIRELESS) && p.tickCount % 80 == 0) h.heal(1f);
        }
        // разведка: опыт за путешествия
        Vec3 last = LAST_SCOUT_POS.get(p.getUUID());
        if (p.tickCount % 600 == 0) {
            if (last != null && last.distanceToSqr(p.position()) > 48 * 48 && !p.isPassenger()) Skills.addXp(p, Skill.SCOUTING, 3);
            if (last != null && last.distanceToSqr(p.position()) > 48 * 48 && p.isPassenger()) Skills.addXp(p, Skill.RIDING, 3);
            LAST_SCOUT_POS.put(p.getUUID(), p.position());
        }
        // плутовство: красться рядом с врагами незамеченным
        if (p.isCrouching() && p.tickCount % 60 == 0 && !sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(8),
                m -> m instanceof Enemy && m.getTarget() != p && m.isAlive()).isEmpty()) {
            Skills.addXp(p, Skill.ROGUERY, 1);
        }
    }

    // ================================================================== мир: добыча, плавка, ремёсла, стройка

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p) || p.isCreative() || !(event.getLevel() instanceof ServerLevel sl)) return;
        BlockState st = event.getState();
        if (st.is(net.neoforged.neoforge.common.Tags.Blocks.ORES)) {
            Skills.addXp(p, Skill.ENGINEERING, 1);
            if (d(p).has(Perk.EN_MINER) && p.getRandom().nextFloat() < 0.05f) {
                BlockPos pos = event.getPos();
                for (ItemStack drop : net.minecraft.world.level.block.Block.getDrops(st, sl, pos, null, p, p.getMainHandItem())) {
                    net.minecraft.world.level.block.Block.popResource(sl, pos, drop);
                }
                p.displayClientMessage(Component.literal("Богатая жила!").withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !p.isCreative() && p.getRandom().nextFloat() < 0.2f) {
            Skills.addXp(p, Skill.ENGINEERING, 1);
        }
    }

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack s = event.getSmelting();
        Skills.addXp(p, Skill.SMITHING, Math.max(1, s.getCount() / 4));
        if (d(p).has(Perk.SM_SMELT)) {
            int extra = 0;
            for (int i = 0; i < s.getCount(); i++) if (p.getRandom().nextFloat() < 0.2f) extra++;
            if (extra > 0 && (s.is(Items.IRON_INGOT) || s.is(Items.GOLD_INGOT) || s.is(Items.COPPER_INGOT) || s.is(Items.NETHERITE_SCRAP))) {
                ItemStack bonus = new ItemStack(s.getItem(), extra);
                if (!p.getInventory().add(bonus)) p.drop(bonus, false);
            }
        }
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack s = event.getCrafting();
        var it = s.getItem();
        if (it instanceof net.minecraft.world.item.BlockItem bi) {
            var b = bi.getBlock();
            if (b instanceof net.minecraft.world.level.block.piston.PistonBaseBlock || b instanceof net.minecraft.world.level.block.DispenserBlock
                    || b instanceof net.minecraft.world.level.block.BaseRailBlock || b instanceof net.minecraft.world.level.block.ObserverBlock
                    || b instanceof net.minecraft.world.level.block.RepeaterBlock || b instanceof net.minecraft.world.level.block.ComparatorBlock
                    || b instanceof net.minecraft.world.level.block.HopperBlock) {
                Skills.addXp(p, Skill.ENGINEERING, 4);
            }
        }
        if (d(p).has(Perk.SM_ARMORER) && (it instanceof net.minecraft.world.item.TieredItem || it instanceof net.minecraft.world.item.ArmorItem)
                && !s.has(com.alkimor.regnum.crafting.CraftingModule.TEMPER.get())) {
            s.set(com.alkimor.regnum.crafting.CraftingModule.TEMPER.get(), 1);
        }
    }
}
