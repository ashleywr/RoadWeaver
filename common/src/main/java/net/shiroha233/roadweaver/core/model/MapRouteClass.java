package net.shiroha233.roadweaver.core.model;

/**
 * Presentation-only hierarchy for authored campaign links.
 *
 * <p>These classes do not alter RoadWeaver's pathfinding or block placement.
 * They let an integration show a proposed foot trail, inter-town road, or
 * town street before it chooses to request a physical route.</p>
 */
public enum MapRouteClass {
    TRAIL,
    ROAD,
    STREET
}
