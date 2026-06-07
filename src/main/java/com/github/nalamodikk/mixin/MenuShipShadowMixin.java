package com.github.nalamodikk.mixin;

import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 讓綁在飛船影子維度 BE 的選單跨維度也保持有效。多數機器選單用靜態 stillValid(access, player, block)
 * 做距離檢查；玩家在正常世界、機器在影子維度，距離永遠超標 → 選單秒關。這裡偵測 access 指向 ship_shadow
 * 就回 true（跳過距離），讓玩家能操作真正在運轉的影子機器（VM3）。
 */
@Mixin(AbstractContainerMenu.class)
public class MenuShipShadowMixin {

    @Inject(
            method = "stillValid(Lnet/minecraft/world/inventory/ContainerLevelAccess;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/Block;)Z",
            at = @At("HEAD"), cancellable = true
    )
    private static void koniava$shipShadowAlwaysValid(ContainerLevelAccess access, Player player, Block block,
                                                      CallbackInfoReturnable<Boolean> cir) {
        boolean isShadow = access.evaluate(
                (level, pos) -> level.dimension().equals(ModDimensions.SHIP_SHADOW), false);
        if (isShadow) cir.setReturnValue(true);
    }
}
