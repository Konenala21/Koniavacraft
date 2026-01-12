package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.utils.MathPresets;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PresetsTest {

    @Test
    public void testRomaI() {
        List<RelativeLocation> points = MathPresets.romaI(1.0);
        assertFalse(points.isEmpty());
        // 蝪∪撽?暺??賊?
        // romaI ??3 璇?嚗?璇????詨?瘙箸 scale
        // preLineCount = max(1, 5) = 5
        // 3璇?嚗?璇??韏琿?蝯??葉?? (getLineLocations ?摩)
        // 憭扳? 15-20 ??
        assertTrue(points.size() > 5);
    }

    @Test
    public void testRomaX() {
        List<RelativeLocation> points = MathPresets.romaX(1.0);
        assertFalse(points.isEmpty());
    }
}
