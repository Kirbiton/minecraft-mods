package dev.kirbiton.lonelyocean.world;

/**
 * Deterministic 3D noise field for broad underground cave chambers.
 * The cavities are air-filled so vanilla monster-room features can connect
 * to them and place functional spawners.
 */
public final class UnderwaterCaves {
    private UnderwaterCaves() { }

    public static boolean isCave(int x, int y, int z, long seed) {
        double broad = valueNoise(x, y, z, seed, 48.0, 30.0, 48.0);
        double detail = valueNoise(x + 173, y - 91, z + 317,
                seed ^ 0x6A09E667F3BCC909L, 24.0, 18.0, 24.0);
        return broad > 0.55 && detail > 0.36;
    }

    private static double valueNoise(int x, int y, int z, long seed,
            double sx, double sy, double sz) {
        double px=x/sx, py=y/sy, pz=z/sz;
        int x0=(int)Math.floor(px), y0=(int)Math.floor(py), z0=(int)Math.floor(pz);
        double tx=smooth(px-x0), ty=smooth(py-y0), tz=smooth(pz-z0);
        double x00=lerp(hash01(x0,y0,z0,seed),hash01(x0+1,y0,z0,seed),tx);
        double x10=lerp(hash01(x0,y0+1,z0,seed),hash01(x0+1,y0+1,z0,seed),tx);
        double x01=lerp(hash01(x0,y0,z0+1,seed),hash01(x0+1,y0,z0+1,seed),tx);
        double x11=lerp(hash01(x0,y0+1,z0+1,seed),hash01(x0+1,y0+1,z0+1,seed),tx);
        return lerp(lerp(x00,x10,ty),lerp(x01,x11,ty),tz);
    }

    private static double hash01(int x,int y,int z,long seed) {
        long v=seed ^ ((long)x*0x9E3779B185EBCA87L)
                ^ ((long)y*0xC2B2AE3D27D4EB4FL)
                ^ ((long)z*0x165667B19E3779F9L);
        v^=v>>>30; v*=0xBF58476D1CE4E5B9L;
        v^=v>>>27; v*=0x94D049BB133111EBL; v^=v>>>31;
        return (v>>>11)*0x1.0p-53;
    }
    private static double smooth(double t) { return t*t*(3.0-2.0*t); }
    private static double lerp(double a,double b,double t) { return a+(b-a)*t; }
}
