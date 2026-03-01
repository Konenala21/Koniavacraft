package com.github.nalamodikk.common.block.blockentity.mana_crafting;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class ManaCraftingTablePersistenceGameTests {

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 200)
    public static void tableSaveLoadKeepsManaAndIoConfig(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get());

        helper.runAtTickTime(2, () -> {
            BlockEntity be = helper.getBlockEntity(pos);
            if (!(be instanceof ManaCraftingTableBlockEntity table)) {
                helper.fail("找不到魔力工作台方塊實體");
                return;
            }

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

            if (reloaded.getManaStored() != expectedMana) {
                helper.fail("魔力工作台重載後 mana 不一致，預期=" + expectedMana + " 實際=" + reloaded.getManaStored());
                return;
            }
            if (reloaded.getIOConfig(Direction.SOUTH) != IOHandlerUtils.IOType.OUTPUT) {
                helper.fail("魔力工作台重載後 SOUTH IO 設定未保留");
                return;
            }
            if (reloaded.getIOConfig(Direction.UP) != IOHandlerUtils.IOType.DISABLED) {
                helper.fail("魔力工作台重載後 UP IO 設定未保留");
                return;
            }

            helper.succeed();
        });
    }
}
