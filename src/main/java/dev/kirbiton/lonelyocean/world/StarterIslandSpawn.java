package dev.kirbiton.lonelyocean.world;

import dev.kirbiton.lonelyocean.LonelyOcean;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

/**
 * Keeps the world's default respawn point on the generated starter island.
 * Existing player beds/anchors are not changed.
 */
public final class StarterIslandSpawn {
    private StarterIslandSpawn() {
    }

    public static void register() {
        ServerLevelEvents.LOAD.register((server, level) -> {
            if (level.dimension() != Level.OVERWORLD
                    || !(level.getChunkSource().getGenerator() instanceof LonelyOceanChunkGenerator)) {
                return;
            }

            IslandShape island = IslandShape.fromSeed(level.getSeed());
            LevelData.RespawnData current = level.getRespawnData();
            int currentSurface = island.surfaceY(current.pos().getX(), current.pos().getZ());
            if (current.dimension() == Level.OVERWORLD
                    && currentSurface != Integer.MIN_VALUE
                    && current.pos().getY() == currentSurface + 1) {
                return;
            }

            int spawnX = island.centerX();
            int spawnZ = island.centerZ();
            int spawnY = island.surfaceY(spawnX, spawnZ) + 1;
            BlockPos spawn = new BlockPos(spawnX, spawnY, spawnZ);
            level.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, spawn, 0.0F, 0.0F));

            LonelyOcean.LOGGER.info("Set Lonely Ocean world spawn to starter island at {}", spawn);
        });
    }
}
