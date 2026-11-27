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
        if (event.getTarget() instanceof ServerPlayer targetPlayer &&
                event.getEntity() instanceof ServerPlayer observerPlayer) {
            syncLoadoutToClient(targetPlayer, observerPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();

        for (String key : new String[]{"bos_torso", "bos_head", "bos_arms", "bos_legs"}) {
            if (oldData.contains(key)) {
                newData.putString(key, oldData.getString(key));
            }
        }
    }

    private static ResourceLocation parseSlotId(CompoundTag tag, String key, ResourceLocation fallback) {
        if (!tag.contains(key)) return fallback;
        String raw = tag.getString(key);
        if (raw == null || raw.isBlank()) return fallback;
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        return parsed != null ? parsed : fallback;
    }

    public static void syncLoadoutToClient(ServerPlayer sourcePlayer, ServerPlayer recipientPlayer) {
        CompoundTag tag = sourcePlayer.getPersistentData();
        ResourceLocation torso = parseSlotId(tag, "bos_torso", null);
        if (torso == null) return;

        ResourceLocation head = parseSlotId(tag, "bos_head", torso);
        ResourceLocation arms = parseSlotId(tag, "bos_arms", torso);
        ResourceLocation legs = parseSlotId(tag, "bos_legs", torso);

        SyncModularLoadoutPacket packet = new SyncModularLoadoutPacket(
                sourcePlayer.getUUID(), torso, head, arms, legs
        );
        BOSNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipientPlayer), packet);
    }
}