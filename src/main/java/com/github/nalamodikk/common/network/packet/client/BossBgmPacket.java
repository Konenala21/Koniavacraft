package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.config.ModClientConfig;
import com.github.nalamodikk.register.ModSounds;
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
 *
 * 注意：所有 SoundInstance / Minecraft 相關 client-only 類別載入都封進
 * 巢狀 ClientHandler。dedicated server 載入本類別時不會觸發 ClientHandler 的驗證，
 * 因此不會誤抓 net.minecraft.client.resources.sounds.SoundInstance。
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

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (!FMLEnvironment.dist.isClient()) return;
                    ClientHandler.handle(packet);
                }));
    }

    /**
     * 所有 client-only 類別載入都集中在這個巢狀類別。
     * Java 規範：巢狀類別只在第一次被參照時載入，所以 dedicated server 永遠不會載入此類別，
     * 也就不會觸發 SoundInstance / Minecraft 等 client-only class 的 verifier 連鎖載入。
     */
    private static final class ClientHandler {
        private static net.minecraft.client.resources.sounds.SoundInstance currentInstance;

        static void handle(BossBgmPacket packet) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
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
            // 用 tickable instance：音量每 tick 重讀 bossMusicVolume config，拖滑桿即時生效、歸 0 即時停。
            // 保留 SoundSource.MASTER（vanilla「音樂」slider 關掉時 boss BGM 仍會播）+ relative/NONE（全域固定音量）。
            BgmInstance inst = new BgmInstance(ModSounds.BOSS_BGM_MIRROR_IMAGE.get());
            currentInstance = inst;
            mc.getSoundManager().play(inst);
        }

        // 戰鬥 BGM。音量不寫死，每 tick 跟 bossMusicVolume config 走 → 設定即時生效，不用等下一場。
        private static final class BgmInstance extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
            BgmInstance(net.minecraft.sounds.SoundEvent sound) {
                super(sound, net.minecraft.sounds.SoundSource.MASTER,
                        net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom());
                this.relative = true;
                this.attenuation = net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE;
                this.volume = currentConfigVolume();
            }

            private static float currentConfigVolume() {
                return (float) ModClientConfig.INSTANCE.bossMusicVolume.get().doubleValue();
            }

            @Override
            public void tick() {
                float v = currentConfigVolume();
                if (v <= 0.0f) {
                    this.stop(); // 拖到 0 即時停止
                    return;
                }
                if (v != this.volume) { // 只有滑桿真的動了才寫，沒變就跳過
                    this.volume = v;
                }
            }
        }
    }
}
