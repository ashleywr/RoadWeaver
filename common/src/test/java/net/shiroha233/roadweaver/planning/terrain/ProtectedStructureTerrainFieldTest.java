package net.shiroha233.roadweaver.planning.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.shiroha233.roadweaver.pathfinding.terrain.PathTerrainField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectedStructureTerrainFieldTest {
    @Test
    void onlyUnrelatedPlannedEndpointsBecomeRouteExclusions() {
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos end = new BlockPos(1_000, 0, 0);
        ProtectedStructureTerrainField field = new ProtectedStructureTerrainField(
                new PassableTerrain(),
                List.of(start, end, new BlockPos(500, 0, 0)),
                start, end, 128, 24);

        assertTrue(field.hasProtectedCenters());
        assertFalse(field.isBlocked(0, 0));
        assertFalse(field.isBlocked(1_000, 0));
        assertTrue(field.isBlocked(500, 0));
        assertTrue(field.isBlocked(600, 0));
        assertFalse(field.isBlocked(629, 0));
    }

    private static final class PassableTerrain implements PathTerrainField {
        @Override public int seaLevel() { return 63; }
        @Override public int height(int x, int z) { return 64; }
        @Override public int oceanFloor(int x, int z) { return 64; }
        @Override public boolean isColumnWater(int x, int z) { return false; }
        @Override public boolean isNearWater(int x, int z, int neighborDistance) { return false; }
        @Override public Holder<Biome> biome(int x, int z) { return null; }
        @Override public boolean contains(int x, int z) { return true; }
        @Override public int step() { return 8; }
    }
}
