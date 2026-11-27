package cursed.bos_starborn.client;

import com.mojang.blaze3d.platform.InputConstants;
import cursed.bos_starborn.BOS;
import cursed.bos_starborn.core.BOSNetwork;
import cursed.bos_starborn.core.ToggleAltModePacket;
import cursed.bos_starborn.core.TransformerPlayer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BOS.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AltModeKeyHandler {

    public static final KeyMapping TRANSFORM_KEY = new KeyMapping(
            "key." + BOS.MODID + ".transform",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories." + BOS.MODID
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TRANSFORM_KEY);
        MinecraftForge.EVENT_BUS.register(ForgeBusHandler.class);
    }

    public static class ForgeBusHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            while (TRANSFORM_KEY.consumeClick()) {
                TransformerPlayer animatable = TransformerPlayer.getOrCreateAnimatable(mc.player.getUUID());
                boolean nextAltMode = !animatable.isAltMode();

                // Optimistically toggle locally so animation/model responds immediately
                animatable.setAltMode(nextAltMode);

                // Tell the server so it persists and syncs to everyone
                BOSNetwork.CHANNEL.sendToServer(new ToggleAltModePacket(nextAltMode));
            }
        }
    }
}