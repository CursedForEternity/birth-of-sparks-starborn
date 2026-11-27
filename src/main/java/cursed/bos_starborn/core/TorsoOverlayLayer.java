package cursed.bos_starborn.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public class TorsoOverlayLayer extends GeoRenderLayer<TransformerPlayer> {

    public TorsoOverlayLayer(GeoRenderer<TransformerPlayer> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, TransformerPlayer animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        ModularSet set = ModularSetManager.getSet(animatable.getTorsoSetId());
        if (set == null) return;

        List<TextureLayer> layers = animatable.isAltMode()
                ? set.torso().altLayers()
                : set.torso().layers();

        for (TextureLayer layer : layers) {
            RenderType layerType = layer.emissive()
                    ? RenderType.entityTranslucentEmissive(layer.texture())
                    : RenderType.entityCutoutNoCull(layer.texture());

            VertexConsumer layerBuffer = bufferSource.getBuffer(layerType);
            int light = layer.getPackedLight(packedLight);

            getRenderer().reRender(
                    bakedModel, poseStack, bufferSource, animatable, layerType,
                    layerBuffer, partialTick, light, packedOverlay,
                    layer.red(), layer.green(), layer.blue(), layer.alpha()
            );
        }
    }
}
