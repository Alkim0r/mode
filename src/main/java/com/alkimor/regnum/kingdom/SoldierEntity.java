package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.ai.SoldierAssistKingGoal;
import com.alkimor.regnum.kingdom.ai.SoldierMoveGoal;
import com.alkimor.regnum.kingdom.ai.SoldierRangedGoal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Солдат королевства. Подчиняется владельцу-королю, состоит в одном из 4 отрядов,
 * выполняет приказы и держит строй.
 */
public class SoldierEntity extends PathfinderMob implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CULTURE = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DODGE_START = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DODGE_LEN = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DODGE_YAW = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.FLOAT);

    @Nullable private UUID owner;
    @Nullable private UUID cityId;
    /** Королевство ИИ, которому служит солдат (null — обычный солдат игрока). */
    @Nullable private UUID realmId;
    @Nullable private UUID attackTargetId;
    /** Военачальник, которому король передал бойца (null — подчиняется королю). */
    @Nullable private UUID commander;
    private float morale = 60f;
    private int routTicks = 0;
    private int squad = 1;
    private Order order = Order.FOLLOW;
    private Formation formation = Formation.LINE;
    private int slot = 0;
    private int squadSize = 1;
    private float facing = 0f;
    private Vec3 post = Vec3.ZERO;
    private Vec3 patrolPoint = Vec3.ZERO;
    private int kills = 0;
    private final int[] techXp = new int[Technique.values().length];
    /** Радиус территории для приказов «Патруль» и «Охрана города». */
    private int cityRadius = 32;
    private int retreatTicks = 0;
    @Nullable private UUID sparWith;
    private int sparTicks = 0;

    private static final net.minecraft.resources.ResourceLocation RANK_HP = com.alkimor.regnum.Regnum.id("soldier_rank_health");
    private static final net.minecraft.resources.ResourceLocation RANK_DMG = com.alkimor.regnum.Regnum.id("soldier_rank_damage");
    private static final net.minecraft.resources.ResourceLocation LEADER_ARMOR = com.alkimor.regnum.Regnum.id("leadership_armor");

    public SoldierEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        ((net.minecraft.world.entity.ai.navigation.GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, SoldierType.SWORDSMAN.ordinal());
        builder.define(DATA_SKIN, 0);
        builder.define(DATA_CULTURE, 0);
        builder.define(DATA_DODGE_START, 0);
        builder.define(DATA_DODGE_LEN, 0);
        builder.define(DATA_DODGE_YAW, 0f);
    }

    // ---- синхронизированное уклонение: клиент рисует настоящий клип ухода, а не угадывает по скорости
    /** Запускает уклонение: dir — мировое направление ухода, ticks — ожидаемая длительность. */
    public void startDodge(net.minecraft.world.phys.Vec3 dir, int ticks) {
        entityData.set(DATA_DODGE_START, (int) level().getGameTime());
        entityData.set(DATA_DODGE_LEN, Math.max(4, Math.min(30, ticks)));
        entityData.set(DATA_DODGE_YAW, (float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
    }

    public void endDodge() {
        entityData.set(DATA_DODGE_LEN, 0);
    }

    public boolean isDodging() {
        int len = entityData.get(DATA_DODGE_LEN);
        return len > 0 && (int) level().getGameTime() - entityData.get(DATA_DODGE_START) <= len;
    }

    public int getDodgeAge() {
        return isDodging() ? (int) level().getGameTime() - entityData.get(DATA_DODGE_START) : 0;
    }

    public int getDodgeDuration() {
        return isDodging() ? entityData.get(DATA_DODGE_LEN) : 0;
    }

    /** Мировой угол направления ухода (градусы, как yaw). */
    public float getDodgeYaw() {
        return entityData.get(DATA_DODGE_YAW);
    }

    /** Угол ухода относительно корпуса: 0 — вперёд, 90 — вправо, ±180 — назад, −90 — влево. */
    public float getDodgeRelativeYaw() {
        return net.minecraft.util.Mth.wrapDegrees(entityData.get(DATA_DODGE_YAW) - yBodyRot);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(1, new com.alkimor.regnum.kingdom.ai.SoldierDodgeGoal(this));
        goalSelector.addGoal(2, new SoldierRangedGoal(this));
        goalSelector.addGoal(2, new com.alkimor.regnum.kingdom.ai.SoldierCavalryGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() {
                return usesMelee() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return usesMelee() && super.canContinueToUse();
            }

            @Override
            protected boolean canPerformAttack(LivingEntity t) {
                SoldierType.Role r = getSoldierType().role;
                if (r == SoldierType.Role.SPEAR) return isTimeToAttack() && distanceToSqr(t) <= 13.0 && getSensing().hasLineOfSight(t);
                if (r == SoldierType.Role.HEAVY) return isTimeToAttack() && distanceToSqr(t) <= 7.5 && getSensing().hasLineOfSight(t);
                return super.canPerformAttack(t);
            }

            private int lastSwing = -100;

            @Override
            protected void resetAttackCooldown() {
                super.resetAttackCooldown();
                lastSwing = tickCount;
            }

            @Override
            protected boolean isTimeToAttack() {
                // двуручник замахивается медленнее: пауза 34 тика вместо 20
                return super.isTimeToAttack() && (getSoldierType() != SoldierType.GREATSWORD || tickCount - lastSwing >= 34);
            }
        });
        goalSelector.addGoal(3, new com.alkimor.regnum.kingdom.ai.RealmBreachGoal(this));
        goalSelector.addGoal(4, new SoldierMoveGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this, SoldierEntity.class, Player.class));
        targetSelector.addGoal(2, new SoldierAssistKingGoal(this));
        targetSelector.addGoal(2, new com.alkimor.regnum.kingdom.ai.FoeScanGoal<>(this, LivingEntity.class, 10, true, false, e -> Realms.hostile(this, e)).range(() -> Math.max(12, order.engageRadius * 1.5 + 4)));
        targetSelector.addGoal(3, new com.alkimor.regnum.kingdom.ai.FoeScanGoal<>(this, Mob.class, 10, true, false,
                e -> e instanceof Enemy && !(e instanceof Creeper) && withinEngagement(e)).range(() -> Math.max(12, order.engageRadius * 1.5 + 4)));
    }

    // ------------------------------------------------------------------ настройка

    public void setup(SoldierType type, UUID owner, @Nullable UUID cityId, int squad) {
        this.owner = owner;
        this.cityId = cityId;
        this.squad = Math.max(1, Math.min(4, squad));
        entityData.set(DATA_TYPE, type.ordinal());
        entityData.set(DATA_SKIN, random.nextInt(1000));
        if (cityId != null && level() instanceof net.minecraft.server.level.ServerLevel sl) {
            City c = KingdomData.get(sl.getServer()).byId(cityId);
            if (c != null) entityData.set(DATA_CULTURE, c.culture);
        }
        AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(type.health);
        AttributeInstance spd = getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(type.speed());
        AttributeInstance ar = getAttribute(Attributes.ARMOR);
        if (ar != null) ar.setBaseValue(type == SoldierType.SHIELDMAN ? 6.0 : type == SoldierType.HEAVY_CAV ? 4.0 : 2.0);
        setHealth(getMaxHealth());
        equip(type);
        updateName();
    }

    private void equip(SoldierType type) {
        switch (type) {
            case SHIELDMAN -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CultureWeapons.forSoldier(getCulture(), type)));
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            }
            case SPEARMAN -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CultureWeapons.forSoldier(getCulture(), type)));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            }
            case GREATSWORD -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CultureWeapons.forSoldier(getCulture(), type)));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            }
            case LIGHT_CAV -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CultureWeapons.forSoldier(getCulture(), type)));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            }
            case HEAVY_CAV -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CultureWeapons.forSoldier(getCulture(), type)));
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            }
            case HORSE_ARCHER, SCOUT -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            }
            case CROSSBOW -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            }
            case MUSKETEER -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            }
            case BOMBARDIER -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TNT));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            }
            case MILITIA -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CultureWeapons.forSoldier(getCulture(), type)));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            }
            case SWORDSMAN -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CultureWeapons.forSoldier(getCulture(), type)));
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            }
            case ARCHER -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            }
            case KNIGHT -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CultureWeapons.forSoldier(getCulture(), type)));
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            }
        }
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0f);
    }

    // ------------------------------------------------------------------ опыт и звания

    /** 0 — новобранец, 1 — ветеран (5 побед), 2 — элита (15 побед). */
    public int rank() {
        return kills >= 15 ? 2 : kills >= 5 ? 1 : 0;
    }

    public static String rankTitle(int rank) {
        return switch (rank) {
            case 2 -> "Элитный";
            case 1 -> "Ветеран-";
            default -> "";
        };
    }

    public void addKill() {
        int before = rank();
        kills++;
        addMorale(8f);
        if (commander != null && level() instanceof ServerLevel sl0) Commissions.get(sl0.getServer()).addRep(commander, 1);
        if (getOwnerPlayer() instanceof ServerPlayer owner) {
            com.alkimor.regnum.survival.Skills.addXp(owner, com.alkimor.regnum.survival.Skill.TACTICS, 3);
            if (com.alkimor.regnum.survival.Skills.has(owner, com.alkimor.regnum.survival.Perk.TC_VETERANS) && random.nextBoolean()) kills++;
        }
        if (rank() > before) {
            applyRank();
            updateName();
            heal(getMaxHealth());
            if (getOwnerPlayer() instanceof ServerPlayer king) {
                Text.gold(king, getSoldierType().title + " из отряда " + squad + " получил звание: "
                        + (rank() == 2 ? "Элита" : "Ветеран") + " (побед: " + kills + ")");
            }
        }
    }

    private void applyRank() {
        int r = rank();
        AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.removeModifier(RANK_HP);
            if (r > 0) hp.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(RANK_HP, r == 2 ? 0.4 : 0.2,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        AttributeInstance dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.removeModifier(RANK_DMG);
            if (r > 0) dmg.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(RANK_DMG, r,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public int getKills() {
        return kills;
    }

    /** Обновить имя после смены статуса полевого командира. */
    public void refreshLeaderName() {
        updateName();
    }

    private void updateName() {
        String prefix = rankTitle(rank());
        String type = getSoldierType().title;
        if (!prefix.isEmpty() && prefix.endsWith("-")) type = prefix + type.toLowerCase();
        else if (!prefix.isEmpty()) type = prefix + " " + type.toLowerCase();
        boolean fl = getTags().contains("regnum_fieldleader");
        setCustomName(Component.literal((fl ? "★ Полевой командир: " : "") + type + " · Отряд " + squad));
        setCustomNameVisible(fl);
    }

    // ------------------------------------------------------------------ приёмы и спарринг

    public int tech(Technique t) {
        return Technique.levelFor(techXp[t.ordinal()]);
    }

    public void trainTech(Technique t, int xp) {
        int before = tech(t);
        techXp[t.ordinal()] = Math.min(Technique.XP[Technique.MAX], techXp[t.ordinal()] + xp);
        int after = tech(t);
        if (after > before && getOwnerPlayer() instanceof ServerPlayer king) {
            Text.gold(king, getName().getString() + ": приём «" + t.title + "» освоен до " + after + " ур.");
        }
    }

    public boolean isSparringWith(@Nullable Entity e) {
        return sparWith != null && e != null && sparWith.equals(e.getUUID());
    }

    public boolean isSparring() {
        return sparWith != null;
    }

    public void startSpar(Player king) {
        sparWith = king.getUUID();
        sparTicks = 20 * 120;
        setTarget(king);
        Text.gold(king, "Учебный поединок начат! Бой не насмерть. Боец перенимает ваши приёмы: блоки щитом, удары в прыжке, удары с разбега, уклонение, стрельбу издалека.");
    }

    public void endSpar(String reason) {
        if (sparWith == null) return;
        Player king = level().getPlayerByUUID(sparWith);
        sparWith = null;
        sparTicks = 0;
        setTarget(null);
        if (king != null) Text.info(king, "Поединок окончен: " + reason);
    }

    public String techSummary() {
        StringBuilder sb = new StringBuilder();
        for (Technique t : Technique.values()) {
            int l = tech(t);
            if (l > 0) sb.append(sb.isEmpty() ? "" : ", ").append(t.title).append(" ").append(l);
        }
        return sb.isEmpty() ? "приёмов нет" : sb.toString();
    }

    // ------------------------------------------------------------------ доступ

    public SoldierType getSoldierType() {
        return SoldierType.byId(entityData.get(DATA_TYPE));
    }

    public Culture getCulture() {
        return Culture.byId(entityData.get(DATA_CULTURE));
    }

    public void setCulture(Culture c) {
        entityData.set(DATA_CULTURE, c.ordinal());
        if (!level().isClientSide()) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CultureWeapons.forSoldier(c, getSoldierType())));
    }

    public int getSkinIndex() {
        return entityData.get(DATA_SKIN);
    }

    public boolean isArcher() {
        return getSoldierType().ranged();
    }

    /** Ближний бой по общему правилу: не стрелок и не конник в седле (тот бьёт своим манёвром). */
    public boolean usesMelee() {
        SoldierType t = getSoldierType();
        if (t.ranged()) return false;
        return !(t.mounted() && isPassenger());
    }

    public boolean isMountedCavalry() {
        return getSoldierType().role == SoldierType.Role.CAVALRY && isPassenger();
    }

    @Nullable
    public UUID getOwnerId() {
        return owner;
    }

    @Nullable
    public UUID getCityId() {
        return cityId;
    }

    public void clearCity() {
        cityId = null;
    }

    @Nullable
    public UUID realmId() {
        return realmId;
    }

    public void setRealm(@Nullable UUID id) {
        this.realmId = id;
    }

    public boolean isOwnedBy(Entity e) {
        return e != null && ((owner != null && owner.equals(e.getUUID())) || (commander != null && commander.equals(e.getUUID())));
    }

    @Nullable
    public UUID getCommander() {
        return commander;
    }

    public void setCommander(@Nullable UUID c) {
        this.commander = c;
    }

    /** Кто сейчас ведёт бойца: военачальник (если на связи) или король. */
    @Nullable
    public Player getControllerPlayer() {
        if (commander != null) {
            Player c = level().getPlayerByUUID(commander);
            if (c != null) return c;
        }
        return getOwnerPlayer();
    }

    public float morale() {
        return morale;
    }

    public boolean routing() {
        return routTicks > 0;
    }

    public void addMorale(float d) {
        morale = Math.max(0f, Math.min(100f, morale + d));
    }

    @Nullable
    public Player getOwnerPlayer() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    public int getSquad() {
        return squad;
    }

    public void setSquad(int squad) {
        this.squad = squad;
        updateName();
    }

    public Order getOrder() {
        return order;
    }

    public Formation getFormation() {
        return formation;
    }

    public float getFacing() {
        return facing;
    }

    // ------------------------------------------------------------------ приказы

    public void setCityRadius(int r) {
        this.cityRadius = r;
    }

    public void command(Order order, Vec3 post, float facing, Formation formation, int slot, int size, @Nullable UUID target) {
        this.order = order;
        this.post = post;
        this.facing = facing;
        this.formation = formation;
        this.slot = slot;
        this.squadSize = size;
        this.attackTargetId = target;
        if (order != Order.ATTACK_TARGET && getTarget() != null && !withinEngagement(getTarget())) setTarget(null);
        getNavigation().stop();
    }

    /** Точка, где боец должен стоять по текущему приказу. */
    public Vec3 desiredPosition() {
        if (order == Order.FOLLOW) {
            Player king = getControllerPlayer();
            if (king != null) return formation.worldPos(king.position(), king.getYRot(), slot, squadSize, 3.0);
            return position();
        }
        if (order == Order.ATTACK_TARGET) return position();
        if (order == Order.PATROL) return patrolPoint;
        return post;
    }

    public boolean withinEngagement(LivingEntity e) {
        if (isAlliedTo(e)) return false;
        Vec3 center = order == Order.FOLLOW && getControllerPlayer() != null ? getControllerPlayer().position()
                : order == Order.GUARD || order == Order.PATROL ? post : desiredPosition();
        double r = order == Order.GUARD ? cityRadius + 16 : order == Order.PATROL ? cityRadius + 8 : order.engageRadius;
        return e.position().distanceToSqr(center) <= r * r;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (isSparringWith(other)) return false;
        if (other instanceof SoldierEntity s && owner != null && owner.equals(s.owner)) return true;
        if (owner != null && other.getUUID().equals(owner)) return true;
        if (commander != null && other.getUUID().equals(commander)) return true;
        return super.isAlliedTo(other);
    }

    // ------------------------------------------------------------------ тик

    /** Оптимизация: стоящий на месте мирный солдат вдали от игроков думает раз в 4 тика. */
    @Override
    public boolean isEffectiveAi() {
        if (!level().isClientSide && tickCount % 4 != 0 && realmId == null && onGround() && getNavigation().isDone() && getTarget() == null
                && com.alkimor.regnum.core.RegnumConfig.ARMY_LOD.get() && level().getNearestPlayer(this, 72) == null) return false;
        return super.isEffectiveAi();
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
        if (level().isClientSide) return;

        if (!mountSpawned && getSoldierType().mounted() && tickCount > 2) spawnMount();
        if (tickCount > 7200 && getTags().contains("regnum_levy")) { discard(); return; }

        if (order == Order.ATTACK_TARGET && tickCount % 10 == 0) {
            Entity t = attackTargetId != null && level() instanceof ServerLevel sl ? sl.getEntity(attackTargetId) : null;
            if (t instanceof LivingEntity le && le.isAlive()) {
                setTarget(le);
            } else {
                // цель повержена — занять позицию там, где стоим
                command(Order.HOLD, position(), getYRot(), formation, slot, squadSize, null);
            }
        }

        // спарринг
        if (sparWith != null) {
            Player sparKing = level().getPlayerByUUID(sparWith);
            if (sparKing == null || --sparTicks <= 0 || distanceToSqr(sparKing) > 24 * 24) {
                endSpar("время вышло");
            } else if (getTarget() != sparKing) {
                setTarget(sparKing);
            }
            return;
        }

        tickMorale();
        if (routTicks > 0) {
            routTicks--;
            setTarget(null);
            if (tickCount % 10 == 0) {
                LivingEntity foe = nearestFoe(14);
                Vec3 home = desiredPosition();
                Vec3 away = foe == null ? home : position().add(position().subtract(foe.position()).normalize().scale(10));
                getNavigation().moveTo(away.x, away.y, away.z, 1.5);
            }
            if (routTicks == 0) morale = 45f;
            return;
        }

        // патруль: новая точка обхода
        if (order == Order.PATROL && (tickCount % 200 == 0 || patrolPoint.equals(Vec3.ZERO))) {
            double a = random.nextDouble() * Math.PI * 2, d = random.nextDouble() * cityRadius * 0.85;
            patrolPoint = new Vec3(post.x + Math.cos(a) * d, post.y, post.z + Math.sin(a) * d);
        }

        // отход на перевязку
        int heal = tech(Technique.RETREAT_HEAL);
        if (heal > 0 && retreatTicks == 0 && getTarget() != null && getHealth() < getMaxHealth() * 0.3f) {
            retreatTicks = 40 + 40 * heal;
        }
        if (retreatTicks > 0) {
            retreatTicks--;
            setTarget(null);
            if (tickCount % 10 == 0) heal(1.0f);
            if (tickCount % 10 == 0) getNavigation().moveTo(desiredPosition().x, desiredPosition().y, desiredPosition().z, 1.3);
            return;
        }

        // фокус цели: соседи по отряду бьют ту же цель
        int focus = tech(Technique.FOCUS_FIRE);
        if (focus > 0 && getTarget() != null && tickCount % 20 == 0) {
            LivingEntity t = getTarget();
            for (SoldierEntity mate : level().getEntitiesOfClass(SoldierEntity.class, getBoundingBox().inflate(8 + 4 * focus),
                    s -> s != this && s.squad == squad && s.isAlliedTo(this) && s.getTarget() == null && !s.isSparring())) {
                if (mate.withinEngagement(t)) mate.setTarget(t);
            }
        }

        LivingEntity target = getTarget();
        if (target != null && order != Order.ATTACK_TARGET) {
            double r = order.engageRadius * 1.5;
            if (target.position().distanceToSqr(desiredPosition()) > r * r || isAlliedTo(target)) setTarget(null);
        }

        Player kingOnline = getOwnerPlayer();
        boolean warlord = kingOnline != null && com.alkimor.regnum.survival.Skills.has(kingOnline, com.alkimor.regnum.survival.Perk.LD_WARLORD);
        boolean inspire = kingOnline != null && com.alkimor.regnum.survival.Skills.has(kingOnline, com.alkimor.regnum.survival.Perk.LD_INSPIRE)
                && kingOnline.distanceToSqr(this) < 16 * 16;
        int regenEvery = warlord ? 30 : 60;
        if (tickCount % regenEvery == 0 && getTarget() == null && getHealth() < getMaxHealth()) heal(1.0f);
        if (inspire && tickCount % 80 == 0 && getHealth() < getMaxHealth()) heal(1.0f);
        if (order == Order.RETREAT && tickCount % 40 == 0 && kingOnline != null
                && com.alkimor.regnum.survival.Skills.has(kingOnline, com.alkimor.regnum.survival.Perk.TC_RETREAT)) {
            addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 60, 1, true, false));
        }
        if (tickCount % 200 == 0) {
            AttributeInstance armor = getAttribute(Attributes.ARMOR);
            if (armor != null) {
                int want = 0;
                if (kingOnline != null && com.alkimor.regnum.survival.Skills.has(kingOnline, com.alkimor.regnum.survival.Perk.LD_ARMOR)) want += 2;
                if (kingOnline != null && com.alkimor.regnum.survival.Skills.has(kingOnline, com.alkimor.regnum.survival.Perk.TC_DRILL)) want += 2;
                if (cityId != null && level() instanceof ServerLevel fl) {
                    City fc = KingdomData.get(fl.getServer()).byId(cityId);
                    if (fc != null) want += fc.forged;
                }
                var cur = armor.getModifier(LEADER_ARMOR);
                if (cur == null || cur.amount() != want) {
                    armor.removeModifier(LEADER_ARMOR);
                    if (want > 0) armor.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(LEADER_ARMOR, want,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
                }
            }
        }

        // Проверка верности: солдат, которого вычеркнули из списков города (не заплатили жалованье), дезертирует.
        if (cityId != null && tickCount % 200 == 0 && level() instanceof ServerLevel sl) {
            City c = KingdomData.get(sl.getServer()).byId(cityId);
            if (c == null) {
                cityId = null;
            } else if (!c.soldiers.containsKey(getUUID())) {
                if (getOwnerPlayer() instanceof ServerPlayer king) {
                    Text.bad(king, getSoldierType().title + " из отряда " + squad + " дезертировал: казна не платит жалованье.");
                }
                discard();
                return;
            }
        }

        if (getSoldierType() == SoldierType.SCOUT && tickCount % 100 == 40) scoutScan();
        if (tickCount % 20 == 5 && commander != null && level() instanceof ServerLevel al) {
            var cm = Commissions.get(al.getServer()).of(commander);
            if (cm == null) commander = null;
            else if (cm.branch == Commissions.Branch.AIR) tickAirCrew();
        }
        if (order == Order.FOLLOW && tickCount % 20 == 0) {
            Player king = getOwnerPlayer();
            if (king != null && king.level() == level() && distanceToSqr(king) > 40 * 40) tryTeleportNear(king);
        }
    }

    @Nullable
    private LivingEntity nearestFoe(double r) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r), e -> e != this && e.isAlive() && isFoe(e))) {
            double d = e.distanceToSqr(this);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    public boolean isFoePublic(LivingEntity e) {
        return isFoe(e);
    }

    private boolean isFoe(LivingEntity e) {
        if (isAlliedTo(e) || e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (e instanceof Enemy && !(e instanceof Creeper)) return true;
        return Realms.hostile(this, e);
    }

    /**
     * Боевой дух (раз в секунду): падает, когда врагов заметно больше, когда бойцу тяжело или нет командира рядом,
     * растёт, когда рядом свои и командир. Ниже 20 — бегство на 8 секунд.
     */
    private void tickMorale() {
        if ((tickCount + getId()) % 20 != 3 || getTags().contains(Realms.RULER_TAG)) return;
        SoldierType t = getSoldierType();
        float resist = switch (t.role) {
            case SHIELD, HEAVY -> 0.6f;
            case CAVALRY -> 0.8f;
            default -> t == SoldierType.MILITIA ? 1.4f : t == SoldierType.KNIGHT ? 0.7f : 1.0f;
        };
        int foes = 0, friends = 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10), e -> e != this && e.isAlive())) {
            if (e instanceof SoldierEntity so && isAlliedTo(so)) friends++;
            else if (isFoe(e)) foes++;
        }
        float d = 0f;
        if (foes > 0) {
            float ratio = foes / (float) (friends + 1);
            if (ratio > 1.4f) d -= 2.5f * (ratio - 1.2f) * resist;
            else d += 0.6f;
        } else {
            d += 2.5f;
        }
        if (getHealth() < getMaxHealth() * 0.3f) d -= 2f * resist;
        Player lead = getControllerPlayer();
        if (lead != null && lead.level() == level()) {
            double dist = lead.distanceTo(this);
            if (dist < 24) d += 1.2f;
            else if (dist > 64) d -= 0.6f;
        }
        if (commander != null && level() instanceof ServerLevel sl) {
            var c = Commissions.get(sl.getServer()).of(commander);
            if (c != null) d += c.moraleBonus() * 0.1f;
        }
        if (realmId != null) d += 0.4f;
        d += SquadRoles.bannerBonus(this);
        d += supplyMorale(lead);
        addMorale(d);
        if (morale < 20f && foes > 0 && routTicks == 0) {
            routTicks = 160;
            if (getOwnerPlayer() instanceof ServerPlayer king && random.nextInt(4) == 0) Text.bad(king, getName().getString() + " бежит с поля боя — дух сломлен!");
        }
    }

    /**
     * Снабжение (I026): далеко от родного города (160+ блоков) армия держится на провианте командира. Нет еды в сумках —
     * дух падает; есть — иногда съедается ломоть. Рядом с городом ничего не меняется.
     */
    private float supplyMorale(Player lead) {
        if (cityId == null || !(level() instanceof ServerLevel sl)) return 0f;
        City home = KingdomData.get(sl.getServer()).byId(cityId);
        if (home == null || home.hall.distSqr(blockPosition()) < 160 * 160) return 0f;
        if (lead == null) return -1.0f;
        for (var st : lead.getInventory().items) {
            if (st.isEmpty() || st.get(net.minecraft.core.component.DataComponents.FOOD) == null) continue;
            if (random.nextInt(600) == 0) { st.shrink(1); }
            return 0f;
        }
        if (random.nextInt(40) == 0 && lead instanceof ServerPlayer sp) Text.bad(sp, "Армия вдали от города без провианта: дух падает. Возьмите еду в сумку или вернитесь.");
        return -1.5f;
    }

    private boolean mountSpawned = false;
    public static final String MOUNT_TAG = "regnum_mount";

    /** Верховому выдаётся конь (один раз за жизнь бойца): лёгкий, тяжёлый в броне или степной скакун. */
    private void spawnMount() {
        mountSpawned = true;
        if (isPassenger() || !(level() instanceof ServerLevel sl)) return;
        net.minecraft.world.entity.animal.horse.Horse h = EntityType.HORSE.create(sl);
        if (h == null) return;
        h.moveTo(getX(), getY(), getZ(), getYRot(), 0f);
        h.setTamed(true);
        h.addTag(MOUNT_TAG);
        h.setPersistenceRequired();
        h.equipSaddle(new ItemStack(Items.SADDLE), null);
        SoldierType t = getSoldierType();
        double hp = t == SoldierType.HEAVY_CAV ? 34 : t == SoldierType.HORSE_ARCHER ? 22 : 24;
        double sp = t == SoldierType.HEAVY_CAV ? 0.27 : t == SoldierType.HORSE_ARCHER ? 0.34 : t == SoldierType.SCOUT ? 0.38 : 0.32;
        AttributeInstance a = h.getAttribute(Attributes.MAX_HEALTH);
        if (a != null) a.setBaseValue(hp);
        a = h.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a != null) a.setBaseValue(sp);
        a = h.getAttribute(Attributes.JUMP_STRENGTH);
        if (a != null) a.setBaseValue(0.7);
        a = h.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (a != null) a.setBaseValue(t == SoldierType.HEAVY_CAV ? 0.6 : 0.2);
        h.setHealth((float) hp);
        if (t == SoldierType.HEAVY_CAV) h.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.IRON_HORSE_ARMOR));
        else if (t == SoldierType.LIGHT_CAV) h.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.LEATHER_HORSE_ARMOR));
        h.setDropChance(EquipmentSlot.BODY, 0f);
        boolean added = sl.addFreshEntity(h);
        boolean mounted = startRiding(h, true);
        if (!added || !mounted) com.alkimor.regnum.Regnum.LOGGER.warn("[Soldier] конь не выдан: added={} riding={} type={} pos={}", added, mounted, getSoldierType(), blockPosition());
    }

    @Override
    public void remove(RemovalReason reason) {
        Entity v = getVehicle();
        super.remove(reason);
        if (reason.shouldDestroy() && v != null && v.getTags().contains(MOUNT_TAG) && !v.isRemoved()) v.discard();
    }

    /** Разведчик: открывает королевства в 110 блоках и копит разведданные о них. */
    private void scoutScan() {
        if (!(level() instanceof ServerLevel sl) || owner == null || realmId != null || sl.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        KingdomData data = KingdomData.get(sl.getServer());
        boolean dirty = false;
        for (Realm r : data.realms()) {
            double d = Math.hypot(r.x - getX(), r.z - getZ());
            if (d > 110) continue;
            boolean news = false;
            if (r.known < 2) {
                r.known = 2;
                news = true;
            }
            if (r.intel < 60) {
                r.intel = Math.min(60, r.intel + 4);
                dirty = true;
            }
            if (news) dirty = true;
            if (news || tickCount % 600 == 40) {
                if (getOwnerPlayer() instanceof ServerPlayer king) {
                    Text.good(king, "Разведчик донёс: «" + r.name + "» — гарнизон ~" + r.strength + ", отношения " + r.relation + " (" + r.stateTitle() + ").");
                }
            }
        }
        if (dirty) data.setDirty();
    }

    /** Воздушный экипаж: не разбивается при падении и летит вместе с военачальником на дирижабле/самолёте. */
    private void tickAirCrew() {
        if (order != Order.FOLLOW || !(Compat.aeronautics() || Compat.create())) return;
        addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 60, 0, true, false));
        Player lead = level().getPlayerByUUID(commander);
        if (lead == null || lead.level() != level()) return;
        net.minecraft.world.entity.Entity veh = lead.getVehicle();
        boolean techVehicle = veh != null && Compat.isTechNamespace(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(veh.getType()).getNamespace());
        boolean airborne = lead.isFallFlying() || techVehicle;
        double d2 = distanceToSqr(lead);
        if (airborne && d2 > 9 * 9 && d2 < 110 * 110) {
            double nx = lead.getX() + random.nextInt(5) - 2, ny = lead.getY() + 0.2, nz = lead.getZ() + random.nextInt(5) - 2;
            if (level().noCollision(this, getBoundingBox().move(nx - getX(), ny - getY(), nz - getZ()))) {
                if (isPassenger()) stopRiding();
                moveTo(nx, ny, nz, getYRot(), getXRot());
                getNavigation().stop();
                fallDistance = 0;
            }
        }
    }

    private void tryTeleportNear(Player king) {
        if (isPassenger()) stopRiding();
        BlockPos base = king.blockPosition();
        for (int i = 0; i < 10; i++) {
            BlockPos p = base.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            if (level().getBlockState(p.below()).isSolid() && level().noCollision(this, getBoundingBox().move(Vec3.atBottomCenterOf(p).subtract(position())))) {
                moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, getYRot(), getXRot());
                getNavigation().stop();
                return;
            }
        }
    }

    // ------------------------------------------------------------------ дальний бой

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND, true); // сигнал выстрела для клиентской анимации
        ItemStack bow = getMainHandItem();
        if (getSoldierType() == SoldierType.BOMBARDIER) {
            fireBombard(target);
            return;
        }
        boolean musket = getSoldierType() == SoldierType.MUSKETEER;
        boolean xbow = getSoldierType() == SoldierType.CROSSBOW;
        Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), bow.is(Items.BOW) || bow.is(Items.CROSSBOW) ? bow : null);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (musket) {
            // мушкетная пуля: очень быстро, очень больно, почти без разброса
            arrow.setBaseDamage(arrow.getBaseDamage() + 2.4);
            arrow.shoot(dx, dy + dist * 0.03, dz, 3.8f, 0.7f);
            playSound(SoundEvents.FIREWORK_ROCKET_BLAST, 1.6f, 0.6f);
            if (level() instanceof ServerLevel sl) {
                Vec3 look = getLookAngle();
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX() + look.x, getEyeY() - 0.2, getZ() + look.z, 6, 0.1, 0.1, 0.1, 0.01);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, getX() + look.x, getEyeY() - 0.2, getZ() + look.z, 1, 0, 0, 0, 0);
            }
        } else if (xbow) {
            // арбалетный болт: быстрее, точнее и больнее
            arrow.setBaseDamage(arrow.getBaseDamage() + 1.6);
            arrow.shoot(dx, dy + dist * 0.14, dz, 2.6f, 1.0f);
            playSound(SoundEvents.CROSSBOW_SHOOT, 1.0f, 1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
        } else {
            double inacc = isPassenger() ? 7.0 : 4.0;
            arrow.shoot(dx, dy + dist * 0.2, dz, 1.6f, (float) inacc);
            playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
        }
        level().addFreshEntity(arrow);
    }

    /** Выстрел бомбарды: урон по площади только врагам, без разрушения мира. */
    private void fireBombard(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 c = target.position();
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.8f, 0.7f);
        Vec3 look = getLookAngle();
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX() + look.x * 1.2, getEyeY() - 0.3, getZ() + look.z * 1.2, 12, 0.2, 0.2, 0.2, 0.02);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 3, 0.8, 0.4, 0.8, 0);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.4f, 0.9f);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(3.5), e -> e != this && e.isAlive() && isFoe(e))) {
            float f = (float) Math.max(0.25, 1.0 - e.position().distanceTo(c) / 4.0);
            e.hurt(damageSources().explosion(this, this), 15f * f);
            Vec3 push = e.position().subtract(c).normalize().scale(0.6 * f);
            e.push(push.x, 0.3 * f, push.z);
        }
    }

    // ------------------------------------------------------------------ взаимодействие

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() instanceof CommanderBatonItem) return InteractionResult.PASS;
        if (realmId != null && getTags().contains(Realms.RULER_TAG)) {
            if (!level().isClientSide && player instanceof ServerPlayer sp) Realms.talk(sp, this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isOwnedBy(player)) return InteractionResult.PASS;
        if (!level().isClientSide && player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (isSparring()) endSpar("король остановил бой");
            else startSpar(player);
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide) {
            player.sendSystemMessage(Text.of(getName().getString() + " — побед: " + kills, ChatFormatting.GOLD)
                    .append(Text.of(" — здоровье " + Math.round(getHealth()) + "/" + Math.round(getMaxHealth())
                            + ", дух " + Math.round(morale) + "%" + (routTicks > 0 ? " (бежит!)" : "") + ", приказ: " + order.title + ", строй: " + formation.title, ChatFormatting.GRAY)));
            player.sendSystemMessage(Text.of("  Приёмы: " + techSummary() + ". Shift+ПКМ пустой рукой — учебный поединок.", ChatFormatting.DARK_AQUA));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            float loss = getTags().contains(Realms.RULER_TAG) ? 25f : 6f;
            for (SoldierEntity al : sl.getEntitiesOfClass(SoldierEntity.class, getBoundingBox().inflate(12), a -> a != this && a.isAlive() && a.isAlliedTo(this))) {
                al.addMorale(-loss * (al.getSoldierType().role == SoldierType.Role.SHIELD || al.getSoldierType().role == SoldierType.Role.HEAVY ? 0.5f : 1f));
            }
            if (commander != null) {
                Player cp = sl.getPlayerByUUID(commander);
                if (cp != null) Text.bad(cp, "Ваш " + getSoldierType().title.toLowerCase() + " пал в бою.");
            }
            KingdomData.get(sl.getServer()).onSoldierLost(cityId, getUUID());
            if (realmId != null) {
                Realm rr = KingdomData.get(sl.getServer()).realm(realmId);
                if (rr != null) Realms.onRealmSoldierKilled(sl, rr, source.getEntity());
            }
            if (getOwnerPlayer() instanceof ServerPlayer king) {
                Text.bad(king, "Ваш " + getSoldierType().title.toLowerCase() + " из отряда " + squad + " пал в бою.");
            }
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
        if (owner != null) tag.putUUID("Owner", owner);
        if (cityId != null) tag.putUUID("City", cityId);
        if (realmId != null) tag.putUUID("Realm", realmId);
        if (attackTargetId != null) tag.putUUID("AttackTarget", attackTargetId);
        tag.putInt("SoldierType", entityData.get(DATA_TYPE));
        tag.putInt("Skin", entityData.get(DATA_SKIN));
        tag.putInt("Culture", entityData.get(DATA_CULTURE));
        tag.putInt("Squad", squad);
        tag.putInt("Kills", kills);
        tag.putIntArray("TechXp", techXp);
        tag.putInt("CityRadius", cityRadius);
        tag.putBoolean("MountSpawned", mountSpawned);
        if (commander != null) tag.putUUID("Commander", commander);
        tag.putFloat("Morale", morale);
        tag.putInt("Order", order.ordinal());
        tag.putInt("Formation", formation.ordinal());
        tag.putInt("Slot", slot);
        tag.putInt("SquadSize", squadSize);
        tag.putFloat("Facing", facing);
        tag.putDouble("PostX", post.x);
        tag.putDouble("PostY", post.y);
        tag.putDouble("PostZ", post.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        cityId = tag.hasUUID("City") ? tag.getUUID("City") : null;
        realmId = tag.hasUUID("Realm") ? tag.getUUID("Realm") : null;
        attackTargetId = tag.hasUUID("AttackTarget") ? tag.getUUID("AttackTarget") : null;
        entityData.set(DATA_TYPE, tag.getInt("SoldierType"));
        entityData.set(DATA_SKIN, tag.getInt("Skin"));
        entityData.set(DATA_CULTURE, tag.getInt("Culture"));
        squad = Math.max(1, tag.getInt("Squad"));
        kills = tag.getInt("Kills");
        int[] tx = tag.getIntArray("TechXp");
        System.arraycopy(tx, 0, techXp, 0, Math.min(tx.length, techXp.length));
        if (tag.contains("CityRadius")) cityRadius = tag.getInt("CityRadius");
        mountSpawned = tag.getBoolean("MountSpawned");
        commander = tag.hasUUID("Commander") ? tag.getUUID("Commander") : null;
        if (tag.contains("Morale")) morale = tag.getFloat("Morale");
        order = Order.byId(tag.getInt("Order"));
        formation = Formation.byId(tag.getInt("Formation"));
        slot = tag.getInt("Slot");
        squadSize = Math.max(1, tag.getInt("SquadSize"));
        facing = tag.getFloat("Facing");
        post = new Vec3(tag.getDouble("PostX"), tag.getDouble("PostY"), tag.getDouble("PostZ"));
    }
}
