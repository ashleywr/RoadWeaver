package net.shiroha233.roadweaver.api;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.List;

/**
 * Lets a worldgen provider enumerate candidates for a non-vanilla structure
 * placement while RoadWeaver remains the sole regional structure/road planner.
 * Implementations must be pure: no chunk loads, registry reads, disk I/O, or
 * server-thread access. Returned chunks are still checked by RoadWeaver using
 * {@link StructurePlacement#isStructureChunk}.
 */
public interface StructurePlacementAdapter {
    boolean supports(StructurePlacement placement);

    List<ChunkPos> potentialChunks(StructurePlacement placement, long worldSeed,
                                   int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ);
}
