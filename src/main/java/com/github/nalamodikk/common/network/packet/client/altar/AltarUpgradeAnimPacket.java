package com.github.nalamodikk.common.network.packet.client.altar;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.renderer.altar.AltarUpgradeAnimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record AltarUpgradeAnimPacket(BlockPos pos, int tier) implements CustomPacketPayload {

    public static final Type<AltarUpgradeAnimPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "altar_upgrade_anim"));

    private static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> POS_CODEC =
            StreamCodec.of((buf, p) -> buf.writeLong(p.asLong()), buf -> BlockPos.of(buf.readLong()));

    public static final StreamCodec<RegistryFriendlyByteBuf, AltarUpgradeAnimPacket> STREAM_CODEC =
            StreamCodec.composite(
                    POS_CODEC, AltarUpgradeAnimPacket::pos,
                    ByteBufCodecs.VAR_INT, AltarUpgradeAnimPacket::tier,
                    AltarUpgradeAnimPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        AltarUpgradeAnimManager.startAnimation(packet.pos(), packet.tier());
                    }
                }));
    }
}
