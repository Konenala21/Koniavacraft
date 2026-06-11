package com.github.nalamodikk.mixin;

import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 飛船遮蔽 → 不算「在下雨」。Level.isRainingAt 用真實世界高度圖判斷,船方塊不在裡面 → 船上的人即使有頂
 * 也被算成淋雨(滅火/淋濕/雨聲)。攔回傳:本來判定有下雨、但該座標被某艘船遮著 → 改回 false。
 * 只在「本來就回 true(真的在那下雨)」時才搜尋船,平時不付出成本。
 */
@Mixin(Level.class)
public abstract class LevelRainMixin {

    @Inject(method = "isRainingAt", at = @At("RETURN"), cancellable = true)
    private void koniava$shipBlocksRain(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return; // 本來就沒在那下雨
        Level self = (Level) (Object) this;
        List<ShipEntity> near = self.getEntitiesOfClass(ShipEntity.class, new AABB(pos).inflate(0.5));
        for (ShipEntity s : near) {
            if (s.shelters(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
