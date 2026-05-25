package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

public class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, KoniavacraftMod.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MANA_ALLOY =
            ARMOR_MATERIALS.register("mana_alloy", () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.HELMET,     2,
                            ArmorItem.Type.CHESTPLATE, 6,
                            ArmorItem.Type.LEGGINGS,   5,
                            ArmorItem.Type.BOOTS,      2,
                            ArmorItem.Type.BODY,       4
                    ),
                    15,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModItems.MANA_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_alloy")
                    )),
                    0.0f,
                    0.0f
            ));

    public static void register(IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
    }
}
