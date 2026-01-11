package com.github.nalamodikk.particle.utils;

import com.github.nalamodikk.particle.utils.builder.PointsBuilder;
import com.github.nalamodikk.particle.utils.math.RelativeLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 數學預設圖形 (羅馬數字生成器)
 */
public class MathPresets {

    public static List<RelativeLocation> romaI(double scale) {
        if (scale < 0.01) throw new IllegalArgumentException("Scale must be at least 0.01");
        
        PointsBuilder builder = new PointsBuilder();
        int preLineCount = Math.max(1, (int)(5 * scale));

        double height = 0.25 * scale;
        double weight = 0.125 * scale;
        
        builder.addLine(
            new RelativeLocation(-weight, height, 0.0),
            new RelativeLocation(weight, height, 0.0),
            preLineCount
        ).addLine(
            new RelativeLocation(-weight, -height, 0.0),
            new RelativeLocation(weight, -height, 0.0),
            preLineCount
        ).addLine(
            new RelativeLocation(0.0, height, 0.0),
            new RelativeLocation(0.0, -height, 0.0),
            preLineCount
        );
        return builder.create();
    }

    public static List<RelativeLocation> romaV(double scale) {
        if (scale < 0.01) throw new IllegalArgumentException("Scale must be at least 0.01");
        
        PointsBuilder builder = new PointsBuilder();
        int preLineCount = Math.max(1, (int)(5 * scale));

        double height = 0.25 * scale;
        double weight = 0.125 * scale;
        
        builder.addLine(
            new RelativeLocation(-weight, height, 0.0),
            new RelativeLocation(0.0, -height, 0.0),
            preLineCount
        ).addLine(
            new RelativeLocation(weight, height, 0.0),
            new RelativeLocation(0.0, -height, 0.0),
            preLineCount
        );
        return builder.create();
    }

    public static List<RelativeLocation> romaX(double scale) {
        if (scale < 0.01) throw new IllegalArgumentException("Scale must be at least 0.01");
        
        PointsBuilder builder = new PointsBuilder();
        int preLineCount = Math.max(1, (int)(5 * scale));

        double height = 0.25 * scale;
        double weight = 0.125 * scale;
        
        builder.addLine(
            new RelativeLocation(-weight, height, 0.0),
            new RelativeLocation(weight, -height, 0.0),
            preLineCount
        ).addLine(
            new RelativeLocation(-weight, -height, 0.0),
            new RelativeLocation(weight, height, 0.0),
            preLineCount
        );
        return builder.create();
    }

    private static double getRomaOffsetX(double scale) {
        return 0.125 * scale * 2;
    }

    private static void shiftX(List<RelativeLocation> points, double offset) {
        for (RelativeLocation p : points) {
            p.x += offset;
        }
    }

    // 組合邏輯
    public static List<RelativeLocation> romaII(double scale) {
        List<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2;
        
        List<RelativeLocation> p1 = romaI(scale);
        shiftX(p1, -offset);
        res.addAll(p1);
        
        List<RelativeLocation> p2 = romaI(scale);
        shiftX(p2, offset);
        res.addAll(p2);
        
        return res;
    }

    public static List<RelativeLocation> romaIII(double scale) {
        List<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale);
        
        res.addAll(romaI(scale));
        
        List<RelativeLocation> p2 = romaI(scale);
        shiftX(p2, -offset);
        res.addAll(p2);
        
        List<RelativeLocation> p3 = romaI(scale);
        shiftX(p3, offset);
        res.addAll(p3);
        
        return res;
    }

    public static List<RelativeLocation> romaIV(double scale) {
        List<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2;
        
        List<RelativeLocation> v = romaV(scale);
        shiftX(v, offset);
        res.addAll(v);
        
        List<RelativeLocation> i = romaI(scale);
        shiftX(i, -offset);
        res.addAll(i);
        
        return res;
    }

    public static List<RelativeLocation> romaVI(double scale) {
        List<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2;
        
        List<RelativeLocation> v = romaV(scale);
        shiftX(v, -offset);
        res.addAll(v);
        
        List<RelativeLocation> i = romaI(scale);
        shiftX(i, offset);
        res.addAll(i);
        
        return res;
    }

    public static List<RelativeLocation> romaVII(double scale) {
        List<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale);
        
        List<RelativeLocation> v = romaV(scale);
        shiftX(v, -offset);
        res.addAll(v);
        
        res.addAll(romaI(scale));
        
        List<RelativeLocation> i = romaI(scale);
        shiftX(i, offset);
        res.addAll(i);
        
        return res;
    }

    public static List<RelativeLocation> romaVIII(double scale) {
        List<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2;
        
        List<RelativeLocation> vii = romaVII(scale);
        shiftX(vii, -offset);
        res.addAll(vii);
        
        List<RelativeLocation> i = romaI(scale);
        shiftX(i, offset * 3);
        res.addAll(i);
        
        return res;
    }

    public static List<RelativeLocation> romaIX(double scale) {
        List<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2;
        
        List<RelativeLocation> x = romaX(scale);
        shiftX(x, offset);
        res.addAll(x);
        
        List<RelativeLocation> i = romaI(scale);
        shiftX(i, -offset);
        res.addAll(i);
        
        return res;
    }

    public static List<RelativeLocation> romaXI(double scale) {
        List<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2;
        
        List<RelativeLocation> x = romaX(scale);
        shiftX(x, -offset);
        res.addAll(x);
        
        List<RelativeLocation> i = romaI(scale);
        shiftX(i, offset);
        res.addAll(i);
        
        return res;
    }

    public static List<RelativeLocation> romaXII(double scale) {
        List<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2;
        
        List<RelativeLocation> xi = romaXI(scale);
        shiftX(xi, -offset * 2); // 這裡可能是原邏輯的筆誤或特殊位移，先照抄
        res.addAll(xi);
        
        List<RelativeLocation> i = romaI(scale);
        shiftX(i, offset);
        res.addAll(i);
        
        return res;
    }

    public static List<RelativeLocation> withRomaNumber(int i, double scale) {
        if (i < 1 || i > 12) throw new IllegalArgumentException("Support 1-12 only");
        
        return switch (i) {
            case 1 -> romaI(scale);
            case 2 -> romaII(scale);
            case 3 -> romaIII(scale);
            case 4 -> romaIV(scale);
            case 5 -> romaV(scale);
            case 6 -> romaVI(scale);
            case 7 -> romaVII(scale);
            case 8 -> romaVIII(scale);
            case 9 -> romaIX(scale);
            case 10 -> romaX(scale);
            case 11 -> romaXI(scale);
            case 12 -> romaXII(scale);
            default -> new ArrayList<>();
        };
    }
}
