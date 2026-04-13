package com.github.nalamodikk.common.block.blockentity.mana_generator;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_generator.logic.ManaGeneratorStateManager;
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
 * ManaGenerator 功能測試：
 * - 燃燒煤炭產生魔力（MANA 模式）
 * - 燃燒煤炭產生能量（ENERGY 模式）
 * - 魔力儲量已滿時發電機暫停燃燒
 * - NBT 存取保留所有狀態
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class ManaGeneratorGameTests {

    private static final BlockPos POS = new BlockPos(1, 2, 1);

    // -------------------------------------------------------------------------
    // 1. 放入煤炭後，發電機應在數 tick 內開始產生魔力
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void coalGeneratesManaInManaMode(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_GENERATOR.get().defaultBlockState());

        helper.runAtTickTime(2, () -> {
            ManaGeneratorBlockEntity gen = getGenerator(helper);
            // 確認預設是 MANA 模式，然後塞入煤炭
            helper.assertTrue(
                    gen.getStateManager().getCurrentMode() == ManaGeneratorStateManager.Mode.MANA,
                    "預設應為 MANA 模式"
            );
            gen.getFuelHandler().setStackInSlot(0, new ItemStack(Items.COAL));
        });

        // 等夠久讓燃料被消耗並產生魔力（tryConsumeFuel 每 4 tick 嘗試一次，之後每 tick 產魔）
        helper.runAtTickTime(40, () -> {
            ManaGeneratorBlockEntity gen = getGenerator(helper);
            helper.assertTrue(
                    gen.getManaStorage().getManaStored() > 0,
                    "燒了 38 tick 後魔力應大於 0，實際=" + gen.getManaStorage().getManaStored()
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 2. ENERGY 模式下，煤炭應產生能量而非魔力
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void coalGeneratesEnergyInEnergyMode(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_GENERATOR.get().defaultBlockState());

        helper.runAtTickTime(2, () -> {
            ManaGeneratorBlockEntity gen = getGenerator(helper);
            // 切換到 ENERGY 模式再塞燃料
            gen.toggleMode();
            helper.assertTrue(
                    gen.getStateManager().getCurrentMode() == ManaGeneratorStateManager.Mode.ENERGY,
                    "切換後應為 ENERGY 模式"
            );
            gen.getFuelHandler().setStackInSlot(0, new ItemStack(Items.COAL));
        });

        helper.runAtTickTime(40, () -> {
            ManaGeneratorBlockEntity gen = getGenerator(helper);
            helper.assertTrue(
                    gen.getEnergyStorage().getEnergyStored() > 0,
                    "ENERGY 模式下能量應大於 0，實際=" + gen.getEnergyStorage().getEnergyStored()
            );
            // MANA 模式關閉時魔力不應增加
            helper.assertTrue(
                    gen.getManaStorage().getManaStored() == 0,
                    "ENERGY 模式下魔力應保持 0，實際=" + gen.getManaStorage().getManaStored()
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 3. 魔力儲量滿時，發電機應暫停（不繼續消耗燃料的燃燒時間）
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 80)
    public static void generatorPausesWhenManaStorageFull(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_GENERATOR.get().defaultBlockState());

        helper.runAtTickTime(2, () -> {
            ManaGeneratorBlockEntity gen = getGenerator(helper);
            // 填滿魔力儲量，再放入煤炭
            gen.getManaStorage().setMana(ManaGeneratorBlockEntity.getMaxMana());
            gen.getFuelHandler().setStackInSlot(0, new ItemStack(Items.COAL, 5));
        });

        helper.runAtTickTime(30, () -> {
            ManaGeneratorBlockEntity gen = getGenerator(helper);
            // 魔力已滿所以發電機應該 pause，不應把 5 顆煤炭全部燒掉
            // 至少還有 4 顆煤炭留在槽位（最多只消耗 1 顆就卡住）
            int remaining = gen.getFuelHandler().getStackInSlot(0).getCount();
            helper.assertTrue(
                    remaining >= 4,
                    "魔力滿時燃料不應持續消耗，剩餘=" + remaining
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 4. NBT 存取：魔力、能量、燃燒時間、IO 設定全部保留
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void nbtPersistencePreservesState(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_GENERATOR.get().defaultBlockState());

        helper.runAtTickTime(2, () -> {
            ManaGeneratorBlockEntity gen = getGenerator(helper);

            // 設定各種狀態
            gen.getManaStorage().setMana(12345);
            gen.getEnergyStorage().receiveEnergy(99999, false);
            gen.setBurnTimeFromNbt(500);
            gen.setCurrentBurnTimeFromNbt(800);
            gen.setIOConfig(Direction.NORTH, IOHandlerUtils.IOType.INPUT);
            gen.setIOConfig(Direction.SOUTH, IOHandlerUtils.IOType.DISABLED);

            // 手動序列化 / 反序列化
            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            CompoundTag tag = new CompoundTag();
            gen.saveAdditional(tag, registries);

            ManaGeneratorBlockEntity reloaded = new ManaGeneratorBlockEntity(
                    BlockPos.ZERO,
                    ModBlocks.MANA_GENERATOR.get().defaultBlockState()
            );
            reloaded.loadAdditional(tag, registries);

            // 驗證每個欄位
            helper.assertTrue(reloaded.getManaStorage().getManaStored() == 12345,
                    "魔力應為 12345，實際=" + reloaded.getManaStorage().getManaStored());

            helper.assertTrue(reloaded.getBurnTime() == 500,
                    "燃燒時間應為 500，實際=" + reloaded.getBurnTime());

            helper.assertTrue(reloaded.getIOConfig(Direction.NORTH) == IOHandlerUtils.IOType.INPUT,
                    "NORTH IO 應為 INPUT，實際=" + reloaded.getIOConfig(Direction.NORTH));

            helper.assertTrue(reloaded.getIOConfig(Direction.SOUTH) == IOHandlerUtils.IOType.DISABLED,
                    "SOUTH IO 應為 DISABLED，實際=" + reloaded.getIOConfig(Direction.SOUTH));

            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 輔助方法
    // -------------------------------------------------------------------------
    private static ManaGeneratorBlockEntity getGenerator(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(POS);
        if (!(be instanceof ManaGeneratorBlockEntity gen)) {
            helper.fail("預期 ManaGeneratorBlockEntity at " + POS + "，實際=" + be);
            throw new AssertionError("unreachable");
        }
        return gen;
    }
}
