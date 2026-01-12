package com.github.nalamodikk.particle.utils;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QuaternionUtilTest {

    @Test
    public void testRotateVector() {
        // 皜祈岫撠?(1, 0, 0) 蝜?Y 頠豢?頧?90 摨佗???敺 (0, 0, -1)
        Vector3f original = new Vector3f(1.0f, 0.0f, 0.0f);
        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(90));
        
        Vector3f result = QuaternionUtil.rotate(original, rotation);
        
        assertEquals(0.0f, result.x, 0.0001f);
        assertEquals(0.0f, result.y, 0.0001f);
        assertEquals(-1.0f, result.z, 0.0001f);
    }
    
    @Test
    public void testSlerp() {
        Quaternionf start = new Quaternionf(); // Identity
        Quaternionf end = new Quaternionf().rotateY((float) Math.toRadians(90));
        
        // ??0.5嚗?閰脫?頧?45 摨?        Quaternionf result = QuaternionUtil.slerp(start, end, 0.5f);
        Vector3f vec = new Vector3f(1.0f, 0.0f, 0.0f);
        vec.rotate(result);
        
        float expectedX = (float) Math.cos(Math.toRadians(45));
        float expectedZ = (float) -Math.sin(Math.toRadians(45));
        
        assertEquals(expectedX, vec.x, 0.0001f);
        assertEquals(0.0f, vec.y, 0.0001f);
        assertEquals(expectedZ, vec.z, 0.0001f);
    }
}
