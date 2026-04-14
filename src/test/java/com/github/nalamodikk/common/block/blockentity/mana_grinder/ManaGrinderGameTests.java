package com.github.nalamodikk.common.block.blockentity.mana_grinder;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlocks;
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
 * ManaGrinder 功能測試：
 * - NBT 存取保留魔力與 IO 設定
 * - 放入石頭並補滿魔力後，磨完應輸出沙子
 * - 沒有魔力時，進度不應推進
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class ManaGrinderGameTests {

    private static final BlockPos POS = new BlockPos(1, 2, 1);

    // -------------------------------------------------------------------------
    // 1. NBT 存取：魔力與 IO 設定保留
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void grinderSaveLoadKeepsManaAndIoConfig(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_GRINDER.get());

        helper.runAtTickTime(2, () -> {
            ManaGrinderBlockEntity grinder = getGrinder(helper);

            int expectedMana = 4321;
            grinder.getManaStorage().setMana(expectedMana);
            grinder.setIOConfig(Direction.EAST, IOHandlerUtils.IOType.OUTPUT);
            grinder.setIOConfig(Direction.WEST, IOHandlerUtils.IOType.DISABLED);

            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            CompoundTag saved = new CompoundTag();
            grinder.saveAdditional(saved, registries);

            ManaGrinderBlockEntity reloaded = new ManaGrinderBlockEntity(
                    BlockPos.ZERO,
                    ModBlocks.MANA_GRINDER.get().defaultBlockState()
            );
            reloaded.loadAdditional(saved, registries);

            helper.assertTrue(
                    reloaded.getManaStorage().getManaStored() == expectedMana,
                    "重載後魔力應為 " + expectedMana + "，實際=" + reloaded.getManaStorage().getManaStored()
            );
            helper.assertTrue(
                    reloaded.getIOConfig(Direction.EAST) == IOHandlerUtils.IOType.OUTPUT,
                    "重載後 EAST IO 應為 OUTPUT"
            );
            helper.assertTrue(
                    reloaded.getIOConfig(Direction.WEST) == IOHandlerUtils.IOType.DISABLED,
                    "重載後 WEST IO 應為 DISABLED"
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 2. 放入石頭 + 補滿魔力，100 tick 後輸出槽應有沙子
    //    stone_grind.json: stone → sand, mana_cost=2000, processing_time=100
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 160)
    public static void stoneGrindsToSandWithMana(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_GRINDER.get());

        helper.runAtTickTime(2, () -> {
            ManaGrinderBlockEntity grinder = getGrinder(helper);
            // 補足夠的魔力（stone_grind 需要 2000）
            grinder.getManaStorage().setMana(10000);
            // 放入石頭，觸發配方偵測
            grinder.getItemHandler().setStackInSlot(0, new ItemStack(Items.STONE));
            grinder.markInputChanged();
        });

        // stone_grind processing_time=100，給 130 tick 緩衝
        helper.runAtTickTime(135, () -> {
            ManaGrinderBlockEntity grinder = getGrinder(helper);
            ItemStack output = grinder.getItemHandler().getStackInSlot(2); // OUTPUT_SLOT_1 = 2
            helper.assertTrue(
                    !output.isEmpty() && output.is(Items.SAND),
                    "100 tick 研磨後輸出槽應有沙子，實際=" + output
            );
            // 魔力應有被扣除（2000），剩餘應少於初始值 10000
            int manaLeft = grinder.getManaStorage().getManaStored();
            helper.assertTrue(
                    manaLeft < 10000,
                    "研磨過程魔力應被消耗，剩餘=" + manaLeft
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 3. 沒有魔力時，放入石頭後進度不應推進
    //    (50 tick 後 progress 應仍為 0)
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 80)
    public static void grinderDoesNotProgressWithoutMana(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_GRINDER.get());

        helper.runAtTickTime(2, () -> {
            ManaGrinderBlockEntity grinder = getGrinder(helper);
            // 不補魔力（預設 0）
            grinder.getItemHandler().setStackInSlot(0, new ItemStack(Items.STONE));
            grinder.markInputChanged();
        });

        helper.runAtTickTime(55, () -> {
            ManaGrinderBlockEntity grinder = getGrinder(helper);
            helper.assertTrue(
                    grinder.getProgress() == 0,
                    "沒有魔力時進度應維持 0，實際=" + grinder.getProgress()
            );
            // 輸出槽也不應有東西
            ItemStack output = grinder.getItemHandler().getStackInSlot(2);
            helper.assertTrue(
                    output.isEmpty(),
                    "沒有魔力時輸出槽應為空，實際=" + output
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 輔助方法
    // -------------------------------------------------------------------------
    private static ManaGrinderBlockEntity getGrinder(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(POS);
        if (!(be instanceof ManaGrinderBlockEntity grinder)) {
            helper.fail("預期 ManaGrinderBlockEntity at " + POS + "，實際=" + be);
            throw new AssertionError("unreachable");
        }
        return grinder;
    }
}
