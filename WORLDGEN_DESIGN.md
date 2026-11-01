# Minecraft 26.3 world-generation implementation

## Current implementation

The project now registers a custom ChunkGenerator codec and provides a world preset in `data/lonely_ocean/worldgen/world_preset/lonely_ocean.json`.

The Overworld generator currently:
- uses the world seed from RandomState to derive IslandShape;
- creates a low ocean floor between Y=32 and Y=38;
- carves seed-dependent, air-filled 3D cave chambers beneath the seabed while preserving the bedrock floor;
- uses the same cave field in chunk terrain and base-column sampling, keeping the terrain model consistent for height queries;
- fills the ocean with water up to sea level Y=63;
- creates the single seed-positioned island above sea level;
- places a small deterministic group of oak and birch trees;
- reports the island's chunk as the generator origin to guide spawn search;
- registers a Fabric server-level load callback that sets the world's default respawn point to the island centre when the current world spawn is outside the island; this leaves player beds/anchors untouched and avoids moving returning players;
- exposes matching base-column and base-height calculations for heightmaps and structure placement;
- explicitly creates the normal vanilla structure-placement state, preserving the registered vanilla structure sets;
- extends the biome eligibility tags for ancient cities, trial chambers, and strongholds to include all ocean biome variants, so these structures can start below the seabed.

The preset's biome source uses vanilla checkerboard selection over warm, lukewarm, normal, cold, frozen, and deep ocean biomes. Nether and End retain their vanilla generators.

## Important implementation details

A custom generator is needed because the world must have no other natural surface land. A decoration feature alone cannot remove continents or guarantee the starter island exists before spawn selection.

Minecraft 26.3's Mojang-mapped ChunkGenerator contract includes buildTerrain, getBaseHeight, getBaseColumn, getSeaLevel, getMinY, getGenDepth, spawnOriginalMobs, and codec. The current implementation overrides these terrain-related methods and leaves vanilla structure-generation methods inherited from ChunkGenerator.

## Known limitations and required playtests

The current generator is an initial terrain implementation, not yet a fully validated release.

1. Start Minecraft 26.3 with Fabric and confirm the preset appears in the world-type selector.
2. Create new worlds with multiple seeds and confirm there is exactly one surface island and that spawn lands safely on it.
3. Check chunk edges, heightmaps, and biome transitions.
4. Verify ocean monuments, shipwrecks, ocean ruins, and buried treasure with the chosen ocean biomes and inherited vanilla structure pipeline.
5. Verify ancient cities, trial chambers, and strongholds generate beneath the seabed, with their portal rooms, loot, and internal pieces intact.
6. Inspect the generated air-filled cave chambers in-game for useful passage sizes and connectivity, and confirm vanilla monster-room features produce reachable spawner rooms.
7. Verify coral, kelp, and seagrass distribution.
8. Confirm the default world spawn is at the island centre on first world creation and after restart, while bed/anchor respawns and returning player positions remain unchanged.
9. Test on a dedicated server and with common worldgen mods.

The multi-noise biome source replaces square checkerboard regions with irregular climate-driven ocean zones. Tree canopies are currently kept inside their owning chunk, so edge canopies need refinement. Cave geometry is now generated deterministically with larger air-filled chambers intended to provide openings for vanilla dungeon features; their in-game connectivity and spawner frequency still require playtesting. The default world spawn is now explicitly set through the Fabric level-load event, but the first-join spawn flow and respawn behavior still require in-game validation.

Existing worlds are not modified by the world preset; it is intended for newly created worlds only.
