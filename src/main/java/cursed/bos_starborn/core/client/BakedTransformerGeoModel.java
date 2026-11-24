package cursed.bos_starborn.core.client;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;

public class BakedTransformerGeoModel extends GeoModel<TransformerAnimatable> {

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

    @Override
    public BakedGeoModel getBakedModel(ResourceLocation location) {
        // If we queued a composite loadout on the current animatable, return the stitched model
        BakedGeoModel composite = TransformerRenderContext.getCurrentModel();
        if (composite != null) {
            return composite;
        }
        return super.getBakedModel(location);
    }
}