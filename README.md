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

**Early development; not a playable world-generation mod yet.**

Implemented:
- Fabric 26.3 project scaffold and automated GitHub Actions build.
- Seed-deterministic starter-island geometry with variable center, rotation, dimensions, and irregular shoreline.
- JUnit tests covering deterministic output, seed variation, approximate island dimensions across 100 seeds, and surface bounds.

Not implemented yet:
- Custom world preset and chunk generator.
- Ocean-only biome source and terrain generation.
- Island block placement, trees, beach materials, and player spawn placement.
- Verification that vanilla ocean structures generate correctly in the custom world.

A successful build confirms compilation and automated geometry tests only. It does **not** mean the planned world can be selected or played yet.

## Build

GitHub Actions builds the project on pushes to the main branch and on manual workflow runs. The resulting JAR is uploaded as a workflow artifact.

Toolchain: Minecraft 26.3, Java 25, Fabric Loader 0.19.5, Fabric API 0.165.0+26.3, Gradle 9.8.2, Fabric Loom 1.19-SNAPSHOT.
