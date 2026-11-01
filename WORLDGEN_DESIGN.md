# Minecraft 26.3 world-generation implementation notes

This note records the API direction for the custom world. It is intentionally not a claim that world generation is implemented.

## Why a custom generator is needed

A normal noise generator can create ocean-heavy terrain, but the core requirement is stricter: the surface world must contain exactly one small, seed-positioned island and no other naturally generated land. A deterministic island mask is therefore required at the terrain stage, not as a rare decoration feature. A decoration feature cannot reliably remove continents or guarantee the island exists before spawn selection and structure placement.

## 26.3 API facts checked

The 26.3 Mojang-mapped ChunkGenerator contract includes:
- buildTerrain(...) returning CompletableFuture<ChunkAccess>;
- getBaseHeight(...) and getBaseColumn(...), which must agree with generated terrain;
- getSeaLevel(), getMinY(), and getGenDepth();
- spawnOriginalMobs(...);
- a MapCodec returned by codec().

The generator base class still exposes structure creation/reference methods. The custom implementation must preserve the vanilla structure pipeline and supply a valid structure state rather than simply placing water and island blocks.

A custom BiomeSource must provide:
- a MapCodec through codec();
- all possible biome holders through collectPossibleBiomes();
- a resolver for biome sampling.

Minecraft 26.3 changed several world-generation registries and contracts. In particular, the old configured-feature pattern is replaced by the Feature/FEATURE_TYPE model, and material rules replace surface rules. Do not copy pre-26.3 registration examples directly.

## Planned implementation sequence

1. Resolve biome holders from the worldgen registry access and build an ocean-only biome source that chooses among warm, lukewarm, normal, cold, and frozen ocean families, including deep variants.
2. Implement the terrain generator using the existing seed-derived IslandShape. Every chunk column outside the island mask becomes ocean floor below sea level; inside the mask it becomes the small island profile. Generate a stable seabed and water column.
3. Make getBaseHeight and getBaseColumn return the same terrain model as buildTerrain; structure placement and spawn searches depend on these methods.
4. Preserve vanilla structure sets and decoration stages, then test monuments, shipwrecks, ruins, buried treasure, coral, and underwater caves.
5. Add the world preset and ensure only new worlds use it. Existing save files must remain unchanged.
6. Add automated tests for seed repeatability, chunk-boundary continuity, ocean-only surface bounds, and spawn safety; then test the actual game.

## Important limitations

The geometry helper currently chooses an island center in a bounded area around world origin. It does not yet receive the real world seed from Minecraft's generator lifecycle, write blocks into chunks, register a world preset, or set the player's spawn. Those are separate integration tasks.