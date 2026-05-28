package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 主選單客製化：
 *   - 隱藏 vanilla "Minecraft" logo，改畫自訂浮動圖片（位置：assets/koniava/textures/gui/title.png）
 *   - 隱藏 vanilla splash（"Better than the leading brand!" 那種黃字），改用自訂池抽 (中英混合)
 * 可由 ModClientConfig.customTitleScreenEnabled 關掉，恢復原版。
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Unique private static final ResourceLocation KONIAVA$TITLE_TEX =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/title.png");
    // 保 512x341 比例縮小到 vanilla logo 大小級別（vanilla 274x44，我們略大）
    @Unique private static final int KONIAVA$TITLE_W = 200;
    @Unique private static final int KONIAVA$TITLE_H = 133;
    // 標題相對螢幕頂端的 y 位置（仿 vanilla logo 的 y=30）
    @Unique private static final int KONIAVA$TITLE_Y_BASE = 15;

    // 自訂 splash 池（從 nara_character.md / 娜拉配音.md / 未來想法.md / suno_songs.md 摘錄）
    @Unique private static final List<String> KONIAVA$SPLASHES = List.of(
            "我應該沒來遲吧？！",
            "I'm not late, right?!",
            "本源是什麼？以後你會懂的。",
            "What is an aspect? You'll find out.",
            "看起來像魔法，玩起來像科學。",
            "Looks like magic, plays like science.",
            "欸嘿！我要走了掰掰~~~~",
            "Gotta go, bye~~~",
            "職業？嗯…存在？反正我在這裡。",
            "Job? Existing? Does that count?",
            "在你前面走，比你早幾步而已。",
            "Just a few steps ahead of you.",
            "魔力維度裡只有我一個人。",
            "Only me in the mana dimension.",
            "可惡你這個壞人！…算了算了。",
            "Meanie! ...Whatever, I forgive you.",
            "雜魚！(小聲)",
            "Pathetic. (whispered)",
            "哦齁！看起來有好戲看了哦！",
            "Looks like things are getting good!",
            "別讓我說第二次。",
            "Don't make me say it twice.",
            "找到它們最深處的名字。",
            "Find the names underneath.",
            "兩個視角的同一個人。",
            "Two views, same person.",
            "我不告訴你答案。",
            "I won't give you the answer.",
            "理想中的自己。",
            "The self you wanted to be.",
            "就知道你會來這裡。",
            "I knew you'd come here.",
            "魔力可以被量化。",
            "Mana can be quantified.",
            "靈魂選擇入世，消除記憶。",
            "Soul: omniscient. Body: forgetful.",
            "去用那個手錶。",
            "Just use the watch."
    );

    @Unique private static String koniava$currentSplash;

    @Inject(method = "init", at = @At("RETURN"))
    private void koniava$pickSplash(CallbackInfo ci) {
        if (!ModClientConfig.INSTANCE.customTitleScreenEnabled.get()) return;
        koniava$currentSplash = KONIAVA$SPLASHES.get(RandomSource.create().nextInt(KONIAVA$SPLASHES.size()));
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"))
    private void koniava$skipVanillaLogo(LogoRenderer instance, GuiGraphics graphics, int width, float alpha) {
        if (!ModClientConfig.INSTANCE.customTitleScreenEnabled.get()) {
            instance.renderLogo(graphics, width, alpha);
        }
        // config 開啟 → 跳過 vanilla logo，交給 koniava$drawCustomTitle
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"))
    private void koniava$skipVanillaSplash(SplashRenderer instance, GuiGraphics graphics, int width, Font font, int alpha) {
        if (!ModClientConfig.INSTANCE.customTitleScreenEnabled.get()) {
            instance.render(graphics, width, font, alpha);
        }
        // 跳過 vanilla splash → koniava$drawCustomSplash 接手
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void koniava$drawCustomTitle(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!ModClientConfig.INSTANCE.customTitleScreenEnabled.get()) return;
        TitleScreen self = (TitleScreen) (Object) this;

        // 標題圖片：水平置中 + 垂直固定，加上輕微縮放（±3% 微脈動）取代上下浮動
        int titleX = (self.width - KONIAVA$TITLE_W) / 2;
        int titleY = KONIAVA$TITLE_Y_BASE;
        float scaleT = Util.getMillis() / 600.0f;
        float titleScale = 1.0f + Mth.sin(scaleT) * 0.03f;
        float titleCenterX = self.width / 2.0f;
        float titleCenterY = titleY + KONIAVA$TITLE_H / 2.0f;
        PoseStack tilePose = graphics.pose();
        tilePose.pushPose();
        tilePose.translate(titleCenterX, titleCenterY, 0);
        tilePose.scale(titleScale, titleScale, 1.0f);
        tilePose.translate(-titleCenterX, -titleCenterY, 0);
        graphics.blit(KONIAVA$TITLE_TEX, titleX, titleY, 0, 0, KONIAVA$TITLE_W, KONIAVA$TITLE_H,
                KONIAVA$TITLE_W, KONIAVA$TITLE_H);
        tilePose.popPose();

        // splash：vanilla 經典位置 (width/2 + 88, 70)，黃字、傾斜 -20°、跳動
        String splash = koniava$currentSplash;
        if (splash == null) return;
        Minecraft mc = Minecraft.getInstance();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(self.width / 2.0f + 88.0f, 70.0f, 0);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-20.0f));
        // 字體浮動縮放
        float scale = 1.8f - Math.abs(Mth.sin(Util.getMillis() / 1000.0f * (float) Math.PI * 2.0f) * 0.1f);
        float widthScale = scale * 100.0f / (mc.font.width(splash) + 32.0f);
        widthScale = Math.min(widthScale, scale);
        pose.scale(widthScale, widthScale, widthScale);
        int textWidth = mc.font.width(splash);
        graphics.drawString(mc.font, splash, -textWidth / 2, -8, 0xFFFFFF00, true);
        pose.popPose();
    }
}
