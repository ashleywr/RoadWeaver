/* File purpose: invalidates only derived terrain map tiles when their presentation changes. */
package net.shiroha233.roadweaver.map.tile.storage;

import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.sub.MapAppearanceConfig;
import net.shiroha233.roadweaver.map.tile.core.MapTileLayer;
import net.shiroha233.roadweaver.persistence.files.FileStorageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Keeps the terrain-image cache separate from planning and world data.
 * A palette or hillshade edit is therefore visible after the next resample,
 * without invalidating roads, endpoints, or generated terrain.
 */
public final class MapAppearanceTileCacheGuard {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    private static final String MARKER = "terrain-map-appearance-v1.txt";

    private MapAppearanceTileCacheGuard() {}

    public static void ensure(ServerLevel level) {
        if (level == null) return;
        MapAppearanceConfig appearance = ConfigService.get().mapAppearance();
        ensure(level, signature(appearance));
    }

    static String signature(MapAppearanceConfig appearance) {
        Objects.requireNonNull(appearance, "appearance");
        return appearance.terrainStyle().name()
                + "|" + appearance.biomePalette().name()
                + "|" + appearance.hillshadeStrengthPercent();
    }

    static void ensure(ServerLevel level, String expected) {
        Objects.requireNonNull(level, "level");
        Path root = ServerMapTilePathResolver.dimensionRoot(level);
        Path marker = root.resolve("migration").resolve(MARKER);
        synchronized ((marker.toAbsolutePath().normalize() + "#lock").intern()) {
            try {
                String current = Files.exists(marker)
                        ? Files.readString(marker, StandardCharsets.UTF_8).trim()
                        : "";
                if (expected.equals(current)) return;
                FileStorageIO.deleteTree(
                        ServerMapTilePathResolver.layerRoot(level, MapTileLayer.TERRAIN_COARSE),
                        LOGGER,
                        "Failed to clear stale coarse terrain-map tiles");
                FileStorageIO.deleteTree(
                        ServerMapTilePathResolver.layerRoot(level, MapTileLayer.TERRAIN_ACCURATE),
                        LOGGER,
                        "Failed to clear stale accurate terrain-map tiles");
                FileStorageIO.writeStringAtomic(marker, expected);
            } catch (IOException failure) {
                LOGGER.warn("Failed to validate terrain-map appearance cache dimension={}",
                        level.dimension().location(), failure);
            }
        }
    }
}
