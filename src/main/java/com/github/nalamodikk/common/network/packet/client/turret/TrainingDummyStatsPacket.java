package com.github.nalamodikk.common.network.packet.client.turret;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.hud.TrainingDummyHudOverlay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * S2C：訓練假人戰鬥 session 更新。每次命中（ended=false）與逾時結束（ended=true）各送一次給該玩家。
 * client 端 {@link TrainingDummyHudOverlay} 收到後更新中下方的傷害/計時顯示。
 */
public record TrainingDummyStatsPacket(boolean ended, double totalDamage, int hitCount, int durationTicks)
        implements CustomPacketPayload {

    public static final Type<TrainingDummyStatsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "training_dummy_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrainingDummyStatsPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBoolean(p.ended);
                        buf.writeDouble(p.totalDamage);
                        buf.writeInt(p.hitCount);
                        buf.writeInt(p.durationTicks);
                    },
                    buf -> new TrainingDummyStatsPacket(
                            buf.readBoolean(), buf.readDouble(), buf.readInt(), buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        TrainingDummyHudOverlay.accept(packet);
                    }
                }));
    }
}
