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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class ManaGrinderPersistenceGameTests {

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 200)
    public static void grinderSaveLoadKeepsManaAndIoConfig(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.MANA_GRINDER.get());

        helper.runAtTickTime(2, () -> {
            BlockEntity be = helper.getBlockEntity(pos);
            if (!(be instanceof ManaGrinderBlockEntity grinder)) {
                helper.fail("找不到魔力粉碎機方塊實體");
                return;
            }

            ManaStorage manaStorage = grinder.getManaStorage();
            if (manaStorage == null) {
                helper.fail("粉碎機 manaStorage 為空");
                return;
            }

            int expectedMana = 4321;
            manaStorage.setMana(expectedMana);
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

            ManaStorage reloadedManaStorage = reloaded.getManaStorage();
            if (reloadedManaStorage == null) {
                helper.fail("重載後粉碎機 manaStorage 為空");
                return;
            }
            if (reloadedManaStorage.getManaStored() != expectedMana) {
                helper.fail("粉碎機重載後 mana 不一致，預期=" + expectedMana + " 實際=" + reloadedManaStorage.getManaStored());
                return;
            }
            if (reloaded.getIOConfig(Direction.EAST) != IOHandlerUtils.IOType.OUTPUT) {
                helper.fail("粉碎機重載後 EAST IO 設定未保留");
                return;
            }
            if (reloaded.getIOConfig(Direction.WEST) != IOHandlerUtils.IOType.DISABLED) {
                helper.fail("粉碎機重載後 WEST IO 設定未保留");
                return;
            }

            helper.succeed();
        });
    }
}
