package net.shiroha233.roadweaver.client.map.data;

import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.core.model.PlannedMapRoute;
import net.shiroha233.roadweaver.persistence.files.StructureFileStorage;

import java.util.List;

/** Queries persisted presentation-only campaign links for a map viewport. */
public final class MapRouteCollector {
    private MapRouteCollector() {}

    public static List<PlannedMapRoute> collect(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        if (level == null) return List.of();
        return StructureFileStorage.getPlannedMapRoutes(level).stream()
                .filter(route -> route != null && intersects(route, minX, minZ, maxX, maxZ))
                .toList();
    }

    private static boolean intersects(PlannedMapRoute route, int minX, int minZ, int maxX, int maxZ) {
        int routeMinX = Math.min(route.from().getX(), route.to().getX());
        int routeMaxX = Math.max(route.from().getX(), route.to().getX());
        int routeMinZ = Math.min(route.from().getZ(), route.to().getZ());
        int routeMaxZ = Math.max(route.from().getZ(), route.to().getZ());
        return routeMaxX >= minX && routeMinX <= maxX && routeMaxZ >= minZ && routeMinZ <= maxZ;
    }
}
