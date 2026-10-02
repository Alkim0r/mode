package com.alkimor.regnum.wanderers;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Странник с характером. Разговор идёт в чате: реплики и кликабельные варианты ответа.
 * Варианты обрабатывает {@link WandererDialogs}.
 */
public class WandererEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_PERSONA = SynchedEntityData.defineId(WandererEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(WandererEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CHILD = SynchedEntityData.defineId(WandererEntity.class, EntityDataSerializers.BOOLEAN);

    // --- Семья (династия)
    /** Игрок, к роду которого принадлежит NPC (супруга, наследник). */
    @Nullable public UUID familyOf;
    public long bornDay = -1;
    /** Роль взрослого наследника: "", "commander", "governor". */
    public String heirRole = "";
    /** Расположение знатной особы к игрокам (0..100). */
    public final Map<UUID, Integer> affection = new HashMap<>();
    public final Map<UUID, Long> lastTalkDay = new HashMap<>();

    /** Для раненого: на самом деле это засада. */
    boolean trap;
    /** Игроки, с которыми история уже завершена. */
    final Set<UUID> done = new HashSet<>();
    /** Текущая загадка отшельника для игрока. */
    final Map<UUID, Integer> riddle = new HashMap<>();
    private int lifetime = 0;
    @Nullable private com.alkimor.regnum.trade.MarketState market;
    int leaveIn = -1;

    public WandererEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PERSONA, 0);
        builder.define(DATA_SKIN, 0);
        builder.define(DATA_CHILD, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4) {
            @Override
            public boolean canUse() {
                return getPersona() != Persona.WOUNDED && super.canUse();
            }
        });
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 8f, 1.0, 1.3) {
            @Override
            public boolean canUse() {
                return getPersona() != Persona.WOUNDED && super.canUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return getPersona() != Persona.WOUNDED && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        if (!hasCustomName()) setup(Persona.random(random));
        return d;
    }

    public void setup(Persona persona) {
        entityData.set(DATA_PERSONA, persona.ordinal());
        entityData.set(DATA_SKIN, random.nextInt(1000));
        String name = persona.names[random.nextInt(persona.names.length)];
        setCustomName(Component.literal(persona.title + " " + name));
        setCustomNameVisible(true);
        switch (persona) {
            case GUIDE -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COMPASS));
            case TRICKSTER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.EMERALD));
            case HERMIT -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOOK));
            case MERCHANT -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLD_INGOT));
            case NOBLE -> setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            default -> {}
            case WOUNDED -> {
                trap = random.nextFloat() < 0.35f;
                setHealth(6f);
            }
        }
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0f);
    }

    /** Свой маленький рынок у купца (цены зависят от места, где он сейчас стоит). */
    public com.alkimor.regnum.trade.MarketState market(long day) {
        if (market == null) market = new com.alkimor.regnum.trade.MarketState(day);
        market.decay(day);
        return market;
    }

    public boolean isChild() {
        return entityData.get(DATA_CHILD);
    }

    public void setChild(boolean child) {
        entityData.set(DATA_CHILD, child);
    }

    /** Сделать NPC членом семьи игрока. */
    public void becomeFamily(Persona p, UUID player, String name) {
        entityData.set(DATA_PERSONA, p.ordinal());
        familyOf = player;
        leaveIn = -1;
        setCustomName(Component.literal(name));
        setCustomNameVisible(true);
        setPersistenceRequired();
    }

    public void setPersonaRaw(Persona p) {
        entityData.set(DATA_PERSONA, p.ordinal());
    }

    public Persona getPersona() {
        return Persona.byId(entityData.get(DATA_PERSONA));
    }

    public int getSkinIndex() {
        return entityData.get(DATA_SKIN);
    }

    public String shortName() {
        return getCustomName() != null ? getCustomName().getString() : getPersona().title;
    }

    // ------------------------------------------------------------------ разговор

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            getNavigation().stop();
            getLookControl().setLookAt(sp);
            WandererDialogs.greet(this, sp);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void say(ServerPlayer p, String line) {
        p.sendSystemMessage(Text.of("[" + shortName() + "] ", ChatFormatting.YELLOW).append(Text.of(line, ChatFormatting.WHITE)));
    }

    public void options(ServerPlayer p, String... labels) {
        MutableComponent row = Component.literal("  ");
        int shown = 1;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == null) continue;
            row.append(Text.button((shown++) + ". " + labels[i], "/regnum talk " + getId() + " " + i, "Нажмите, чтобы ответить"));
            row.append(Component.literal("  "));
        }
        p.sendSystemMessage(row);
    }

    /** Исчезнуть через n тиков (ушёл по своим делам). */
    public void leaveSoon(int ticks) {
        leaveIn = ticks;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (getPersona() == Persona.WOUNDED && tickCount % 40 == 0 && !hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MobEffectInstance.INFINITE_DURATION, 4, false, false));
        }
        if (leaveIn > 0 && --leaveIn == 0) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 1, getZ(), 15, 0.3, 0.6, 0.3, 0.02);
            }
            discard();
            return;
        }
        if (getPersona().isFamily()) {
            com.alkimor.regnum.dynasty.Dynasty.tickFamily(this);
            return;
        }
        // странники уходят спустя пару дней, если рядом нет игроков (кроме тех, кто кому-то приглянулся)
        lifetime++;
        if (!affection.isEmpty()) lifetime = Math.min(lifetime, 40000);
        if (lifetime > 48000 && tickCount % 200 == 0 && level().getNearestPlayer(this, 48) == null) {
            discard();
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (familyOf != null && level() instanceof ServerLevel sl) com.alkimor.regnum.dynasty.Dynasty.onFamilyDeath(sl, this);
        if (source.getEntity() instanceof ServerPlayer p && !(getPersona() == Persona.WOUNDED && trap) && familyOf == null) {
            Skills.addHonor(p, getPersona() == Persona.TRICKSTER ? -5 : -15, "убит мирный странник");
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ------------------------------------------------------------------ сохранение

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Persona", entityData.get(DATA_PERSONA));
        tag.putInt("Skin", entityData.get(DATA_SKIN));
        tag.putBoolean("Trap", trap);
        tag.putInt("Lifetime", lifetime);
        if (market != null) tag.put("Market", market.save());
        if (familyOf != null) tag.putUUID("FamilyOf", familyOf);
        tag.putLong("BornDay", bornDay);
        tag.putString("HeirRole", heirRole);
        tag.putBoolean("Child", isChild());
        CompoundTag aff = new CompoundTag();
        affection.forEach((k, v) -> aff.putInt(k.toString(), v));
        tag.put("Affection", aff);
        ListTag list = new ListTag();
        for (UUID u : done) list.add(NbtUtils.createUUID(u));
        tag.put("Done", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_PERSONA, tag.getInt("Persona"));
        entityData.set(DATA_SKIN, tag.getInt("Skin"));
        trap = tag.getBoolean("Trap");
        lifetime = tag.getInt("Lifetime");
        if (tag.contains("Market")) market = com.alkimor.regnum.trade.MarketState.load(tag.getCompound("Market"));
        familyOf = tag.hasUUID("FamilyOf") ? tag.getUUID("FamilyOf") : null;
        bornDay = tag.contains("BornDay") ? tag.getLong("BornDay") : -1;
        heirRole = tag.getString("HeirRole");
        setChild(tag.getBoolean("Child"));
        affection.clear();
        CompoundTag aff = tag.getCompound("Affection");
        for (String k : aff.getAllKeys()) {
            try {
                affection.put(UUID.fromString(k), aff.getInt(k));
            } catch (IllegalArgumentException ignored) {
            }
        }
        done.clear();
        for (Tag t : tag.getList("Done", Tag.TAG_INT_ARRAY)) done.add(NbtUtils.loadUUID(t));
    }
}
