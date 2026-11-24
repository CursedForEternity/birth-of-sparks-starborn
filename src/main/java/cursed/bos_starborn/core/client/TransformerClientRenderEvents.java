package cursed.bos_starborn.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import cursed.bos_starborn.core.PartSpec;
import cursed.bos_starborn.core.TransformerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;

@Mod.EventBusSubscriber(modid = "bos_starborn", value = Dist.CLIENT)
public class TransformerClientRenderEvents {
    public static final String MOD_ID = "bos_starborn";
    public static final TransformerPartRenderer RENDERER = new TransformerPartRenderer();
    public static final ResourceLocation ROBOT_ANIM = new ResourceLocation(MOD_ID, "animations/robot.animation.json");
    public static final ResourceLocation ALT_ANIM   = new ResourceLocation(MOD_ID, "animations/alt.animation.json");

    private static final PartSpec FALLBACK_SPEC = new PartSpec(
            new ResourceLocation(MOD_ID, "geo/torso.geo.json"),
            new ResourceLocation(MOD_ID, "textures/entity/transformer.png"),
            "biped"
    );

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

        PartSpec rootSpec = data.isAltMode() ? altSpec : torsoSpec;
        BakedGeoModel modelToRender = data.isAltMode()
                ? GeckoLibCache.getBakedModels().get(rootSpec.geoModel())
                : CompositeTransformerBaker.getOrBake(
                        torsoSpec.geoModel(),
                        headSpec.geoModel(),
                        armsSpec.geoModel(),
                        legsSpec.geoModel()
                );

        if (modelToRender == null) {
            modelToRender = GeckoLibCache.getBakedModels().get(rootSpec.geoModel());
            if (modelToRender == null) return;
        }

        ResourceLocation animFile = data.isAltMode() ? ALT_ANIM : ROBOT_ANIM;
        String animName = player.getDeltaMovement().horizontalDistanceSqr() > 0.002 ? "move" : "idle";

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        try {
            float bodyYaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));

            // Pass modelToRender into your renderer/context so it actually renders the composite model
            RENDERER.renderTransformer(
                    player.getUUID(),
                    data,
                    rootSpec,
                    modelToRender,
                    animFile,
                    animName,
                    poseStack,
                    event.getMultiBufferSource(),
                    event.getPackedLight()
            );
        } finally {
            poseStack.popPose();
        }
    }

    public static TransformerData getTransformerData(Player player) {
        // TODO: return player's synced capability
        return null;
    }

    public static PartSpec lookupSpec(ResourceLocation id) {
        if (id == null) return FALLBACK_SPEC;
        // TODO: return PartSpec from your JSON loader, or FALLBACK_SPEC if missing
        return FALLBACK_SPEC;
    }
}