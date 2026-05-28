/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.nalamodikk.client.screenAPI;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;

import java.util.function.Supplier;

public class MIRenderTypes {
    private static RenderType MACHINE_WRENCH_OVERLAY;
    private static RenderType SOLID_HIGHLIGHT; // used by hatch preview and wrong block highlight

    public static RenderType machineOverlay() {
        if (MACHINE_WRENCH_OVERLAY == null) {
            MACHINE_WRENCH_OVERLAY = Factory.makeMachineOverlay();
        }
        return MACHINE_WRENCH_OVERLAY;
    }

    public static RenderType solidHighlight() {
        if (SOLID_HIGHLIGHT == null) {
            SOLID_HIGHLIGHT = Factory.makeSolidHighlight();
        }
        return SOLID_HIGHLIGHT;
    }

    private static RenderType SOLAR_GLOW;

    public static RenderType solarGlow() {
        if (SOLAR_GLOW == null) {
            SOLAR_GLOW = Factory.makeSolarGlow();
        }
        return SOLAR_GLOW;
    }

    private static RenderType VOID_CORE;

    // 黑洞核心：POSITION_COLOR、QUADS、普通 alpha 混合（黑色顯示為黑）、不寫深度
    public static RenderType voidCore() {
        if (VOID_CORE == null) {
            VOID_CORE = Factory.makeVoidCore();
        }
        return VOID_CORE;
    }

    // 自訂 shader 提供徑向漸層 + dithering（在 fragment shader 處理 banding）
    // 由 ShaderRegistration 客戶端事件 setter 注入
    private static Supplier<ShaderInstance> VOID_CORE_SHADER_SUPPLIER = () -> null;

    public static void setVoidCoreShader(Supplier<ShaderInstance> supplier) {
        VOID_CORE_SHADER_SUPPLIER = supplier;
    }

    private static RenderType VOID_CORE_SHADER;

    public static RenderType voidCoreShader() {
        if (VOID_CORE_SHADER == null) {
            VOID_CORE_SHADER = Factory.makeVoidCoreShader();
        }
        return VOID_CORE_SHADER;
    }

    // 紫色光暈版本：同一個 shader 但用 additive blending，給 SpaceCrack 外圈用
    private static RenderType VOID_GLOW_SHADER;

    public static RenderType voidGlowShader() {
        if (VOID_GLOW_SHADER == null) {
            VOID_GLOW_SHADER = Factory.makeVoidGlowShader();
        }
        return VOID_GLOW_SHADER;
    }

    private static final RenderStateShard.ShaderStateShard VOID_CORE_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(() -> VOID_CORE_SHADER_SUPPLIER.get());

    // 紫色撕裂縫 shader（lens + zigzag + 鋸齒邊緣，無 strip seam）
    private static Supplier<ShaderInstance> VOID_RIFT_SHADER_SUPPLIER = () -> null;

    public static void setVoidRiftShader(Supplier<ShaderInstance> supplier) {
        VOID_RIFT_SHADER_SUPPLIER = supplier;
    }

    private static final RenderStateShard.ShaderStateShard VOID_RIFT_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(() -> VOID_RIFT_SHADER_SUPPLIER.get());

    private static RenderType VOID_RIFT;

    public static RenderType voidRiftShader() {
        if (VOID_RIFT == null) {
            VOID_RIFT = Factory.makeVoidRiftShader();
        }
        return VOID_RIFT;
    }

    // Lightning RT 變體：穿透方塊用（POSITION_COLOR + LIGHTNING_TRANSPARENCY + NO_DEPTH_TEST）
    private static RenderType LIGHTNING_NO_DEPTH;

    public static RenderType lightningNoDepth() {
        if (LIGHTNING_NO_DEPTH == null) {
            LIGHTNING_NO_DEPTH = Factory.makeLightningNoDepth();
        }
        return LIGHTNING_NO_DEPTH;
    }

    private static RenderType SEAL_CHAIN;

    // 封印鍊子：半透明、寫深度、無貼圖、POSITION_COLOR
    public static RenderType sealChain() {
        if (SEAL_CHAIN == null) {
            SEAL_CHAIN = Factory.makeSealChain();
        }
        return SEAL_CHAIN;
    }

