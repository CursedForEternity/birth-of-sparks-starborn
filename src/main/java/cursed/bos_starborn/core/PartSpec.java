package cursed.bos_starborn.core;

import net.minecraft.resources.ResourceLocation;

public record PartSpec(
    ResourceLocation geoModel,
    ResourceLocation texture,
    String movementType
) {}