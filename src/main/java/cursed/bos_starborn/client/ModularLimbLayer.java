package cursed.bos_starborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cursed.bos_starborn.core.ModularSet;
import cursed.bos_starborn.core.ModularSetManager;
import cursed.bos_starborn.core.TextureLayer;
import cursed.bos_starborn.core.TransformerPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

public class ModularLimbLayer extends GeoRenderLayer<TransformerPlayer> {

    public ModularLimbLayer(GeoRenderer<TransformerPlayer> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void renderForBone(PoseStack poseStack, TransformerPlayer animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {

        if (animatable.isAltMode()) return;

        // 1. Map bone name ("head", "right_arm", etc.) to PartSlot
        ModularSet.PartSlot slot = ModularSet.PartSlot.fromBoneName(bone.getName());
        if (slot == null) return;

        // 2. Look up the ModularSet equipped in this limb slot
        ModularSet set = ModularSetManager.getSet(animatable.getLimbSetId(slot));
        if (set == null) return;

        ModularSet.LimbPart part = set.limbs().get(slot);
        if (part == null) return;

        // 3. Fetch the cached BakedGeoModel
        BakedGeoModel limbModel = GeckoLibCache.getBakedModels().get(part.model());
        if (limbModel == null) return;

        poseStack.pushPose();
        RenderUtils.translateToPivotPoint(poseStack, bone);

        for (TextureLayer layer : part.layers()) {
            RenderType limbRenderType = layer.emissive()
                    ? RenderType.entityTranslucentEmissive(layer.texture())
                    : RenderType.entityCutoutNoCull(layer.texture());

            VertexConsumer limbBuffer = bufferSource.getBuffer(limbRenderType);
            int light = layer.getPackedLight(packedLight);

            getRenderer().reRender(
                    limbModel, poseStack, bufferSource, animatable, limbRenderType,
                    limbBuffer, partialTick, light, packedOverlay,
                    layer.red(), layer.green(), layer.blue(), layer.alpha()
            );
        }

        poseStack.popPose();
    }
}
