package net.shiroha233.roadweaver.api;

import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.core.model.StructureInfo;

import java.util.List;

/**
 * Receives the durable candidate set produced by RoadWeaver's one-time
 * overworld startup pass. The callback runs on the server thread, after
 * candidate prediction/verification and before initial road generation.
 * Implementations must be quick and must not start their own regional scan.
 */
@FunctionalInterface
public interface InitialPlanningListener {
    void onInitialStructuresPlanned(ServerLevel level, List<StructureInfo> structures);
}
