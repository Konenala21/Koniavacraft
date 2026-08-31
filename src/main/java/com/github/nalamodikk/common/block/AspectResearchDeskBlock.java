package com.github.nalamodikk.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 本源研究員村民的職業方塊。純方塊、無 BlockEntity(避免 POI 卡頓，見 ModBlocks 註冊處說明），
 * 只加 FACING 讓蕎麥麵的自訂模型能照放置方向轉。
 */
public class AspectResearchDeskBlock extends Block {

    public static final MapCodec<AspectResearchDeskBlock> CODEC = simpleCodec(AspectResearchDeskBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public AspectResearchDeskBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends AspectResearchDeskBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }
}
