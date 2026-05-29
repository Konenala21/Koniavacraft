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
    /**
     * 用真實存在的 blank_core 配方（3x3 shaped，mana_cost=600）：
     * 　W　 / CIC / 　W　   W=mana_wire, C=mana_crystal, I=mana_ingot → blank_core
     */
    private static final int BLANK_CORE_RECIPE_COST = 600;

    /** 依 blank_core pattern 放滿 9 格輸入（slot 1/3/4/5/7）。 */
    private static void placeBlankCoreIngredients(ManaCraftingTableBlockEntity table) {
        var h = table.getItemHandler();
        h.setStackInSlot(1, new ItemStack(ModItems.MANA_WIRE.get()));
        h.setStackInSlot(3, new ItemStack(ModItems.MANA_CRYSTAL.get()));
        h.setStackInSlot(4, new ItemStack(ModItems.MANA_INGOT.get()));
        h.setStackInSlot(5, new ItemStack(ModItems.MANA_CRYSTAL.get()));
        h.setStackInSlot(7, new ItemStack(ModItems.MANA_WIRE.get()));
    }

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
    // 2. 放入 blank_core 材料 + 足夠魔力，updateCraftingResult 應在輸出槽放出 blank_core
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void craftingResultAppearsWithSufficientMana(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get());

        helper.runAtTickTime(2, () -> {
            ManaCraftingTableBlockEntity table = getTable(helper);
            // 放入材料並補足魔力
            placeBlankCoreIngredients(table);
            table.getManaStorage().setMana(BLANK_CORE_RECIPE_COST + 500);
            // 手動觸發配方更新（模擬 serverTick）
            table.updateCraftingResult();

            ItemStack output = table.getItemHandler().getStackInSlot(ManaCraftingTableBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(
                    !output.isEmpty() && output.is(ModItems.BLANK_CORE.get()),
                    "有材料且魔力充足時，輸出槽應顯示 blank_core，實際=" + output
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 3. 放入相同的完整 blank_core 材料，但魔力為 0，輸出槽應為空
    //    （配方符合但魔力不足 → 不出貨，這才是這個 case 真正要驗的事）
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void craftingResultEmptyWithoutMana(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get());

        helper.runAtTickTime(2, () -> {
            ManaCraftingTableBlockEntity table = getTable(helper);
            // 有完整材料但沒有魔力（mana 預設為 0，不補充）
            placeBlankCoreIngredients(table);
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
