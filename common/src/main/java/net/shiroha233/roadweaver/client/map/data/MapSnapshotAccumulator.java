package net.shiroha233.roadweaver.client.map.data;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.core.model.ConnectionStatus;
import net.shiroha233.roadweaver.core.model.StructureConnection;
import net.shiroha233.roadweaver.core.model.StructureInfo;
import net.shiroha233.roadweaver.core.model.PlannedMapRoute;
import net.shiroha233.roadweaver.map.search.MapStructureSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 地图快照增量合并器。
 */
public final class MapSnapshotAccumulator {
    private final LinkedHashMap<Long, BlockPos> structuresByPos = new LinkedHashMap<>();
    private final LinkedHashMap<Long, StructureInfo> structureInfosByPos = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Integer> structureSourcesByPos = new LinkedHashMap<>();
    private final LinkedHashMap<ConnectionKey, StructureConnection> connectionsByKey = new LinkedHashMap<>();
    private final LinkedHashMap<ConnectionKey, PlannedMapRoute> plannedMapRoutesByKey = new LinkedHashMap<>();
    private final LinkedHashMap<Long, List<BlockPos>> roadsByKey = new LinkedHashMap<>();

    public void merge(MapSnapshot snapshot) {
        if (snapshot == null) return;
        mergeStructures(snapshot);
        mergeConnections(snapshot.connections());
        mergePlannedMapRoutes(snapshot.plannedMapRoutes());
        mergeRoads(snapshot.roadPolylines());
    }

    public MapSnapshot snapshot() {
        ArrayList<BlockPos> structures = new ArrayList<>(structuresByPos.values());
        ArrayList<StructureConnection> connections = new ArrayList<>(connectionsByKey.values());
        ArrayList<PlannedMapRoute> plannedMapRoutes = new ArrayList<>(plannedMapRoutesByKey.values());
        ArrayList<StructureInfo> infos = new ArrayList<>(structureInfosByPos.size());
        java.util.Map<BlockPos, Integer> sources = new java.util.HashMap<>();
        for (Map.Entry<Long, StructureInfo> entry : structureInfosByPos.entrySet()) {
            BlockPos pos = structuresByPos.get(entry.getKey());
            StructureInfo info = entry.getValue();
            if (pos != null && info != null && !info.structureId().isBlank()) {
                infos.add(new StructureInfo(pos, info.structureId(), info.mapIcon()));
            }
        }
        for (Map.Entry<Long, BlockPos> entry : structuresByPos.entrySet()) {
            Integer source = structureSourcesByPos.get(entry.getKey());
            if (source != null) sources.put(entry.getValue(), source);
        }
        ArrayList<List<BlockPos>> roads = new ArrayList<>(roadsByKey.values());
        return new MapSnapshot(structures, connections, infos, roads, sources, plannedMapRoutes);
    }

    private void mergeStructures(MapSnapshot snapshot) {
        for (BlockPos pos : snapshot.structures()) {
            if (pos == null) continue;
            long key = posKey(pos);
            structuresByPos.putIfAbsent(key, normalize(pos));
            structureSourcesByPos.putIfAbsent(key, snapshot.structureSource(pos));
            mergeStructureInfo(key, snapshot.structureInfo(pos));
        }
    }

    private void mergeStructureInfo(long key, StructureInfo next) {
        if (next == null || next.structureId() == null || next.structureId().isBlank()) return;
        StructureInfo previous = structureInfosByPos.get(key);
        if (previous == null || !StructureInfo.isKnownId(previous.structureId())
                || (next.mapIcon() != null && previous.mapIcon() == null)) {
            structureInfosByPos.put(key, new StructureInfo(structuresByPos.get(key), next.structureId(), next.mapIcon()));
        }
    }

    private void mergeConnections(List<StructureConnection> connections) {
        if (connections == null || connections.isEmpty()) return;
        for (StructureConnection connection : connections) {
            if (connection == null || connection.from() == null || connection.to() == null) continue;
            ConnectionKey key = ConnectionKey.of(connection);
            StructureConnection normalized = normalize(connection);
            StructureConnection previous = connectionsByKey.get(key);
            if (previous == null || statusPriority(normalized.status()) >= statusPriority(previous.status())) {
                connectionsByKey.put(key, normalized);
            }
        }
    }

    private void mergePlannedMapRoutes(List<PlannedMapRoute> routes) {
        if (routes == null || routes.isEmpty()) return;
        for (PlannedMapRoute route : routes) {
            if (route == null) continue;
            plannedMapRoutesByKey.put(ConnectionKey.of(route.from(), route.to()), route);
        }
    }

    private void mergeRoads(List<List<BlockPos>> roads) {
        if (roads == null || roads.isEmpty()) return;
        for (List<BlockPos> road : roads) {
            if (road == null || road.size() < 2) continue;
            ArrayList<BlockPos> normalized = new ArrayList<>(road.size());
            for (BlockPos pos : road) {
                if (pos != null) normalized.add(normalize(pos));
            }
            if (normalized.size() < 2) continue;
            roadsByKey.putIfAbsent(roadKey(normalized), List.copyOf(normalized));
        }
    }

    private static BlockPos normalize(BlockPos pos) {
        return new BlockPos(pos.getX(), 0, pos.getZ());
    }

    private static StructureConnection normalize(StructureConnection connection) {
        return new StructureConnection(normalize(connection.from()), normalize(connection.to()), connection.status());
    }

    private static long posKey(BlockPos pos) {
        return (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xffffffffL);
    }

    private static long roadKey(List<BlockPos> road) {
        long hash = 0xcbf29ce484222325L;
        for (BlockPos pos : road) {
            hash ^= pos.getX();
            hash *= 0x100000001b3L;
            hash ^= pos.getZ();
            hash *= 0x100000001b3L;
        }
        hash ^= road.size();
        hash *= 0x100000001b3L;
        return hash;
    }

    private static int statusPriority(ConnectionStatus status) {
        if (status == null) return 0;
        return switch (status) {
            case COMPLETED -> 4;
            case GENERATING -> 3;
            case PLANNED -> 2;
            case FAILED -> 1;
        };
    }

    private record ConnectionKey(long a, long b) {
        private static ConnectionKey of(StructureConnection connection) {
            long from = posKey(connection.from());
            long to = posKey(connection.to());
            return from <= to ? new ConnectionKey(from, to) : new ConnectionKey(to, from);
        }

        private static ConnectionKey of(BlockPos from, BlockPos to) {
            long a = posKey(from);
            long b = posKey(to);
            return a <= b ? new ConnectionKey(a, b) : new ConnectionKey(b, a);
        }
    }
}
