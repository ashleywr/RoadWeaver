/* 文件职责：验证地图快照仓库可以重新加载主动采样区域的结构数据。 */
package net.shiroha233.roadweaver.client.map.data;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.client.map.MapLoadPhase;
import net.shiroha233.roadweaver.client.map.MapViewportController;
import net.shiroha233.roadweaver.core.model.StructureInfo;
import net.shiroha233.roadweaver.core.model.MapRouteClass;
import net.shiroha233.roadweaver.core.model.PlannedMapRoute;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSnapshotStoreTest {
    @Test
    void plannedMapRouteContributesBoundsWithoutCreatingAWorldRoad() {
        PlannedMapRoute route = new PlannedMapRoute(
                new BlockPos(-100, 72, 40), new BlockPos(300, 12, -60), MapRouteClass.TRAIL, "ridge trail");

        MapSnapshot snapshot = new MapSnapshot(List.of(), List.of(), List.of(), List.of(), java.util.Map.of(), List.of(route));

        assertEquals(List.of(route), snapshot.plannedMapRoutes());
        assertEquals(1, snapshot.plannedMapRouteCount(MapRouteClass.TRAIL));
        assertEquals(0, snapshot.plannedMapRouteCount(MapRouteClass.ROAD));
        assertEquals(-100, snapshot.minX());
        assertEquals(-60, snapshot.minZ());
        assertEquals(300, snapshot.maxX());
        assertEquals(40, snapshot.maxZ());
    }

    @Test
    void clearingSampledStructureRectAllowsASecondLoad() {
        MapSnapshotStore store = new MapSnapshotStore();
        MapViewportController.RequestRect loaded = new MapViewportController.RequestRect(0, 0, 64, 64);
        BlockPos structure = new BlockPos(32, 0, 32);
        store.merge(
                MapLoadPhase.STRUCTURES,
                loaded,
                new MapSnapshot(
                        List.of(structure),
                        List.of(),
                        List.of(new StructureInfo(structure, "minecraft:village")),
                        List.of()));

        assertEquals(List.of(loaded), store.loadedRects(MapLoadPhase.STRUCTURES));
        store.clearRect(MapLoadPhase.STRUCTURES, loaded);

        assertTrue(store.loadedRects(MapLoadPhase.STRUCTURES).isEmpty());
        assertTrue(store.snapshot().structures().isEmpty());
    }
}
