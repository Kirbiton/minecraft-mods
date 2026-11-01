# Lonely Ocean: Survival

Fabric mod project for Minecraft 26.3 and Java 25.

## Planned gameplay

- A dedicated world preset for an ocean-only survival world.
- Oceanic biomes throughout the world: warm, lukewarm, normal, cold, frozen, and deep variants.
- No naturally generated continents or additional surface islands.
- One small starter island, generated deterministically from the world seed.
- The island's position and outline vary between worlds; target diameter is approximately 20–35 blocks.
- A grassy/dirt island with 2–3 oak or birch trees and a modest sandy shore.
- Player spawn safely placed on the starter island.
- Vanilla ocean structures such as monuments, shipwrecks, and ocean ruins retained.

## Current status

**Development scaffold only.** The repository currently contains the Fabric entry point and automated build workflow. The custom world preset, ocean-only terrain/biome source, starter-island feature, and spawn placement have **not yet been implemented**. A successful build at this stage would only confirm the project scaffold compiles; it would not provide the promised gameplay.

## Build

GitHub Actions builds the project on pushes to the main branch and on manual workflow runs. The resulting JAR is uploaded as a workflow artifact.

Toolchain: Minecraft 26.3, Java 25, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Gradle 9.6, Fabric Loom 1.19-SNAPSHOT.
