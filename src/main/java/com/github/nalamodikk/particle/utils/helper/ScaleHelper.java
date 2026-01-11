package com.github.nalamodikk.particle.utils.helper;

import com.github.nalamodikk.particle.style.ParticleShapeStyle;
import com.github.nalamodikk.particle.utils.GraphMathHelper;

public class ScaleHelper {
    private final double minScale;
    private final double maxScale;
    private final int scaleTick;
    private int current = 0;
    
    private ParticleShapeStyle target;

    public ScaleHelper(double minScale, double maxScale, int scaleTick) {
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.scaleTick = scaleTick;
    }

    public void setTarget(ParticleShapeStyle target) {
        this.target = target;
    }

    public void doScale() {
        if (target == null || over()) return;
        
        current++;
        double scale = GraphMathHelper.lerp((double) current / scaleTick, minScale, maxScale);
        target.setScale(scale);
    }

    public void doScaleReversed() {
        if (target == null || isZero()) return;
        
        current--;
        double scale = GraphMathHelper.lerp((double) current / scaleTick, minScale, maxScale);
        target.setScale(scale);
    }

    public boolean over() {
        return current >= scaleTick;
    }

    public boolean isZero() {
        return current <= 0;
    }
}
