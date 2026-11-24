package cursed.bos_starborn.core.client;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;

public class ModularPartGeoModel extends GeoModel<TransformerAnimatable> {
    @Override
    public BakedGeoModel getBakedModel(ResourceLocation location) {
        BakedGeoModel override = TransformerRenderContext.getCurrentModel();
        if (override != null) {
            return override;
        }
        return super.getBakedModel(location);
    }

    @Override
    public ResourceLocation getModelResource(TransformerAnimatable animatable) {
        return animatable.getActiveSpec().geoModel();
    }

    @Override
    public ResourceLocation getTextureResource(TransformerAnimatable animatable) {
        return animatable.getActiveSpec().texture();
    }

    @Override
    public ResourceLocation getAnimationResource(TransformerAnimatable animatable) {
        return animatable.getActiveAnimation();
    }
}