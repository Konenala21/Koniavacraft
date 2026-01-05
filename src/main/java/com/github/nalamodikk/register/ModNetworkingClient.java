package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.rpg.SyncRPGDataPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraSyncPacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraSystemIntroMessagePacket;
import com.github.nalamodikk.narasystem.nara.network.client.OpenNaraInitScreenPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ModNetworkingClient {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        ManaUpdatePacket.registerClientOnly(registrar);
        SyncRPGDataPacket.registerToClient(registrar);
        // 打開玩家第一次登入的GUI
        OpenNaraInitScreenPacket.registerToClient(registrar);
        NaraSystemIntroMessagePacket.registerToClient(registrar);
        NaraSyncPacket.registerToClient(registrar); // ✅ 完整放這沒問題

    }
}
