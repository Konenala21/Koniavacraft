package com.github.nalamodikk.mixin;

import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class SpaceVoidDamageMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void cancelVoidDamageInSpace(DamageSource source, float amount,
                                         CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && self.level().dimension().equals(ModDimensions.SPACE)) {
            cir.setReturnValue(false); // 取消整個 hurt()，不觸發動畫也不扣血
        }
    }
}
