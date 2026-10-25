# RoadWeaver
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/6jk8Pote?style=flat-square&logo=Modrinth&label=Modrinth)](https://modrinth.com/mod/roadweaver)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1358489?style=flat-square&logo=CurseForge&label=CurseForge&color=f16436)](https://www.curseforge.com/minecraft/mc-mods/roadweaver)
[![Discord Invite](https://img.shields.io/badge/Discord-Join-5865F2?logo=discord&logoColor=white&style=flat-square)](https://discord.gg/tUJMJkbbr2)
[![Github Stars](https://img.shields.io/github/stars/shiroha-233/RoadWeaver?logo=github&style=flat-square)](https://github.com/shiroha-233/RoadWeaver)

English | [简体中文](README_CN.md)

A Minecraft mod that automatically generates beautiful roads between villages or custom structures.

## Key Features

### 1. Smart Road Generation

- **Intelligent Pathfinding**: Multiple pathfinding algorithms that avoid steep and dangerous areas; adjusts routes based on terrain height, biomes, and ground stability
- **Bezier Curves**: Applies Bezier curve smoothing to polyline paths, creating natural smooth curves and avoiding sharp turns
- **Multiple Road Types**:
  - Artificial roads: (stone bricks, slabs), (dirt, mud bricks), etc., or customize your own in the preset editor
  - Natural roads: Biome-adaptive road materials
- **Obstacle Avoidance**: Cuts, fills terrain and removes trees to ensure road passability
- **Tunnel & Bridge System**: Tunnels through mountains, bridges over water
- **Slab System**: Fills slabs at elevation changes to improve passability
- **Road Foundation**: Smoothly interpolates surrounding height field based on road elevation, fills foundation only when road is above original terrain, creating natural convex slopes that blend seamlessly with the landscape

### 2. Decoration System

- **Lamp System**: Redstone lamps with automatic day/night control
- **Signpost System**: Distance markers and directional signs
- **Roadside Structure Decorations**: Randomly generates benches, campfires and other decorative structures along roads

### 3. Configuration Options

- **Performance Optimization**: Multi-threaded async generation with concurrency control; height and terrain caching to reduce redundant calculations
- **Multiple Network Planning Algorithms**: KNN (sparsest) / Delaunay (densest) / RNG (balanced)
- **Multiple Pathfinding Algorithms**: A* / Bidirectional A* / Fluid Simulation
- **Road Block Customization**: Mix and match in the preset editor

### 4. Visualization Tools

- **Visual Debugging**: Road network map; status colors (planned/generating/completed/failed); interactions (drag, zoom, right-click teleport); statistics for road count, length and status
- **Manual Link Mode**: Plan road networks according to your preferences

## Compatibility

### Road-generation dimensions

RoadWeaver generates only in dimensions selected by the
`#roadweaver:road_generation` level-stem tag. The bundled tag contains
`minecraft:overworld`, preserving the historical default. A datapack can
replace that list to move roads to a campaign dimension instead, for example:

```json
{
  "replace": true,
  "values": ["your_mod:campaign"]
}
```

Place the file at
`data/roadweaver/tags/worldgen/dimension/road_generation.json`. Road storage
is dimension-scoped. Initial planning currently starts one tagged dimension per
server start; choose one campaign dimension for a clean initial pass.

- New versions (2.0.0+) completely abandon the old `/locate` command search mechanism, no longer blocking the game main thread
- Structure prediction, road network planning and pathfinding all run in dedicated thread pools; main thread only handles driving and result application

### Known Compatibility & Performance Issues

- This mod (2.0.6+) is compatible with Tectonic-V2 / Epic Terrain / Terralith, but **incompatible with Tectonic-V3**
- This mod relies on vanilla mechanics, so it's incompatible with mods that overhaul vanilla mechanics like TerraFirmaCraft: The Next Generation

**Incompatibility symptoms:**
- Extremely slow road generation
- Very slow or blank map data loading
- Map shows roads as generated but nothing appears when approaching

**Performance factors:**
- World terrain complexity
- Pathfinding step size and weight configuration
- Concurrent road generation count and thread pool size

## Usage

- **Auto Generation**: Roads automatically generate between structures after entering the world
- **Road Network Map**: Press **H** to open the debug map and view the road network
- **Configuration**: Cloth Config API settings screen (built-in since 2.0.2), accessible from the world creation screen or by pressing **H** in-game and clicking the top-right corner

## Inspiration

Based on [Countered's Settlement Roads](https://modrinth.com/mod/countereds-settlement-roads), map inspired by [RoadArchitect](https://github.com/FranckRJ/RoadArchitect).

## Future Plans

- [ ] More roadside decorations?
- [x] Link multiple structure types
- [ ] Link biomes?
- [ ] More beautiful buildings?
- [ ] Road events?
- [x] Custom linking
- [ ] Main road system?
- [x] Slab transitions
- [x] Bezier curve smoothing

## Notes

- The more structures configured to locate when loading a world, the longer world creation takes, but the more complete the road network
- Roads cannot generate on already-loaded chunks, so don't approach road segments before they finish generating

<!-- ashley-release-information -->

## Ashley's builds

This is an unofficial maintenance fork. Original authors retain ownership and credit. See the [upstream project](https://github.com/shiroha-233/RoadWeaver) for the original mod. Local changes do not imply upstream endorsement.

Licensing: **upstream LICENSE and NOTICE**. Existing license files, copyright notices, attribution and any separate asset terms remain in force. This fork does not relicense upstream work.

Provided **AS IS**, without warranty of any kind. Maintenance, updates and support are best effort; compatibility with every modpack is not guaranteed.

### Downloads and installation

Download a runtime `.jar` from [GitHub Releases](https://github.com/ashleywr/RoadWeaver/releases). Use the Minecraft version and loader named in that release and install its required dependencies. Do not install `-sources`, `-dev`, `-slim`, `-common` or test JARs as the mod.

Close Minecraft, back up your world, and place the matching runtime JAR in the instance's `mods/` directory. Replace older versions of this same mod rather than loading duplicates. For content mods, install matching versions on the server and its clients unless the release explicitly says client-only or server-only.

### Building and reporting problems

Use Java 21 and the checked-in Gradle wrapper:

```powershell
.\gradlew.bat build
```

Build outputs are under `build/libs/` or each loader subproject's `build/libs/`. Some projects require the local development dependencies described above; those dependencies are not included in the download.

For a problem report, include the exact release filename, Minecraft and loader versions, relevant mod versions, `logs/latest.log`, and a crash report if one was written. Remove private information before sharing logs. Remove the JAR to uninstall; worlds containing this mod's blocks, entities or world generation may require restoring a backup.
