package com.github.nalamodikk.client.network;

import com.github.nalamodikk.client.event.ClientParticleTickEvent;
import com.github.nalamodikk.network.packet.ParticleStylePayload;
import com.github.nalamodikk.particle.style.examples.RomaMagicTestStyle;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class ClientPacketHandler {

    public static void handleParticleStyle(final ParticleStylePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            switch (payload.controlType()) {
                case CREATE:
                    if ("roma_magic".equals(payload.styleTypeId())) {
                        RomaMagicTestStyle style = new RomaMagicTestStyle(UUID.randomUUID());
                        style.spawn(context.player().level(), payload.pos());
                        ClientParticleTickEvent.registerStyle(style);
                    }
                    break;
                case REMOVE:
                    break;
                default:
                    break;
            }
        });
    }
}