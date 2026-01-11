package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.utils.MathPresets;
import com.github.nalamodikk.particle.utils.math.RelativeLocation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PresetsTest {

    @Test
    public void testRomaI() {
        List<RelativeLocation> points = MathPresets.romaI(1.0);
        assertFalse(points.isEmpty());
        // 簡單驗證點的數量
        // romaI 是 3 條線，每條線的點數取決於 scale
        // preLineCount = max(1, 5) = 5
        // 3條線，每條線包含起點終點和中間點 (getLineLocations 邏輯)
        // 大概 15-20 個點
        assertTrue(points.size() > 5);
    }

    @Test
    public void testRomaX() {
        List<RelativeLocation> points = MathPresets.romaX(1.0);
        assertFalse(points.isEmpty());
    }
}
