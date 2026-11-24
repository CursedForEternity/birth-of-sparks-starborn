package cursed.bos_starborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cursed.bos_starborn.core.TransformerPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

public class ModularPlayerRenderer extends GeoObjectRenderer<TransformerPlayer> {
    public ModularPlayerRenderer() {
        super(new TransformerModel());
        this.addRenderLayer(new ModularLimbLayer(this));
    }

    @Override
    public void preRender(PoseStack poseStack, TransformerPlayer animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
    }
}