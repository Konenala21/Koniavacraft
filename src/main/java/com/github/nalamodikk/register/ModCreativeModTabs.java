package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.manabase.BaseMachineBlock;
import com.github.nalamodikk.common.item.equipment.armor.ArmorCapacityUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ArmorDefenseUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ChestplateUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.HelmetUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.LeggingsUpgradeItem;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeItem;
import com.github.nalamodikk.common.item.equipment.boots.BootsUpgradeItem;
import com.github.nalamodikk.common.item.wand.core.WandCoreItem;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ModCreativeModTabs {

    // 開發者專用物品的 registry path，這些會從一般欄位排除、只出現在 dev tab
    private static final Set<String> DEV_ITEM_PATHS = Set.of(
            "mana_debug_tool",
            "dev_render_test_1",
            "dev_render_test_2",
            "dev_render_test_3",
            "dev_render_test_4",
            // deprecated wands — replaced by wand_rod modular system
            "basic_tech_wand",
            "advanced_tech_wand",
            "ritual_wand",
            "structure_build_wand"
    );

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB , KoniavacraftMod.MOD_ID);

    public static final Supplier<CreativeModeTab> koniava_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("koniava_items_tab", () ->
                    CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.MANA_DUST.get()))
                            .title(Component.translatable("creativetab.koniava_items"))
                            .displayItems((parameters, output) -> {
                                List<Item> upgradeItems = new ArrayList<>();
                                ModItems.ITEMS.getEntries().forEach(item -> {
                                    if (item.get() instanceof BlockItem) return;
                                    String path = item.getId().getPath();
                                    if (DEV_ITEM_PATHS.contains(path)) return;
                                    Item i = item.get();
                                    if (i instanceof WandCoreItem || i instanceof WandUpgradeItem || i instanceof BootsUpgradeItem
                            || i instanceof HelmetUpgradeItem || i instanceof ChestplateUpgradeItem || i instanceof LeggingsUpgradeItem
                            || i instanceof ArmorCapacityUpgradeItem || i instanceof ArmorDefenseUpgradeItem
                            || i instanceof TurretUpgradeItem) {
                                        upgradeItems.add(i);
                                    } else {
                                        output.accept(i);
                                    }
                                });
                                upgradeItems.forEach(output::accept);
                            })
                            .build());

    public static final Supplier<CreativeModeTab> koniava_DEV_TAB =
            CREATIVE_MODE_TABS.register("koniava_dev_tab", () ->
                    CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.MANA_DEBUG_TOOL.get()))
                            .title(Component.translatable("creativetab.koniava_dev"))
                            .displayItems((parameters, output) -> {
                                ModItems.ITEMS.getEntries().forEach(item -> {
                                    String path = item.getId().getPath();
                                    if (DEV_ITEM_PATHS.contains(path)) {
                                        output.accept(item.get());
                                    }
                                });

                                // 結構相關原版方塊
                                output.accept(Items.STRUCTURE_BLOCK);
                                output.accept(Items.STRUCTURE_VOID);
                                output.accept(Items.JIGSAW);
                            })
                            .build());


    public static final Supplier<CreativeModeTab> koniava_BLOCKS_TAB =
            CREATIVE_MODE_TABS.register("koniava_blocks_tab", () ->
                    CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModBlocks.MANA_BLOCK.get()))
                            .title(Component.translatable("creativetab.koniava_blocks"))
                            .displayItems((parameters, output) -> {
                                List<Item> machineItems = new ArrayList<>();
                                List<Item> otherBlockItems = new ArrayList<>();

                                ModItems.ITEMS.getEntries().forEach(item -> {
                                    if (item.get() instanceof BlockItem blockItem) {
                                        String path = blockItem.getBlock().builtInRegistryHolder().key().location().getPath();
                                        if ("arcane_conduit".equals(path) || "altar_pillar".equals(path) || "resonance_ring".equals(path)
                                                || "mana_warp_engine".equals(path) || "mana_warp_input".equals(path)) { // 曲速大結構部件,先不開放
                                            return;
                                        }

                                        if (blockItem.getBlock() instanceof BaseMachineBlock) {
                                            machineItems.add(item.get());
                                        } else {
                                            otherBlockItems.add(item.get());
                                        }
                                    }
                                });

                                machineItems.forEach(output::accept);
                                otherBlockItems.forEach(output::accept);
                            })
                            .build());


    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
