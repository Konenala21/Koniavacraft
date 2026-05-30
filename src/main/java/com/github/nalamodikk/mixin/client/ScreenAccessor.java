package com.github.nalamodikk.mixin.client;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 開放 Screen.addRenderableWidget（protected）。
 * 必須掛在宣告該方法的 Screen，不能掛在子類（OptionsSubScreen），否則 @Invoker 找不到方法會 crash。
 */
@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T koniava_addRenderableWidget(T widget);
}
