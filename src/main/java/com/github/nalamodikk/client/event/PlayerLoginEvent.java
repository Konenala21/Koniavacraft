package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.config.ModCommonConfig;
import com.github.nalamodikk.narasystem.nara.network.client.OpenNaraInitScreenPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraSyncPacket;
import com.github.nalamodikk.narasystem.nara.util.NaraHelper;
import com.github.nalamodikk.register.ModDataAttachments;
import com.mojang.logging.LogUtils;
import net.minecraft.core.NonNullList;
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

        // ===== 動畫相關邏輯（這部分可以被設定關閉） =====
        // ✅ 若關閉登入動畫，不執行動畫相關封包
        if (!ModCommonConfig.INSTANCE.showIntroAnimation.get()) {
            LOGGER.debug("Login animation disabled by config. Skipping intro for player {}", player.getGameProfile().getName());
            return;
        }

        // 傳送同步封包（可保留）
        PacketDistributor.sendToPlayer(player, new NaraSyncPacket(NaraHelper.isBound(player)));

        // 根據綁定狀態開啟初始畫面
        if (!NaraHelper.isBound(player)) {
            PacketDistributor.sendToPlayer(player, new OpenNaraInitScreenPacket());
            LOGGER.debug("open player one login gui!");
        }
    }
}
