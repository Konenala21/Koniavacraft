package com.github.nalamodikk.client.renderer.dimension;

import com.github.nalamodikk.space.ship.ShipEntity;
import com.github.nalamodikk.space.ship.ShipTravel;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 主世界 → 太空的「大氣層漸變」狀態（client only）。
 *
 * <p>切換是硬 {@code changeDimension}（機器要在影子維度 tick、行星在絕對軌道座標，省不掉）。
 * 無縫的訣竅不是消除切換，而是讓切換前後畫面長得一樣：爬升到 {@link #Y_FADE_END} 時主世界
 * 天空已經是全黑星空 + 地球在下方，這刻換維度兩邊一致，看不出 reload。
 *
 * <p>只在「騎著 tier 足夠的飛船、且爬過起點高度」時 > 0。一般玩家飛到高空不會誤觸發。
 */
public final class AtmosphereTransition {
    private AtmosphereTransition() {}

    /** 漸變起點高度：低於此 = 正常主世界天空。 */
    public static final int Y_FADE_START = 700;
    /** 漸變終點高度 = 實際切換維度的高度（{@link ShipTravel#SPACE_ENTRY_Y}），自動同步。 */
    public static final int Y_FADE_END = ShipTravel.SPACE_ENTRY_Y;

    /**
     * 0 = 正常主世界天空，1 = 全太空。非主世界、沒騎合格飛船、或低於 {@link #Y_FADE_START} → 0。
     * 用相機 Y（含 partial tick）算，渲染期間呼叫平滑不頓。
     */
    public static float blend(Minecraft mc) {
        if (mc.level == null || mc.player == null) return 0f;
        if (!mc.level.dimension().equals(Level.OVERWORLD)) return 0f;
        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof ShipEntity ship) || ship.getShipTier() < 1) return 0f;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        return (float) Mth.clamp(
                (camY - Y_FADE_START) / (double) (Y_FADE_END - Y_FADE_START), 0.0, 1.0);
    }

    /** 天空/霧的淡出在起點上方這麼多格內就淡完(太陽月亮消失),不隨傳送高度拉長 → 一爬就有太空感。 */
    public static final double SKY_FADE_BAND = 300.0;

    /**
     * 天空/霧用的漸變：綁「起點上方絕對高度」（{@link #SKY_FADE_BAND} 格內淡完），不像 {@link #blend} 那樣
     * 被整段 700→SPACE_ENTRY_Y 攤平。所以傳送高度拉高也不會害低空的太陽月亮淡很慢。地球球體仍用原 blend
     * （要它整段都可見、慢慢縮），所以分開兩條曲線。
     */
    public static float skyFade(Minecraft mc) {
        if (blend(mc) <= 0f) return 0f; // 共用 blend 的閘門:只在主世界騎合格飛船爬過起點才生效
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        return (float) Mth.clamp((camY - Y_FADE_START) / SKY_FADE_BAND, 0.0, 1.0);
    }
}
