package dev.kirbiton.lonelyocean.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnderwaterCavesTest {
    @Test
    void caveFieldIsDeterministic() {
        for (int x = -96; x <= 96; x += 7) {
            for (int y = -56; y <= 24; y += 5) {
                for (int z = -96; z <= 96; z += 7) {
                    assertEquals(
                            UnderwaterCaves.isCave(x, y, z, 24680L),
                            UnderwaterCaves.isCave(x, y, z, 24680L));
                }
            }
        }
    }

    @Test
    void caveFieldProducesBothSolidAndCarvedRegions() {
        int carved = 0;
        int solid = 0;
        for (int x = -128; x <= 128; x += 4) {
            for (int y = -56; y <= 24; y += 4) {
                for (int z = -128; z <= 128; z += 4) {
                    if (UnderwaterCaves.isCave(x, y, z, 13579L)) {
                        carved++;
                    } else {
                        solid++;
                    }
                }
            }
        }

        assertTrue(carved > 0, "The cave field should produce submerged passages");
        assertTrue(solid > carved, "Most of the seabed should remain solid");
    }

    @Test
    void differentSeedsCanProduceDifferentCaveLayouts() {
        boolean differs = false;
        for (int x = -80; x <= 80 && !differs; x += 8) {
            for (int y = -48; y <= 16 && !differs; y += 8) {
                for (int z = -80; z <= 80; z += 8) {
                    if (UnderwaterCaves.isCave(x, y, z, 1L)
                            != UnderwaterCaves.isCave(x, y, z, 2L)) {
                        differs = true;
                        break;
                    }
                }
            }
        }
        assertTrue(differs);
    }
}
