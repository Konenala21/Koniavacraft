package com.github.nalamodikk.common.player.equipment;

import net.minecraft.world.entity.player.Player;

// ISpecificEquipment.java
public interface ISpecificEquipment {
    EquipmentType getEquipmentType();
    default void applyEffects(Player player) {}
    default void removeEffects(Player player) {}
}