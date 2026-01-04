package com.github.nalamodikk.experimental.examples;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 魔法效果測試物品註冊
 * 
 * 註冊用於測試和演示魔法效果 API 的物品
 */
public class MagicEffectTestItems {
    
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(net.minecraft.core.registries.Registries.ITEM, KoniavacraftMod.MOD_ID);
    
    /**
     * 魔法陣測試工具
     */
    public static final Supplier<Item> MAGIC_CIRCLE_TEST = ITEMS.register("magic_circle_test", 
        () -> new MagicCircleTest(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.UNCOMMON)
        )
    );
    
    /**
     * 魔法效果除錯工具
     */
    public static final Supplier<Item> MAGIC_EFFECT_DEBUG = ITEMS.register("magic_effect_debug", 
        () -> new MagicEffectDebugTool(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.RARE)
        )
    );
    
    /**
     * 魔法效果清除工具
     */
    public static final Supplier<Item> MAGIC_EFFECT_CLEANER = ITEMS.register("magic_effect_cleaner", 
        () -> new MagicEffectCleaner(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.COMMON)
        )
    );
    
    /**
     * 註冊到模組事件總線
     */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}