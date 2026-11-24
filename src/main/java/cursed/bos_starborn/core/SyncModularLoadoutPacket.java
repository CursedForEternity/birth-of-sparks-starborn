package cursed.bos_starborn.core;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncModularLoadoutPacket(
        UUID playerUuid,
        ResourceLocation torso,
        ResourceLocation head,
        ResourceLocation arms,
        ResourceLocation legs
) {
    public static void encode(SyncModularLoadoutPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUuid);
        buf.writeResourceLocation(msg.torso);
        buf.writeResourceLocation(msg.head);
        buf.writeResourceLocation(msg.arms);
        buf.writeResourceLocation(msg.legs);
    }

    public static SyncModularLoadoutPacket decode(FriendlyByteBuf buf) {
        return new SyncModularLoadoutPacket(
                buf.readUUID(),
                buf.readResourceLocation(),
                buf.readResourceLocation(),
                buf.readResourceLocation(),
                buf.readResourceLocation()
        );
    }

    public static void handle(SyncModularLoadoutPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Look up or create the TransformerPlayer wrapper on the client and update its slots
            TransformerPlayer animatable = TransformerPlayer.getOrCreateAnimatable(msg.playerUuid);
            animatable.setTorsoSetId(msg.torso);
            animatable.equipLimb(ModularSet.PartSlot.HEAD, msg.head);
            animatable.equipLimb(ModularSet.PartSlot.RIGHT_ARM, msg.arms);
            animatable.equipLimb(ModularSet.PartSlot.LEFT_ARM, msg.arms);
            animatable.equipLimb(ModularSet.PartSlot.RIGHT_LEG, msg.legs);
            animatable.equipLimb(ModularSet.PartSlot.LEFT_LEG, msg.legs);
        });
        ctx.get().setPacketHandled(true);
    }
}
