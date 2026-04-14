package com.github.nalamodikk.common.block.blockentity.mana_crafting;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * ManaCraftingTable 功能測試：
 * - NBT 存取保留魔力與 IO 設定
 * - 放入材料 + 足夠魔力時，輸出槽顯示合成結果
 * - 魔力不足時，輸出槽應為空
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class ManaCraftingTableGameTests {

    private static final BlockPos POS = new BlockPos(1, 2, 1);
    /** 配方：slot 0 放 1 顆 diamond → mana_dust，mana_cost=1500 */
    private static final int MANA_DUST_RECIPE_COST = 1500;

    // -------------------------------------------------------------------------
    // 1. NBT 存取：魔力與 IO 設定保留
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void tableSaveLoadKeepsManaAndIoConfig(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get());

        helper.runAtTickTime(2, () -> {
            ManaCraftingTableBlockEntity table = getTable(helper);

            int expectedMana = 9876;
            table.getManaStorage().setMana(expectedMana);
            table.setIOConfig(Direction.SOUTH, IOHandlerUtils.IOType.OUTPUT);
            table.setIOConfig(Direction.UP, IOHandlerUtils.IOType.DISABLED);

            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            CompoundTag saved = new CompoundTag();
            table.saveAdditional(saved, registries);

            ManaCraftingTableBlockEntity reloaded = new ManaCraftingTableBlockEntity(
                    BlockPos.ZERO,
                    ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get().defaultBlockState()
            );
            reloaded.loadAdditional(saved, registries);

            helper.assertTrue(
                    reloaded.getManaStored() == expectedMana,
                    "重載後魔力應為 " + expectedMana + "，實際=" + reloaded.getManaStored()
            );
            helper.assertTrue(
                    reloaded.getIOConfig(Direction.SOUTH) == IOHandlerUtils.IOType.OUTPUT,
                    "重載後 SOUTH IO 應為 OUTPUT"
            );
            helper.assertTrue(
                    reloaded.getIOConfig(Direction.UP) == IOHandlerUtils.IOType.DISABLED,
                    "重載後 UP IO 應為 DISABLED"
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 2. 放入 diamond + 足夠魔力，updateCraftingResult 應在輸出槽放出 mana_dust
    //    mana_dust.json: slot 0 = diamond, mana_cost = 1500, result = mana_dust
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void craftingResultAppearsWithSufficientMana(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get());

        helper.runAtTickTime(2, () -> {
            ManaCraftingTableBlockEntity table = getTable(helper);
            // 放入材料並補足魔力
            table.getItemHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
            table.getManaStorage().setMana(MANA_DUST_RECIPE_COST + 500);
            // 手動觸發配方更新（模擬 serverTick）
            table.updateCraftingResult();

            ItemStack output = table.getItemHandler().getStackInSlot(ManaCraftingTableBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(
                    !output.isEmpty() && output.is(ModItems.MANA_DUST.get()),
                    "有材料且魔力充足時，輸出槽應顯示 mana_dust，實際=" + output
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 3. 同樣放入 diamond，但魔力為 0，輸出槽應為空
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void craftingResultEmptyWithoutMana(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get());

        helper.runAtTickTime(2, () -> {
            ManaCraftingTableBlockEntity table = getTable(helper);
            // 有材料但沒有魔力
            table.getItemHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
            // mana 預設為 0，不額外補充
            table.updateCraftingResult();

            ItemStack output = table.getItemHandler().getStackInSlot(ManaCraftingTableBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(
                    output.isEmpty(),
                    "魔力不足時輸出槽應為空，實際=" + output
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 輔助方法
    // -------------------------------------------------------------------------
    private static ManaCraftingTableBlockEntity getTable(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(POS);
        if (!(be instanceof ManaCraftingTableBlockEntity table)) {
            helper.fail("預期 ManaCraftingTableBlockEntity at " + POS + "，實際=" + be);
            throw new AssertionError("unreachable");
        }
        return table;
    }
}
