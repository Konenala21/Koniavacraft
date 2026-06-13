package com.github.nalamodikk.client.renderer.dimension;

import com.github.nalamodikk.space.ship.ShipEntity;
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
    /** 漸變終點高度：必須等於 {@code ShipEntity.SPACE_ENTRY_Y}（實際切換維度的高度）。 */
    public static final int Y_FADE_END = 1000;

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
}
