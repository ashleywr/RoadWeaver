package net.shiroha233.roadweaver.planning.terrain;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.core.model.ConnectionStatus;
import net.shiroha233.roadweaver.core.model.StructureConnection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoarseCorridorPlanningAdapterTest {
    @Test
    void samplesOnlyTheConnectionDetourBand() {
        RoadTerrainPlanningPort.Bounds continent = new RoadTerrainPlanningPort.Bounds(-10_000, -10_000, 10_000, 10_000);
        StructureConnection connection = new StructureConnection(
                new BlockPos(-2_000, 0, 200), new BlockPos(1_000, 0, 1_000), ConnectionStatus.PLANNED);

        RoadTerrainPlanningPort.Bounds corridor = CoarseCorridorPlanningAdapter.corridorBounds(
                continent, connection, 1_024);

        assertEquals(-3_024, corridor.minX());
        assertEquals(-824, corridor.minZ());
        assertEquals(2_024, corridor.maxX());
        assertEquals(2_024, corridor.maxZ());
    }

    @Test
    void clipsTheDetourBandToTheRegionalPlan() {
        RoadTerrainPlanningPort.Bounds plan = new RoadTerrainPlanningPort.Bounds(-1_000, -1_000, 1_000, 1_000);
        StructureConnection connection = new StructureConnection(
                new BlockPos(-950, 0, -900), new BlockPos(-800, 0, -700), ConnectionStatus.PLANNED);

        RoadTerrainPlanningPort.Bounds corridor = CoarseCorridorPlanningAdapter.corridorBounds(plan, connection, 1_024);

        assertEquals(-1_000, corridor.minX());
        assertEquals(-1_000, corridor.minZ());
        assertEquals(224, corridor.maxX());
        assertEquals(324, corridor.maxZ());
    }
}
