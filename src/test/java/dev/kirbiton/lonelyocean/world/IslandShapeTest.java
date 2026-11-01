package dev.kirbiton.lonelyocean.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IslandShapeTest {
    @Test
    void sameSeedAlwaysProducesSameIsland() {
        IslandShape first = IslandShape.fromSeed(123456789L);
        IslandShape second = IslandShape.fromSeed(123456789L);

        assertEquals(first.centerX(), second.centerX());
        assertEquals(first.centerZ(), second.centerZ());

        for (int x = -40; x <= 40; x++) {
            for (int z = -40; z <= 40; z++) {
                assertEquals(first.contains(first.centerX() + x, first.centerZ() + z),
                        second.contains(second.centerX() + x, second.centerZ() + z));
            }
        }
    }

    @Test
    void differentSeedsCanProduceDifferentLocations() {
        IslandShape first = IslandShape.fromSeed(1L);
        IslandShape second = IslandShape.fromSeed(2L);

        assertTrue(first.centerX() != second.centerX() || first.centerZ() != second.centerZ());
    }

    @Test
    void islandFitsExpectedDiameter() {
        for (long seed = 0; seed < 100; seed++) {
            IslandShape island = IslandShape.fromSeed(seed);
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (int dx = -40; dx <= 40; dx++) {
                for (int dz = -40; dz <= 40; dz++) {
                    if (island.contains(island.centerX() + dx, island.centerZ() + dz)) {
                        minX = Math.min(minX, dx);
                        maxX = Math.max(maxX, dx);
                        minZ = Math.min(minZ, dz);
                        maxZ = Math.max(maxZ, dz);
                    }
                }
            }

            assertTrue(maxX - minX + 1 >= 20, "Island is too narrow for seed " + seed);
            assertTrue(maxX - minX + 1 <= 36, "Island is too wide for seed " + seed);
            assertTrue(maxZ - minZ + 1 >= 20, "Island is too short for seed " + seed);
            assertTrue(maxZ - minZ + 1 <= 36, "Island is too long for seed " + seed);
        }
    }

    @Test
    void centerIsLandAndOutsideHasNoSurface() {
        IslandShape island = IslandShape.fromSeed(987654321L);

        assertTrue(island.contains(island.centerX(), island.centerZ()));
        assertTrue(island.surfaceY(island.centerX(), island.centerZ()) > IslandShape.SEA_LEVEL);
        assertEquals(Integer.MIN_VALUE, island.surfaceY(island.centerX() + 100, island.centerZ() + 100));
    }
}
