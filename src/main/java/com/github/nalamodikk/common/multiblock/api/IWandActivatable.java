package com.github.nalamodikk.common.multiblock.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public interface IWandActivatable {
    Component onWandActivate(Player player);
}
