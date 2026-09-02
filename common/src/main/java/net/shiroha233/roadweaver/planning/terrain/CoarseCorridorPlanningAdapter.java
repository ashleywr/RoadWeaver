/* 文件职责：使用粗地形高度场规划道路，避免在线阶段重复构造原版 NoiseChunk。 */
package net.shiroha233.roadweaver.planning.terrain;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.sub.TerrainSamplingMode;
import net.shiroha233.roadweaver.core.model.StructureConnection;
import net.shiroha233.roadweaver.generation.progress.InitialGenerationCoarseSamplingProgress;
import net.shiroha233.roadweaver.generation.progress.InitialGenerationProgressTracker;
import net.shiroha233.roadweaver.generation.progress.InitialGenerationStage;
import net.shiroha233.roadweaver.pathfinding.cache.TerrainSamplingCache;
import net.shiroha233.roadweaver.pathfinding.terrain.region.CoarseTerrainPngWriter;
import net.shiroha233.roadweaver.pathfinding.terrain.region.CoarseTerrainRegion;
import net.shiroha233.roadweaver.pathfinding.terrain.region.CoarseTerrainRegionSampler;
import net.shiroha233.roadweaver.runtime.ThreadPoolManager;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 粗地形道路规划器。
 *
 * <p>粗高度场与原版 preliminary surface 使用相同的噪声语义。路径的最终切填高度由
 * RoadChunkPlan 在区块生成前编译，不在规划线程重新执行一次完整区块噪声生成。</p>
 */
final class CoarseCorridorPlanningAdapter implements RoadTerrainPlanningPort {
    @Override
    public Result plan(Request request) {
        if (request.connections().isEmpty()) {
            return Result.empty(TerrainSamplingMode.COARSE_CORRIDOR);
        }

        int corridorPadding = ConfigService.get().planning().coarseCorridorPaddingBlocks();
        int step = request.pathfinding().effectiveAStarStep();
            InitialGenerationProgressTracker.enterStage(
                    InitialGenerationStage.COARSE_SAMPLING, "sampling_route_corridors");
            InitialGenerationProgressTracker.setCoarseSamplingPlan(
                    estimatedSamples(request, corridorPadding, step), "sampling_route_corridors");
            InitialGenerationProgressTracker.setCoarsePathPlan(
                    request.connections().size(), "sampling_route_corridors");

            // Each route corridor is independent. Solving them serially makes
            // a Terra world wait once per town edge even though the initial
            // planner already owns a bounded worker pool. Retain the input
            // order when collecting results so route selection stays seed
            // deterministic; only the expensive sampling/search runs in
            // parallel. The shared tile cache deduplicates overlapping work.
            Executor executor = ThreadPoolManager.computeExecutor();
            List<CompletableFuture<CoarsePath>> futures = new ArrayList<>();
            for (StructureConnection connection : request.connections()) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> solveConnection(request, connection, corridorPadding, step), executor));
            }

            LinkedHashMap<StructureConnection, List<BlockPos>> paths = new LinkedHashMap<>();
            for (CompletableFuture<CoarsePath> future : futures) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new java.util.concurrent.CancellationException("coarse planning interrupted");
                }
                CoarsePath result = future.join();
                if (result.path() != null && !result.path().isEmpty()) {
                    paths.put(result.connection(), result.path());
                }
            }
        return new Result(TerrainSamplingMode.COARSE_CORRIDOR, paths);
    }

    private static CoarsePath solveConnection(Request request,
                                              StructureConnection connection,
                                              int corridorPadding,
                                              int step) {
        TerrainSamplingCache cache = new TerrainSamplingCache();
        CoarseTerrainRegion terrain = null;
        try {
            Bounds corridor = corridorBounds(request.bounds(), connection, corridorPadding);
            terrain = CoarseTerrainRegionSampler.sample(
                    request.level(), corridor.minX(), corridor.minZ(), corridor.maxX(), corridor.maxZ(), step,
                    InitialGenerationCoarseSamplingProgress.INSTANCE);
            writeMapTiles(request, terrain);
            InitialGenerationProgressTracker.enterStage(
                    InitialGenerationStage.COARSE_PATHING, "computing_coarse_paths");
            List<BlockPos> path = RawPathSearch.find(
                    request.level(), connection, request.pathfinding(), cache, terrain);
            return new CoarsePath(connection, path == null ? List.of() : List.copyOf(path));
        } finally {
            if (terrain != null) terrain.dispose();
            cache.clear();
            InitialGenerationProgressTracker.recordCoarsePathDone();
        }
    }

    /** Limits a coarse solve to the connection's detour band, clipped to its regional plan. */
    static Bounds corridorBounds(Bounds planningBounds,
                                 StructureConnection connection,
                                 int paddingBlocks) {
        int padding = Math.max(128, paddingBlocks);
        BlockPos from = connection.from();
        BlockPos to = connection.to();
        int minX = Math.max(planningBounds.minX(), Math.min(from.getX(), to.getX()) - padding);
        int minZ = Math.max(planningBounds.minZ(), Math.min(from.getZ(), to.getZ()) - padding);
        int maxX = Math.min(planningBounds.maxX(), Math.max(from.getX(), to.getX()) + padding);
        int maxZ = Math.min(planningBounds.maxZ(), Math.max(from.getZ(), to.getZ()) + padding);
        return new Bounds(minX, minZ, maxX, maxZ);
    }

    private static long estimatedSamples(Request request, int paddingBlocks, int step) {
        long total = 0L;
        int safeStep = Math.max(1, step);
        for (StructureConnection connection : request.connections()) {
            Bounds corridor = corridorBounds(request.bounds(), connection, paddingBlocks);
            int alignedMinX = Math.floorDiv(corridor.minX(), safeStep) * safeStep;
            int alignedMinZ = Math.floorDiv(corridor.minZ(), safeStep) * safeStep;
            int alignedMaxX = Math.floorDiv(corridor.maxX() + safeStep - 1, safeStep) * safeStep;
            int alignedMaxZ = Math.floorDiv(corridor.maxZ() + safeStep - 1, safeStep) * safeStep;
            long width = Math.floorDiv(alignedMaxX - alignedMinX, safeStep) + 1L;
            long height = Math.floorDiv(alignedMaxZ - alignedMinZ, safeStep) + 1L;
            total = Math.addExact(total, Math.multiplyExact(width, height));
        }
        return total;
    }

    private static void writeMapTiles(Request request, CoarseTerrainRegion terrain) {
        try {
            // The writer already takes a lock per output tile, which prevents
            // conflicting writes while still allowing separate corridors to
            // materialize different map tiles concurrently.
            CoarseTerrainPngWriter.writeTerrainTiles(request.level(), terrain);
        } catch (RuntimeException ignored) {
            // 地图输出是旁路能力，不能阻断道路规划。
        }
    }

    private record CoarsePath(StructureConnection connection, List<BlockPos> path) {}
}
