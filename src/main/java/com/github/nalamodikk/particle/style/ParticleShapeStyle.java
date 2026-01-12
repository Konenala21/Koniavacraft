package com.github.nalamodikk.particle.style;

import com.github.nalamodikk.particle.utils.RelativeLocation;
import com.github.nalamodikk.particle.utils.builder.PointsBuilder;
import com.github.nalamodikk.particle.network.buffer.ParticleControlerDataBuffer;

import java.util.*;
import java.util.function.Function;

/**
 * 撟曆?敶Ｙ?璅??
 */
public class ParticleShapeStyle extends ParticleGroupStyle {

    private final Map<PointsBuilder, Function<RelativeLocation, StyleData>> pointBuilders = new LinkedHashMap<>();
    private double currentScale = 1.0;

    public ParticleShapeStyle(UUID uuid) { super(uuid); }
    public ParticleShapeStyle() { super(); }

    public ParticleShapeStyle appendBuilder(PointsBuilder builder, Function<RelativeLocation, StyleData> dataFactory) {
        pointBuilders.put(builder, dataFactory);
        return this;
    }

    @Override
    public Map<StyleData, RelativeLocation> getCurrentFrames() {
        Map<StyleData, RelativeLocation> frames = new HashMap<>();
        for (Map.Entry<PointsBuilder, Function<RelativeLocation, StyleData>> entry : pointBuilders.entrySet()) {
            List<RelativeLocation> points = entry.getKey().create();
            for (RelativeLocation loc : points) {
                frames.put(entry.getValue().apply(loc), loc);
            }
        }
        return frames;
    }

    @Override
    public void onDisplay() {}

    @Override
    public void rotateToWithAngle(RelativeLocation to, double angle) {
        rotateToPoint(to);
        rotateAsAxis(angle);
    }

    @Override
    public Map<String, ParticleControlerDataBuffer<?>> writePacketArgsMap() {
        return new HashMap<>();
    }

    @Override
    public void readPacketArgs(Map<String, ParticleControlerDataBuffer<?>> args) {}

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void tick() {
        if (!valid) return;
    }
}