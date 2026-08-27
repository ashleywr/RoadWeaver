package net.shiroha233.roadweaver.api;

import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/**
 * Optional, server-thread-only extension for a world-layout owner to define a
 * bounded first-pass domain. Returning empty preserves RoadWeaver's configured
 * spawn-radius behavior.
 */
@FunctionalInterface
public interface InitialPlanningBoundsProvider {
    Optional<InitialPlanningBounds> initialBounds(ServerLevel level);
}
