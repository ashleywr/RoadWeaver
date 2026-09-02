package net.shiroha233.roadweaver.client.map.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaver.core.model.RoadData;
import net.shiroha233.roadweaver.core.model.RoadSegmentPlacement;
import net.shiroha233.roadweaver.core.model.PlannedMapRoute;
import net.shiroha233.roadweaver.persistence.sharded.RoadShardStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * 道路折线快照收集。
 */
public final class MapRoadCollector {
    private MapRoadCollector() {}

    public static List<List<BlockPos>> collect(ServerLevel level, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        return collect(level, minBlockX, minBlockZ, maxBlockX, maxBlockZ, List.of());
    }

    /**
     * Classed campaign links replace their matching physical polyline only in
     * the map view, preventing a path/road/street overlay from appearing as a
     * second parallel route.
     */
    public static List<List<BlockPos>> collect(ServerLevel level, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ,
                                               List<PlannedMapRoute> mapRoutes) {
        if (level == null || !Level.OVERWORLD.equals(level.dimension())) return List.of();
        List<RoadData> roadDataList = RoadShardStorage.queryRect(level, minBlockX, minBlockZ, maxBlockX, maxBlockZ);
        ArrayList<List<BlockPos>> roads = new ArrayList<>();
        for (RoadData rd : roadDataList) {
            if (MapRoutePresentationFilter.isRoadRepresentedByMapRoute(rd, mapRoutes)) continue;
            List<RoadSegmentPlacement> segs = rd.roadSegmentList();
            if (segs == null || segs.isEmpty()) continue;
            ArrayList<BlockPos> poly = new ArrayList<>(segs.size());
            for (RoadSegmentPlacement sp : segs) poly.add(sp.middlePos());
            if (poly.size() >= 2) roads.add(poly);
        }
        return roads;
    }
}
