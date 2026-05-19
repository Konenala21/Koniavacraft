package com.github.nalamodikk.common.block.normal;


import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ManaSoilBlock extends Block {

    public ManaSoilBlock(Properties properties) {
        super(properties);
    }

    // 🎨 踩踏時產生魔力粒子
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);

        if (!level.isClientSide && entity instanceof Player) {
            // 10% 機率產生粒子效果
            if (level.random.nextFloat() < 0.1f) {
                ((ServerLevel) level).sendParticles(
                        ParticleTypes.ENCHANT,
                        pos.getX() + 0.5,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5,
                        3, 0.3, 0.1, 0.3, 0.02
                );

                // 輕微的魔力音效
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.BLOCKS, 0.1f, 1.5f);
            }
        }
    }



    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= 0.01f) return;

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);

        if (aboveState.isAir() && level.getMaxLocalRawBrightness(abovePos) >= 9) {
            level.setBlock(abovePos, ModBlocks.MANA_GRASS_BLOCK.get().defaultBlockState(), 3);
        }
    }
}
