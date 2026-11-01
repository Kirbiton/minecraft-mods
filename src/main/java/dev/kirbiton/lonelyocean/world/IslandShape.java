package dev.kirbiton.lonelyocean.world;

import java.util.SplittableRandom;

/** Seed-deterministic geometry for a small, naturally irregular floating island. */
public final class IslandShape {
    public static final int SEA_LEVEL = 63;
    private final int centerX, centerZ;
    private final double radiusX, radiusZ, rotation, phaseA, phaseB, phaseC;

    private IslandShape(long seed) {
        SplittableRandom random = new SplittableRandom(mixSeed(seed));
        centerX = random.nextInt(-192, 193);
        centerZ = random.nextInt(-192, 193);
        radiusX = random.nextDouble(10.0, 15.3);
        radiusZ = random.nextDouble(10.0, 15.3);
        rotation = random.nextDouble(0.0, Math.PI * 2.0);
        phaseA = random.nextDouble(0.0, Math.PI * 2.0);
        phaseB = random.nextDouble(0.0, Math.PI * 2.0);
        phaseC = random.nextDouble(0.0, Math.PI * 2.0);
    }
    public static IslandShape fromSeed(long seed) { return new IslandShape(seed); }
    public int centerX() { return centerX; }
    public int centerZ() { return centerZ; }
    public boolean contains(int x, int z) { return normalizedRadius(x, z) <= edgeRadius(angle(x, z)); }
    public boolean isBeach(int x, int z) {
        double r = normalizedRadius(x, z), edge = edgeRadius(angle(x, z));
        if (r > edge || r < edge * 0.84) return false;
        double a = angle(x, z);
        double patches = Math.sin(a * 6.0 + phaseA) + 0.55 * Math.sin(a * 11.0 + phaseB)
                + 0.3 * Math.sin(a * 17.0 + phaseC);
        return patches > 0.62;
    }
    public int surfaceY(int x, int z) {
        double r = normalizedRadius(x, z), edge = edgeRadius(angle(x, z));
        if (r > edge) return Integer.MIN_VALUE;
        double inland = Math.max(0.0, 1.0 - r / edge);
        double dx = x - centerX, dz = z - centerZ;
        double broad = 2.8 * Math.sin(x * 0.11 + phaseA) * Math.cos(z * 0.13 + phaseB)
                + 1.4 * Math.sin((x + z) * 0.21 + phaseC);
        double hill = 1.0 + 4.0 * inland + broad * inland
                + 1.1 * Math.sin(phaseA + r * 8.0);
        return SEA_LEVEL + (int) Math.round(hill);
    }

    /** Bottom tapers to a rounded point: thin at the shore, thickest beneath the center. */
    public int bottomY(int x, int z) {
        int surface = surfaceY(x, z);
        if (surface == Integer.MIN_VALUE) return Integer.MIN_VALUE;
        double r = normalizedRadius(x, z), edge = edgeRadius(angle(x, z));
        double inland = Math.max(0.0, 1.0 - r / edge);
        int thickness = 3 + (int) Math.round(14.0 * Math.pow(inland, 0.72));
        return surface - thickness;
    }

    private double normalizedRadius(int x, int z) {
        double dx = x - centerX, dz = z - centerZ;
        double cos = Math.cos(rotation), sin = Math.sin(rotation);
        double rx = dx * cos - dz * sin, rz = dx * sin + dz * cos;
        return Math.sqrt((rx * rx) / (radiusX * radiusX) + (rz * rz) / (radiusZ * radiusZ));
    }
    private double angle(int x, int z) { return Math.atan2(z - centerZ, x - centerX) - rotation; }
    private double edgeRadius(double a) {
        return 1.0 + 0.075 * Math.sin(a * 3.0 + phaseA)
                + 0.045 * Math.sin(a * 5.0 + phaseB) + 0.025 * Math.sin(a * 9.0 + phaseC);
    }
    private static long mixSeed(long seed) {
        seed ^= seed >>> 30; seed *= 0xbf58476d1ce4e5b9L;
        seed ^= seed >>> 27; seed *= 0x94d049bb133111ebL;
        return seed ^ (seed >>> 31);
    }
}
