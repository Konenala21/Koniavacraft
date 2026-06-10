package com.github.nalamodikk.space.ship;

import net.minecraft.world.level.block.Block;

/**
 * 飛船曲速引擎（T2 引擎）：高速 tier。每顆速度貢獻遠高於一般引擎，且在場時把速度上限從 200 拉到曲速段（600 格/秒）。
 * 代價是吃燃料兇很多 → 實務上要靠高密度燃料(曲速燃料)才撐得住。也算「引擎」滿足組裝骨架。
 * 單純方塊，跟船走;之後再加噴口/扭曲粒子。
 */
public class ManaWarpEngineBlock extends Block {
    public ManaWarpEngineBlock(Properties properties) {
        super(properties);
    }
}
