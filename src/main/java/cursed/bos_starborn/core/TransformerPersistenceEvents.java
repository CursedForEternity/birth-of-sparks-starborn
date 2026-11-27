package cursed.bos_starborn.core;

import cursed.bos_starborn.BOS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = BOS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TransformerPersistenceEvents {

    public static final String TAG_MORPHED = "bos_morphed";
    public static final String TAG_ALT_MODE = "bos_alt_mode";
    public static final String TAG_TORSO = "bos_torso";
    public static final String TAG_HEAD = "bos_head";
    public static final String TAG_ARMS = "bos_arms";
    public static final String TAG_LEGS = "bos_legs";

    private static final String[] ALL_KEYS = {
            TAG_MORPHED, TAG_ALT_MODE, TAG_TORSO, TAG_HEAD, TAG_ARMS, TAG_LEGS
    };

    private static CompoundTag getPersistedRoot(Player player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!data.contains(Player.PERSISTED_NBT_TAG, CompoundTag.TAG_COMPOUND)) {
            data.put(Player.PERSISTED_NBT_TAG, persisted);
        }
        return persisted;
    }

    public static void saveAndSync(ServerPlayer player, boolean morphed, boolean altMode,
                                    ResourceLocation torsoId, ResourceLocation headId,
                                    ResourceLocation armsId, ResourceLocation legsId) {
        CompoundTag tag = getPersistedRoot(player);
        tag.putBoolean(TAG_MORPHED, morphed);
        tag.putBoolean(TAG_ALT_MODE, altMode);

        if (torsoId != null) tag.putString(TAG_TORSO, torsoId.toString());
        if (headId != null)  tag.putString(TAG_HEAD, headId.toString());
        if (armsId != null)  tag.putString(TAG_ARMS, armsId.toString());
        if (legsId != null)  tag.putString(TAG_LEGS, legsId.toString());

        syncToTrackingAndSelf(player);
    }

    public static void syncLoadoutToClient(ServerPlayer targetStateOwner, ServerPlayer recipient) {
        CompoundTag tag = getPersistedRoot(targetStateOwner);
        ResourceLocation fallback = ModularSetManager.getFirstSetId();

        ResourceLocation torsoId = parseOrDefault(tag.getString(TAG_TORSO), fallback);
        if (torsoId == null) return;

        ResourceLocation headId = parseOrDefault(tag.getString(TAG_HEAD), torsoId);
        ResourceLocation armsId = parseOrDefault(tag.getString(TAG_ARMS), torsoId);
        ResourceLocation legsId = parseOrDefault(tag.getString(TAG_LEGS), torsoId);

        SyncModularLoadoutPacket packet = new SyncModularLoadoutPacket(
                targetStateOwner.getUUID(), torsoId, headId, armsId, legsId
        );
        BOSNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), packet);
    }

    public static void syncToTrackingAndSelf(ServerPlayer player) {
        syncLoadoutToClient(player, player);
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player) {
                syncLoadoutToClient(player, other);
            }
        }
    }

    public static ResourceLocation parseOrDefault(String raw, ResourceLocation fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        return parsed != null ? parsed : fallback;
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag oldTag = getPersistedRoot(event.getOriginal());
        CompoundTag newTag = getPersistedRoot(event.getEntity());

        for (String key : ALL_KEYS) {
            if (oldTag.contains(key)) {
                newTag.put(key, oldTag.get(key).copy());
            }
        }
    }

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
        if (event.getTarget() instanceof ServerPlayer targetPlayer
                && event.getEntity() instanceof ServerPlayer observerPlayer) {
            syncLoadoutToClient(targetPlayer, observerPlayer);
        }
    }
}