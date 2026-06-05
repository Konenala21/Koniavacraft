package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.KoniavacraftMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

@OnlyIn(Dist.CLIENT)
public final class GLCapabilityCache {

    public static boolean gl32 = false; // Geometry Shaders
    public static boolean gl40 = false; // Tessellation
    public static boolean gl43 = false; // Compute Shaders, SSBOs, layout bindings
    public static boolean gl46 = false; // DSA improvements

    private static boolean detected = false;

    public static void detect() {
        if (detected) return;
        GLCapabilities caps = GL.getCapabilities();
        gl32 = caps.OpenGL32;
        gl40 = caps.OpenGL40;
        gl43 = caps.OpenGL43;
        gl46 = caps.OpenGL46;
        detected = true;
        KoniavacraftMod.LOGGER.info("[GL] Detected: GL32={} GL40={} GL43={} GL46={}", gl32, gl40, gl43, gl46);
    }

    /** GLSL version string to use: "430 core" if GL43, else "150" */
    public static String glslVersion() {
        return gl43 ? "430 core" : "150";
    }

    private GLCapabilityCache() {}
}
