package net.shiroha233.roadweaver.api;

import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.core.model.StructureInfo;

import java.util.List;

/**
 * Supplies pure, bounded structure candidates for RoadWeaver's first plan.
 * Implementations must not load chunks or generate terrain.  RoadWeaver
 * persists the candidates, selects the graph, and owns route generation.
 */
@FunctionalInterface
public interface InitialPlanningCandidateProvider {
    List<StructureInfo> initialCandidates(ServerLevel level, InitialPlanningBounds bounds);

    /**
     * Requests that the supplied roster, rather than RoadWeaver's generic
     * structure scan, defines the initial road graph.  This is opt-in so the
     * default behavior for every existing integration remains additive.
     */
    default boolean exclusive() {
        return false;
    }

    /** World-aware form used by discovery paths that do not share initial-plan state. */
    default boolean exclusive(ServerLevel level, InitialPlanningBounds bounds) {
        return exclusive();
    }
}
