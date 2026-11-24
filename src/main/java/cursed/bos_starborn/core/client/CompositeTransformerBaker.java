package cursed.bos_starborn.core.client;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CompositeTransformerBaker {
    private static final Map<String, BakedGeoModel> BAKED_CACHE = new ConcurrentHashMap<>();

    public static void clearCache() {
        BAKED_CACHE.clear();
    }

    public static BakedGeoModel getOrBake(
            ResourceLocation torsoGeo,
            ResourceLocation headGeo,
            ResourceLocation armsGeo,
            ResourceLocation legsGeo
    ) {
        String key = torsoGeo + "|" + headGeo + "|" + armsGeo + "|" + legsGeo;
        return BAKED_CACHE.computeIfAbsent(key, k -> bake(torsoGeo, headGeo, armsGeo, legsGeo));
    }

    private static BakedGeoModel bake(
            ResourceLocation torsoGeo,
            ResourceLocation headGeo,
            ResourceLocation armsGeo,
            ResourceLocation legsGeo
    ) {
        BakedGeoModel rawTorso = GeckoLibCache.getBakedModels().get(torsoGeo);
        if (rawTorso == null) return null;

        List<GeoBone> rootBones = new ArrayList<>();
        for (GeoBone root : rawTorso.topLevelBones()) {
            rootBones.add(cloneBoneTree(root, null));
        }
        BakedGeoModel composite = new BakedGeoModel(rootBones, rawTorso.properties());

        // Optional: parent standard body pivots under "torso" if present so torso animations move them
        composite.getBone("torso").ifPresent(torsoBone -> {
            for (String slot : List.of("head", "left_arm", "right_arm", "left_leg", "right_leg")) {
                composite.getBone(slot).ifPresent(slotBone -> {
                    if (slotBone != torsoBone && slotBone.getParent() == null) {
                        composite.topLevelBones().remove(slotBone);
                        torsoBone.getChildBones().add(slotBone);
                    }
                });
            }
        });

        // Attach head cubes onto torso's "head" pivot
        attachCubesFromPart(composite, "head", headGeo, "head");

        // Attach arms cubes onto torso's arm pivots (avoids duplicate "left_arm"/"right_arm" bone names)
        attachCubesFromPart(composite, "left_arm", armsGeo, "left_arm");
        attachCubesFromPart(composite, "right_arm", armsGeo, "right_arm");

        // Attach legs cubes onto torso's leg pivots
        attachCubesFromPart(composite, "left_leg", legsGeo, "left_leg");
        attachCubesFromPart(composite, "right_leg", legsGeo, "right_leg");

        return composite;
    }

    private static void attachCubesFromPart(
            BakedGeoModel composite,
            String targetBoneName,
            ResourceLocation partGeo,
            String sourceBoneName
    ) {
        if (partGeo == null) return;
        Optional<GeoBone> targetOpt = composite.getBone(targetBoneName);
        BakedGeoModel partModel = GeckoLibCache.getBakedModels().get(partGeo);
        if (targetOpt.isEmpty() || partModel == null) return;

        Optional<GeoBone> sourceOpt = partModel.getBone(sourceBoneName);
        if (sourceOpt.isEmpty()) return;

        GeoBone target = targetOpt.get();
        GeoBone source = sourceOpt.get();

        // Copy cubes from the modular part bone onto the target pivot bone
        target.getCubes().addAll(source.getCubes());

        // Copy any sub-bones (e.g., armor/details) as children of the target bone
        for (GeoBone child : source.getChildBones()) {
            target.getChildBones().add(cloneBoneTree(child, target));
        }
    }

    private static GeoBone cloneBoneTree(GeoBone source, GeoBone newParent) {
        GeoBone copy = new GeoBone(
                newParent,
                source.getName(),
                source.getMirror(),
                source.getInflate(),
                source.shouldNeverRender(),
                source.getReset()
        );

        copy.setPivotX(source.getPivotX());
        copy.setPivotY(source.getPivotY());
        copy.setPivotZ(source.getPivotZ());

        copy.setRotX(source.getRotX());
        copy.setRotY(source.getRotY());
        copy.setRotZ(source.getRotZ());

        copy.setPosX(source.getPosX());
        copy.setPosY(source.getPosY());
        copy.setPosZ(source.getPosZ());

        copy.setScaleX(source.getScaleX());
        copy.setScaleY(source.getScaleY());
        copy.setScaleZ(source.getScaleZ());

        copy.getCubes().addAll(source.getCubes());

        for (GeoBone child : source.getChildBones()) {
            copy.getChildBones().add(cloneBoneTree(child, copy));
        }
        return copy;
    }
}