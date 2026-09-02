package net.shiroha233.roadweaver.pathfinding.cache;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.shiroha233.roadweaver.generation.progress.InitialGenerationProgressTracker;

/**
 * Samples a custom chunk generator through Minecraft's public, unloaded-column
 * height API. This keeps RoadWeaver compatible with generators such as OTG
 * whose terrain cannot be reconstructed from vanilla density functions.
 */
public final class GeneratorCoarseHeightBatchSampler implements CoarseHeightBatchSampler {
    /**
     * Custom generators can make each unloaded-height query substantially more
     * expensive than evaluating a vanilla density function. Sixty-four blocks is
     * deliberately limited to the preliminary corridor pass: the resulting
     * height field is interpolated back onto the planner's 32-block grid, and
     * the normal per-chunk road compiler still resolves the final road against
     * real terrain. This reduces expensive generator column queries by 64x
     * compared with the normal eight-block grid (and 4x compared with the old
     * custom-generator coarse pass) without making the placed road follow an
     * invented heightmap.
     */
    private static final int MIN_GENERATOR_SAMPLE_SPACING_BLOCKS = 64;
    private final ServerLevel level;
    private final ChunkGenerator generator;
    private final RandomState randomState;

    private GeneratorCoarseHeightBatchSampler(ServerLevel level,
                                              ChunkGenerator generator,
                                              RandomState randomState) {
        this.level = level;
        this.generator = generator;
        this.randomState = randomState;
    }

    public static GeneratorCoarseHeightBatchSampler create(ServerLevel level) {
        var chunkSource = level.getChunkSource();
        return new GeneratorCoarseHeightBatchSampler(
                level,
                chunkSource.getGenerator(),
                chunkSource.getGeneratorState().randomState());
    }

    @Override
    public int[] sampleHeights(CoarseHeightBatchRequest request) {
        long startedAt = System.currentTimeMillis();
        int[] heights = new int[request.sampleCount()];
        int stride = Math.max(1, ceilDiv(MIN_GENERATOR_SAMPLE_SPACING_BLOCKS, request.step()));
        int sourceWidth = sourceCount(request.sampleWidth(), stride);
        int sourceHeight = sourceCount(request.sampleHeight(), stride);
        int[] sourceHeights = new int[Math.multiplyExact(sourceWidth, sourceHeight)];

        for (int sourceZ = 0; sourceZ < sourceHeight; sourceZ++) {
            if (Thread.currentThread().isInterrupted()) return null;
            int z = sourceIndex(sourceZ, stride, request.sampleHeight());
            int blockZ = request.blockZAt(z);
            for (int sourceX = 0; sourceX < sourceWidth; sourceX++) {
                int x = sourceIndex(sourceX, stride, request.sampleWidth());
                int blockX = request.blockXAt(x);
                sourceHeights[sourceZ * sourceWidth + sourceX] = generator.getBaseHeight(
                        blockX,
                        blockZ,
                        Heightmap.Types.OCEAN_FLOOR_WG,
                        level,
                        randomState);
            }
        }
        for (int z = 0; z < request.sampleHeight(); z++) {
            if (Thread.currentThread().isInterrupted()) return null;
            for (int x = 0; x < request.sampleWidth(); x++) {
                heights[z * request.sampleWidth() + x] = interpolate(
                        sourceHeights, sourceWidth, sourceHeight, x, z, stride,
                        request.sampleWidth(), request.sampleHeight());
            }
        }
        InitialGenerationProgressTracker.recordSampleBatch(
                "CPU_GENERATOR", request.sampleCount(), System.currentTimeMillis() - startedAt,
                "CPU", null);
        return heights;
    }

    private static int sourceCount(int length, int stride) {
        return Math.floorDiv(length - 1, stride) + 1;
    }

    private static int ceilDiv(int value, int divisor) {
        return Math.floorDiv(value + divisor - 1, divisor);
    }

    private static int sourceIndex(int sourceIndex, int stride, int length) {
        return Math.min(length - 1, sourceIndex * stride);
    }

    private static int interpolate(int[] sourceHeights,
                                   int sourceWidth,
                                   int sourceHeight,
                                   int x,
                                   int z,
                                   int stride,
                                   int sampleWidth,
                                   int sampleHeight) {
        int x0 = Math.min(sourceWidth - 1, x / stride);
        int z0 = Math.min(sourceHeight - 1, z / stride);
        int x1 = Math.min(sourceWidth - 1, x0 + 1);
        int z1 = Math.min(sourceHeight - 1, z0 + 1);
        int x0Position = sourceIndex(x0, stride, sampleWidth);
        int z0Position = sourceIndex(z0, stride, sampleHeight);
        int x1Position = sourceIndex(x1, stride, sampleWidth);
        int z1Position = sourceIndex(z1, stride, sampleHeight);
        double xProgress = x1Position == x0Position ? 0.0D : (x - x0Position) / (double) (x1Position - x0Position);
        double zProgress = z1Position == z0Position ? 0.0D : (z - z0Position) / (double) (z1Position - z0Position);
        double top = lerp(sourceHeights[z0 * sourceWidth + x0], sourceHeights[z0 * sourceWidth + x1], xProgress);
        double bottom = lerp(sourceHeights[z1 * sourceWidth + x0], sourceHeights[z1 * sourceWidth + x1], xProgress);
        return (int) Math.round(lerp(top, bottom, zProgress));
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }
}
