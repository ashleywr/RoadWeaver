package net.shiroha233.roadweaver.core.model;

import net.minecraft.core.BlockPos;

/** A durable, non-generating campaign link shown only in the RoadWeaver map. */
public record PlannedMapRoute(BlockPos from, BlockPos to, MapRouteClass routeClass, String label) {
    public PlannedMapRoute {
        from = normalize(from);
        to = normalize(to);
        routeClass = routeClass == null ? MapRouteClass.ROAD : routeClass;
        label = label == null ? "" : label;
    }

    public boolean matchesEndpoints(BlockPos first, BlockPos second) {
        BlockPos a = normalize(first);
        BlockPos b = normalize(second);
        return from.equals(a) && to.equals(b) || from.equals(b) && to.equals(a);
    }

    private static BlockPos normalize(BlockPos pos) {
        if (pos == null) throw new IllegalArgumentException("map route endpoint is required");
        return new BlockPos(pos.getX(), 0, pos.getZ());
    }
}
