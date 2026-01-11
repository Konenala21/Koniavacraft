package com.github.nalamodikk.common.network;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.network.ClientPacketHandler;
import com.github.nalamodikk.network.packet.ParticleStylePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModMessages {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0");
        
        registrar.playToClient(
            ParticleStylePayload.TYPE,
            ParticleStylePayload.STREAM_CODEC,
            ClientPacketHandler::handleParticleStyle
        );
    }
}
