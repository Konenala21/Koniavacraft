package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.utils.builder.PointsBuilder;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PointsBuilderTest {

    @Test
    public void testCircleGeneration() {
        PointsBuilder builder = new PointsBuilder();
        List<RelativeLocation> circle = builder.addCircle(5.0, 4).create();
        
        assertEquals(4, circle.size());
        
        // 撽?蝚砌??? (??5, 閫漲0 -> x=5, z=0)
        RelativeLocation p1 = circle.get(0);
        assertEquals(5.0, p1.x, 0.001);
        assertEquals(0.0, p1.z, 0.001);
    }

    @Test
    public void testRotation() {
        PointsBuilder builder = new PointsBuilder();
        // 撱箇?銝?? (1, 0, 0)
        builder.addPoint(new RelativeLocation(1, 0, 0));
        
        // 蝜?Y 頠豢?頧?90摨?(PI/2)
        builder.rotateAsAxis(Math.PI / 2);
        
        List<RelativeLocation> points = builder.create();
        RelativeLocation p = points.get(0);
        
        System.out.println("Rotated Point: " + p);

        // ??敺??亥? (0, 0, 1) ??(0, 0, -1) ?捱?潭?頧??        assertEquals(0.0, p.x, 0.001, "X should be 0");
        assertEquals(1.0, Math.abs(p.z), 0.001, "Z should be 1 or -1");
    }
}
