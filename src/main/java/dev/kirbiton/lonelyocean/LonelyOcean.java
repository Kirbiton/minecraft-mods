package dev.kirbiton.lonelyocean;

import dev.kirbiton.lonelyocean.world.LonelyOceanChunkGenerator;
import dev.kirbiton.lonelyocean.world.StarterIslandSpawn;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for Lonely Ocean: Survival.
 */
public final class LonelyOcean implements ModInitializer {
    public static final String MOD_ID = "lonely_ocean";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LonelyOceanChunkGenerator.register();
        StarterIslandSpawn.register();
        LOGGER.info("Lonely Ocean: Survival registered its ocean-island chunk generator and starter-island spawn.");
    }
}
