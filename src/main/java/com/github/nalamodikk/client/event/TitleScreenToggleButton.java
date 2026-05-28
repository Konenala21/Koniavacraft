package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.config.ModClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * 在 TitleScreen 右上角加切換按鈕：切換 customTitleScreenEnabled 並即時 reload screen。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class TitleScreenToggleButton {

    @SubscribeEvent
    public static void onTitleScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) return;
        boolean enabled = ModClientConfig.INSTANCE.customTitleScreenEnabled.get();
        Component label = enabled
                ? Component.translatable("message.koniava.title_toggle.custom") // 「★ 模組主選單」
                : Component.translatable("message.koniava.title_toggle.vanilla"); // 「○ 原版主選單」
        Button btn = Button.builder(label, b -> {
            ModClientConfig.INSTANCE.customTitleScreenEnabled.set(!enabled);
            Minecraft.getInstance().setScreen(new TitleScreen()); // 重新生 screen，按鈕跟外觀都更新
        }).bounds(screen.width - 110, 8, 100, 20).build();
        event.addListener(btn);
    }
}
