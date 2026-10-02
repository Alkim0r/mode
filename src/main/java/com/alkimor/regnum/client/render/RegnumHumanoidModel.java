package com.alkimor.regnum.client.render;

import com.alkimor.regnum.client.model.ModelIndex;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.kingdom.SoldierType;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Человекоподобная модель Regnum со своей геометрией (из tools/modelgen):
 * позы рук по оружию, развевающийся плащ, скрытие декора в облегчённом режиме.
 */
public class RegnumHumanoidModel<T extends LivingEntity> extends HumanoidModel<T> {
    private final List<ModelPart> decor;
    protected final List<ModelPart> poseParts;
    private final ModelPart cape;
    final AdaptedModelAnimator rangedAnimator;

    public RegnumHumanoidModel(ModelPart root, ModelLayerLocation layer) {
        super(root);
        this.poseParts = root.getAllParts().toList();
        this.decor = ModelIndex.decor(layer, root);
        this.cape = child(body, "cape");
        this.rangedAnimator = new AdaptedModelAnimator(List.of(head, body, rightArm, leftArm));
    }

    private static ModelPart child(ModelPart p, String name) {
        try {
            return p.getChild(name);
        } catch (java.util.NoSuchElementException e) {
            return null;
        }
    }

    public void showDecor(boolean visible) {
        for (ModelPart p : decor) p.visible = visible;
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        rightArmPose = ArmPose.EMPTY;
        leftArmPose = ArmPose.EMPTY;
        ItemStack main = entity.getMainHandItem();
        ItemStack off = entity.getOffhandItem();
        boolean aggressive = entity instanceof Mob m && m.isAggressive();
        if (entity instanceof SoldierEntity soldier && soldier.getSoldierType().role == SoldierType.Role.GUNNER && aggressive) {
            rightArmPose = ArmPose.CROSSBOW_HOLD;
        } else if (main.getItem() instanceof BowItem && aggressive) {
            rightArmPose = ArmPose.BOW_AND_ARROW;
        } else if (main.getItem() instanceof CrossbowItem && aggressive) {
            rightArmPose = ArmPose.CROSSBOW_HOLD;
        } else if (!main.isEmpty()) {
            rightArmPose = ArmPose.ITEM;
        }
        if (off.is(Items.SHIELD)) {
            leftArmPose = entity.isBlocking() ? ArmPose.BLOCK : ArmPose.ITEM;
        } else if (!off.isEmpty()) {
            leftArmPose = ArmPose.ITEM;
        }
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    protected void setupAttackAnimation(T entity, float ageInTicks) {
        // A server swing also signals a ranged shot. Do not turn that signal into
        // vanilla's sword slash; the ranged clip below supplies release/recoil.
        if (entity instanceof SoldierEntity soldier && (soldier.getSoldierType().role == SoldierType.Role.GUNNER
                || entity.getMainHandItem().getItem() instanceof BowItem
                || entity.getMainHandItem().getItem() instanceof CrossbowItem)) return;
        super.setupAttackAnimation(entity, ageInTicks);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Renderers share a model between entities. Vanilla leaves some axes untouched;
        // additive breathing/recoil must start from the authored pose every frame.
        for (ModelPart part : poseParts) part.resetPose();
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (cape != null) {
            float sway = Mth.sin(ageInTicks * 0.08f) * 0.03f;
            cape.xRot = 0.09f + Math.min(1f, limbSwingAmount) * 0.55f + sway + (crouching ? 0.35f : 0f);
        }
        if (entity instanceof SoldierEntity soldier) {
            SoldierPoseAnimator.apply(soldier, this, limbSwing, limbSwingAmount, ageInTicks);
        }
    }
}
