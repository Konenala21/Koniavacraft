package com.github.nalamodikk.particle.style.examples;

import com.github.nalamodikk.particle.style.ParticleShapeStyle;
import com.github.nalamodikk.particle.style.ParticleStyle;
import com.github.nalamodikk.particle.utils.MathPresets;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import com.github.nalamodikk.particle.utils.builder.PointsBuilder;

import java.util.List;
import java.util.UUID;

/**
 * 羅馬魔法陣測試樣式
 */
public class RomaMagicTestStyle extends ParticleShapeStyle {

    public RomaMagicTestStyle(UUID uuid) {
        super(uuid);
        
        PointsBuilder circleBuilder = new PointsBuilder().addCircle(5.0, 60);
        List<RelativeLocation> circlePoints = circleBuilder.create();
        
        for (int i = 0; i < 12; i++) {
            final int num = i + 1;
            RelativeLocation center = circlePoints.get((i * 5) % circlePoints.size());
            
            this.appendBuilder(
                new PointsBuilder()
                    .addPoints(MathPresets.withRomaNumber(num, 0.5))
                    .rotateTo(center),
                loc -> new ParticleStyle.StyleData("coo_particle")
            );
        }
    }
}
