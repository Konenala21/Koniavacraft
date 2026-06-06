package com.github.nalamodikk.space.ship;

import net.minecraft.world.level.block.Block;

/**
 * 飛船座椅：組進飛船的乘客座位（它是飛船的一部分，會被組裝、跟船一起移動）。
 * 組裝後每個座椅方塊對應一個 passenger 位置；核心本身是駕駛位（舵）。
 */
public class ShipSeatBlock extends Block {
    public ShipSeatBlock(Properties properties) {
        super(properties);
    }
}
