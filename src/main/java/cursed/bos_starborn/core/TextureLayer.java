package cursed.bos_starborn.core;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record TextureLayer(
        ResourceLocation texture,
        float red,
        float green,
        float blue,
        float alpha,
        boolean emissive,
        boolean tintable
) {
    public static TextureLayer of(ResourceLocation texture, String hexColor,
                                  @Nullable Boolean explicitEmissive,
                                  @Nullable Boolean explicitTintable) {
        String path = texture.getPath();

        // 1. Auto-detect emissive from "_glow.png" unless explicitly set in JSON
        boolean isGlow = (explicitEmissive != null)
                ? explicitEmissive
                : path.endsWith("_glow.png");

        // 2. Auto-detect tintable from "_tint.png" (or "_glow.png") unless explicitly set in JSON
        boolean canTint = (explicitTintable != null)
                ? explicitTintable
                : (path.endsWith("_tint.png") || path.endsWith("_glow.png"));

        // 3. Only apply RGB tint if this layer is allowed to be tinted
        float r = 1.0F, g = 1.0F, b = 1.0F;
        if (canTint && hexColor != null && !hexColor.isEmpty()) {
            String clean = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
            int rgb = Integer.parseInt(clean, 16);
            r = ((rgb >> 16) & 0xFF) / 255.0F;
            g = ((rgb >> 8) & 0xFF) / 255.0F;
            b = (rgb & 0xFF) / 255.0F;
        }

        return new TextureLayer(texture, r, g, b, 1.0F, isGlow, canTint);
    }

    /**
     * Helper to apply a dynamic player-chosen RGB color at render time,
     * automatically ignoring the override if this layer has tintable == false.
     */
    public TextureLayer withDynamicTint(float overrideR, float overrideG, float overrideB) {
        if (!this.tintable) {
            return this; // Disallowed from tinting: stays untouched!
        }
        return new TextureLayer(this.texture, overrideR, overrideG, overrideB, this.alpha, this.emissive, true);
    }

    public int getPackedLight(int defaultLight) {
        return emissive ? LightTexture.FULL_BRIGHT : defaultLight;
    }
}
