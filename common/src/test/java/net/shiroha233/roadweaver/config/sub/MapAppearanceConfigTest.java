package net.shiroha233.roadweaver.config.sub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class MapAppearanceConfigTest {
    @Test
    void sanitizeAndSnapshotRetainPresentationChoices() {
        MapAppearanceConfig config = new MapAppearanceConfig();
        config.setTerrainStyle(MapAppearanceConfig.TerrainStyle.HEIGHTMAP);
        config.setBiomePalette(MapAppearanceConfig.BiomePalette.VIVID);
        config.setHillshadeStrengthPercent(150);
        config.sanitize();

        MapAppearanceConfig snapshot = config.snapshot();

        assertNotSame(config, snapshot);
        assertEquals(MapAppearanceConfig.TerrainStyle.HEIGHTMAP, snapshot.terrainStyle());
        assertEquals(MapAppearanceConfig.BiomePalette.VIVID, snapshot.biomePalette());
        assertEquals(100, snapshot.hillshadeStrengthPercent());
    }
}
