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

        // Also mirror keys directly on persistentData if other code reads them there[cite: 29]
        player.getPersistentData().putBoolean(TAG_MORPHED, morphed);
        player.getPersistentData().putBoolean(TAG_ALT_MODE, altMode);

        if (torsoId != null) {
            tag.putString(TAG_TORSO, torsoId.toString());
            player.getPersistentData().putString(TAG_TORSO, torsoId.toString());
        }
        if (headId != null) {
            tag.putString(TAG_HEAD, headId.toString());
            player.getPersistentData().putString(TAG_HEAD, headId.toString());
        }
        if (armsId != null) {
            tag.putString(TAG_ARMS, armsId.toString());
            player.getPersistentData().putString(TAG_ARMS, armsId.toString());
        }
        if (legsId != null) {
            tag.putString(TAG_LEGS, legsId.toString());
            player.getPersistentData().putString(TAG_LEGS, legsId.toString());
        }

        syncToTrackingAndSelf(player);
    }

    public static void syncToPlayer(ServerPlayer targetStateOwner, ServerPlayer recipient) {
        CompoundTag tag = getPersistedRoot(targetStateOwner);
        boolean morphed = tag.getBoolean(TAG_MORPHED);
        boolean altMode = tag.getBoolean(TAG_ALT_MODE);

        ResourceLocation defaultSet = ModularSetManager.getFirstSetId();
        ResourceLocation torsoId = parseOrDefault(tag.getString(TAG_TORSO), defaultSet);
        ResourceLocation headId  = parseOrDefault(tag.getString(TAG_HEAD), torsoId);
        ResourceLocation armsId  = parseOrDefault(tag.getString(TAG_ARMS), torsoId);
        ResourceLocation legsId  = parseOrDefault(tag.getString(TAG_LEGS), torsoId);

        if (torsoId == null) return;

        // TODO: Update SyncModularLoadoutPacket constructor/fields if it also carries `morphed` and `altMode`
        SyncModularLoadoutPacket packet = new SyncModularLoadoutPacket(
                targetStateOwner.getUUID(), torsoId, headId, armsId, legsId
        );
        BOSNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), packet);
    }

    public static void syncToTrackingAndSelf(ServerPlayer player) {
        syncToPlayer(player, player);
        for (ServerPlayer tracking : player.serverLevel().players()) {
            if (tracking != player && tracking.getUUID() != null) {
                syncToPlayer(player, tracking);
            }
        }
    }

    private static ResourceLocation parseOrDefault(String raw, ResourceLocation fallback) {
        if (raw == null || raw.isEmpty()) return fallback;
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        return parsed != null ? parsed : fallback;
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        CompoundTag oldPersisted = getPersistedRoot(event.getOriginal());
        CompoundTag newPersisted = getPersistedRoot(event.getEntity());

        for (String key : new String[]{TAG_MORPHED, TAG_ALT_MODE, TAG_TORSO, TAG_HEAD, TAG_ARMS, TAG_LEGS}) {
            if (oldPersisted.contains(key)) {
                newPersisted.put(key, oldPersisted.get(key).copy());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToPlayer(serverPlayer, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer
                && event.getTarget() instanceof ServerPlayer tracked) {
            syncToPlayer(tracked, viewer);
        }
    }
}