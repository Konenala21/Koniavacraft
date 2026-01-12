package com.github.nalamodikk.particle.utils;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * ?詨飛撌亙憿? */
public class MathUtil {

    public static List<Vec3> getCirclePoints(double radius, int points) {
        List<Vec3> result = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            result.add(new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
        }
        return result;
    }

    public static List<Vec3> getCycloidPoints(double R, double r, int points, double winding) {
        List<Vec3> result = new ArrayList<>(points);
        double step = (2 * Math.PI * winding) / points;
        for (int i = 0; i < points; i++) {
            double t = i * step;
            double x = (R - r) * Math.cos(t) + r * Math.cos(((R - r) / r) * t);
            double z = (R - r) * Math.sin(t) - r * Math.sin(((R - r) / r) * t);
            result.add(new Vec3(x, 0, z));
        }
        return result;
    }

    public static List<Vec3> getSpiralPoints(double startRadius, double endRadius, double rounds, int points) {
        List<Vec3> result = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            double angle = 2 * Math.PI * rounds * progress;
            double currentRadius = startRadius + (endRadius - startRadius) * progress;
            result.add(new Vec3(Math.cos(angle) * currentRadius, 0, Math.sin(angle) * currentRadius));
        }
        return result;
    }
}