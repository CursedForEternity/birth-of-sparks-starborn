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

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = BOS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModularSetManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, ModularSet> SETS = new HashMap<>();

    public ModularSetManager() {
        // Looks inside data/<namespace>/modular_sets/*.json
        super(GSON, "modular_sets");
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
                JsonObject root = GsonHelper.convertToJsonObject(jsonElement, "modular_set");
                String namespace = fileId.getNamespace();
                String folderName = fileId.getPath();

                ResourceLocation setId = root.has("set_id")
                        ? new ResourceLocation(GsonHelper.getAsString(root, "set_id"))
                        : fileId;

                float hipOffsetY = GsonHelper.getAsFloat(root, "hip_offset_y", 0.75F);

                // Parse Torso (required)
                JsonObject torsoObj = GsonHelper.getAsJsonObject(root, "torso");
                ModularSet.TorsoPart torso = parseTorso(torsoObj, namespace, folderName);

                // Parse Optional Limbs
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
        ResourceLocation texture = resolvePath(obj, "texture", namespace, "textures/entity/parts/" + folder + "/torso.png");

        ResourceLocation altModel = obj.has("alt")
                ? new ResourceLocation(GsonHelper.getAsString(obj, "alt")) : null;
        ResourceLocation altTexture = obj.has("alt_texture")
                ? new ResourceLocation(GsonHelper.getAsString(obj, "alt_texture"))
                : (altModel != null ? new ResourceLocation(namespace, "textures/entity/parts/" + folder + "/alt.png") : null);

        String propStr = GsonHelper.getAsString(obj, "propulsion", "NONE").toUpperCase();
        ModularSet.PropulsionType propulsion = ModularSet.PropulsionType.valueOf(propStr);

        ResourceLocation animation = obj.has("animation")
                ? new ResourceLocation(GsonHelper.getAsString(obj, "animation")) : null;

        return new ModularSet.TorsoPart(model, texture, altModel, altTexture, propulsion, animation);
    }

    private ModularSet.LimbPart parseLimb(JsonObject obj, String namespace, String folder, ModularSet.PartSlot slot) {
        String defaultFile = switch (slot) {
            case HEAD -> "head";
            case RIGHT_ARM -> "arm_right";
            case LEFT_ARM -> "arm_left";
            case RIGHT_LEG -> "leg_right";
            case LEFT_LEG -> "leg_left";
        };
        ResourceLocation model = resolvePath(obj, "path", namespace, "geo/parts/" + folder + "/" + defaultFile + ".geo.json");
        ResourceLocation texture = resolvePath(obj, "texture", namespace, "textures/entity/parts/" + folder + "/" + defaultFile + ".png");
        return new ModularSet.LimbPart(model, texture);
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
