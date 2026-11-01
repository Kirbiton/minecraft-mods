package dev.kirbiton.lonelyocean.world;

import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Seeded archipelago field: one irregular island in each jittered 900-block cell. */
final class LargeIslandField {
    private static final int CELL = 900;
    private final long seed;
    private final IslandShape starter;

    private LargeIslandField(long seed) {
        this.seed = seed;
        this.starter = IslandShape.fromSeed(seed);
    }

    static LargeIslandField fromSeed(long seed) { return new LargeIslandField(seed); }

    private Island at(int gx, int gz) {
        long key = mix(seed ^ ((long) gx * 0x9E3779B97F4A7C15L) ^ ((long) gz * 0xC2B2AE3D27D4EB4FL));
        SplittableRandom r = new SplittableRandom(key);
        int cx = gx * CELL + r.nextInt(-100, 101);
        int cz = gz * CELL + r.nextInt(-100, 101);
        // Keep the starter island's immediate horizon clear.
        long dx0 = (long) cx - starter.centerX(), dz0 = (long) cz - starter.centerZ();
        if (dx0 * dx0 + dz0 * dz0 < 500L * 500L) return null;
        return new Island(cx, cz, r.nextDouble(92, 126), r.nextDouble(92, 126),
                r.nextDouble(0, Math.PI * 2), r.nextDouble(0, Math.PI * 2),
                r.nextDouble(0, Math.PI * 2), r.nextDouble(0, Math.PI * 2),
                71 + r.nextInt(0, 17), 24 + r.nextInt(0, 13));
    }

