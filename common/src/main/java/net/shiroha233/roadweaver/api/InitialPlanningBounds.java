package net.shiroha233.roadweaver.api;

/** Inclusive block rectangle requested by an optional initial-planning owner. */
public record InitialPlanningBounds(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
    public InitialPlanningBounds {
        if (maxBlockX < minBlockX || maxBlockZ < minBlockZ) {
            throw new IllegalArgumentException("Initial planning bounds are inverted");
        }
    }
}
