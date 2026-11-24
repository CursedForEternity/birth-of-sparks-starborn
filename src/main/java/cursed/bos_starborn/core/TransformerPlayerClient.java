package cursed.bos_starborn.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import cursed.bos_starborn.core.client.CompositeTransformerBaker;
import cursed.bos_starborn.core.client.TransformerAnimatable;
import cursed.bos_starborn.core.client.TransformerRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import static cursed.bos_starborn.core.client.TransformerClientRenderEvents.*;
import static cursed.bos_starborn.core.client.TransformerPartRenderer.getOrCreateAnimatable;

public class TransformerPlayerClient {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        TransformerData data = getTransformerData(player);
        if (data == null || !data.isMorphed()) return;

        event.setCanceled(true);

        PartSpec torsoSpec = lookupSpec(data.getTorsoId());
        PartSpec headSpec  = lookupSpec(data.getHeadId());
        PartSpec armsSpec  = lookupSpec(data.getArmsId());
        PartSpec legsSpec  = lookupSpec(data.getLegsId());
        PartSpec altSpec   = lookupSpec(data.getAltId());

        BakedGeoModel composite = data.isAltMode()
                ? null
                : CompositeTransformerBaker.getOrBake(
                torsoSpec.geoModel(),
                headSpec.geoModel(),
                armsSpec.geoModel(),
                legsSpec.geoModel()
        );

        PartSpec activeRootSpec = data.isAltMode() ? altSpec : torsoSpec;
        ResourceLocation animFile = data.isAltMode() ? ALT_ANIM : ROBOT_ANIM;
        String animName = player.getDeltaMovement().horizontalDistanceSqr() > 0.002 ? "move" : "idle";

        TransformerAnimatable animatable = getOrCreateAnimatable(player.getUUID());
        animatable.setRenderTarget(activeRootSpec, animFile, animName);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        float bodyYaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));

        try {
            TransformerRenderContext.set(data, composite);
            RenderType renderType = RenderType.entityCutoutNoCull(activeRootSpec.texture());
            RENDERER.render(
                    poseStack,
                    animatable,
                    event.getMultiBufferSource(),
                    renderType,
                    event.getMultiBufferSource().getBuffer(renderType),
                    event.getPackedLight()
            );
        } finally {
            TransformerRenderContext.clear();
            poseStack.popPose();
        }
    }
}
