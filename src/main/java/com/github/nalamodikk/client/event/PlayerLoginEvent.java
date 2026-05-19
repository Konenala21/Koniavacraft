package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.config.ModCommonConfig;
import com.github.nalamodikk.narasystem.nara.event.NaraServerEvents;
import com.github.nalamodikk.narasystem.nara.network.client.OpenNaraInitScreenPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraSyncPacket;
import com.github.nalamodikk.narasystem.nara.util.NaraHelper;
import com.github.nalamodikk.register.ModDataAttachments;
import com.github.nalamodikk.research.network.KnowledgeSyncPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class PlayerLoginEvent {
    public static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // ===== 初始化附加資料（這部分要放在外面，總是執行） =====
        // 初始化玩家9格儲存資料
        if (!player.hasData(ModDataAttachments.NINE_GRID.get())) {
            player.setData(ModDataAttachments.NINE_GRID.get(), NonNullList.withSize(9, ItemStack.EMPTY));
        }

        // 初始化玩家飾品裝備資料
        if (!player.hasData(ModDataAttachments.EXTRA_EQUIPMENT.get())) {
            player.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), NonNullList.withSize(8, ItemStack.EMPTY));
        }

        if (player.getServer() instanceof GameTestServer) {
            LOGGER.debug("GameTest 伺服器登入，略過娜拉系統封包: {}", player.getGameProfile().getName());
            return;
        }

        // 傳送同步封包（無論動畫是否開啟都需要）
        PacketDistributor.sendToPlayer(player, new NaraSyncPacket(NaraHelper.isBound(player)));
        KnowledgeSyncPacket.sendTo(player);

        // ===== 動畫相關邏輯（這部分可以被設定關閉） =====
        if (!ModCommonConfig.INSTANCE.showIntroAnimation.get()) {
            LOGGER.debug("Login animation disabled by config. Skipping intro for player {}", player.getGameProfile().getName());
            return;
        }

        // 根據綁定狀態開啟初始畫面
        if (!NaraHelper.isBound(player)) {
            PacketDistributor.sendToPlayer(player, new OpenNaraInitScreenPacket());
            LOGGER.debug("open player one login gui!");
        }

        // 恢復未完成的教學 pending 狀態（server 重啟後重填記憶體 Set）
        NaraServerEvents.restorePendingTutorials(player);
    }
}
