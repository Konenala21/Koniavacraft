package com.github.nalamodikk.client.network;

import com.github.nalamodikk.client.event.ClientParticleTickEvent;
import com.github.nalamodikk.network.packet.ParticleStylePayload;
import com.github.nalamodikk.particle.control.ControlType;
import com.github.nalamodikk.particle.style.examples.RomaMagicTestStyle;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPacketHandler {

    public static void handleParticleStyle(final ParticleStylePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // 在主線程處理
            switch (payload.controlType()) {
                case CREATE:
                    if ("roma_magic".equals(payload.styleTypeId())) {
                        RomaMagicTestStyle style = new RomaMagicTestStyle();
                        // 這裡可以設置 uuid，如果我們想追蹤它的話
                        style.display(context.player().level(), payload.pos());
                        ClientParticleTickEvent.registerStyle(style);
                    }
                    break;
                case REMOVE:
                    // TODO: 根據 UUID 移除 style
                    break;
                default:
                    break;
            }
        });
    }
}
