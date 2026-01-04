package com.github.nalamodikk.common.block.normal;

import com.github.nalamodikk.register.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * 🌼 魔力花方塊，專門用來在地表隨機生成。
 * <p>
 * - 無碰撞體積，玩家可直接穿過。<br>
 * - 可放置於一般草方塊、泥土以及模組自訂的魔力土壤上。<br>
 * - 採收時透過戰利品表掉落魔力素材。<br>
 * </p>
 */
public class ManaBloomBlock extends BushBlock {

    /**
     * NeoForge 1.21 需要提供 MapCodec，讓方塊狀態可以序列化。
     */
    public static final MapCodec<ManaBloomBlock> CODEC = Block.simpleCodec(ManaBloomBlock::new);

    /**
     * 提供給資料序列化使用的建構子。
     *
     * @param properties 由 NeoForge 建立的屬性集合
     */
    public ManaBloomBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * 預設建構子，用於註冊時快速建立魔力花。
     */
    public ManaBloomBlock() {
        this(defaultProperties());
    }

    /**
     * 回傳魔力花的預設屬性設定。
     */
    private static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)              // 使用植物色調
                .noCollission()                       // 無碰撞方塊
                .instabreak()                         // 一擊即可破壞
                .sound(SoundType.GRASS)               // 草地踩踏音效
                .offsetType(BlockBehaviour.OffsetType.XZ); // 隨機微移，讓外觀更自然
    }

    /**
     * 傳回專屬的 MapCodec，支援資料驅動載入。
     */
    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    /**
     * 判斷魔力花是否可以放置在指定方塊上。
     *
     * @param state   目標方塊狀態
     * @param level   世界讀取介面
     * @param pos     方塊座標
     * @return 是否允許放置
     */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // 允許放置在一般泥土類、草地以及模組自製土壤上
        return state.is(BlockTags.DIRT) ||
                state.is(BlockTags.SAND) ||
                state.is(ModBlocks.MANA_SOIL.get()) ||
                state.is(ModBlocks.DEEP_MANA_SOIL.get()) ||
                state.is(ModBlocks.MANA_GRASS_BLOCK.get());
    }
}
