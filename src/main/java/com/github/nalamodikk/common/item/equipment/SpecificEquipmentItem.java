package com.github.nalamodikk.common.item.equipment;

import com.github.nalamodikk.common.player.equipment.EquipmentType;
import com.github.nalamodikk.common.player.equipment.ISpecificEquipment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// SpecificEquipmentItem.java
public class SpecificEquipmentItem extends Item implements ISpecificEquipment {
    private final EquipmentType equipmentType;

    public SpecificEquipmentItem(EquipmentType type, Properties properties) {
        super(properties);
        this.equipmentType = type;
    }

    @Override
    public EquipmentType getEquipmentType() {
        return equipmentType;
    }
    @Override
    public Component getName(ItemStack stack) {
        return equipmentType.getDisplayName();
    }

}