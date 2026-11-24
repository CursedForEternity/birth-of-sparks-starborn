package cursed.bos_starborn.core;

import cursed.bos_starborn.BOS;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static cursed.bos_starborn.client.ClientEvents.ANIMATABLE_CACHE;

public class TransformerPlayer implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private AbstractClientPlayer player;

    // Default test set; later populated from your Player Capability / sync packet
    private ResourceLocation torsoSetId = new ResourceLocation(BOS.MODID, "testformer");
    private final Map<ModularSet.PartSlot, ResourceLocation> limbSetIds = new EnumMap<>(ModularSet.PartSlot.class);
    private boolean altMode = false;

    public TransformerPlayer() {
        equipFullSet(torsoSetId);
    }

    public void setTorsoSetId(ResourceLocation torsoSetId) {
        this.torsoSetId = torsoSetId;
    }

    public static TransformerPlayer getOrCreateAnimatable(UUID uuid) {
        return ANIMATABLE_CACHE.computeIfAbsent(uuid, id -> new TransformerPlayer());
    }


    public void equipFullSet(ResourceLocation setId) {
        this.torsoSetId = setId;
        for (ModularSet.PartSlot slot : ModularSet.PartSlot.values()) {
            this.limbSetIds.put(slot, setId);
        }
    }

    public void equipLimb(ModularSet.PartSlot slot, ResourceLocation setId) {
        this.limbSetIds.put(slot, setId);
    }

    public ResourceLocation getTorsoSetId() {
        return torsoSetId;
    }

    public ResourceLocation getLimbSetId(ModularSet.PartSlot slot) {
        return limbSetIds.getOrDefault(slot, torsoSetId);
    }

    public boolean isAltMode() {
        return altMode;
    }

    public void setAltMode(boolean altMode) {
        this.altMode = altMode;
    }

    public void setPlayer(AbstractClientPlayer player) {
        this.player = player;
    }

    public AbstractClientPlayer getPlayer() {
        return this.player;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Add custom animation controllers here if needed
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getTick(Object object) {
        return player != null ? player.tickCount : 0;
    }
}
