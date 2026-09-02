package net.shiroha233.roadweaver.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * The single data-driven gate for RoadWeaver world generation.
 *
 * <p>RoadWeaver historically hard-coded {@code minecraft:overworld}. The
 * {@code #roadweaver:road_generation} Level Stem tag now opts dimensions in
 * instead, allowing a campaign dimension to receive roads without turning a
 * server's hub world into a road network.</p>
 */
public final class RoadGenerationDimensions {
    public static final TagKey<LevelStem> ROAD_GENERATION = TagKey.create(
            Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath("roadweaver", "road_generation"));

    private RoadGenerationDimensions() {
    }

    public static boolean isEnabled(ServerLevel level) {
        if (level == null) return false;
        try {
            ResourceKey<LevelStem> stem = ResourceKey.create(
                    Registries.LEVEL_STEM, level.dimension().location());
            return level.registryAccess().registryOrThrow(Registries.LEVEL_STEM)
                    .getHolder(stem)
                    .map(holder -> holder.is(ROAD_GENERATION))
                    .orElse(false);
        } catch (RuntimeException ignored) {
            // During incomplete registry bootstrap, fail closed rather than
            // accidentally applying roads to an unrelated dimension.
            return false;
        }
    }
}
