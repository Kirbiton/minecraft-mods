# Lonely Ocean: Survival

Fabric mod for Minecraft 26.3 and Java 25.

## Intended gameplay

- A dedicated world preset available in the new-world screen.
- A single small forest island, approximately 20–35 blocks across, at a seed-dependent location.
- Ocean-only surface terrain around the island.
- Warm, lukewarm, normal, cold, and frozen ocean biome families, including deep variants.
- 2–3 oak or birch trees and a sandy shoreline.
- Ocean structures and underwater exploration, plus rare ancient cities, trial chambers, and strongholds beneath the seabed.

## Implemented

- Fabric 26.3 mod entry point and registered custom chunk-generator codec.
- A world preset named **Lonely Ocean: Survival**, included in the normal world-preset selection tag.
- A custom terrain generator that builds ocean water to sea level, a low seabed, and the seed-derived starter island.
- Seed-dependent, flooded 3D cave pockets carved into the stone beneath the ocean floor, with deterministic unit tests.
- Deterministic oak/birch tree placement on the island.
- A Fabric server-level load hook sets the default world spawn to the island centre if the saved spawn is not already on safe island ground; it does not move returning players or override bed/anchor respawns.
- A checkerboard biome source containing ocean-only biomes across warm, lukewarm, normal, cold, frozen, and deep variants.
- The normal vanilla structure-set registry is explicitly retained by the custom generator.
- Ocean biomes are added to the biome eligibility tags for ancient cities, trial chambers, and strongholds, allowing these structures to generate beneath the seabed.
- Automated geometry tests and a GitHub Actions build.

## Not yet verified in the running game

The current code compiles and packages successfully, but a full Minecraft client/server playtest has not yet been performed. In particular, these still need in-game verification or further work:

- The world preset appearing and loading correctly in the Create World screen.
- First-join spawn flow and respawn behavior after restart (the default spawn is now explicitly set to the island centre, but this still needs an in-game playtest).
- Structure placement in the custom generator (especially monuments, shipwrecks, ruins, buried treasure, ancient cities, trial chambers, and strongholds).
- Tree canopy continuity at chunk edges and island appearance across many seeds.
- In-game cave shape, connectivity, and exploration quality; coral distribution and terrain/biome visual transitions.
- Dedicated-server behavior.

The current biome source uses vanilla checkerboard placement, so ocean biome regions are arranged in large square regions rather than smoothly blended climate zones. Existing saves are not rewritten; the preset affects newly created worlds only.

## Build

GitHub Actions builds the project on pushes to the main branch and on manual workflow runs. Toolchain: Minecraft 26.3, Java 25, Fabric Loader 0.19.5, Fabric API 0.165.0+26.3, Gradle 9.8.2, Fabric Loom 1.19-SNAPSHOT.
