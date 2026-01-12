package com.github.nalamodikk.particle.utils;

import com.github.nalamodikk.mixin.client.MinecraftAccessor;
import net.minecraft.client.Minecraft;

/**
 * ?扯??葫
 */
public class PerformanceMonitor {

    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();
    private static final int LOW_FPS_THRESHOLD = 30;
    private static final int CRITICAL_FPS_THRESHOLD = 15;

    private PerformanceMonitor() {}

    public static PerformanceMonitor getInstance() {
        return INSTANCE;
    }

    public int getCurrentFps() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return 60;
        return ((MinecraftAccessor) mc).getFps();
    }

    public boolean isPerformanceLow() {
        return getCurrentFps() < LOW_FPS_THRESHOLD;
    }
    
    public int getParticleLimit() {
        int fps = getCurrentFps();
        if (fps < CRITICAL_FPS_THRESHOLD) return 500;
        if (fps < LOW_FPS_THRESHOLD) return 2000;
        return 4000;
    }
}