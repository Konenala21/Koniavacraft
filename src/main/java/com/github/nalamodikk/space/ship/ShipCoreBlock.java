package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 飛船核心方塊。右鍵 = 組裝飛船（M1：只跑 ShipContraption.assemble 並回報抓到的方塊）。
 * M2 之後這裡會改成「組裝 → 移除世界方塊 → 生成 ShipEntity」。
 */
public class ShipCoreBlock extends Block {

    public ShipCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            ShipContraption ship = new ShipContraption();
            boolean ok = ship.assemble(level, pos);
            if (ok) {
                String msg = "Ship assembled: " + ship.size() + " blocks, bounds "
                        + describe(ship.bounds());
                KoniavacraftMod.LOGGER.info("[ShipCore] {} (anchor {})", msg, pos);
                player.displayClientMessage(Component.literal(msg), false);
            } else {
                KoniavacraftMod.LOGGER.info("[ShipCore] assembly failed (too large or empty) at {}", pos);
                player.displayClientMessage(Component.literal("Ship assembly failed (too large or empty)"), false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static String describe(AABB b) {
        return String.format("%.0fx%.0fx%.0f", b.getXsize() + 1, b.getYsize() + 1, b.getZsize() + 1);
    }
}
