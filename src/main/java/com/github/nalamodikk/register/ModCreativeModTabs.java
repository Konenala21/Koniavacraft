package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ModCreativeModTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB , KoniavacraftMod.MOD_ID);

    public static final Supplier<CreativeModeTab> koniava_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("koniava_items_tab", () ->
                    CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.MANA_DUST.get()))
                            .title(Component.translatable("creativetab.koniava_items"))
                            .displayItems((parameters, output) -> {
                                ModItems.ITEMS.getEntries().forEach(item -> {
                                    if (!(item.get() instanceof BlockItem)) {
                                        output.accept(item.get());
                                    }
                                });
                            })
                            .build());


    public static final Supplier<CreativeModeTab> koniava_BLOCKS_TAB =
            CREATIVE_MODE_TABS.register("koniava_blocks_tab", () ->
                    CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModBlocks.MANA_BLOCK.get()))
                            .title(Component.translatable("creativetab.koniava_blocks"))
                            .displayItems((parameters, output) -> {
                                Set<Item> addedItems = new HashSet<>();

                                // 機器群組（集中放一起）
                                Item machineCraftingTable = ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get().asItem();
                                Item machineGenerator = ModBlocks.MANA_GENERATOR.get().asItem();
                                Item machineSolarCollector = ModBlocks.SOLAR_MANA_COLLECTOR.get().asItem();
                                Item machineInfuser = ModBlocks.MANA_INFUSER.get().asItem();
                                Item machineGrinder = ModBlocks.MANA_GRINDER.get().asItem();
                                Item machineConduitBasic = ModBlocks.BASIC_ARCANE_CONDUIT.get().asItem();
                                Item machineConduitAdvanced = ModBlocks.ADVANCED_ARCANE_CONDUIT.get().asItem();
                                Item machineConduitElite = ModBlocks.ELITE_ARCANE_CONDUIT.get().asItem();
                                Item machineConduitLegacy = ModBlocks.ARCANE_CONDUIT.get().asItem();

                                output.accept(machineCraftingTable);
                                addedItems.add(machineCraftingTable);
                                output.accept(machineGenerator);
                                addedItems.add(machineGenerator);
                                output.accept(machineSolarCollector);
                                addedItems.add(machineSolarCollector);
                                output.accept(machineInfuser);
                                addedItems.add(machineInfuser);
                                output.accept(machineGrinder);
                                addedItems.add(machineGrinder);
                                output.accept(machineConduitBasic);
                                addedItems.add(machineConduitBasic);
                                output.accept(machineConduitAdvanced);
                                addedItems.add(machineConduitAdvanced);
                                output.accept(machineConduitElite);
                                addedItems.add(machineConduitElite);
                                output.accept(machineConduitLegacy);
                                addedItems.add(machineConduitLegacy);

                                // 其餘方塊（資源 / 生態）
                                Item manaBlock = ModBlocks.MANA_BLOCK.get().asItem();
                                Item magicOre = ModBlocks.MAGIC_ORE.get().asItem();
                                Item deepslateMagicOre = ModBlocks.DEEPSLATE_MAGIC_ORE.get().asItem();
                                Item manaSoil = ModBlocks.MANA_SOIL.get().asItem();
                                Item deepManaSoil = ModBlocks.DEEP_MANA_SOIL.get().asItem();
                                Item manaGrassBlock = ModBlocks.MANA_GRASS_BLOCK.get().asItem();
                                Item manaBloom = ModBlocks.MANA_BLOOM.get().asItem();

                                output.accept(manaBlock);
                                addedItems.add(manaBlock);
                                output.accept(magicOre);
                                addedItems.add(magicOre);
                                output.accept(deepslateMagicOre);
                                addedItems.add(deepslateMagicOre);
                                output.accept(manaSoil);
                                addedItems.add(manaSoil);
                                output.accept(deepManaSoil);
                                addedItems.add(deepManaSoil);
                                output.accept(manaGrassBlock);
                                addedItems.add(manaGrassBlock);
                                output.accept(manaBloom);
                                addedItems.add(manaBloom);

                                // fallback：新增方塊若未手動排序，仍自動顯示在最後
                                ModItems.ITEMS.getEntries().forEach(item -> {
                                    if (item.get() instanceof BlockItem && addedItems.add(item.get())) {
                                        output.accept(item.get());
                                    }
                                });
                            })
                            .build());


    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
