package dev.kirbiton.lonelyocean;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for Lonely Ocean: Survival.
 * World generation and starter-island placement are not registered yet.
 */
public final class LonelyOcean implements ModInitializer {
    public static final String MOD_ID = "lonely_ocean";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Lonely Ocean: Survival initialized. World generation is not implemented yet.");
    }
}
