package com.github.nalamodikk.register.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.MIRenderTypes;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/**
 * 自訂 GLSL core shader 註冊。目前只有 void_core（黑洞核心徑向漸層 + dithering 解 banding）。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ModShaders {

    private static ShaderInstance voidCoreShader;
    private static ShaderInstance voidRiftShader;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "void_core"),
                DefaultVertexFormat.POSITION_TEX_COLOR), shader -> voidCoreShader = shader);
        MIRenderTypes.setVoidCoreShader(() -> voidCoreShader);

        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "void_rift"),
                DefaultVertexFormat.POSITION_TEX_COLOR), shader -> voidRiftShader = shader);
        MIRenderTypes.setVoidRiftShader(() -> voidRiftShader);
    }
}
