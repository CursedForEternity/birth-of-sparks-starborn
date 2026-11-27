package cursed.bos_starborn.core;

import com.google.gson.*;
import cursed.bos_starborn.BOS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.*;

@Mod.EventBusSubscriber(modid = BOS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModularSetManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, ModularSet> SETS = new HashMap<>();

    public ModularSetManager() {
        super(GSON, "modular_sets");
    }

    @Nullable
    public static ResourceLocation getFirstSetId() {
        return SETS.keySet().stream()
                .min(Comparator.comparing(ResourceLocation::toString))
                .orElse(null);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ModularSetManager());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        SETS.clear();

        objects.forEach((fileId, jsonElement) -> {
            try {
                JsonObject root = GsonHelper.convertToJsonObject(jsonElement, "modular_sets");
                String namespace = fileId.getNamespace();
                String folderName = fileId.getPath();

                ResourceLocation setId = root.has("set_id")
                        ? new ResourceLocation(GsonHelper.getAsString(root, "set_id"))
                        : fileId;

                float hipOffsetY = GsonHelper.getAsFloat(root, "hip_offset_y", 0.75F);

                JsonObject torsoObj = GsonHelper.getAsJsonObject(root, "torso");
                ModularSet.TorsoPart torso = parseTorso(torsoObj, namespace, folderName);

                Map<ModularSet.PartSlot, ModularSet.LimbPart> limbs = new EnumMap<>(ModularSet.PartSlot.class);
                for (ModularSet.PartSlot slot : ModularSet.PartSlot.values()) {
                    if (root.has(slot.getBoneName())) {
                        JsonObject limbObj = GsonHelper.getAsJsonObject(root, slot.getBoneName());
                        limbs.put(slot, parseLimb(limbObj, namespace, folderName, slot));
                    }
                }

                SETS.put(setId, new ModularSet(setId, hipOffsetY, torso, Collections.unmodifiableMap(limbs)));
            } catch (Exception e) {
                BOS.LOGGER.error("Failed to load modular set {}: {}", fileId, e.getMessage());
            }
        });

        BOS.LOGGER.info("Loaded {} modular part sets.", SETS.size());
    }

    private ModularSet.TorsoPart parseTorso(JsonObject obj, String namespace, String folder) {
        ResourceLocation model = resolvePath(obj, "path", namespace, "geo/parts/" + folder + "/torso.geo.json");
        ResourceLocation defaultTex = new ResourceLocation(namespace, "textures/entity/parts/" + folder + "/" + folder + ".png");
        List<TextureLayer> layers = parseLayers(obj, "layers", "texture", defaultTex);

        ResourceLocation altModel = obj.has("alt")
                ? new ResourceLocation(GsonHelper.getAsString(obj, "alt")) : null;

        List<TextureLayer> altLayers;
        if (altModel == null) {
            altLayers = List.of();
        } else if (obj.has("alt_layers") || obj.has("alt_texture")) {
            ResourceLocation defaultAltTex = new ResourceLocation(namespace, "textures/entity/parts/" + folder + "/alt.png");
            altLayers = parseLayers(obj, "alt_layers", "alt_texture", defaultAltTex);
        } else {
            altLayers = layers;
        }

        String propStr = GsonHelper.getAsString(obj, "propulsion", "NONE").toUpperCase();
        ModularSet.PropulsionType propulsion = ModularSet.PropulsionType.valueOf(propStr);

        ResourceLocation animation = obj.has("animation")
                ? new ResourceLocation(GsonHelper.getAsString(obj, "animation")) : null;

        return new ModularSet.TorsoPart(model, layers, altModel, altLayers, propulsion, animation);
    }

    private ModularSet.LimbPart parseLimb(JsonObject obj, String namespace, String folder, ModularSet.PartSlot slot) {
        ResourceLocation model = resolvePath(obj, "path", namespace, "geo/parts/" + folder + "/" + slot.getBoneName() + ".geo.json");
        ResourceLocation defaultTex = new ResourceLocation(namespace, "textures/entity/parts/" + folder + "/" + folder + ".png");
        List<TextureLayer> layers = parseLayers(obj, "layers", "texture", defaultTex);
        return new ModularSet.LimbPart(model, layers);
    }

    private List<TextureLayer> parseLayers(JsonObject obj, String arrayKey, String singleKey, ResourceLocation fallbackTex) {
        if (obj.has(arrayKey) && obj.get(arrayKey).isJsonArray()) {
            List<TextureLayer> result = new ArrayList<>();
            for (JsonElement elem : obj.getAsJsonArray(arrayKey)) {
                JsonObject layerObj = GsonHelper.convertToJsonObject(elem, "texture_layer");
                ResourceLocation tex = new ResourceLocation(GsonHelper.getAsString(layerObj, "texture"));
                String color = GsonHelper.getAsString(layerObj, "color", "#FFFFFF");
                Boolean emissive = layerObj.has("emissive") ? GsonHelper.getAsBoolean(layerObj, "emissive") : null;
                Boolean tintable = layerObj.has("tintable") ? GsonHelper.getAsBoolean(layerObj, "tintable") : null;
                result.add(TextureLayer.of(tex, color, emissive, tintable));
            }
            return Collections.unmodifiableList(result);
        }

        ResourceLocation singleTex = obj.has(singleKey)
                ? new ResourceLocation(GsonHelper.getAsString(obj, singleKey))
                : fallbackTex;
        String singleColor = GsonHelper.getAsString(obj, "color", "#FFFFFF");
        Boolean singleTintable = obj.has("tintable") ? GsonHelper.getAsBoolean(obj, "tintable") : null;
        return List.of(TextureLayer.of(singleTex, singleColor, null, singleTintable));
    }


    private ResourceLocation resolvePath(JsonObject obj, String key, String namespace, String fallbackPath) {
        if (obj.has(key)) {
            return new ResourceLocation(GsonHelper.getAsString(obj, key));
        }
        return new ResourceLocation(namespace, fallbackPath);
    }

    public static ModularSet getSet(ResourceLocation id) {
        return SETS.get(id);
    }

    public static Map<ResourceLocation, ModularSet> getAllSets() {
        return Collections.unmodifiableMap(SETS);
    }
}
