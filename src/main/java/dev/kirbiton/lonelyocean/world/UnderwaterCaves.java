package dev.kirbiton.lonelyocean.world;

/**
 * Seeded 3D value-noise field used to carve submerged cave pockets beneath
 * the ocean floor. It is independent of Minecraft classes for unit testing.
 */
public final class UnderwaterCaves {
    private UnderwaterCaves() {
    }

    public static boolean isCave(int x, int y, int z, long seed) {
        double broad = valueNoise(x, y, z, seed, 28.0, 18.0, 28.0);
        double detail = valueNoise(x + 173, y - 91, z + 317, seed ^ 0x6A09E667F3BCC909L,
                13.0, 9.0, 13.0);
        return broad > 0.72 && detail > 0.58;
    }

    private static double valueNoise(int x, int y, int z, long seed,
                                     double scaleX, double scaleY, double scaleZ) {
        double px = x / scaleX;
        double py = y / scaleY;
        double pz = z / scaleZ;

        int x0 = (int) Math.floor(px);
        int y0 = (int) Math.floor(py);
        int z0 = (int) Math.floor(pz);
        double tx = smooth(px - x0);
        double ty = smooth(py - y0);
        double tz = smooth(pz - z0);

        double x00 = lerp(hash01(x0, y0, z0, seed), hash01(x0 + 1, y0, z0, seed), tx);
        double x10 = lerp(hash01(x0, y0 + 1, z0, seed), hash01(x0 + 1, y0 + 1, z0, seed), tx);
        double x01 = lerp(hash01(x0, y0, z0 + 1, seed), hash01(x0 + 1, y0, z0 + 1, seed), tx);
        double x11 = lerp(hash01(x0, y0 + 1, z0 + 1, seed), hash01(x0 + 1, y0 + 1, z0 + 1, seed), tx);

        return lerp(lerp(x00, x10, ty), lerp(x01, x11, ty), tz);
    }

    private static double hash01(int x, int y, int z, long seed) {
        long value = seed
                ^ ((long) x * 0x9E3779B185EBCA87L)
                ^ ((long) y * 0xC2B2AE3D27D4EB4FL)
                ^ ((long) z * 0x165667B19E3779F9L);
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static double lerp(double from, double to, double delta) {
        return from + (to - from) * delta;
    }
}
