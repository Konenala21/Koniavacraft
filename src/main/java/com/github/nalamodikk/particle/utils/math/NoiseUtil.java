package com.github.nalamodikk.particle.utils.math;

/**
 * 噪聲工具類
 * 提供 Perlin 噪聲和 Simplex 噪聲生成
 */
public class NoiseUtil {

    private static final int[] PERMUTATION = new int[512];
    
    static {
        // 初始化置換表
        int[] p = {151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,140,36,
                   103,30,69,142,8,99,37,240,21,10,23,190,6,148,247,120,234,75,0,
                   26,197,62,94,252,219,203,117,35,11,32,57,177,33,88,237,149,56,
                   87,174,20,125,136,171,168,68,175,74,165,71,134,139,48,27,166,
                   77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,
                   46,245,40,244,102,143,54,65,25,63,161,1,216,80,73,209,76,132,
                   187,208,89,18,169,200,196,135,130,116,188,159,86,164,100,109,
                   198,173,186,3,64,52,217,226,250,124,123,5,202,38,147,118,126,
                   255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,223,183,
                   170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,43,
                   172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,
                   104,218,246,97,228,251,34,242,193,238,210,144,12,191,179,162,
                   241,81,51,145,235,249,14,239,107,49,192,214,31,181,199,106,
                   157,184,84,204,176,115,121,50,45,127,4,150,254,138,236,205,
                   93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180};
        
        for (int i = 0; i < 256; i++) {
            PERMUTATION[i] = p[i];
            PERMUTATION[256 + i] = p[i];
        }
    }

    /**
     * Perlin 噪聲（1D）
     * @param x X 坐標
     * @return 噪聲值（-1 到 1）
     */
    public static double perlin1D(double x) {
        int X = (int) Math.floor(x) & 255;
        x -= Math.floor(x);
        double u = fade(x);
        
        return lerp(u, grad1D(PERMUTATION[X], x),
                      grad1D(PERMUTATION[X + 1], x - 1));
    }

    /**
     * Perlin 噪聲（2D）
     * @param x X 坐標
     * @param y Y 坐標
     * @return 噪聲值（-1 到 1）
     */
    public static double perlin2D(double x, double y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        
        x -= Math.floor(x);
        y -= Math.floor(y);
        
        double u = fade(x);
        double v = fade(y);
        
        int A = PERMUTATION[X] + Y;
        int B = PERMUTATION[X + 1] + Y;
        
        return lerp(v, lerp(u, grad2D(PERMUTATION[A], x, y),
                              grad2D(PERMUTATION[B], x - 1, y)),
                      lerp(u, grad2D(PERMUTATION[A + 1], x, y - 1),
                              grad2D(PERMUTATION[B + 1], x - 1, y - 1)));
    }

    /**
     * Perlin 噪聲（3D）
     * @param x X 坐標
     * @param y Y 坐標
     * @param z Z 坐標
     * @return 噪聲值（-1 到 1）
     */
    public static double perlin3D(double x, double y, double z) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;
        
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);
        
        double u = fade(x);
        double v = fade(y);
        double w = fade(z);
        
        int A = PERMUTATION[X] + Y;
        int AA = PERMUTATION[A] + Z;
        int AB = PERMUTATION[A + 1] + Z;
        int B = PERMUTATION[X + 1] + Y;
        int BA = PERMUTATION[B] + Z;
        int BB = PERMUTATION[B + 1] + Z;
        
        return lerp(w, lerp(v, lerp(u, grad3D(PERMUTATION[AA], x, y, z),
                                      grad3D(PERMUTATION[BA], x - 1, y, z)),
                              lerp(u, grad3D(PERMUTATION[AB], x, y - 1, z),
                                      grad3D(PERMUTATION[BB], x - 1, y - 1, z))),
                      lerp(v, lerp(u, grad3D(PERMUTATION[AA + 1], x, y, z - 1),
                                      grad3D(PERMUTATION[BA + 1], x - 1, y, z - 1)),
                              lerp(u, grad3D(PERMUTATION[AB + 1], x, y - 1, z - 1),
                                      grad3D(PERMUTATION[BB + 1], x - 1, y - 1, z - 1))));
    }

    /**
     * 分形噪聲（多個八度疊加）
     * @param x X 坐標
     * @param y Y 坐標
     * @param z Z 坐標
     * @param octaves 八度數
     * @param persistence 持續度
     * @return 噪聲值
     */
    public static double fractalNoise(double x, double y, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;
        
        for (int i = 0; i < octaves; i++) {
            total += perlin3D(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        
        return total / maxValue;
    }

    /**
     * 湍流噪聲（噪聲的絕對值）
     * @param x X 坐標
     * @param y Y 坐標
     * @param z Z 坐標
     * @param size 大小
     * @return 噪聲值
     */
    public static double turbulence(double x, double y, double z, double size) {
        double value = 0;
        double initialSize = size;
        
        while (size >= 1) {
            value += Math.abs(perlin3D(x / size, y / size, z / size)) * size;
            size /= 2.0;
        }
        
        return value / initialSize;
    }

    // 輔助函數
    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad1D(int hash, double x) {
        return (hash & 1) == 0 ? x : -x;
    }

    private static double grad2D(int hash, double x, double y) {
        int h = hash & 3;
        return ((h & 1) == 0 ? x : -x) + ((h & 2) == 0 ? y : -y);
    }

    private static double grad3D(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
