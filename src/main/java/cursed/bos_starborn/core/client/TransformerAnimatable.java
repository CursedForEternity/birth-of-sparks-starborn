package cursed.bos_starborn.core.client;

import cursed.bos_starborn.core.PartSpec;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TransformerAnimatable implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private PartSpec activeSpec;
    private ResourceLocation activeAnimation;
    private String currentAnimName = "idle";

    public void setRenderTarget(PartSpec spec, ResourceLocation animFile, String animName) {
        this.activeSpec = spec;
        this.activeAnimation = animFile;
        this.currentAnimName = animName;
    }

    public PartSpec getActiveSpec() { return activeSpec; }
    public ResourceLocation getActiveAnimation() { return activeAnimation; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, state -> {
            if (currentAnimName == null || currentAnimName.isEmpty()) return PlayState.STOP;
            return state.setAndContinue(RawAnimation.begin().thenLoop(currentAnimName));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return System.currentTimeMillis() / 50.0;
    }
}