    int surfaceY(int x, int z) {
        Island i = nearestContaining(x, z);
        return i == null ? Integer.MIN_VALUE : i.surface(x, z);
    }
    int bottomY(int x, int z) {
        Island i = nearestContaining(x, z);
        return i == null ? Integer.MIN_VALUE : i.bottom(x, z);
    }
    int biomeAt(int x, int z) { Island i = nearestContaining(x, z); return i == null ? -1 : i.biome(); }
    boolean isBeach(int x, int z) {
        Island i = nearestContaining(x, z);
        return i != null && i.beach(x, z);
    }
    private Island nearestContaining(int x, int z) {
        int gx = Math.floorDiv(x, CELL), gz = Math.floorDiv(z, CELL);
        Island best = null; double bestD = Double.MAX_VALUE;
        for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) {
            Island i = at(gx + ox, gz + oz);
            if (i == null || !i.contains(x, z)) continue;
            double dx = x - i.cx, dz = z - i.cz, d = dx * dx + dz * dz;
            if (d < bestD) { best = i; bestD = d; }
        }
        return best;
    }
    private static final class Island {
        final int cx, cz, top, thickness;
        final double rx, rz, rot, a, b, c;
        final int biome;
        Island(int cx,int cz,double rx,double rz,double rot,double a,double b,double c,int top,int thickness) {
            this.cx=cx; this.cz=cz; this.rx=rx; this.rz=rz; this.rot=rot;
            this.a=a; this.b=b; this.c=c; this.top=top; this.thickness=thickness;
            this.biome=(int)Math.floorMod(mix(((long)cx << 32) ^ cz ^ Double.doubleToLongBits(a)), 5L);
        }
        double radius(int x,int z) {
            double dx=x-cx,dz=z-cz,co=Math.cos(rot),si=Math.sin(rot);
            double u=dx*co-dz*si,v=dx*si+dz*co;
            return Math.sqrt(u*u/(rx*rx)+v*v/(rz*rz));
        }
        double edge(int x,int z) {
            double angle=Math.atan2(z-cz,x-cx)-rot;
            return 1+0.055*Math.sin(angle*3+a)+0.035*Math.sin(angle*5+b)+0.02*Math.sin(angle*9+c);
        }
        boolean contains(int x,int z) { return radius(x,z)<=edge(x,z); }
        boolean beach(int x,int z) {
            double r=radius(x,z), e=edge(x,z);
            if (r > e || r < e * 0.88) return false;
            double angle = Math.atan2(z-cz, x-cx);
            double patches = Math.sin(angle*6+a) + 0.55*Math.sin(angle*11+b) + 0.3*Math.sin(angle*17+c);
            return patches > 0.72;
        }
        int biome() { return biome; }
        int surface(int x,int z) {
            double r=radius(x,z),e=edge(x,z),in=Math.max(0,1-r/e);
            double broad = 5.8*Math.sin(x*0.026+a)*Math.cos(z*0.029+b)
                    + 2.8*Math.sin((x+z)*0.052+c)
                    + 1.4*Math.cos(x*0.091+a)*Math.sin(z*0.083+b);
            double ridge = 2.2*Math.sin(Math.atan2(z-cz,x-cx)*4.0+a) * in;
            return top+(int)Math.round(4.0*in+broad*in+ridge);
        }
        int bottom(int x,int z) {
            double r=radius(x,z),e=edge(x,z),in=Math.max(0,1-r/e);
            return Math.max(44,surface(x,z)-(3+(int)Math.round(thickness*Math.pow(in,0.7))));
        }
    }

    void placeTrees(ChunkAccess chunk, long worldSeed, int minY, int maxY) {
        ChunkPos pos = chunk.getPos();
        int minX = pos.getMinBlockX(), maxX = pos.getMaxBlockX();
        int minZ = pos.getMinBlockZ(), maxZ = pos.getMaxBlockZ();
        int gx0 = Math.floorDiv(minX, CELL), gx1 = Math.floorDiv(maxX, CELL);
        int gz0 = Math.floorDiv(minZ, CELL), gz1 = Math.floorDiv(maxZ, CELL);
        for (int gx = gx0 - 1; gx <= gx1 + 1; gx++) for (int gz = gz0 - 1; gz <= gz1 + 1; gz++) {
            Island island = at(gx, gz);
            if (island == null) continue;
            long key = mix(worldSeed ^ ((long) gx * 0x632BE59BD9B4E019L)
                    ^ ((long) gz * 0x9E3779B97F4A7C15L) ^ 0x54524545534C4F4EL);
            SplittableRandom random = new SplittableRandom(key);
            int count = 9 + random.nextInt(8);
            for (int n = 0; n < count; n++) {
                int tx = island.cx + random.nextInt(-82, 83);
                int tz = island.cz + random.nextInt(-82, 83);
                if (!island.contains(tx, tz) || island.beach(tx, tz)) continue;
                if (tx < minX - 3 || tx > maxX + 3 || tz < minZ - 3 || tz > maxZ + 3) continue;
                int ground = island.surface(tx, tz);
                if (ground + 7 >= maxY || ground < minY) continue;
                if (island.biome == 4) continue; // Mushroom fields stay tree-free.
                int treeType = switch (island.biome) {
                    case 1, 3 -> 2; // Taiga and snowy islands: spruce only.
                    case 2 -> 3;    // Jungle islands: jungle trees only.
                    default -> random.nextInt(4) == 0 ? 1 : 0; // Temperate forest.
                };
                BlockState log = switch (treeType) {
                    case 1 -> Blocks.BIRCH_LOG.defaultBlockState();
                    case 2 -> Blocks.SPRUCE_LOG.defaultBlockState();
                    case 3 -> Blocks.JUNGLE_LOG.defaultBlockState();
                    default -> Blocks.OAK_LOG.defaultBlockState();
                };
                BlockState leaves = switch (treeType) {
                    case 1 -> Blocks.BIRCH_LEAVES.defaultBlockState();
                    case 2 -> Blocks.SPRUCE_LEAVES.defaultBlockState();
                    case 3 -> Blocks.JUNGLE_LEAVES.defaultBlockState();
                    default -> Blocks.OAK_LEAVES.defaultBlockState();
                };
                for (int dy = 1; dy <= 4; dy++) setIfChunk(chunk, pos, tx, ground + dy, tz, log, false);
                for (int dy = 3; dy <= 5; dy++) {
                    int radius = dy == 5 ? 1 : 2;
                    for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) > radius * 2 - 1) continue;
                        setIfChunk(chunk, pos, tx + dx, ground + dy, tz + dz, leaves, true);
                    }
                }
            }
        }
    }
    private static void setIfChunk(ChunkAccess chunk, ChunkPos pos, int x, int y, int z,
            BlockState state, boolean onlyAir) {
        if (x < pos.getMinBlockX() || x > pos.getMaxBlockX()
                || z < pos.getMinBlockZ() || z > pos.getMaxBlockZ()) return;
        BlockPos blockPos = new BlockPos(x, y, z);
        if (!onlyAir || chunk.getBlockState(blockPos).isAir()) chunk.setBlockState(blockPos, state, 0);
    }

    private static long mix(long v) {
        v^=v>>>30; v*=0xbf58476d1ce4e5b9L; v^=v>>>27; v*=0x94d049bb133111ebL; return v^(v>>>31);
    }
}
