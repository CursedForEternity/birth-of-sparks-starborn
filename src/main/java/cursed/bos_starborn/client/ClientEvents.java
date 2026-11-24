package cursed.bos_starborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import cursed.bos_starborn.BOS;
import cursed.bos_starborn.core.TransformerPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import static cursed.bos_starborn.core.TransformerPlayer.getOrCreateAnimatable;

@Mod.EventBusSubscriber(modid = BOS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    private static ModularPlayerRenderer renderer;
    public static final Map<UUID, TransformerPlayer> ANIMATABLE_CACHE = new WeakHashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;
        TransformerPlayer animatable = getOrCreateAnimatable(clientPlayer.getUUID());
        if (animatable.getTorsoSetId() == null) return;
        // Cancel vanilla player rendering
        event.setCanceled(true);

        if (renderer == null) {
            renderer = new ModularPlayerRenderer();
        }
        animatable.setPlayer(clientPlayer);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // 1. Match player body rotation
        float bodyYaw = Mth.rotLerp(event.getPartialTick(), clientPlayer.yBodyRotO, clientPlayer.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));

        // 2. Shift the entire assembled model so the feet touch the ground (Y = 0)
        // 1 Blockbench pixel = 1/16th of a block (0.0625).
        // Example: If your legs are 12 pixels tall and sink underground, raise Y by 12 / 16.0 = 0.75:
        poseStack.translate(0.0, 0.75, 0.0);

        renderer.render(
                poseStack,
                animatable,
                event.getMultiBufferSource(),
                null,
                null,
                event.getPackedLight()
        );

        poseStack.popPose();
    }
}