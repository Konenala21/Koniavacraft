package com.github.nalamodikk.space.ship;

import net.minecraft.world.level.block.Block;

/**
 * 飛船魔力引擎方塊：推進力來源。組裝必要骨架之一（≥1 才能啟動）。
 * 引擎數量決定飛船的速度上限（越多越快），玩家油門決定實際用多少。
 * 單純方塊，跟船走；之後再加 tier / 噴射粒子。
 */
public class ManaEngineBlock extends Block {
    public ManaEngineBlock(Properties properties) {
        super(properties);
    }
}
