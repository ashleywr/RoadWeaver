/* File role: adds immutable predicted-structure exclusion zones to a sampled terrain field. */
package net.shiroha233.roadweaver.planning.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.shiroha233.roadweaver.pathfinding.terrain.PathTerrainField;

import java.util.List;

/**
 * A planning-only terrain view which prevents a route between two endpoints
 * from crossing any other already-planned structure.  The two endpoints of
 * the current connection stay passable: endpoint trimming moves the physical
 * road out to the town edge later in the generation pipeline.
 */
final class ProtectedStructureTerrainField implements PathTerrainField {
    private final PathTerrainField delegate;
    private final List<BlockPos> protectedCenters;
    private final long clearanceSquared;

    ProtectedStructureTerrainField(PathTerrainField delegate,
                                   List<BlockPos> centers,
                                   BlockPos connectionStart,
                                   BlockPos connectionEnd,
                                   int clearanceBlocks,
                                   int endpointToleranceBlocks) {
        this.delegate = delegate;
        long endpointToleranceSquared = square(Math.max(0, endpointToleranceBlocks));
        this.protectedCenters = centers.stream()
                .filter(center -> center != null
                        && distanceSquared(center, connectionStart) > endpointToleranceSquared
                        && distanceSquared(center, connectionEnd) > endpointToleranceSquared)
                .toList();
        this.clearanceSquared = square(Math.max(0, clearanceBlocks));
    }

    boolean hasProtectedCenters() {
        return !protectedCenters.isEmpty() && clearanceSquared > 0L;
    }

    @Override public int seaLevel() { return delegate.seaLevel(); }
    @Override public int height(int x, int z) { return delegate.height(x, z); }
    @Override public int oceanFloor(int x, int z) { return delegate.oceanFloor(x, z); }
    @Override public boolean isColumnWater(int x, int z) { return delegate.isColumnWater(x, z); }
    @Override public boolean isNearWater(int x, int z, int neighborDistance) { return delegate.isNearWater(x, z, neighborDistance); }
    @Override public Holder<Biome> biome(int x, int z) { return delegate.biome(x, z); }
    @Override public boolean contains(int x, int z) { return delegate.contains(x, z); }
    @Override public int step() { return delegate.step(); }
    @Override public boolean hasAccurateSample(int x, int z) { return delegate.hasAccurateSample(x, z); }
    @Override public void dispose() { delegate.dispose(); }
    @Override public SampleBundle sampleBundle(int x, int z) { return delegate.sampleBundle(x, z); }

    @Override
    public boolean isBlocked(int x, int z) {
        if (delegate.isBlocked(x, z)) return true;
        for (BlockPos center : protectedCenters) {
            long dx = (long) x - center.getX();
            long dz = (long) z - center.getZ();
            if (dx * dx + dz * dz < clearanceSquared) return true;
        }
        return false;
    }

    private static long distanceSquared(BlockPos a, BlockPos b) {
        if (a == null || b == null) return Long.MAX_VALUE;
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static long square(long value) {
        return value * value;
    }
}
