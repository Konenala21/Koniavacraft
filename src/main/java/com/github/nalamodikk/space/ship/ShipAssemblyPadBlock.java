package com.github.nalamodikk.space.ship;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 飛船組裝台。右鍵 = 開組裝 GUI（同時掃描一次盒內飛船並回報）。
 * 組裝台投射建造盒（見 ShipAssemblyPadBlockEntity），界定哪些方塊算飛船。
 */
public class ShipAssemblyPadBlock extends BaseEntityBlock {

    public static final MapCodec<ShipAssemblyPadBlock> CODEC = simpleCodec(ShipAssemblyPadBlock::new);

    public ShipAssemblyPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ShipAssemblyPadBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShipAssemblyPadBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShipAssemblyPadBlockEntity pad) {
            pad.scan(); // 開介面前先掃一次，GUI 立刻看到現況
            player.openMenu(pad, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
