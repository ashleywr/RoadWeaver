package net.shiroha233.roadweaver.map.tile.render;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.sub.MapAppearanceConfig;

/**
 * 低精度瓦片调色板。
 */
public final class TerrainTilePalette {
    private TerrainTilePalette() {}

    public static int colorFor(Holder<Biome> biome,
                               int height,
                               int seaLevel,
                               int oceanFloor,
                               boolean columnWater,
                               boolean nearWater,
                               double heightShade) {
        if (columnWater) {
            return waterColor(biome, seaLevel, oceanFloor, nearWater);
        }
        String biomePath = biomePath(biome);
        MapAppearanceConfig appearance = ConfigService.get().mapAppearance();
        int base = appearance.terrainStyle() == MapAppearanceConfig.TerrainStyle.HEIGHTMAP
                ? heightmapBaseColor(height, seaLevel)
                : nativeBiomeLandColor(biome, landBaseColor(biomePath, height, seaLevel, nearWater));
        if (appearance.biomePalette() == MapAppearanceConfig.BiomePalette.VIVID
                && appearance.terrainStyle() != MapAppearanceConfig.TerrainStyle.HEIGHTMAP) {
            base = vivid(base);
        }
        return applyHillshade(base, heightShade);
    }

    public static int colorFor(Holder<Biome> biome,
                               int height,
                               int seaLevel,
                               int oceanFloor,
                               boolean columnWater,
                               boolean nearWater) {
        return colorFor(biome, height, seaLevel, oceanFloor, columnWater, nearWater, 1.0);
    }

    /** Applies the configured relief contrast to a terrain color. */
    public static int applyHillshade(int argb, double computedShade) {
        MapAppearanceConfig appearance = ConfigService.get().mapAppearance();
        if (appearance.terrainStyle() == MapAppearanceConfig.TerrainStyle.BIOME_FLAT) return argb;
        double strength = appearance.hillshadeStrengthPercent() / 100.0;
        double shade = 1.0 + (computedShade - 1.0) * strength;
        return HeightShader.multiplyRgb(argb, shade);
    }

    private static int waterColor(Holder<Biome> biome, int seaLevel, int oceanFloor, boolean nearWater) {
        int depth = Math.max(0, seaLevel - oceanFloor);
        double t = clamp(depth / 32.0, 0.0, 1.0);
        int nativeWater = biome == null ? 0xFF3F76E4 : opaque(biome.value().getWaterColor());
        int shallow = nearWater ? mix(nativeWater, 0xFFFFFFFF, 0.16) : nativeWater;
        int deep = HeightShader.multiplyRgb(nativeWater, 0.56);
        return mix(shallow, deep, t);
    }

    /**
     * Respect a selected biome's native visual identity without turning the
     * overview into a literal top-down block render. Explicit grass and
     * foliage overrides are authored specifically for this purpose; the
     * terrain palette remains the fallback for ordinary biomes.
     */
    private static int nativeBiomeLandColor(Holder<Biome> biome, int fallback) {
        if (biome == null) return fallback;
        var effects = biome.value().getSpecialEffects();
        Integer grass = effects.getGrassColorOverride().orElse(null);
        Integer foliage = effects.getFoliageColorOverride().orElse(null);
        if (grass == null && foliage == null) return fallback;
        int dominantGrass = opaque(grass == null ? foliage : grass);
        int dominantFoliage = opaque(foliage == null ? grass : foliage);
        // Ground cover dominates a map tile while canopy remains visible.
        return mix(dominantGrass, dominantFoliage, 0.32);
    }

    private static int landBaseColor(String biomePath, int height, int seaLevel, boolean nearWater) {
        int base;
        if (biomePath.contains("snow") || biomePath.contains("frozen") || biomePath.contains("ice")) {
            base = 0xFFE9EEF2;
        } else if (biomePath.contains("badlands")) {
            base = 0xFFC6784A;
        } else if (biomePath.contains("desert") || biomePath.contains("beach")) {
            base = 0xFFD5C089;
        } else if (biomePath.contains("savanna")) {
            base = 0xFFB9AE64;
        } else if (biomePath.contains("swamp") || biomePath.contains("mangrove")) {
            base = 0xFF516D42;
        } else if (biomePath.contains("jungle")) {
            base = 0xFF3A7643;
        } else if (biomePath.contains("taiga") || biomePath.contains("spruce")) {
            base = 0xFF5B775F;
        } else if (height >= seaLevel + 90) {
            base = 0xFF7B7A76;
        } else if (height >= seaLevel + 60) {
            base = 0xFF749060;
        } else {
            base = 0xFF6B9B52;
        }
        if (nearWater) {
            base = mix(base, 0xFFB0A06C, 0.14);
        }
        return base;
    }

    private static int heightmapBaseColor(int height, int seaLevel) {
        double elevation = clamp((height - seaLevel + 16) / 176.0, 0.0, 1.0);
        if (elevation > 0.84) return mix(0xFF8A8984, 0xFFF2F4F6, (elevation - 0.84) / 0.16);
        if (elevation > 0.48) return mix(0xFF6F995B, 0xFF8A8984, (elevation - 0.48) / 0.36);
        return mix(0xFF739C52, 0xFF6F995B, elevation / 0.48);
    }

    private static int vivid(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        int midpoint = (max + min) / 2;
        r = HeightShader.clampChannel(midpoint + (int) Math.round((r - midpoint) * 1.25));
        g = HeightShader.clampChannel(midpoint + (int) Math.round((g - midpoint) * 1.25));
        b = HeightShader.clampChannel(midpoint + (int) Math.round((b - midpoint) * 1.25));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static String biomePath(Holder<Biome> biome) {
        if (biome == null) return "";
        return biome.unwrapKey().map(key -> key.location().getPath()).orElse("");
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    private static int mix(int from, int to, double t) {
        double clamped = clamp(t, 0.0, 1.0);
        int a0 = (from >>> 24) & 0xFF;
        int r0 = (from >>> 16) & 0xFF;
        int g0 = (from >>> 8) & 0xFF;
        int b0 = from & 0xFF;
        int a1 = (to >>> 24) & 0xFF;
        int r1 = (to >>> 16) & 0xFF;
        int g1 = (to >>> 8) & 0xFF;
        int b1 = to & 0xFF;
        int a = HeightShader.clampChannel((int) Math.round(a0 + (a1 - a0) * clamped));
        int r = HeightShader.clampChannel((int) Math.round(r0 + (r1 - r0) * clamped));
        int g = HeightShader.clampChannel((int) Math.round(g0 + (g1 - g0) * clamped));
        int b = HeightShader.clampChannel((int) Math.round(b0 + (b1 - b0) * clamped));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
