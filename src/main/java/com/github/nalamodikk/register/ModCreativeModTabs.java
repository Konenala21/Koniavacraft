package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

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
                                ModItems.ITEMS.getEntries().forEach(item -> {
                                    if (item.get() instanceof BlockItem) {
                                        output.accept(item.get());
                                    }
                                });
                            })
                            .build());


    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
