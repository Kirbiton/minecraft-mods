package dev.kirbiton.lonelyocean.world;

import java.util.SplittableRandom;

/**
 * Seed-deterministic geometry for the single starter island.
 *
 * This class is deliberately independent of Minecraft classes so the
 * geometry can be tested before it is connected to the 26.3 chunk generator.
 */
public final class IslandShape {
    public static final int SEA_LEVEL = 63;

    private final int centerX;
    private final int centerZ;
    private final double radiusX;
    private final double radiusZ;
    private final double rotation;
    private final double phaseA;
    private final double phaseB;
    private final double phaseC;

    private IslandShape(long seed) {
        SplittableRandom random = new SplittableRandom(mixSeed(seed));
        this.centerX = random.nextInt(-192, 193);
        this.centerZ = random.nextInt(-192, 193);

        // A diameter between roughly 20 and 35 blocks.
        double diameter = random.nextDouble(20.0, 35.1);
        this.radiusX = diameter * random.nextDouble(0.43, 0.57);
        this.radiusZ = diameter - this.radiusX;
        this.rotation = random.nextDouble(0.0, Math.PI * 2.0);
        this.phaseA = random.nextDouble(0.0, Math.PI * 2.0);
        this.phaseB = random.nextDouble(0.0, Math.PI * 2.0);
        this.phaseC = random.nextDouble(0.0, Math.PI * 2.0);
    }

    public static IslandShape fromSeed(long seed) {
        return new IslandShape(seed);
    }

    public int centerX() {
        return centerX;
    }

    public int centerZ() {
        return centerZ;
    }

    public boolean contains(int worldX, int worldZ) {
        return normalizedRadius(worldX, worldZ) <= edgeRadius(angle(worldX, worldZ));
    }

    public boolean isBeach(int worldX, int worldZ) {
        double radius = normalizedRadius(worldX, worldZ);
        double edge = edgeRadius(angle(worldX, worldZ));
        return radius <= edge && radius >= edge * 0.78;
    }

    /**
     * Returns the top block Y for a land position, or Integer.MIN_VALUE
     * when the position lies outside the island.
     */
    public int surfaceY(int worldX, int worldZ) {
        double radius = normalizedRadius(worldX, worldZ);
        double edge = edgeRadius(angle(worldX, worldZ));
        if (radius > edge) {
            return Integer.MIN_VALUE;
        }

        double inland = Math.max(0.0, 1.0 - radius / edge);
        double hill = 1.0 + 3.2 * inland + 0.65 * Math.sin(phaseA + radius * 8.0);
        return SEA_LEVEL + (int) Math.round(hill);
    }

    private double normalizedRadius(int worldX, int worldZ) {
        double dx = worldX - centerX;
        double dz = worldZ - centerZ;
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        double rotatedX = dx * cos - dz * sin;
        double rotatedZ = dx * sin + dz * cos;
        return Math.sqrt(
                (rotatedX * rotatedX) / (radiusX * radiusX)
                + (rotatedZ * rotatedZ) / (radiusZ * radiusZ)
        );
    }

    private double angle(int worldX, int worldZ) {
        return Math.atan2(worldZ - centerZ, worldX - centerX) - rotation;
    }

    private double edgeRadius(double angle) {
        double variation = 1.0
                + 0.075 * Math.sin(angle * 3.0 + phaseA)
                + 0.045 * Math.sin(angle * 5.0 + phaseB)
                + 0.025 * Math.sin(angle * 9.0 + phaseC);
        return variation;
    }

    private static long mixSeed(long seed) {
        seed ^= seed >>> 30;
        seed *= 0xbf58476d1ce4e5b9L;
        seed ^= seed >>> 27;
        seed *= 0x94d049bb133111ebL;
        return seed ^ (seed >>> 31);
    }
}
