package com.github.nalamodikk.common.block.blockentity.collector.solarmana;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class SolarManaCollectorGameTests {
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID)
    public static void solarCollectorLoadsLegacyNbt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.SOLAR_MANA_COLLECTOR.get());

        helper.runAtTickTime(2, () -> {
            BlockEntity be = helper.getBlockEntity(pos);
            if (!(be instanceof SolarManaCollectorBlockEntity collector)) {
                helper.fail("找不到太陽魔法能收集器方塊實體");
                return;
            }

            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            CompoundTag tag = new CompoundTag();

            ManaStorage legacyMana = new ManaStorage(SolarManaCollectorBlockEntity.getMaxMana());
            legacyMana.setMana(1234);
            tag.put("ManaStorage", legacyMana.serializeNBT(registries));

            UpgradeInventory legacyUpgrades = new UpgradeInventory(8);
            legacyUpgrades.setItem(0, new ItemStack(ModItems.SPEED_UPGRADE.get()));
            tag.put("UpgradeInventory", legacyUpgrades.serializeNBT(registries));

            collector.loadAdditional(tag, registries);

            if (collector.getManaStorage().getManaStored() != 1234) {
                helper.fail("舊版 ManaStorage 未正確轉換");
                return;
            }
            if (collector.getUpgradeInventory().getUpgradeCount(UpgradeType.SPEED) != 1) {
                helper.fail("舊版 UpgradeInventory 未正確轉換");
                return;
            }
            if (collector.getIOConfig(net.minecraft.core.Direction.DOWN) != IOHandlerUtils.IOType.OUTPUT) {
                helper.fail("舊版 IO 設定未套用預設值");
                return;
            }

            helper.succeed();
        });
    }
}
