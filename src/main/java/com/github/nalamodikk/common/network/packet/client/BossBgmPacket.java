package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.config.ModClientConfig;
import com.github.nalamodikk.register.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * S2C：通知 client 播放或停止 boss BGM。
 * stop=false → 開始播放（用本地配置 bossMusicVolume 音量）
 * stop=true  → 停止當前播放實例
 */
public record BossBgmPacket(boolean stop) implements CustomPacketPayload {

    public static final BossBgmPacket PLAY = new BossBgmPacket(false);
    public static final BossBgmPacket STOP = new BossBgmPacket(true);
    /** 向後相容：舊呼叫點仍可用 INSTANCE 觸發播放 */
    public static final BossBgmPacket INSTANCE = PLAY;

    public static final Type<BossBgmPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "boss_bgm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BossBgmPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, BossBgmPacket::stop, BossBgmPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // Client 側追蹤目前播放的 BGM instance，方便停止
    private static SoundInstance currentInstance;

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (!FMLEnvironment.dist.isClient()) return;
                    Minecraft mc = Minecraft.getInstance();
                    if (packet.stop) {
                        if (currentInstance != null) {
                            mc.getSoundManager().stop(currentInstance);
                            currentInstance = null;
                        }
                        return;
                    }
                    float vol = (float) ModClientConfig.INSTANCE.bossMusicVolume.get().doubleValue();
                    if (vol <= 0.0f) return; // 配置靜音直接跳過
                    // 重播前先停舊的（避免存檔重進時兩條 BGM 疊播）
                    if (currentInstance != null) {
                        mc.getSoundManager().stop(currentInstance);
                    }
                    SimpleSoundInstance inst = SimpleSoundInstance.forUI(
                            ModSounds.BOSS_BGM_MIRROR_IMAGE.get(), 1.0f, vol);
                    currentInstance = inst;
                    mc.getSoundManager().play(inst);
                }));
    }
}
