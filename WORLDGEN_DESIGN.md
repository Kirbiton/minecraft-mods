# Minecraft 26.3 world-generation implementation

## Current implementation

The project now registers a custom ChunkGenerator codec and provides a world preset in `data/lonely_ocean/worldgen/world_preset/lonely_ocean.json`.

The Overworld generator currently:
- uses the world seed from RandomState to derive IslandShape;
- creates a low ocean floor between Y=32 and Y=38;
- fills the ocean with water up to sea level Y=63;
- creates the single seed-positioned island above sea level;
- places a small deterministic group of oak and birch trees;
- reports the island's chunk as the generator origin to guide spawn search;
- exposes matching base-column and base-height calculations for heightmaps and structure placement.

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
5. Verify coral, kelp, seagrass, and underwater cave distribution.
6. Test on a dedicated server and with common worldgen mods.

The checkerboard biome source is a first pass. A later custom biome source can replace square biome regions with broader seed-dependent ocean climate zones. Tree canopies are currently kept inside their owning chunk, so edge canopies need refinement. Underwater caves and a guaranteed player spawn still require dedicated work.

Existing worlds are not modified by the world preset; it is intended for newly created worlds only.
