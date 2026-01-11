package com.github.nalamodikk.particle.animation;

import com.github.nalamodikk.particle.utils.math.RelativeLocation;

public interface PathMotion {
    RelativeLocation getMotion(double progress);
}
