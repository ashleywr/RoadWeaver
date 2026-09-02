package net.shiroha233.roadweaver.client.map.data;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.core.model.PlannedMapRoute;
import net.shiroha233.roadweaver.core.model.RoadData;
import net.shiroha233.roadweaver.core.model.StructureConnection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps one map presentation per endpoint pair. A durable classified map
 * route is intentional campaign metadata, so it takes precedence over the
 * generic connection and physical-polyline overlays for that same pair.
 * This affects only the map; planning, persistence, and block placement keep
 * their normal physical-road data.
 */
final class MapRoutePresentationFilter {
    private MapRoutePresentationFilter() {}

    static List<StructureConnection> withoutConnectionsRepresentedByMapRoutes(
            List<StructureConnection> connections, List<PlannedMapRoute> mapRoutes) {
        if (connections == null || connections.isEmpty() || mapRoutes == null || mapRoutes.isEmpty()) {
            return connections == null || connections.isEmpty() ? List.of() : connections;
        }
        Set<EndpointPair> classifiedPairs = classifiedPairs(mapRoutes);
        if (classifiedPairs.isEmpty()) return connections;
        ArrayList<StructureConnection> filtered = new ArrayList<>(connections.size());
        for (StructureConnection connection : connections) {
            if (connection == null || connection.from() == null || connection.to() == null
                    || !classifiedPairs.contains(EndpointPair.of(connection.from(), connection.to()))) {
                filtered.add(connection);
            }
        }
        return filtered;
    }

    static boolean isRoadRepresentedByMapRoute(RoadData road, List<PlannedMapRoute> mapRoutes) {
        if (road == null || !road.hasOwnerPair() || mapRoutes == null || mapRoutes.isEmpty()) return false;
        return classifiedPairs(mapRoutes).contains(EndpointPair.of(road.ownerA2dKey(), road.ownerB2dKey()));
    }

    private static Set<EndpointPair> classifiedPairs(List<PlannedMapRoute> mapRoutes) {
        HashSet<EndpointPair> pairs = new HashSet<>();
        for (PlannedMapRoute route : mapRoutes) {
            if (route != null && route.from() != null && route.to() != null) {
                pairs.add(EndpointPair.of(route.from(), route.to()));
            }
        }
        return pairs;
    }

    private record EndpointPair(long first, long second) {
        static EndpointPair of(BlockPos first, BlockPos second) {
            return of(posKey(first), posKey(second));
        }

        static EndpointPair of(long first, long second) {
            return first <= second ? new EndpointPair(first, second) : new EndpointPair(second, first);
        }

        private static long posKey(BlockPos pos) {
            return (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xffffffffL);
        }
    }
}
