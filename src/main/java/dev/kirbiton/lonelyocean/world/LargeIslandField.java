package dev.kirbiton.lonelyocean.world;

import java.util.SplittableRandom;

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
        Island(int cx,int cz,double rx,double rz,double rot,double a,double b,double c,int top,int thickness) {
            this.cx=cx; this.cz=cz; this.rx=rx; this.rz=rz; this.rot=rot;
            this.a=a; this.b=b; this.c=c; this.top=top; this.thickness=thickness;
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
        boolean beach(int x,int z) { double r=radius(x,z),e=edge(x,z); return r<=e && r>=e*0.91; }
        int surface(int x,int z) {
            double r=radius(x,z),e=edge(x,z),in=Math.max(0,1-r/e);
            return top+(int)Math.round(2.5*in+0.7*Math.sin(a+r*7));
        }
        int bottom(int x,int z) {
            double r=radius(x,z),e=edge(x,z),in=Math.max(0,1-r/e);
            return Math.max(44,surface(x,z)-(3+(int)Math.round(thickness*Math.pow(in,0.7))));
        }
    }
    private static long mix(long v) {
        v^=v>>>30; v*=0xbf58476d1ce4e5b9L; v^=v>>>27; v*=0x94d049bb133111ebL; return v^(v>>>31);
    }
}
