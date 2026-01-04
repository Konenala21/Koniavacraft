package com.github.nalamodikk.narasystem.nara.message;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModDataAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayDeque;
import java.util.Queue;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class NaraMessageRenderer {
    private static final Queue<Component> messageQueue = new ArrayDeque<>();
    private static Component currentMessage = null;

    private static final int CHAR_DELAY = 2; // 幾 tick 顯示一個字
    private static final int POST_MESSAGE_TICKS = 100; // 顯示完後額外停留的時間

    private static int charIndex = 0;
    private static int tickCounter = 0;
    private static int stayTicks = 0;

    public static void queue(String translationKey) {
        messageQueue.add(Component.translatable(translationKey));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) return;

        if (currentMessage == null && !messageQueue.isEmpty()) {
            currentMessage = messageQueue.poll();
            charIndex = 0;
            tickCounter = 0;
            stayTicks = 0;
        }

        if (currentMessage != null) {
            if (charIndex < currentMessage.getString().length()) {
                tickCounter++;
                if (tickCounter >= CHAR_DELAY) {
                    tickCounter = 0;
                    charIndex++;
                }
            } else {
                stayTicks++;
                if (stayTicks > POST_MESSAGE_TICKS) {
                    currentMessage = null;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !player.getData(ModDataAttachments.NARA_BOUND)) return;
        if (currentMessage == null || charIndex == 0) return;

        GuiGraphics graphics = event.getGuiGraphics();
        PoseStack pose = graphics.pose();
        Font font = mc.font;

        String fullText = currentMessage.getString();
        String shownText = fullText.substring(0, Math.min(charIndex, fullText.length()));

        int width = font.width(shownText) + 20;
        int height = 20;
        int x = (mc.getWindow().getGuiScaledWidth() - width) / 2;
        int y = mc.getWindow().getGuiScaledHeight() - 60;

        graphics.fill(x, y, x + width, y + height, 0x88000000);
        graphics.drawString(font, shownText, x + 10, y + 6, 0x00FFCC, false);
    }
}


