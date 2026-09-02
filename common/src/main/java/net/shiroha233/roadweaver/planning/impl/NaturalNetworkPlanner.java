package net.shiroha233.roadweaver.planning.impl;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.core.model.StructureConnection;
import net.shiroha233.roadweaver.planning.NetworkPlanner;
import net.shiroha233.roadweaver.planning.PlanningUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A sparse regional graph: a guaranteed MST backbone plus a very small number
 * of short local loops. It avoids KNN's dense cluster while retaining useful
 * alternate routes around terrain failures.
 */
public final class NaturalNetworkPlanner implements NetworkPlanner {
    @Override
    public List<StructureConnection> plan(List<BlockPos> points, int maxEdgeLenBlocks) {
        List<StructureConnection> backbone = MSTPlanner.planMST(points, maxEdgeLenBlocks);
        if (backbone.isEmpty()) return backbone;

        ArrayList<BlockPos> unique = new ArrayList<>();
        HashSet<Long> seen = new HashSet<>();
        for (BlockPos point : points) {
            BlockPos normalized = new BlockPos(point.getX(), 0, point.getZ());
            if (seen.add(PlanningUtils.pos2dKey(normalized))) unique.add(normalized);
        }
        Map<Long, Integer> degrees = new HashMap<>();
        HashSet<EdgeKey> existing = new HashSet<>();
        long totalLength = 0L;
        for (StructureConnection edge : backbone) {
            long from = PlanningUtils.pos2dKey(edge.from());
            long to = PlanningUtils.pos2dKey(edge.to());
            degrees.merge(from, 1, Integer::sum);
            degrees.merge(to, 1, Integer::sum);
            existing.add(edgeKey(from, to));
            totalLength += distanceSquared(edge.from(), edge.to());
        }
        long averageBackboneLength = Math.max(1L, totalLength / backbone.size());
        long maxLoopLength = Math.min((long) maxEdgeLenBlocks * maxEdgeLenBlocks,
                Math.max(averageBackboneLength * 2L, 1_440_000L));
        ArrayList<Edge> candidates = new ArrayList<>();
        for (int i = 0; i < unique.size(); i++) {
            for (int j = i + 1; j < unique.size(); j++) {
                BlockPos a = unique.get(i);
                BlockPos b = unique.get(j);
                long d2 = distanceSquared(a, b);
                EdgeKey key = edgeKey(PlanningUtils.pos2dKey(a), PlanningUtils.pos2dKey(b));
                if (!existing.contains(key) && d2 <= maxLoopLength) candidates.add(new Edge(a, b, d2, key));
            }
        }
        candidates.sort(Comparator.comparingLong(Edge::distanceSquared));
        ArrayList<StructureConnection> result = new ArrayList<>(backbone);
        int loopLimit = Math.max(1, unique.size() / 5);
        for (Edge candidate : candidates) {
            if (loopLimit == 0) break;
            long a = PlanningUtils.pos2dKey(candidate.from());
            long b = PlanningUtils.pos2dKey(candidate.to());
            if (degrees.getOrDefault(a, 0) >= 3 || degrees.getOrDefault(b, 0) >= 3) continue;
            result.add(new StructureConnection(candidate.from(), candidate.to()));
            degrees.merge(a, 1, Integer::sum);
            degrees.merge(b, 1, Integer::sum);
            existing.add(candidate.key());
            loopLimit--;
        }
        return List.copyOf(result);
    }

    private static long distanceSquared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static EdgeKey edgeKey(long a, long b) {
        return a <= b ? new EdgeKey(a, b) : new EdgeKey(b, a);
    }

    private record Edge(BlockPos from, BlockPos to, long distanceSquared, EdgeKey key) {}
    private record EdgeKey(long first, long second) {}
}
