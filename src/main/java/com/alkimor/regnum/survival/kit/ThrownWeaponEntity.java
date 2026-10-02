package com.alkimor.regnum.survival.kit;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Летящий дротик или топорик. */
public class ThrownWeaponEntity extends ThrowableItemProjectile {
    private float damage = 6f;

    public ThrownWeaponEntity(EntityType<? extends ThrownWeaponEntity> type, Level level) {
        super(type, level);
    }

    public ThrownWeaponEntity(Level level, LivingEntity owner, ItemStack stack, float damage) {
        super(KitModule.THROWN_WEAPON.get(), owner, level);
        setItem(stack);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return KitModule.JAVELIN.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity target = hit.getEntity();
        Entity owner = getOwner();
        float speed = (float) getDeltaMovement().length();
        float dmg = damage * Math.max(0.6f, Math.min(1.3f, speed / 1.6f));
        target.hurt(damageSources().thrown(this, owner), dmg);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.9f, 1.1f);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel sl)) return;
        ItemStack stack = getItem().copy();
        if (random.nextFloat() < 0.12f) {
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.05);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.6f, 1.2f);
        } else {
            ItemEntity drop = new ItemEntity(sl, getX(), getY(), getZ(), stack);
            drop.setDeltaMovement(0, 0.1, 0);
            drop.setPickUpDelay(10);
            sl.addFreshEntity(drop);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
    }
}
