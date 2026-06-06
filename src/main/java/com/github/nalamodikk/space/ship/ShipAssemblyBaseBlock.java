package com.github.nalamodikk.space.ship;

import net.minecraft.world.level.block.Block;

/**
 * 飛船組裝底座：鋪在地上的地板。組裝台 flood-fill 連在一起的底座，取其外接矩形當建造盒的
 * 水平範圍(footprint)。底座可以是不規則形狀，範圍用 bounding box。
 */
public class ShipAssemblyBaseBlock extends Block {
    public ShipAssemblyBaseBlock(Properties properties) {
        super(properties);
    }
}
