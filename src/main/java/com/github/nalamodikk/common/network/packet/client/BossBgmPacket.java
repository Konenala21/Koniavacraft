package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.config.ModClientConfig;
import com.github.nalamodikk.register.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** S2C：通知 client 播放 boss BGM，客戶端用本地配置的 bossMusicVolume 控制音量。 */
public record BossBgmPacket() implements CustomPacketPayload {

    public static final BossBgmPacket INSTANCE = new BossBgmPacket();

    public static final Type<BossBgmPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "boss_bgm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BossBgmPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (!FMLEnvironment.dist.isClient()) return;
                    float vol = (float) ModClientConfig.INSTANCE.bossMusicVolume.get().doubleValue();
                    if (vol <= 0.0f) return; // 配置靜音直接跳過
                    Minecraft mc = Minecraft.getInstance();
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(
                            ModSounds.BOSS_BGM_MIRROR_IMAGE.get(), 1.0f, vol));
                }));
    }
}
