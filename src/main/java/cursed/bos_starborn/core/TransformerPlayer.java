package cursed.bos_starborn.core;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static cursed.bos_starborn.client.ClientEvents.ANIMATABLE_CACHE;

public class TransformerPlayer implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private AbstractClientPlayer player;
    private static final RawAnimation TRANSFORM_ANIM = RawAnimation.begin().thenPlay("transform");
    private boolean pendingTransformAnim = false;
    private final boolean usingCustomLocomotion = false;
    public boolean altMode = false;
    private ResourceLocation torsoSetId;
    private final Map<ModularSet.PartSlot, ResourceLocation> limbSetIds = new EnumMap<>(ModularSet.PartSlot.class);
    private boolean morphed = false;

    public boolean isMorphed() {
        return morphed;
    }

    public void setMorphed(boolean morphed) {
        this.morphed = morphed;
    }

    public TransformerPlayer() {
        ResourceLocation initialSet = ModularSetManager.getFirstSetId();
        if (initialSet != null) {
            equipFullSet(initialSet);
        }
    }

    public void setTorsoSetId(ResourceLocation torsoSetId) {
        this.torsoSetId = torsoSetId;
    }

    public static TransformerPlayer getOrCreateAnimatable(UUID uuid) {
        return ANIMATABLE_CACHE.computeIfAbsent(uuid, id -> new TransformerPlayer());
    }

    public void setAltMode(boolean altMode) {
        if (this.altMode != altMode) {
            this.altMode = altMode;
            this.pendingTransformAnim = true;
        }
    }

    public ResourceLocation getTorsoSetId() {
        if (torsoSetId != null && ModularSetManager.getSet(torsoSetId) != null) {
            return torsoSetId;
        }
        ResourceLocation first = ModularSetManager.getFirstSetId();
        if (first != null) {
            this.torsoSetId = first;
        }
        return this.torsoSetId;
    }

    private boolean hasAnimation(ResourceLocation animFile) {
        var baked = software.bernie.geckolib.cache.GeckoLibCache.getBakedAnimations().get(animFile);
        return baked != null && baked.getAnimation("transform") != null;
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

    public ResourceLocation getLimbSetId(ModularSet.PartSlot slot) {
        ResourceLocation id = limbSetIds.get(slot);
        if (id != null && ModularSetManager.getSet(id) != null) {
            return id;
        }
        ResourceLocation fallback = getTorsoSetId();
        return fallback != null ? fallback : torsoSetId;
    }

    public boolean isAltMode() {
        return altMode;
    }

    public void setPlayer(AbstractClientPlayer player) {
        this.player = player;
    }

    public AbstractClientPlayer getPlayer() {
        return this.player;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "transform", 2, state -> {
            if (!this.pendingTransformAnim) {
                return PlayState.STOP;
            }

            ModularSet set = ModularSetManager.getSet(getTorsoSetId());
            ResourceLocation animFile = (set != null && set.torso().animation() != null)
                    ? set.torso().animation()
                    : null;

            if (animFile != null && hasAnimation(animFile)) {
                this.pendingTransformAnim = false;
                state.getController().forceAnimationReset();
                return state.setAndContinue(TRANSFORM_ANIM);
            }

            // If no "transform" clip exists in the animation file, clear the flag cleanly
            this.pendingTransformAnim = false;
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "locomotion", 3, state -> {
            if (this.player == null) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }

            if (this.player.isSwimming() || this.player.isUnderWater()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("swim"));
            }
            if (!this.player.onGround() && this.player.getDeltaMovement().y < -0.15) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("fall"));
            }
            if (this.player.isShiftKeyDown()) {
                double speedSqr = this.player.getDeltaMovement().horizontalDistanceSqr();
                String sneakAnim = speedSqr > 0.002 ? "sneak_walk" : "sneak_idle";
                return state.setAndContinue(RawAnimation.begin().thenLoop(sneakAnim));
            }

            double horizontalSpeedSqr = this.player.getDeltaMovement().horizontalDistanceSqr();
            if (horizontalSpeedSqr > 0.002) {
                String moveAnim = this.player.isSprinting() ? "run" : "walk";
                return state.setAndContinue(RawAnimation.begin().thenLoop(moveAnim));
            }

            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "action", 2, state -> {
            if (this.player != null && this.player.swinging) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("attack"));
            }
            return PlayState.STOP;
        }));
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
