package com.github.nalamodikk.client.screen.cinematic;

import com.github.nalamodikk.common.config.ModClientConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 統一入口：玩家按 R 想跳過 cinematic 時呼叫 {@link #requestSkip}。
 * 內部依 {@link ModClientConfig#cinematicSkipDontAsk} 決定直接跳過，還是先彈確認視窗。
 */
@OnlyIn(Dist.CLIENT)
public final class CinematicSkipHelper {

    private CinematicSkipHelper() {}

    /**
     * 請求跳過 cinematic。
     * @param doSkip 真正執行跳過的動作（送 server packet、推進 ticks 等）
     */
    public static void requestSkip(Runnable doSkip) {
        if (ModClientConfig.INSTANCE.cinematicSkipDontAsk.get()) {
            doSkip.run();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // 防止已開啟其他 Screen 時又疊一層
        if (mc.screen instanceof CinematicSkipConfirmScreen) return;
        mc.setScreen(new CinematicSkipConfirmScreen(doSkip));
    }
}
