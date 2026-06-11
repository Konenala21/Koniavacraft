package com.github.nalamodikk.mixin;

import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.space.ship.ShipShadowManager;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 飛船音效橋接：影子維度(ship_shadow)裡的方塊/機器聲音沒有玩家聽得到。攔截影子的 playSeededSound，
 * 找擁有該座標的船 → 在船的世界位置重播給真玩家。一次涵蓋所有 BE/方塊音效(機器運轉、漏斗、熔爐...)，
 * 不必一個個做。非影子維度 / 沒對應船時原樣放行。
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelSoundBridgeMixin {

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), cancellable = true)
    private void koniava$bridgeShadowSound(@Nullable Player player, double x, double y, double z,
                                           Holder<SoundEvent> sound, SoundSource source, float vol, float pitch,
                                           long seed, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (self.dimension() != ModDimensions.SHIP_SHADOW) return;
        if (ShipShadowManager.forwardShadowSound(x, y, z, sound, source, vol, pitch, seed)) ci.cancel();
    }
}
