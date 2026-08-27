package net.shiroha233.roadweaver.api;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Thread-safe registry for pure addon placement enumerators. */
public final class StructurePlacementAdapters {
    private static final CopyOnWriteArrayList<StructurePlacementAdapter> ADAPTERS = new CopyOnWriteArrayList<>();

    private StructurePlacementAdapters() {}

    public static void register(StructurePlacementAdapter adapter) {
        if (adapter != null) ADAPTERS.addIfAbsent(adapter);
    }

    public static void unregister(StructurePlacementAdapter adapter) {
        ADAPTERS.remove(adapter);
    }

    public static List<ChunkPos> potentialChunks(StructurePlacement placement, long seed,
                                                   int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        for (StructurePlacementAdapter adapter : ADAPTERS) {
            try {
                if (adapter.supports(placement)) {
                    List<ChunkPos> chunks = adapter.potentialChunks(placement, seed, minChunkX, minChunkZ, maxChunkX, maxChunkZ);
                    return chunks == null ? List.of() : List.copyOf(chunks);
                }
            } catch (RuntimeException ignored) {
                // An optional provider must not break RoadWeaver's planner.
                return List.of();
            }
        }
        return List.of();
    }
}
