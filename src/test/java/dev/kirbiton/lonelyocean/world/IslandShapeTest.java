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
        for (int x=-40;x<=40;x++) for (int z=-40;z<=40;z++)
            assertEquals(first.contains(first.centerX()+x,first.centerZ()+z),
                    second.contains(second.centerX()+x,second.centerZ()+z));
    }

    @Test
    void differentSeedsCanProduceDifferentLocations() {
        IslandShape first=IslandShape.fromSeed(1L), second=IslandShape.fromSeed(2L);
        assertTrue(first.centerX()!=second.centerX() || first.centerZ()!=second.centerZ());
    }

    @Test
    void islandFitsExpectedDiameter() {
        for(long seed=0;seed<100;seed++) {
            IslandShape island=IslandShape.fromSeed(seed);
            int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
            for(int dx=-40;dx<=40;dx++) for(int dz=-40;dz<=40;dz++) {
                if(island.contains(island.centerX()+dx,island.centerZ()+dz)) {
                    minX=Math.min(minX,dx); maxX=Math.max(maxX,dx);
                    minZ=Math.min(minZ,dz); maxZ=Math.max(maxZ,dz);
                }
            }
            assertTrue(maxX-minX+1>=20,"Island too narrow for seed "+seed);
            assertTrue(maxX-minX+1<=36,"Island too wide for seed "+seed);
            assertTrue(maxZ-minZ+1>=20,"Island too short for seed "+seed);
            assertTrue(maxZ-minZ+1<=36,"Island too long for seed "+seed);
        }
    }

    @Test
    void centerIsLandAndOutsideHasNoSurface() {
        IslandShape island=IslandShape.fromSeed(987654321L);
        assertTrue(island.contains(island.centerX(),island.centerZ()));
        assertTrue(island.surfaceY(island.centerX(),island.centerZ())>IslandShape.SEA_LEVEL);
        assertEquals(Integer.MIN_VALUE,island.surfaceY(island.centerX()+100,island.centerZ()+100));
    }

    @Test
    void floatingIslandHasTaperedUnderside() {
        IslandShape island=IslandShape.fromSeed(987654321L);
        int centerSurface=island.surfaceY(island.centerX(),island.centerZ());
        int centerBottom=island.bottomY(island.centerX(),island.centerZ());
        assertTrue(centerSurface-centerBottom>=12);

        int beachSamples=0;
        int thinnestShore= Integer.MAX_VALUE;
        int lowestShoreBottom=Integer.MIN_VALUE;
        for(int dx=-40;dx<=40;dx++) for(int dz=-40;dz<=40;dz++) {
            int x=island.centerX()+dx, z=island.centerZ()+dz;
            if(island.contains(x,z) && island.isBeach(x,z)) {
                int thickness=island.surfaceY(x,z)-island.bottomY(x,z);
                beachSamples++;
                thinnestShore=Math.min(thinnestShore,thickness);
                lowestShoreBottom=Math.max(lowestShoreBottom,island.bottomY(x,z));
            }
        }
        assertTrue(beachSamples>0,"Expected patchy beach samples around the shoreline");
        assertTrue(thinnestShore<=12,"Shore should be thinner than the island center");
        assertTrue(lowestShoreBottom>centerBottom,"Island underside should taper upward toward shore");
    }
}
