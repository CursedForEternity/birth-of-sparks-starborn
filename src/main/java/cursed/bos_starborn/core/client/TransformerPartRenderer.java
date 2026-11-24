package cursed.bos_starborn.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cursed.bos_starborn.core.PartSpec;
import cursed.bos_starborn.core.TransformerData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransformerPartRenderer extends GeoObjectRenderer<TransformerAnimatable> {
    private static final Map<UUID, TransformerAnimatable> ANIMATABLES = new ConcurrentHashMap<>();

    public TransformerPartRenderer() {
        super(new ModularPartGeoModel());
    }

    public static TransformerAnimatable getOrCreateAnimatable(UUID playerId) {
        return ANIMATABLES.computeIfAbsent(playerId, id -> new TransformerAnimatable());
    }

    public void renderTransformer(
            UUID playerId,
            TransformerData data,
            PartSpec rootSpec,
            BakedGeoModel modelToRender,
            ResourceLocation animFile,
            String animName,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        TransformerAnimatable animatable = getOrCreateAnimatable(playerId);
        animatable.setRenderTarget(rootSpec, animFile, animName);

        TransformerRenderContext.set(data, modelToRender);
        try {
            RenderType renderType = RenderType.entityCutoutNoCull(rootSpec.texture());
            this.render(
                    poseStack,
                    animatable,
                    bufferSource,
                    renderType,
                    bufferSource.getBuffer(renderType),
                    packedLight
            );
        } finally {
            TransformerRenderContext.clear();
        }
    }

    @Override
    public Color getRenderColor(TransformerAnimatable animatable, float partialTick, int packedLight) {
        TransformerData data = TransformerRenderContext.getCurrentData();
        if (data == null) return Color.WHITE;
        return Color.ofOpaque(data.getPrimaryColor());
    }

    @Override
    public void renderRecursively(
            PoseStack poseStack,
            TransformerAnimatable animatable,
            GeoBone bone,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        TransformerData data = TransformerRenderContext.getCurrentData();
        if (data != null && !data.isAltMode()) {
            int hex = switch (bone.getName()) {
                case "head" -> data.getAccentColor();
                case "left_arm", "right_arm", "left_leg", "right_leg" -> data.getSecondaryColor();
                default -> -1;
            };

            if (hex != -1) {
                red   = ((hex >> 16) & 0xFF) / 255f;
                green = ((hex >> 8)  & 0xFF) / 255f;
                blue  = (hex         & 0xFF) / 255f;
            }
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha
        );
    }
}