package cursed.bos_starborn.client;

import cursed.bos_starborn.BOS;
import cursed.bos_starborn.core.ModularSet;
import cursed.bos_starborn.core.ModularSetManager;
import cursed.bos_starborn.core.TransformerPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class TransformerModel extends GeoModel<TransformerPlayer> {

    private static final ResourceLocation FALLBACK_ANIM =
            new ResourceLocation(BOS.MODID, "animations/morph/testformer.animation.json");

    @Override
    public ResourceLocation getModelResource(TransformerPlayer animatable) {
        ModularSet set = ModularSetManager.getSet(animatable.getTorsoSetId());
        if (set == null) {
            return new ResourceLocation(BOS.MODID, "geo/parts/testformer/torso.geo.json");
        }
        if (animatable.isAltMode() && set.torso().altModel() != null) {
            return set.torso().altModel();
        }
        return set.torso().model();
    }

    @Override
    public ResourceLocation getTextureResource(TransformerPlayer animatable) {
        ModularSet set = ModularSetManager.getSet(animatable.getTorsoSetId());
        if (set == null) {
            return new ResourceLocation(BOS.MODID, "textures/entity/parts/testformer/testformer.png");
        }
        var layers = animatable.isAltMode() ? set.torso().altLayers() : set.torso().layers();
        if (layers.isEmpty()) {
            return new ResourceLocation(BOS.MODID, "textures/entity/parts/testformer/testformer.png");
        }
        return layers.get(0).texture();
    }


    @Override
    public ResourceLocation getAnimationResource(TransformerPlayer animatable) {
        ModularSet set = ModularSetManager.getSet(animatable.getTorsoSetId());
        if (set != null && set.torso().animation() != null) {
            return set.torso().animation();
        }
        return FALLBACK_ANIM;
    }

    @Override
    public void setCustomAnimations(TransformerPlayer animatable, long instanceId, AnimationState<TransformerPlayer> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        AbstractClientPlayer player = animatable.getPlayer();
        if (player == null) return;

        float partialTick = animationState.getPartialTick();

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
            float netHeadYaw = Mth.wrapDegrees(headYaw - bodyYaw);
            float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());

            head.setRotY(-netHeadYaw * Mth.DEG_TO_RAD);
            head.setRotX(-headPitch * Mth.DEG_TO_RAD);
        }

        float limbSwing = player.walkAnimation.position(partialTick);
        float limbSwingAmount = player.walkAnimation.speed(partialTick);

        CoreGeoBone rightArm = getAnimationProcessor().getBone("right_arm");
        CoreGeoBone leftArm = getAnimationProcessor().getBone("left_arm");
        CoreGeoBone rightLeg = getAnimationProcessor().getBone("right_leg");
        CoreGeoBone leftLeg = getAnimationProcessor().getBone("left_leg");

        // Legs swing opposite each other
        if (rightLeg != null) {
            rightLeg.setRotX(Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount);
        }
        if (leftLeg != null) {
            leftLeg.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount);
        }

        float idleSway = Mth.cos((player.tickCount + partialTick) * 0.09F) * 0.05F;
        if (rightArm != null) {
            rightArm.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount);
            rightArm.setRotZ(idleSway + 0.05F);
        }
        if (leftArm != null) {
            leftArm.setRotX(Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount);
            leftArm.setRotZ(-idleSway - 0.05F);
        }
    }
}

