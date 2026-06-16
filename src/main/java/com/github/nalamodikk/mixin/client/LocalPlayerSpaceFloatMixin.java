package com.github.nalamodikk.mixin.client;

import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 太空零重力漂浮(玩家在太空、徒步時)。玩家走路是 client 權威(client 算位置送 server),所以自訂物理只做在
 * LocalPlayer。阻尼漂浮:按鍵加速、放開慢慢滑停(輕摩擦);W 朝視線方向(含俯仰)→ 全 3D,跳=上、Shift=下。
 * 騎船(乘客定位另管)/創造飛行/旁觀 → 交給 vanilla,不接管。
 *
 * <p>client mixin,只在客戶端套用;再用 instanceof LocalPlayer gate 只接管本地玩家(其他實體走 vanilla travel)。
 */
@Mixin(LivingEntity.class)
public abstract class LocalPlayerSpaceFloatMixin {

    private static final double SPACE_FLOAT_ACCEL = 0.025;   // 每 tick 推力
    private static final double SPACE_FLOAT_DAMPING = 0.9;   // 阻尼(終速 ≈ accel/(1-damping) ≈ 0.25/tick ≈ 5 b/s)

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void koniava$spaceFloat(Vec3 travelVector, CallbackInfo ci) {
        if (!((Object) this instanceof LocalPlayer player)) return;
        if (!player.level().dimension().equals(ModDimensions.SPACE)) return;
        if (player.isPassenger() || player.getAbilities().flying || player.isSpectator()) return;

        Vec3 look = player.getLookAngle();                       // 視線單位向量(含俯仰),W 朝這裡
        // 水平 strafe 軸(不隨俯仰傾斜):look 繞 Y 轉 90°。正負號實機反了就翻。
        Vec3 strafe = new Vec3(look.z, 0.0, -look.x);
        if (strafe.lengthSqr() < 1e-6) strafe = new Vec3(1, 0, 0); // 直視正上/下時退化,給保底
        else strafe = strafe.normalize();
        Vec3 up = new Vec3(0, 1, 0);

        double vert = (player.input.jumping ? 1 : 0) - (player.input.shiftKeyDown ? 1 : 0);
        Vec3 thrust = look.scale(player.zza).add(strafe.scale(player.xxa)).add(up.scale(vert));
        thrust = thrust.lengthSqr() > 1e-6 ? thrust.normalize().scale(SPACE_FLOAT_ACCEL) : Vec3.ZERO;

        Vec3 vel = player.getDeltaMovement().scale(SPACE_FLOAT_DAMPING).add(thrust);
        player.setDeltaMovement(vel);
        player.move(MoverType.SELF, vel);    // 仍走碰撞:撞得到船/星球,不穿牆
        player.fallDistance = 0f;            // 漂浮不累積墜落傷害
        ci.cancel();                          // 取代 vanilla 走路物理
    }
}
