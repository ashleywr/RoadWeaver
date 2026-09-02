/* 文件职责：注册 Fabric 服务端生命周期与 Tick 钩子。 */
package net.shiroha233.roadweaver.planning.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaver.api.RoadNetworkApi;
import net.shiroha233.roadweaver.config.structure.StructureDiscoveryService;
import net.shiroha233.roadweaver.core.model.StructureConnection;
import net.shiroha233.roadweaver.features.path.decoration.text.SignTextService;
import net.shiroha233.roadweaver.generation.IdleRoadGenerationService;
import net.shiroha233.roadweaver.generation.InitialGenManager;
import net.shiroha233.roadweaver.generation.RoadGenerationService;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.planning.RoadPlanningService;
import net.shiroha233.roadweaver.runtime.CacheManager;
import net.shiroha233.roadweaver.runtime.ThreadPoolManager;
import net.shiroha233.roadweaver.world.RoadGenerationDimensions;

import java.util.List;

/**
 * Fabric 服务端规划钩子
 */
public final class ServerPlanningHooks {
    private ServerPlanningHooks() {}

    private static int tick;

    /**
     * Starts the initial campaign plan for the first tag-enabled level that is
     * currently live.  Runtime world-preset integrations can add a level only
     * after {@code SERVER_STARTED}; keep this in one place so both the
     * startup pass and the late-level fallback have exactly the same tag gate.
     */
    private static boolean startInitialPlanning(ServerLevel level, boolean dedicated) {
        if (!RoadGenerationDimensions.isEnabled(level) || InitialGenManager.isActive()) return false;
        StructureDiscoveryService.discoverFromLevel(level);
        if (!InitialGenManager.shouldRunInitialGeneration(level)) return false;
        if (dedicated) {
            RoadPlanningService.initialPlanAsync(level);
        } else if (RoadNetworkApi.initialPlanningBounds(level).isPresent()) {
            InitialGenManager.begin(level);
            InitialGenManager.blockUntilDone(level);
        } else {
            InitialGenManager.beginAsync(level);
        }
        return true;
    }

    private static void startInitialPlanningForLoadedLevels(net.minecraft.server.MinecraftServer server) {
        boolean dedicated = server.isDedicatedServer();
        for (ServerLevel level : server.getAllLevels()) {
            if (startInitialPlanning(level, dedicated)) return;
        }
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CacheManager.onServerStarted();
            ThreadPoolManager.onServerStarted(server);
            SignTextService.clearPending();
            IdleRoadGenerationService.onServerStarted();
            RoadGenerationService.onServerStarted();
            // Initial-generation progress is global today, so start one
            // enabled dimension at a time. Normal ticking remains tagged per
            // dimension and persisted data is already dimension-scoped.
            startInitialPlanningForLoadedLevels(server);
            // League Orchestrator mounts its Terra campaign level from a
            // runtime preset.  That level can become visible immediately
            // after this event, so retry on the next server task instead of
            // silently leaving the tagged campaign without its first roads.
            server.execute(() -> startInitialPlanningForLoadedLevels(server));
        });

        ServerWorldEvents.LOAD.register((server, level) ->
                server.execute(() -> startInitialPlanning(level, server.isDedicatedServer())));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if ((tick++ % 20) == 0) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (RoadGenerationDimensions.isEnabled(p.serverLevel())) {
                        SignTextService.onChunkReady(p.serverLevel(), p.chunkPosition());
                        RoadPlanningService.planAroundPlayer(p);
                        IdleRoadGenerationService.tickPlayer(p);
                    }
                }
            }

            for (ServerLevel level : server.getAllLevels()) {
                if (!RoadGenerationDimensions.isEnabled(level)) continue;
                IdleRoadGenerationService.tick(level);
                // Endless-world routes are prioritized around active players.
                // Finite integration worlds complete their initial routes in
                // SERVER_STARTED above, before a player can enter.
                if (!server.getPlayerList().getPlayers().isEmpty()) {
                    RoadGenerationService.tick(level);
                }
                SignTextService.tick(level);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            RoadGenerationService.onServerStopping();
            RoadPlanningService.resetAll();
            ThreadPoolManager.onServerStopping();
            CacheManager.onServerStopping(server.getAllLevels());
            SignTextService.clearPending();
        });

        ServerWorldEvents.UNLOAD.register((server, level) -> CacheManager.onDimensionUnload(level));
    }
}
