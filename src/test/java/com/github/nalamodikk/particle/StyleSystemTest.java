package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.style.ParticleShapeStyle;
import com.github.nalamodikk.particle.style.ParticleStyle;
import com.github.nalamodikk.particle.utils.builder.PointsBuilder;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StyleSystemTest {

    @Test
    public void testShapeStyleIntegration() {
        ParticleShapeStyle style = new ParticleShapeStyle();
        PointsBuilder builder = new PointsBuilder().addCircle(2.0, 8);
        
        style.appendBuilder(builder, rel -> new ParticleStyle.StyleData("test_particle"));
        
        Map<ParticleStyle.StyleData, RelativeLocation> frames = style.getCurrentFrames();
        
        // 撽??臬??鈭?8 ?????        assertEquals(8, frames.size());
        
        // 撽? UUID ?臬?臭?
        long uniqueUuids = frames.keySet().stream().map(d -> d.uuid).distinct().count();
        assertEquals(8, uniqueUuids);
    }
}
