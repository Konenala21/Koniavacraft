package com.github.nalamodikk.mixin;

import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 箱子/木桶等容器 BE 的 stillValid 走的是 Container.stillValidBlockEntity(BE, player[, dist])，
 * 它做距離檢查；玩家在正常世界、容器 BE 在影子維度，距離永遠超標 → GUI 秒關。
 * 偵測 BE 在 ship_shadow 就回 true（跨維度容器 GUI 不秒關，配合 MenuShipShadowMixin 蓋機器選單）。
 */
@Mixin(Container.class)
public interface ContainerShipShadowMixin {

    @Inject(
            method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;F)Z",
            at = @At("HEAD"), cancellable = true
    )
    private static void koniava$shadowValidF(BlockEntity be, Player player, float distance,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (be.getLevel() != null && be.getLevel().dimension().equals(ModDimensions.SHIP_SHADOW)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("HEAD"), cancellable = true
    )
    private static void koniava$shadowValid(BlockEntity be, Player player,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (be.getLevel() != null && be.getLevel().dimension().equals(ModDimensions.SHIP_SHADOW)) {
            cir.setReturnValue(true);
        }
    }
}
