package net.shiroha233.roadweaver.config.sub;

import net.shiroha233.roadweaver.config.SubConfig;

/** Client-facing terrain-map presentation options; they never affect worldgen or roads. */
public final class MapAppearanceConfig implements SubConfig {
    public enum TerrainStyle { BIOME_SHADED, BIOME_FLAT, HEIGHTMAP }
    public enum BiomePalette { NATURAL, VIVID }

    private TerrainStyle terrainStyle = TerrainStyle.BIOME_SHADED;
    private BiomePalette biomePalette = BiomePalette.NATURAL;
    /** 0 removes relief shading; 100 retains the full computed hillshade. */
    private int hillshadeStrengthPercent = 100;

    @Override
    public void sanitize() {
        if (terrainStyle == null) terrainStyle = TerrainStyle.BIOME_SHADED;
        if (biomePalette == null) biomePalette = BiomePalette.NATURAL;
        hillshadeStrengthPercent = Math.max(0, Math.min(100, hillshadeStrengthPercent));
    }

    @Override
    public MapAppearanceConfig snapshot() {
        MapAppearanceConfig copy = new MapAppearanceConfig();
        copy.terrainStyle = terrainStyle;
        copy.biomePalette = biomePalette;
        copy.hillshadeStrengthPercent = hillshadeStrengthPercent;
        return copy;
    }

    public TerrainStyle terrainStyle() { return terrainStyle; }
    public void setTerrainStyle(TerrainStyle value) { terrainStyle = value; }
    public BiomePalette biomePalette() { return biomePalette; }
    public void setBiomePalette(BiomePalette value) { biomePalette = value; }
    public int hillshadeStrengthPercent() { return hillshadeStrengthPercent; }
    public void setHillshadeStrengthPercent(int value) { hillshadeStrengthPercent = value; }
}
