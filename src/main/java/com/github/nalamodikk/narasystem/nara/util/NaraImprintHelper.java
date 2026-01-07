package com.github.nalamodikk.narasystem.nara.util;

import com.github.nalamodikk.register.ModDataAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 處理 Nara 系統的「銘印 (Imprint)」邏輯。
 * 銘印是玩家與機器或世界建立深層連結的過程。
 */
public class NaraImprintHelper {

    /**
     * 檢查玩家是否已經與 Nara 系統綁定。
     */
    public static boolean isPlayerBound(Player player) {
        return NaraHelper.isBound(player);
    }

    /**
     * 對特定機器進行銘印。
     * 未來可在此加入經驗消耗、屬性檢查或特殊視覺效果。
     */
    public static void imprintMachine(Player player, BlockEntity machine) {
        if (!isPlayerBound(player)) return;

        // 這裡可以實作將玩家 UUID 寫入機器 NBT/DataComponent 的邏輯
        // 目前先作為基礎邏輯預留
    }

    /**
     * 觸發玩家的銘印對話或事件。
     */
    public static void triggerImprintEvent(Player player, String eventId) {
        // 連接至 NaraMessageRenderer 或 NaraIntroScheduler
    }
}