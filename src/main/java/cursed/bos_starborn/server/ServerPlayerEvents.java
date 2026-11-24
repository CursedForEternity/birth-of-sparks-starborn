package cursed.bos_starborn.server;

import cursed.bos_starborn.BOS;
import cursed.bos_starborn.core.BOSNetwork;
import cursed.bos_starborn.core.SyncModularLoadoutPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = BOS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerPlayerEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncLoadoutToClient(serverPlayer, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncLoadoutToClient(serverPlayer, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncLoadoutToClient(serverPlayer, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        // When another player walks into render range, sync their modular parts to the observer
        if (event.getTarget() instanceof ServerPlayer targetPlayer &&
            event.getEntity() instanceof ServerPlayer observerPlayer) {
            syncLoadoutToClient(targetPlayer, observerPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Copy persistent modular loadout across death/end-portal clones
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();

        if (oldData.contains("bos_torso")) {
            newData.putString("bos_torso", oldData.getString("bos_torso"));
            newData.putString("bos_head", oldData.getString("bos_head"));
            newData.putString("bos_arms", oldData.getString("bos_arms"));
            newData.putString("bos_legs", oldData.getString("bos_legs"));
        }
    }

    public static void syncLoadoutToClient(ServerPlayer sourcePlayer, ServerPlayer recipientPlayer) {
        CompoundTag tag = sourcePlayer.getPersistentData();
        if (!tag.contains("bos_torso")) return;

        ResourceLocation torso = new ResourceLocation(tag.getString("bos_torso"));
        ResourceLocation head = new ResourceLocation(tag.getString("bos_head"));
        ResourceLocation arms = new ResourceLocation(tag.getString("bos_arms"));
        ResourceLocation legs = new ResourceLocation(tag.getString("bos_legs"));

        SyncModularLoadoutPacket packet = new SyncModularLoadoutPacket(
                sourcePlayer.getUUID(), torso, head, arms, legs
        );
        BOSNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipientPlayer), packet);
    }
}
