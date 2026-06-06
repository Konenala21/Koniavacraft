package com.github.nalamodikk.space.ship;

import net.minecraft.world.level.block.Block;

/**
 * 飛船組裝架：往上立的框架柱（像火箭發射台的龍門吊架）。組裝台取 footprint 內最高的組裝架，
 * 它的高度就是建造盒的高度。同時是視覺：讓玩家看到要蓋多高、蓋在哪個範圍。
 */
public class ShipAssemblyGantryBlock extends Block {
    public ShipAssemblyGantryBlock(Properties properties) {
        super(properties);
    }
}
