package com.github.nalamodikk.particle.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PhysicsUtilTest {

    @Test
    public void testAABBIntersection() {
        // Box 1: (0,0,0) -> (1,1,1)
        // Box 2: (0.5, 0.5, 0.5) -> (1.5, 1.5, 1.5)
        assertTrue(PhysicsUtil.intersects(
            0, 0, 0, 1, 1, 1,
            0.5, 0.5, 0.5, 1.5, 1.5, 1.5
        ), "Boxes should intersect");
    }

    @Test
    public void testAABBNoIntersection() {
        // Box 1: (0,0,0) -> (1,1,1)
        // Box 2: (2, 2, 2) -> (3, 3, 3)
        assertFalse(PhysicsUtil.intersects(
            0, 0, 0, 1, 1, 1,
            2, 2, 2, 3, 3, 3
        ), "Boxes should not intersect");
    }
}
