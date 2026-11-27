package cursed.bos_starborn.core;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncModularLoadoutPacket(
        UUID playerId,
        boolean morphed,
        boolean altMode,
        ResourceLocation torsoSetId,
        ResourceLocation headSetId,
        ResourceLocation armsSetId,
        ResourceLocation legsSetId
) {

    public static void encode(SyncModularLoadoutPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeBoolean(msg.morphed);
        buf.writeBoolean(msg.altMode);
        buf.writeResourceLocation(msg.torsoSetId);
        buf.writeResourceLocation(msg.headSetId);
        buf.writeResourceLocation(msg.armsSetId);
        buf.writeResourceLocation(msg.legsSetId);
    }

    public static SyncModularLoadoutPacket decode(FriendlyByteBuf buf) {
        return new SyncModularLoadoutPacket(
                buf.readUUID(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readResourceLocation(),
                buf.readResourceLocation(),
                buf.readResourceLocation(),
                buf.readResourceLocation()
        );
    }

    public static void handle(SyncModularLoadoutPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) return;

                    TransformerPlayer animatable = TransformerPlayer.getOrCreateAnimatable(msg.playerId);
                    animatable.setMorphed(msg.morphed);
                    animatable.setAltMode(msg.altMode);
                    animatable.setTorsoSetId(msg.torsoSetId);
                    animatable.equipLimb(ModularSet.PartSlot.HEAD, msg.headSetId);
                    animatable.equipLimb(ModularSet.PartSlot.RIGHT_ARM, msg.armsSetId);
                    animatable.equipLimb(ModularSet.PartSlot.LEFT_ARM, msg.armsSetId);
                    animatable.equipLimb(ModularSet.PartSlot.RIGHT_LEG, msg.legsSetId);
                    animatable.equipLimb(ModularSet.PartSlot.LEFT_LEG, msg.legsSetId);
                })
        );
        ctx.get().setPacketHandled(true);
    }
}