package cursed.bos_starborn.core;

import cursed.bos_starborn.server.ServerPlayerEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ToggleAltModePacket(boolean altMode) {

    public static void encode(ToggleAltModePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.altMode);
    }

    public static ToggleAltModePacket decode(FriendlyByteBuf buf) {
        return new ToggleAltModePacket(buf.readBoolean());
    }

    public static void handle(ToggleAltModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            CompoundTag tag = player.getPersistentData();
            boolean newAlt = msg.altMode;
            tag.putBoolean("bos_alt_mode", newAlt);

            // Re-sync loadout/state to self and tracking players
            ServerPlayerEvents.syncLoadoutToClient(player, player);
            player.serverLevel().players().forEach(other -> {
                if (other != player) {
                    ServerPlayerEvents.syncLoadoutToClient(player, other);
                }
            });

            // TODO: If you add a dedicated SyncAltModePacket(UUID, boolean) or add `altMode`
            // to SyncModularLoadoutPacket, send it here via BOSNetwork.CHANNEL.send(
            // PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), ... )
        });
        ctx.get().setPacketHandled(true);
    }
}