package com.github.nalamodikk.common.block.normal;

import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class DeepManaSoilBlock extends Block {

    public DeepManaSoilBlock(Properties properties) {
        super(properties);
    }

    // 🔄 深層魔力土壤的特殊能力：向上滲透魔力
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);

        // 非常緩慢地向上傳播魔力能量 (0.05% 機率)
        if (random.nextFloat() < 0.0005f) {
            BlockPos abovePos = pos.above();
            BlockState aboveState = level.getBlockState(abovePos);

            // 如果上方是普通土壤，轉換為魔力土壤
            if (aboveState.is(net.minecraft.world.level.block.Blocks.DIRT)) {
                level.setBlock(abovePos, ModBlocks.MANA_SOIL.get().defaultBlockState(), 3);
            }
            // 如果上方是魔力土壤且有光照，轉換為魔力草
            else if (aboveState.is(ModBlocks.MANA_SOIL.get()) &&
                    level.getMaxLocalRawBrightness(abovePos.above()) >= 9) {
                level.setBlock(abovePos, ModBlocks.MANA_GRASS_BLOCK.get().defaultBlockState(), 3);
            }
        }
    }
}