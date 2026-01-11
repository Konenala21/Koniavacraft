package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.utils.builder.PointsBuilder;
import com.github.nalamodikk.particle.utils.math.RelativeLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PointsBuilderTest {

    @Test
    public void testCircleGeneration() {
        PointsBuilder builder = new PointsBuilder();
        List<RelativeLocation> circle = builder.addCircle(5.0, 4).create();
        
        assertEquals(4, circle.size());
        
        // 驗證第一個點 (半徑5, 角度0 -> x=5, z=0)
        RelativeLocation p1 = circle.get(0);
        assertEquals(5.0, p1.x, 0.001);
        assertEquals(0.0, p1.z, 0.001);
    }

    @Test
    public void testRotation() {
        PointsBuilder builder = new PointsBuilder();
        // 建立一個點 (1, 0, 0)
        builder.addPoint(new RelativeLocation(1, 0, 0));
        
        // 繞 Y 軸旋轉 90度 (PI/2)
        builder.rotateAsAxis(Math.PI / 2);
        
        List<RelativeLocation> points = builder.create();
        RelativeLocation p = points.get(0);
        
        System.out.println("Rotated Point: " + p);

        // 旋轉後應接近 (0, 0, 1) 或 (0, 0, -1) 取決於旋轉方向
        assertEquals(0.0, p.x, 0.001, "X should be 0");
        assertEquals(1.0, Math.abs(p.z), 0.001, "Z should be 1 or -1");
    }
}
