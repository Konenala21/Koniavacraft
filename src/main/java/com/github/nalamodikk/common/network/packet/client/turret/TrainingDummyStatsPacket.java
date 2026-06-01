package com.github.nalamodikk.common.network.packet.client.turret;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.hud.TrainingDummyHudOverlay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C：訓練假人戰鬥 session 更新。每次命中（ended=false）與逾時結束（ended=true）各送一次給該玩家。
 * client 端 {@link TrainingDummyHudOverlay} 收到後更新中下方的傷害/計時顯示，以及目前假人身上的效果清單
 * （讓玩家看得到破甲/流血/易傷等技能反應作用在它身上）。
 */
public record TrainingDummyStatsPacket(boolean ended, double totalDamage, int hitCount, int durationTicks,
                                       List<EffectLine> effects) implements CustomPacketPayload {

    /** 假人身上一個效果：翻譯鍵 + 等級（amplifier）。 */
    public record EffectLine(String descriptionId, int amplifier) {}

    public static final Type<TrainingDummyStatsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "training_dummy_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrainingDummyStatsPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBoolean(p.ended);
                        buf.writeDouble(p.totalDamage);
                        buf.writeInt(p.hitCount);
                        buf.writeInt(p.durationTicks);
                        buf.writeVarInt(p.effects.size());
                        for (EffectLine e : p.effects) {
                            buf.writeUtf(e.descriptionId());
                            buf.writeVarInt(e.amplifier());
                        }
                    },
                    buf -> {
                        boolean ended = buf.readBoolean();
                        double dmg = buf.readDouble();
                        int hits = buf.readInt();
                        int dur = buf.readInt();
                        int n = buf.readVarInt();
                        List<EffectLine> effects = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) {
                            effects.add(new EffectLine(buf.readUtf(), buf.readVarInt()));
                        }
                        return new TrainingDummyStatsPacket(ended, dmg, hits, dur, effects);
                    }
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
