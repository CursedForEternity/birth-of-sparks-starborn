package cursed.bos_starborn.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static cursed.bos_starborn.core.TransformerPersistenceEvents.parseOrDefault;

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

            CompoundTag persisted = player.getPersistentData()
                    .getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
            ResourceLocation fallback = ModularSetManager.getFirstSetId();

            ResourceLocation torsoId = parseOrDefault(persisted.getString(TransformerPersistenceEvents.TAG_TORSO), fallback);
            ResourceLocation headId  = parseOrDefault(persisted.getString(TransformerPersistenceEvents.TAG_HEAD), torsoId);
            ResourceLocation armsId  = parseOrDefault(persisted.getString(TransformerPersistenceEvents.TAG_ARMS), torsoId);
            ResourceLocation legsId  = parseOrDefault(persisted.getString(TransformerPersistenceEvents.TAG_LEGS), torsoId);

            boolean morphed = !persisted.contains(TransformerPersistenceEvents.TAG_MORPHED)
                    || persisted.getBoolean(TransformerPersistenceEvents.TAG_MORPHED);

            TransformerPersistenceEvents.saveAndSync(
                    player, morphed, msg.altMode, torsoId, headId, armsId, legsId
            );
        });
        ctx.get().setPacketHandled(true);
    }
}