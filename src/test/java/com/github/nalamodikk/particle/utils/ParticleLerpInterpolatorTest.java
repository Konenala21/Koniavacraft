package com.github.nalamodikk.particle.utils;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ParticleLerpInterpolatorTest {

    @Test
    public void testLerpPosition() {
        // Prev: (0,0,0), Curr: (10, 20, 30), Partial: 0.5
        // Expected: (5, 10, 15)
        Vector3d result = ParticleLerpInterpolator.lerpPosition(0, 0, 0, 10, 20, 30, 0.5f);
        
        assertEquals(5.0, result.x, 0.0001);
        assertEquals(10.0, result.y, 0.0001);
        assertEquals(15.0, result.z, 0.0001);
    }
}