    // This is a subclass to get access to a bunch of fields and classes.
    // TODO: PR more transitive access wideners to fabric
    private static class Factory extends RenderType {
        private Factory(String string, VertexFormat vertexFormat, VertexFormat.Mode mode, int i, boolean bl, boolean bl2, Runnable runnable,
                Runnable runnable2) {
            super(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2);
        }

        private static RenderType makeMachineOverlay() {
            return create("machine_overlay", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.QUADS, 65536, false, true,
                    CompositeState.builder()
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setTextureState(NO_TEXTURE)
                            .setLightmapState(NO_LIGHTMAP)
                            .setShaderState(POSITION_COLOR_SHADER)
                            .createCompositeState(false));
        }

        private static RenderType makeSolidHighlight() {
            return create("solid_highlight", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.QUADS, 65536, false, false,
                    CompositeState.builder()
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setTextureState(NO_TEXTURE)
                            .setLightmapState(NO_LIGHTMAP)
                            .setShaderState(POSITION_COLOR_SHADER)
                            .createCompositeState(false));
        }

        // Lightning RT 穿透方塊變體
        private static RenderType makeLightningNoDepth() {
            return create("koniava_lightning_no_depth",
                    DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 2048, false, false,
                    CompositeState.builder()
                            .setShaderState(POSITION_COLOR_SHADER)
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .setTextureState(NO_TEXTURE)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setCullState(NO_CULL)
                            .createCompositeState(false));
        }

        // 紫色撕裂縫 shader（lens + zigzag + 鋸齒邊緣，additive 加色發光，無 strip seam）
        // NO_DEPTH_TEST + NO_CULL → 雙重保險穿透方塊不被剔除
        private static RenderType makeVoidRiftShader() {
            return create("koniava_void_rift_shader",
                    DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 2048, false, false,
                    CompositeState.builder()
                            .setShaderState(VOID_RIFT_SHADER_STATE)
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .setTextureState(NO_TEXTURE)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setCullState(NO_CULL)
                            .createCompositeState(false));
        }

        // 紫色光暈：同一個 shader（voidCore），但用 LIGHTNING_TRANSPARENCY（additive）→ 加色發光
        private static RenderType makeVoidGlowShader() {
            return create("koniava_void_glow_shader",
                    DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 2048, false, false,
                    CompositeState.builder()
                            .setShaderState(VOID_CORE_SHADER_STATE)
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .setTextureState(NO_TEXTURE)
                            .createCompositeState(false));
        }

        // 封印鍊子：與 solarGlow 相同設定（LIGHTNING + COLOR_WRITE），保證 BER context 可用
        private static RenderType makeSealChain() {
            return create("koniava_seal_chain", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
                    CompositeState.builder()
                            .setShaderState(POSITION_COLOR_SHADER)
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .setTextureState(NO_TEXTURE)
                            .createCompositeState(false));
        }

        // 黑洞核心 + 自訂 GLSL shader（內含 dithering 解 banding）
        // POSITION_TEX_COLOR：頂點需有 UV 座標 (0,0)~(1,1)，fragment 由 UV 算徑向距離
        // NO_DEPTH_TEST + NO_CULL → 穿透方塊
        private static RenderType makeVoidCoreShader() {
            return create("koniava_void_core_shader",
                    DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 2048, false, false,
                    CompositeState.builder()
                            .setShaderState(VOID_CORE_SHADER_STATE)
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .setTextureState(NO_TEXTURE)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setCullState(NO_CULL)
                            .createCompositeState(false));
        }

        // 黑洞核心：用 TRANSLUCENT_TRANSPARENCY（普通 alpha）+ COLOR_WRITE，可正確顯示黑色 + 不寫深度避免遮蔽
        private static RenderType makeVoidCore() {
            return create("koniava_void_core", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 2048, false, false,
                    CompositeState.builder()
                            .setShaderState(POSITION_COLOR_SHADER)
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .setTextureState(NO_TEXTURE)
                            .createCompositeState(false));
        }

        // Additive glow：不寫深度緩衝，解決黑色閃爍
        private static RenderType makeSolarGlow() {
            return create("koniava_solar_glow", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 2048, false, false,
                    CompositeState.builder()
                            .setShaderState(POSITION_COLOR_SHADER)
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .setTextureState(NO_TEXTURE)
                            .createCompositeState(false));
        }
    }
}
