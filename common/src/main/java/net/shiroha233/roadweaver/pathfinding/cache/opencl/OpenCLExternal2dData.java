/* 文件职责：为 GPU density graph 提供少量宿主侧二维兼容字段。 */
package net.shiroha233.roadweaver.pathfinding.cache.opencl;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.shiroha233.roadweaver.pathfinding.cache.CoarseHeightBatchRequest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A compact, exact coordinate lookup table for integration density leaves.
 *
 * <p>The table deliberately contains values, not a reimplementation of a
 * foreign density function.  Region Engine remains authoritative for its
 * Atlas/profile logic, while RoadWeaver can still execute the surrounding
 * vanilla density DAG on OpenCL.</p>
 */
final class OpenCLExternal2dData {
    private static final int EMPTY_KEY = Integer.MIN_VALUE;
    private static final OpenCLExternal2dData EMPTY = new OpenCLExternal2dData(
            new int[]{0, 0, EMPTY_KEY, EMPTY_KEY}, new double[]{0.0D});

    private final int[] keys;
    private final double[] values;

    private OpenCLExternal2dData(int[] keys, double[] values) {
        this.keys = keys;
        this.values = values;
    }

    static OpenCLExternal2dData coarse(DensityGraphProgram program, CoarseHeightBatchRequest request) {
        if (program.external2dFields().isEmpty()) {
            return EMPTY;
        }
        Set<Long> coordinates = new LinkedHashSet<>(request.sampleCount());
        for (int index = 0; index < request.sampleCount(); index++) {
            int sampleX = index % request.sampleWidth();
            int sampleZ = index / request.sampleWidth();
            coordinates.add(key(alignToCpuGrid(request.blockXAt(sampleX)), alignToCpuGrid(request.blockZAt(sampleZ))));
        }
        return build(program.external2dFields(), coordinates);
    }

    static OpenCLExternal2dData accurate(DensityGraphProgram program,
                                         List<Long> chunkKeys,
                                         int[] columnReferences,
                                         int cellWidth,
                                         int[] aquiferPositions,
                                         int[] preliminaryPositions) {
        if (program.external2dFields().isEmpty()) {
            return EMPTY;
        }
        Set<Long> coordinates = new LinkedHashSet<>();
        for (long key : chunkKeys) {
            int chunkX = net.minecraft.world.level.ChunkPos.getX(key);
            int chunkZ = net.minecraft.world.level.ChunkPos.getZ(key);
            for (int localX = 0; localX <= 16; localX += cellWidth) {
                for (int localZ = 0; localZ <= 16; localZ += cellWidth) {
                    addWithAquiferScaledCoordinates(coordinates, chunkX * 16 + localX, chunkZ * 16 + localZ);
                }
            }
        }
        for (int offset = 0; offset < columnReferences.length; offset += 3) {
            int chunkIndex = columnReferences[offset];
            long key = chunkKeys.get(chunkIndex);
            int x = net.minecraft.world.level.ChunkPos.getX(key) * 16 + columnReferences[offset + 1];
            int z = net.minecraft.world.level.ChunkPos.getZ(key) * 16 + columnReferences[offset + 2];
            addWithAquiferScaledCoordinates(coordinates, x, z);
        }
        for (int offset = 0; offset < aquiferPositions.length; offset += 3) {
            addWithAquiferScaledCoordinates(coordinates, aquiferPositions[offset], aquiferPositions[offset + 2]);
        }
        for (int offset = 0; offset < preliminaryPositions.length; offset += 2) {
            addWithAquiferScaledCoordinates(coordinates, preliminaryPositions[offset], preliminaryPositions[offset + 1]);
        }
        return build(program.external2dFields(), coordinates);
    }

    int[] keys() {
        return keys;
    }

    double[] values() {
        return values;
    }

    private static OpenCLExternal2dData build(List<DensityFunction> fields, Set<Long> coordinates) {
        int capacity = 1;
        while (capacity < Math.max(1, coordinates.size() * 2)) {
            capacity <<= 1;
        }
        int[] keys = new int[2 + capacity * 2];
        java.util.Arrays.fill(keys, 2, keys.length, EMPTY_KEY);
        keys[0] = capacity - 1;
        keys[1] = fields.size();
        double[] values = new double[capacity * fields.size()];
        for (long coordinate : coordinates) {
            int x = (int) (coordinate >> 32);
            int z = (int) coordinate;
            int slot = hash(x, z) & (capacity - 1);
            while (keys[2 + slot * 2] != EMPTY_KEY) {
                slot = (slot + 1) & (capacity - 1);
            }
            keys[2 + slot * 2] = x;
            keys[2 + slot * 2 + 1] = z;
            DensityFunction.FunctionContext context = new DensityFunction.SinglePointContext(x, 0, z);
            for (int field = 0; field < fields.size(); field++) {
                values[slot * fields.size() + field] = fields.get(field).compute(context);
            }
        }
        return new OpenCLExternal2dData(keys, values);
    }

    private static void addWithAquiferScaledCoordinates(Set<Long> coordinates, int x, int z) {
        coordinates.add(key(x, z));
        coordinates.add(key(Math.floorDiv(x, 16), Math.floorDiv(z, 16)));
        coordinates.add(key(Math.floorDiv(x, 64), Math.floorDiv(z, 64)));
    }

    private static int alignToCpuGrid(int value) {
        return Math.floorDiv(value, 4) * 4;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int hash(int x, int z) {
        int value = x * 0x9E3779B9 ^ Integer.rotateLeft(z * 0x85EBCA6B, 16);
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        return value ^ value >>> 16;
    }
}
