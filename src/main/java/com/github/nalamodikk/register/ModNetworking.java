package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.OpenUpgradeGuiPacket;
import com.github.nalamodikk.common.network.packet.server.conduit.PriorityUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.conduit.ResetPrioritiesPacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionBatchUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ModeChangePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.TechWandModePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ToggleModePacket;
import com.github.nalamodikk.common.network.packet.server.player.gui.OpenExtraEquipmentPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModNetworking {
    public static final String VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        TechWandModePacket.registerTo(registrar);
        ModeChangePacket.registerTo(registrar);
        ConfigDirectionUpdatePacket.registerTo(registrar);
        ConfigDirectionBatchUpdatePacket.registerTo(registrar);
        ToggleModePacket.registerTo(registrar);
        OpenUpgradeGuiPacket.registerTo(registrar);

        // 娜拉系統
        NaraBindRequestPacket.registerTo(registrar);

        //玩家的額外裝備gui封包
        OpenExtraEquipmentPacket.registerTo(registrar);


        //導管
        PriorityUpdatePacket.registerTo(registrar);
        ResetPrioritiesPacket.registerTo(registrar);
    }

}
