package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 分身 boss 死亡演出期間不套 vanilla 紅色受傷/死亡 overlay。
 *
 * LivingEntityRenderer.getOverlayCoords 的紅色來自 hurtTime>0 || deathTime>0；自製死亡動畫整段
 * deathTime 都 >0，於是 vanilla 把整個死亡演出染紅，蓋掉本來的視覺。這裡只在 PlayerCloneEntity
 * 的死亡階段（getDeathPhase()>0）回 NO_OVERLAY，平時受傷的紅閃與其他所有 entity 完全不受影響。
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void koniava$suppressCloneDeathOverlay(LivingEntity entity, float u,
                                                          CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof PlayerCloneEntity clone && clone.getDeathPhase() > 0) {
            cir.setReturnValue(OverlayTexture.NO_OVERLAY);
        }
    }
}
