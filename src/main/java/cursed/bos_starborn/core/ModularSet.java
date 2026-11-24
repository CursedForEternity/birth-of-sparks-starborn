package cursed.bos_starborn.core;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record ModularSet(
        ResourceLocation setId,
        float hipOffsetY,
        TorsoPart torso,
        Map<PartSlot, LimbPart> limbs
) {
    public enum PropulsionType {
        NONE, TANK, CAR, JET, HELI
    }

    public enum PartSlot {
        HEAD("head"),
        RIGHT_ARM("right_arm"),
        LEFT_ARM("left_arm"),
        RIGHT_LEG("right_leg"),
        LEFT_LEG("left_leg");

        private final String boneName;

        PartSlot(String boneName) {
            this.boneName = boneName;
        }

        public String getBoneName() {
            return boneName;
        }

        @Nullable
        public static PartSlot fromBoneName(String name) {
            for (PartSlot slot : values()) {
                if (slot.boneName.equals(name)) return slot;
            }
            return null;
        }
    }

    public record TorsoPart(
            ResourceLocation model,
            List<TextureLayer> layers,
            @Nullable ResourceLocation altModel,
            List<TextureLayer> altLayers,
            PropulsionType propulsion,
            @Nullable ResourceLocation animation
    ) {}

    public record LimbPart(
            ResourceLocation model,
            List<TextureLayer> layers
    ) {}
}
