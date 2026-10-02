package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.survival.Skills;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Разбойник: участник набегов на города и засад. Варианты: головорез, лучник, атаман. */
public class BanditEntity extends Monster implements RangedAttackMob {
    public static final int THUG = 0, ARCHER = 1, CAPTAIN = 2;
    public static final String CAMP_BOSS_TAG = "regnum_camp_boss";
    private static final String[] CAPTAIN_NAMES = {"Кривой Ждан", "Бурый Тихон", "Ярый Гордей", "Чёрный Вакула", "Хромой Неждан", "Рыжий Казимир"};

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(BanditEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(BanditEntity.class, EntityDataSerializers.INT);

    @Nullable private UUID raidCity;
    @Nullable private BlockPos raidHall;
    private int lootCooldown = 0;

    public BanditEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, THUG);
        builder.define(DATA_SKIN, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedBowAttackGoal<BanditEntity>(this, 1.0, 25, 15f) {
            @Override
            public boolean canUse() {
                return isArcher() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            public boolean canUse() {
                return !isArcher() && super.canUse();
            }
        });
        goalSelector.addGoal(4, new com.alkimor.regnum.kingdom.ai.BanditMarchGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this, BanditEntity.class));
        targetSelector.addGoal(2, new com.alkimor.regnum.kingdom.ai.FoeScanGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new com.alkimor.regnum.kingdom.ai.FoeScanGoal<>(this, SoldierEntity.class, true));
        targetSelector.addGoal(4, new com.alkimor.regnum.kingdom.ai.FoeScanGoal<>(this, IronGolem.class, true));
        targetSelector.addGoal(5, new com.alkimor.regnum.kingdom.ai.FoeScanGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        if (getMainHandItem().isEmpty()) setup(random.nextFloat() < 0.4f ? ARCHER : THUG);
        return d;
    }

    /** Выбрать вариант и выдать снаряжение. */
    public void setup(int variant) {
        entityData.set(DATA_VARIANT, variant);
        entityData.set(DATA_SKIN, random.nextInt(1000));
        ItemStack hood = new ItemStack(Items.LEATHER_HELMET);
        hood.set(DataComponents.DYED_COLOR, new DyedItemColor(variant == CAPTAIN ? 0x3B0A0A : 0x4A3B2A, false));
        setItemSlot(EquipmentSlot.HEAD, hood);
        switch (variant) {
            case ARCHER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            case CAPTAIN -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CultureWeapons.STEPPE_SWORD.get()));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
                AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(50.0);
                AttributeInstance dmg = getAttribute(Attributes.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(5.0);
                setHealth(getMaxHealth());
                setCustomName(Component.literal("Атаман " + CAPTAIN_NAMES[random.nextInt(CAPTAIN_NAMES.length)]));
                setCustomNameVisible(true);
                xpReward = 30;
            }
            default -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(switch (random.nextInt(3)) {
                case 0 -> CultureWeapons.CLANS_AXE.get();
                case 1 -> CultureWeapons.NORTH_SPEAR.get();
                default -> CultureWeapons.WEST_MACE.get();
            }));
        }
        setDropChance(EquipmentSlot.HEAD, 0f);
        setDropChance(EquipmentSlot.CHEST, 0.05f);
        setDropChance(EquipmentSlot.LEGS, 0.05f);
        setDropChance(EquipmentSlot.MAINHAND, 0.08f);
    }

    public void joinRaid(UUID city, BlockPos hall) {
        this.raidCity = city;
        this.raidHall = hall;
        setPersistenceRequired();
    }

    @Nullable
    public BlockPos getRaidHall() {
        return raidHall;
    }

    public int getVariant() {
        return entityData.get(DATA_VARIANT);
    }

    public int getSkinIndex() {
        return entityData.get(DATA_SKIN);
    }

    public boolean isArcher() {
        return getVariant() == ARCHER;
    }

    /** Оптимизация плотных строёв: расталкивание соседей считаем через тик — иначе O(n²) в толпе. */
    @Override
    protected void pushEntities() {
        if (!level().isClientSide && ((tickCount + getId()) & 1) == 1) return;
        super.pushEntities();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || raidCity == null || raidHall == null) return;
        if (lootCooldown > 0) lootCooldown--;
        if (lootCooldown == 0 && blockPosition().closerThan(raidHall, 5.0) && level() instanceof ServerLevel sl) {
            lootCooldown = 40;
            KingdomManager.plunder(sl.getServer(), raidCity, this);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        ItemStack bow = getMainHandItem();
        Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), bow.is(Items.BOW) ? bow : null);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + dist * 0.2, dz, 1.6f, 14 - level().getDifficulty().getId() * 4);
        playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
        level().addFreshEntity(arrow);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (source.getEntity() instanceof ServerPlayer p) {
            Skills.addHonor(p, getVariant() == CAPTAIN ? 3 : 1, "разбойник повержен");
            if (getTags().contains(CAMP_BOSS_TAG)) {
                Skills.addHonor(p, 5, "лагерь разбойников разорён");
                Skills.chronicle(p, "Разорён лагерь разбойников, повержен " + getName().getString());
                Skills.addXp(p, com.alkimor.regnum.survival.Weapons.skill(com.alkimor.regnum.survival.Weapons.melee(p.getMainHandItem())), 20);
                Skills.addXp(p, com.alkimor.regnum.survival.Skill.ROGUERY, 10);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return raidCity == null && !hasCustomName() && super.removeWhenFarAway(distance);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("Skin", getSkinIndex());
        if (raidCity != null) tag.putUUID("RaidCity", raidCity);
        if (raidHall != null) tag.putLong("RaidHall", raidHall.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_VARIANT, tag.getInt("Variant"));
        entityData.set(DATA_SKIN, tag.getInt("Skin"));
        raidCity = tag.hasUUID("RaidCity") ? tag.getUUID("RaidCity") : null;
        raidHall = tag.contains("RaidHall") ? BlockPos.of(tag.getLong("RaidHall")) : null;
    }
}
