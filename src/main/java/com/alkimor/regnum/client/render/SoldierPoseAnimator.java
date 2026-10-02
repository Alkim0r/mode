package com.alkimor.regnum.client.render;

import com.alkimor.regnum.kingdom.Formation;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.kingdom.SoldierType;
import net.minecraft.util.Mth;

/**
 * Небольшие процедурные добавки к ванильной позе. Они только меняют вид модели:
 * попадания, приказы и время атаки по-прежнему полностью определяет сервер.
 */
final class SoldierPoseAnimator {
    private SoldierPoseAnimator() {}

    static void apply(SoldierEntity soldier, RegnumHumanoidModel<?> model, float limbSwing,
                      float limbSwingAmount, float ageInTicks) {
        SoldierType type = soldier.getSoldierType();
        SoldierType.Role role = type.role;
        float movement = Mth.clamp(limbSwingAmount, 0f, 1f);
        float idle = 1f - movement;
        float breath = Mth.sin(ageInTicks * 0.09f) * 0.018f * idle;
        model.body.xRot += breath;
        model.head.xRot += Mth.sin(ageInTicks * 0.065f) * 0.012f * idle;

        boolean mounted = type.mounted() && soldier.isPassenger();
        if (mounted) {
            model.body.xRot -= 0.08f;
            model.rightLeg.xRot = -1.35f + Mth.sin(limbSwing * 0.45f) * movement * 0.08f;
            model.leftLeg.xRot = -1.35f - Mth.sin(limbSwing * 0.45f) * movement * 0.08f;
            model.rightLeg.yRot = 0.12f;
            model.leftLeg.yRot = -0.12f;
        }

        if (soldier.getFormation() == Formation.PHALANX && role == SoldierType.Role.SPEAR) {
            model.rightArm.xRot = -1.85f;
            model.rightArm.zRot = -0.12f;
        } else if (soldier.getFormation() == Formation.SHIELD_WALL && role == SoldierType.Role.SHIELD
                && !soldier.isBlocking()) {
            model.leftArm.xRot = -1.05f;
            model.leftArm.zRot = 0.16f;
        }

        // Не терять визуальную реакцию на удар, когда юнит уже отступает.
        if (soldier.hurtTime > 0) {
            float recoil = Math.min(1f, soldier.hurtTime / 8f);
            float side = (soldier.getId() & 1) == 0 ? 1f : -1f;
            model.body.zRot += side * 0.12f * recoil;
            model.head.zRot -= side * 0.10f * recoil;
        }

        // Слегка расслабить конечности в коротком окне смерти, не меняя
        // ванильное вращение тела, которое выполняет рендерер сущности.
        if (soldier.deathTime > 0) {
            float fallen = Mth.clamp(soldier.deathTime / 18f, 0f, 1f);
            float side = (soldier.getId() & 1) == 0 ? 1f : -1f;
            model.head.xRot += 0.12f * fallen;
            model.rightArm.zRot += side * 0.20f * fallen;
            model.leftArm.zRot -= side * 0.16f * fallen;
            model.rightLeg.xRot += 0.10f * fallen;
            model.leftLeg.xRot -= 0.08f * fallen;
            return;
        }

        if (soldier.isDodging() && !mounted) {
            applyDodge(soldier, model, ageInTicks);
            return;
        }

        if (soldier.routing()) {
            model.body.xRot -= 0.16f;
            model.rightArm.zRot -= 0.12f;
            model.leftArm.zRot += 0.12f;
            return;
        }

        float partialTick = Mth.clamp(ageInTicks - soldier.tickCount, 0f, 1f);
        float attack = Mth.clamp(soldier.getAttackAnim(partialTick), 0f, 1f);
        if (attack > 0f) {
            if (role == SoldierType.Role.GUNNER
                    || soldier.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem
                    || soldier.getMainHandItem().getItem() instanceof net.minecraft.world.item.CrossbowItem) {
                applyRangedShot(type, model, attack);
                return;
            }
            float swing = Mth.sin(attack * (float) Math.PI);
            switch (role) {
                case SPEAR -> {
                    model.rightArm.xRot -= 0.34f * swing;
                    model.rightArm.zRot -= 0.24f * swing;
                }
                case HEAVY -> {
                    model.rightArm.zRot -= 0.42f * swing;
                    model.leftArm.zRot += 0.20f * swing;
                    model.body.yRot += 0.14f * swing;
                }
                case SHIELD -> {
                    model.rightArm.xRot -= 0.18f * swing;
                    if (soldier.isBlocking()) model.leftArm.xRot = -1.35f;
                }
                case CAVALRY, HORSE_ARCHER -> {
                    model.rightArm.zRot -= 0.16f * swing;
                    if (mounted) model.body.xRot -= 0.08f * swing;
                }
                case GUNNER -> model.rightArm.xRot += 0.16f * swing;
                default -> model.rightArm.zRot -= 0.12f * swing;
            }
        }

    }

    /** Direction comes from the actual server escape path; no artificial movement or invulnerability. */
    private static void applyDodge(SoldierEntity soldier, RegnumHumanoidModel<?> model, float age) {
        float partial = Mth.clamp(age - soldier.tickCount, 0, 1);
        float duration = Math.max(4, Math.min(12, soldier.getDodgeDuration()));
        float t = Mth.clamp((soldier.getDodgeAge() + partial) / duration, 0, 1);
        float lean = Mth.sin(t * Mth.PI);
        float direction = soldier.getDodgeRelativeYaw() * Mth.DEG_TO_RAD;
        float side = Mth.sin(direction), forward = Mth.cos(direction);
        model.body.xRot += 0.28F * forward * lean + 0.12F;
        model.body.zRot -= 0.36F * side * lean;
        model.head.xRot -= 0.12F * forward * lean;
        model.head.zRot += 0.18F * side * lean;
        model.rightArm.xRot = -0.85F;
        model.leftArm.xRot = soldier.getSoldierType().role == SoldierType.Role.SHIELD ? -1.35F : -0.85F;
        model.rightArm.zRot = -0.25F - 0.15F * side * lean;
        model.leftArm.zRot = 0.25F - 0.15F * side * lean;
    }

    /** Six-tick server swing is a shot/release signal, not a reload timer. */
    private static void applyRangedShot(SoldierType type, RegnumHumanoidModel<?> model, float attack) {
        var clip = model.rangedAnimator;
        clip.update(attack * 6);
        clip.startKeyframe(1);
        if (type.role == SoldierType.Role.GUNNER || type == SoldierType.CROSSBOW) {
            float recoil = type == SoldierType.BOMBARDIER ? 1.45F : type == SoldierType.CROSSBOW ? 0.55F : 1F;
            clip.rotate(model.body, -0.10F * recoil, 0, 0);
            clip.rotate(model.head, 0.07F * recoil, 0, 0);
            clip.rotate(model.rightArm, -0.15F * recoil, 0, 0);
            clip.rotate(model.leftArm, -0.12F * recoil, 0, 0);
            clip.move(model.rightArm, 0, 0, 0.8F * recoil);
            clip.move(model.leftArm, 0, 0, 0.8F * recoil);
        } else {
            clip.rotate(model.rightArm, 0.18F, -0.24F, 0);
            clip.rotate(model.body, 0, -0.04F, 0);
        }
        clip.endKeyframe();
        clip.resetKeyframe(5);
    }
}
