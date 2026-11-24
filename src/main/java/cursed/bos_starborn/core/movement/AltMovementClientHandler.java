package cursed.bos_starborn.core.movement;

import cursed.bos_starborn.BOS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BOS.MODID, value = Dist.CLIENT)
public class AltMovementClientHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Player player = event.player;
        if (!player.level().isClientSide() || !(player instanceof LocalPlayer localPlayer)) return;

        boolean inAltMode = isInAltMode(player);
        AltMovementController.Mode mode = getActiveAltMode(player);
        if (!inAltMode || mode == null) return;

        var options = Minecraft.getInstance().options;
        float forward = (options.keyUp.isDown() ? 1f : 0f) - (options.keyDown.isDown() ? 1f : 0f);
        float strafe = (options.keyRight.isDown() ? 1f : 0f) - (options.keyLeft.isDown() ? 1f : 0f);
        boolean jump = options.keyJump.isDown();
        boolean crouch = options.keyShift.isDown();

        AltMovementController.InputState input = new AltMovementController.InputState(forward, strafe, jump, crouch);
        mode.tickMovement(localPlayer, input);
        localPlayer.hasImpulse = true;
    }

    private static boolean isInAltMode(Player player) {
        // TODO: read from your synced Transformer capability
        return false;
    }

    private static AltMovementController.Mode getActiveAltMode(Player player) {
        // TODO: parse movementType ("car", "tank", "jet", "heli") from your loaded PartSpec
        return null;
    }
}