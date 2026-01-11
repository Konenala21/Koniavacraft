package com.github.nalamodikk.particle.utils.builder;

import com.github.nalamodikk.particle.utils.Math3DUtil;
import com.github.nalamodikk.particle.utils.math.RelativeLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 幾何點構建器
 * 用於生成各種形狀的粒子點集
 */
public class PointsBuilder {

    private RelativeLocation axis = RelativeLocation.yAxis();
    private final List<RelativeLocation> points = new ArrayList<>();

    public PointsBuilder() {
    }

    public static PointsBuilder of(RelativeLocation axis) {
        PointsBuilder builder = new PointsBuilder();
        builder.axis = axis;
        return builder;
    }

    public static PointsBuilder of(Collection<RelativeLocation> points) {
        PointsBuilder builder = new PointsBuilder();
        builder.addPoints(points);
        return builder;
    }

    public PointsBuilder axis(RelativeLocation axis) {
        this.axis = axis;
        return this;
    }

    public PointsBuilder addPoints(Collection<RelativeLocation> enter) {
        // 深拷貝，避免後續修改影響原集合
        for (RelativeLocation p : enter) {
            this.points.add(p.clone());
        }
        return this;
    }

    public PointsBuilder addPoint(RelativeLocation point) {
        this.points.add(point.clone());
        return this;
    }
    
    // ========== 形狀生成方法 ==========

    public PointsBuilder addCircle(double r, int count) {
        return addPoints(Math3DUtil.getCircleXZ(r, count));
    }

    public PointsBuilder addHalfCircle(double r, int count) {
        return addPoints(Math3DUtil.getHalfCircleXZ(r, count, 0.0));
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

    public PointsBuilder addLine(Vec3 start, Vec3 end, int count) {
        return addPoints(Math3DUtil.getLineLocations(start, end, count));
    }

    // ========== 變換操作 ==========

    public PointsBuilder rotateAsAxis(double angle) {
        Math3DUtil.rotateAsAxis(points, axis, angle);
        return this;
    }

    public PointsBuilder rotateAsAxis(double angle, RelativeLocation customAxis) {
        Math3DUtil.rotateAsAxis(points, customAxis, angle);
        return this;
    }
    
    public PointsBuilder rotateTo(RelativeLocation target) {
        Math3DUtil.rotatePointsToPoint(points, target, axis);
        return this;
    }
    
    public PointsBuilder pointsOnEach(Consumer<RelativeLocation> handler) {
        points.forEach(handler);
        return this;
    }
    
    // ========== 組合操作 ==========
    
    public PointsBuilder withBuilder(PointsBuilder otherBuilder) {
        addPoints(otherBuilder.create());
        return this;
    }

    public PointsBuilder withBuilder(Consumer<PointsBuilder> handler) {
        PointsBuilder builder = new PointsBuilder();
        handler.accept(builder);
        addPoints(builder.create());
        return this;
    }

    public PointsBuilder withPreset(Function<Double, List<RelativeLocation>> preset, double scale) {
        addPoints(preset.apply(scale));
        return this;
    }
    
    /**
     * 專門用於 MathPresets 的適配器
     */
    public PointsBuilder withRomaNumber(int number, double scale) {
        addPoints(com.github.nalamodikk.particle.utils.MathPresets.withRomaNumber(number, scale));
        return this;
    }

    // ========== 輸出 ==========

    public PointsBuilder clear() {
        points.clear();
        return this;
    }

    public List<RelativeLocation> create() {
        // 返回深拷貝列表
        List<RelativeLocation> result = new ArrayList<>(points.size());
        for (RelativeLocation p : points) {
            result.add(p.clone());
        }
        return result;
    }
}
