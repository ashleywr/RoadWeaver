package net.shiroha233.roadweaver.client.map.data;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.core.model.ConnectionStatus;
import net.shiroha233.roadweaver.core.model.MapRouteClass;
import net.shiroha233.roadweaver.core.model.PlannedMapRoute;
import net.shiroha233.roadweaver.core.model.StructureConnection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapRoutePresentationFilterTest {
    private static final BlockPos FIRST = new BlockPos(-320, 84, 112);
    private static final BlockPos SECOND = new BlockPos(928, 63, -64);

    @Test
    void classifiedRouteSuppressesOnlyItsMatchingPhysicalConnection() {
        StructureConnection represented = new StructureConnection(FIRST, SECOND, ConnectionStatus.COMPLETED);
        StructureConnection other = new StructureConnection(FIRST, new BlockPos(1600, 72, 48), ConnectionStatus.COMPLETED);
        PlannedMapRoute route = new PlannedMapRoute(SECOND, FIRST, MapRouteClass.ROAD, "campaign road");

        assertEquals(List.of(other), MapRoutePresentationFilter.withoutConnectionsRepresentedByMapRoutes(
                List.of(represented, other), List.of(route)));
    }

}
