package com.github.nalamodikk.particle.utils.builder;

import com.github.nalamodikk.particle.utils.Math3DUtil;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * ?綜童??踝???? * ??踐??賹?????塚撕?????選???? */
public class PointsBuilder {

    private RelativeLocation axis = RelativeLocation.yAxis();
    private final List<RelativeLocation> points = new ArrayList<>();

    public PointsBuilder() {}

    public static PointsBuilder of(RelativeLocation axis) {
        PointsBuilder builder = new PointsBuilder();
        builder.axis = axis;
        return builder;
    }

    public PointsBuilder addPoints(Collection<RelativeLocation> enter) {
        for (RelativeLocation p : enter) {
            this.points.add(p.clone());
        }
        return this;
    }

    public PointsBuilder addPoint(RelativeLocation point) {
        this.points.add(point.clone());
        return this;
    }

    public PointsBuilder addCircle(double r, int count) {
        return addPoints(Math3DUtil.getCircleXZ(r, count));
    }

    public PointsBuilder addHalfCircle(double r, int count, double rotate) {
        return addPoints(Math3DUtil.getHalfCircleXZ(r, count, rotate));
    }

    public PointsBuilder addPolygonInCircle(int n, int edgeCount, double r) {
        return addPoints(Math3DUtil.getPolygonInCircleLocations(n, edgeCount, r));
    }

    public PointsBuilder addLine(RelativeLocation start, RelativeLocation end, int count) {
        return addPoints(Math3DUtil.getLineLocations(start, end, count));
    }

    public PointsBuilder rotateAsAxis(double angle) {
        Math3DUtil.rotateAsAxis(points, axis, angle);
        return this;
    }

    public PointsBuilder rotateTo(RelativeLocation target) {
        Math3DUtil.rotatePointsToPoint(points, target, axis);
        return this;
    }

    public List<RelativeLocation> create() {
        List<RelativeLocation> result = new ArrayList<>(points.size());
        for (RelativeLocation p : points) {
            result.add(p.clone());
        }
        return result;
    }
}