package cursed.bos_starborn.core.client;

import cursed.bos_starborn.core.TransformerData;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public final class TransformerRenderContext {
    private static TransformerData currentData;
    private static BakedGeoModel currentModel;

    public static void set(TransformerData data, BakedGeoModel model) {
        currentData = data;
        currentModel = model;
    }

    public static void clear() {
        currentData = null;
        currentModel = null;
    }

    public static TransformerData getCurrentData() { return currentData; }
    public static BakedGeoModel getCurrentModel() { return currentModel; }
}