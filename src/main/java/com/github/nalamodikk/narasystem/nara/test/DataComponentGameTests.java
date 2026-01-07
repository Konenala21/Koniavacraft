package com.github.nalamodikk.narasystem.nara.test;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class DataComponentGameTests {

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID)
    public static void testManaComponents(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        
        // 測試寫入
        stack.set(ModDataComponents.MANA_STORED, 500);
        stack.set(ModDataComponents.MAX_MANA, 1000);
        
        // 測試讀取
        Integer stored = stack.get(ModDataComponents.MANA_STORED);
        Integer max = stack.get(ModDataComponents.MAX_MANA);
        
        if (stored == null || stored != 500) {
            helper.fail("MANA_STORED 組件讀寫失敗");
        }
        if (max == null || max != 1000) {
            helper.fail("MAX_MANA 組件讀寫失敗");
        }
        
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID)
    public static void testNaraImprintComponent(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(ModDataComponents.NARA_IMPRINT, true);
        
        Boolean imprint = stack.get(ModDataComponents.NARA_IMPRINT);
        if (imprint == null || !imprint) {
            helper.fail("NARA_IMPRINT 組件讀寫失敗");
        }
        
        helper.succeed();
    }
}
