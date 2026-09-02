package net.shiroha233.roadweaver.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.shiroha233.roadweaver.core.model.ConnectionStatus;
import net.shiroha233.roadweaver.core.model.StructureConnection;
import net.shiroha233.roadweaver.core.model.StructureInfo;
import net.shiroha233.roadweaver.core.model.StructureLocationData;
import net.shiroha233.roadweaver.generation.RoadGenerationService;
import net.shiroha233.roadweaver.map.MapPatchService;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.persistence.files.StructureFileStorage;
import net.shiroha233.roadweaver.planning.RoadPlanningService;
import net.shiroha233.roadweaver.world.RoadGenerationDimensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RoadNetworkApi {
    private static final CopyOnWriteArrayList<InitialPlanningListener> INITIAL_PLANNING_LISTENERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<InitialPlanningBoundsProvider> INITIAL_PLANNING_BOUNDS_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<InitialPlanningCandidateProvider> INITIAL_PLANNING_CANDIDATE_PROVIDERS = new CopyOnWriteArrayList<>();
    /** Per-dimension opt-in; never enabled by default or by a bounds provider alone. */
    private static final Set<ResourceKey<Level>> EXCLUSIVE_INITIAL_PLANNING_DIMENSIONS = ConcurrentHashMap.newKeySet();

    private RoadNetworkApi() {
    }

    /** Registers a pure candidate enumerator for an addon-defined placement type. */
    public static void registerStructurePlacementAdapter(StructurePlacementAdapter adapter) {
        StructurePlacementAdapters.register(adapter);
    }

    /** Removes a previously registered placement enumerator. */
    public static void unregisterStructurePlacementAdapter(StructurePlacementAdapter adapter) {
        StructurePlacementAdapters.unregister(adapter);
    }

    /**
     * Registers an observer of RoadWeaver's initial regional candidate pass.
     * The observer is called once per fresh overworld startup pass, on the
     * server thread, before RoadWeaver starts generating its initial roads.
     */
    public static void registerInitialPlanningListener(InitialPlanningListener listener) {
        if (listener != null) INITIAL_PLANNING_LISTENERS.addIfAbsent(listener);
    }

    public static void unregisterInitialPlanningListener(InitialPlanningListener listener) {
        INITIAL_PLANNING_LISTENERS.remove(listener);
    }

    /** Registers a bounded initial-plan domain supplied by a world-layout integration. */
    public static void registerInitialPlanningBoundsProvider(InitialPlanningBoundsProvider provider) {
        if (provider != null) INITIAL_PLANNING_BOUNDS_PROVIDERS.addIfAbsent(provider);
    }

    public static void unregisterInitialPlanningBoundsProvider(InitialPlanningBoundsProvider provider) {
        INITIAL_PLANNING_BOUNDS_PROVIDERS.remove(provider);
    }

    /** Registers pure addon candidates to seed RoadWeaver's bounded first pass. */
    public static void registerInitialPlanningCandidateProvider(InitialPlanningCandidateProvider provider) {
        if (provider != null) INITIAL_PLANNING_CANDIDATE_PROVIDERS.addIfAbsent(provider);
    }

    public static void unregisterInitialPlanningCandidateProvider(InitialPlanningCandidateProvider provider) {
        INITIAL_PLANNING_CANDIDATE_PROVIDERS.remove(provider);
    }

    /** Returns the first valid optional domain without allowing an addon failure to stop startup. */
    public static Optional<InitialPlanningBounds> initialPlanningBounds(ServerLevel level) {
        for (InitialPlanningBoundsProvider provider : INITIAL_PLANNING_BOUNDS_PROVIDERS) {
            try {
                Optional<InitialPlanningBounds> bounds = provider.initialBounds(level);
                if (bounds != null && bounds.isPresent()) return bounds;
            } catch (RuntimeException ignored) {
                // A provider is advisory; use the normal configured radius on failure.
            }
        }
        return Optional.empty();
    }

    /** Collects optional pure candidates without allowing an addon failure to stop startup. */
    public static List<StructureInfo> initialPlanningCandidates(ServerLevel level, InitialPlanningBounds bounds) {
        ArrayList<StructureInfo> candidates = new ArrayList<>();
        if (level == null || bounds == null) return candidates;
        for (InitialPlanningCandidateProvider provider : INITIAL_PLANNING_CANDIDATE_PROVIDERS) {
            try {
                List<StructureInfo> supplied = provider.initialCandidates(level, bounds);
                if (supplied != null) candidates.addAll(supplied);
            } catch (RuntimeException ignored) {
                // Optional integrations must never cancel RoadWeaver's plan.
            }
        }
        return candidates;
    }

    /** True when an integration deliberately owns the initial regional roster. */
    public static boolean hasExclusiveInitialPlanningCandidates(ServerLevel level, InitialPlanningBounds bounds) {
        if (level == null || bounds == null) return false;
        for (InitialPlanningCandidateProvider provider : INITIAL_PLANNING_CANDIDATE_PROVIDERS) {
            try {
                if (provider.exclusive(level, bounds)) return true;
            } catch (RuntimeException ignored) {
                // Optional integrations must never cancel RoadWeaver's plan.
            }
        }
        return false;
    }

    /** Records the selected first-plan policy so dynamic regional expansion cannot change its endpoint universe. */
    public static void setInitialPlanningExclusive(ServerLevel level, boolean exclusive) {
        if (level == null) return;
        if (exclusive) EXCLUSIVE_INITIAL_PLANNING_DIMENSIONS.add(level.dimension());
        else EXCLUSIVE_INITIAL_PLANNING_DIMENSIONS.remove(level.dimension());
    }

    public static boolean isInitialPlanningExclusive(ServerLevel level) {
        return level != null && EXCLUSIVE_INITIAL_PLANNING_DIMENSIONS.contains(level.dimension());
    }

    /** Internal lifecycle hook used by RoadWeaver's initial-generation manager. */
    public static void notifyInitialStructuresPlanned(ServerLevel level, List<StructureInfo> structures) {
        List<StructureInfo> snapshot = structures == null ? List.of() : List.copyOf(structures);
        for (InitialPlanningListener listener : INITIAL_PLANNING_LISTENERS) {
            try {
                listener.onInitialStructuresPlanned(level, snapshot);
            } catch (RuntimeException ignored) {
                // Optional integrations must never cancel RoadWeaver's plan.
            }
        }
    }

    public static void registerStructureEndpoint(ServerLevel level, BlockPos pos) {
        registerStructureEndpoint(level, pos, null, false);
    }

    public static void registerStructureEndpoint(ServerLevel level, BlockPos pos, boolean autoConnect) {
        registerStructureEndpoint(level, pos, null, autoConnect);
    }

    public static void registerStructureEndpoint(ServerLevel level, BlockPos pos, String structureId,
            boolean autoConnect) {
        if (level == null || pos == null || !Level.OVERWORLD.equals(level.dimension()))
            return;
        WorldDataProvider provider = WorldDataProvider.getInstance();
        StructureLocationData existing = provider.getStructureLocations(level);
        List<BlockPos> locations = existing != null ? new ArrayList<>(existing.structureLocations())
                : new ArrayList<>();
        List<StructureInfo> infos = existing != null ? new ArrayList<>(existing.structureInfos())
                : new ArrayList<>();

        if (structureId != null && !structureId.isEmpty()) {
            StructureInfo info = new StructureInfo(pos, structureId);
            infos.add(info);
            if (!locations.contains(pos)) {
                locations.add(pos);
            }
        } else {
            if (!locations.contains(pos)) {
                locations.add(pos);
            }
        }

        StructureLocationData updated = new StructureLocationData(locations, infos);
        provider.setStructureLocations(level, updated);

        String id = (structureId != null && !structureId.isEmpty()) ? structureId : "unknown";
        StructureFileStorage.addStructures(
                level,
                java.util.List.of(new StructureInfo(new BlockPos(pos.getX(), 0, pos.getZ()), id)),
                StructureFileStorage.SOURCE_MANUAL
        );
    }

    public static void ensureConnection(ServerLevel level, BlockPos from, BlockPos to) {
        ensureConnection(level, from, to, false);
    }

    public static void ensureConnection(ServerLevel level, BlockPos from, BlockPos to, boolean generateImmediately) {
        if (!RoadGenerationDimensions.isEnabled(level) || from == null || to == null)
            return;
        if (from.equals(to))
            return;

        WorldDataProvider provider = WorldDataProvider.getInstance();
        StructureLocationData existing = provider.getStructureLocations(level);
        List<BlockPos> locations = existing != null ? new ArrayList<>(existing.structureLocations())
                : new ArrayList<>();
        List<StructureInfo> infos = existing != null ? new ArrayList<>(existing.structureInfos())
                : new ArrayList<>();

        boolean changed = false;
        if (!locations.contains(from)) {
            locations.add(from);
            changed = true;
        }
        if (!locations.contains(to)) {
            locations.add(to);
            changed = true;
        }
        if (changed) {
            StructureLocationData updated = new StructureLocationData(locations, infos);
            provider.setStructureLocations(level, updated);
        }

        StructureFileStorage.addStructures(
                level,
                java.util.List.of(
                        new StructureInfo(new BlockPos(from.getX(), 0, from.getZ()), "unknown"),
                        new StructureInfo(new BlockPos(to.getX(), 0, to.getZ()), "unknown")
                ),
                StructureFileStorage.SOURCE_MANUAL
        );

        List<StructureConnection> existingConns = provider.getStructureConnections(level);
        List<StructureConnection> list = existingConns != null ? new ArrayList<>(existingConns)
                : new ArrayList<>();
        boolean exists = false;
        for (StructureConnection c : list) {
            if (sameEdge(c, from, to)) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            StructureConnection connection = new StructureConnection(from, to, ConnectionStatus.PLANNED);
            list.add(connection);
            provider.setStructureConnections(level, list);
            MapPatchService.publishConnection(level, connection);
        }

        if (generateImmediately) {
            StructureConnection conn = new StructureConnection(from, to,
                    ConnectionStatus.GENERATING);
            List<StructureConnection> currentList = provider.getStructureConnections(level);
            List<StructureConnection> all = currentList != null ? new ArrayList<>(currentList)
                    : new ArrayList<>();
            boolean found = false;
            for (int i = 0; i < all.size(); i++) {
                if (sameEdge(all.get(i), from, to)) {
                    all.set(i, conn);
                    found = true;
                    break;
                }
            }
            if (!found)
                all.add(conn);
            provider.setStructureConnections(level, all);
            boolean success = RoadGenerationService.generateTask(level, conn);
            ConnectionStatus finalStatus = success ? ConnectionStatus.COMPLETED
                    : ConnectionStatus.FAILED;
            StructureConnection finalConn = new StructureConnection(from, to, finalStatus);

            currentList = provider.getStructureConnections(level);
            all = currentList != null ? new ArrayList<>(currentList) : new ArrayList<>();
            found = false;
            for (int i = 0; i < all.size(); i++) {
                if (sameEdge(all.get(i), from, to)) {
                    all.set(i, finalConn);
                    found = true;
                    break;
                }
            }
            if (!found)
                all.add(finalConn);
            provider.setStructureConnections(level, all);
        }
    }

    public static void planRegion(ServerLevel level, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        if (!RoadGenerationDimensions.isEnabled(level))
            return;
        RoadPlanningService.planRectAsync(level, minBlockX, minBlockZ, maxBlockX, maxBlockZ);
    }

    private static boolean sameEdge(StructureConnection c, BlockPos a, BlockPos b) {
        BlockPos af = c.from();
        BlockPos at = c.to();
        return (af.equals(a) && at.equals(b)) || (af.equals(b) && at.equals(a));
    }
}
