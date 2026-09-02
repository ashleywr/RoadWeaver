/* 文件职责：统一使用量化地形场执行道路原始路径搜索。 */
package net.shiroha233.roadweaver.planning.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.sub.PathfindingCostConfig;
import net.shiroha233.roadweaver.core.model.StructureConnection;
import net.shiroha233.roadweaver.core.model.StructureInfo;
import net.shiroha233.roadweaver.pathfinding.PathResult;
import net.shiroha233.roadweaver.pathfinding.Pathfinder;
import net.shiroha233.roadweaver.pathfinding.PathfinderFactory;
import net.shiroha233.roadweaver.pathfinding.cache.TerrainSamplingCache;
import net.shiroha233.roadweaver.pathfinding.terrain.PathTerrainField;
import net.shiroha233.roadweaver.persistence.files.StructureFileStorage;

import java.util.List;

import static net.shiroha233.roadweaver.pathfinding.impl.PathfindingHelper.snapToGrid;

final class RawPathSearch {
    private RawPathSearch() {}

    static List<BlockPos> find(ServerLevel level,
                               StructureConnection connection,
                               PathfindingCostConfig config,
                               TerrainSamplingCache cache,
                               PathTerrainField terrain) {
        int step = config.effectiveAStarStep();
        int startX = snapToGrid(connection.from().getX(), step);
        int startZ = snapToGrid(connection.from().getZ(), step);
        int endX = snapToGrid(connection.to().getX(), step);
        int endZ = snapToGrid(connection.to().getZ(), step);
        if (!terrain.contains(startX, startZ) || !terrain.contains(endX, endZ)) {
            return null;
        }
        BlockPos start = new BlockPos(startX, terrain.height(startX, startZ), startZ);
        BlockPos end = new BlockPos(endX, terrain.height(endX, endZ), endZ);
        PathTerrainField protectedTerrain = withPredictedStructureClearance(level, terrain, start, end, step);
        Pathfinder pathfinder = PathfinderFactory.create(config.pathfindingAlgorithm());
        PathResult result = pathfinder.findRawPath(start, end, level, config.aStarMaxSteps(), cache, protectedTerrain, config);
        return result.success() && result.hasRawPath() ? result.rawPath() : null;
    }

    private static PathTerrainField withPredictedStructureClearance(ServerLevel level,
                                                                      PathTerrainField terrain,
                                                                      BlockPos start,
                                                                      BlockPos end,
                                                                      int step) {
        var structureConfig = ConfigService.get().structurePrediction();
        int clearance = structureConfig.plannedStructureClearanceBlocks();
        if (!structureConfig.structureAvoidanceEnabled() || clearance <= 0) return terrain;

        // Every supported planner allows a detour of at most 1024 blocks, so
        // include manual campaign endpoints in that complete search envelope.
        int padding = 1024 + clearance;
        List<BlockPos> centers = StructureFileStorage.queryRect(
                        level,
                        Math.min(start.getX(), end.getX()) - padding,
                        Math.min(start.getZ(), end.getZ()) - padding,
                        Math.max(start.getX(), end.getX()) + padding,
                        Math.max(start.getZ(), end.getZ()) + padding,
                        StructureFileStorage.SOURCE_MANUAL)
                .stream()
                .map(StructureInfo::pos)
                .toList();
        ProtectedStructureTerrainField wrapped = new ProtectedStructureTerrainField(
                terrain, centers, start, end, clearance, Math.max(step * 2, 24));
        return wrapped.hasProtectedCenters() ? wrapped : terrain;
    }
}
