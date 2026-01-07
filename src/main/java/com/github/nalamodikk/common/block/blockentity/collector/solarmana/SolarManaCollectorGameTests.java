package com.github.nalamodikk.common.block.blockentity.collector.solarmana;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.manager.SolarUpgradeManager;
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
import net.minecraft.server.level.ServerLevel;

@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class SolarManaCollectorGameTests {
    private static int delayUntilNextCycle(ServerLevel level, int interval) {
        long start = level.getGameTime();
        long next = start + (interval - (start % interval));
        return (int) (next - start + 40); // 包含狀態檢查與生成緩衝
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 400)
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

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 400, skyAccess = true)
    public static void solarCollectorGeneratesInClearDay(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.SOLAR_MANA_COLLECTOR.get());

        helper.runAtTickTime(1, () -> {
            ServerLevel level = helper.getLevel();
            level.setDayTime(1000L);
            level.setWeatherParameters(6000, 0, false, false);
            int delay = delayUntilNextCycle(level, SolarUpgradeManager.BASE_INTERVAL);
            helper.runAtTickTime(delay, () -> {
                BlockEntity be = helper.getBlockEntity(pos);
                if (!(be instanceof SolarManaCollectorBlockEntity collector)) {
                    helper.fail("找不到太陽魔法能收集器方塊實體");
                    return;
                }
                if (collector.getManaStorage().getManaStored() <= 0) {
                    helper.fail("白天晴空下未產生魔力");
                    return;
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 400, skyAccess = true)
    public static void solarCollectorEfficiencyUpgradeIncreasesOutput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.SOLAR_MANA_COLLECTOR.get());

        helper.runAtTickTime(1, () -> {
            BlockEntity be = helper.getBlockEntity(pos);
            if (!(be instanceof SolarManaCollectorBlockEntity collector)) {
                helper.fail("找不到太陽魔法能收集器方塊實體");
                return;
            }
            collector.getUpgradeInventory().setItem(0, new ItemStack(ModItems.EFFICIENCY_UPGRADE.get()));
            collector.getUpgradeInventory().setItem(1, new ItemStack(ModItems.EFFICIENCY_UPGRADE.get()));

            ServerLevel level = helper.getLevel();
            level.setDayTime(1000L);
            level.setWeatherParameters(6000, 0, false, false);
            int delay = delayUntilNextCycle(level, SolarUpgradeManager.BASE_INTERVAL);
            helper.runAtTickTime(delay, () -> {
                int mana = collector.getManaStorage().getManaStored();
                if (mana < 6) {
                    helper.fail("效率升級未提升產量，預期至少 6，實際=" + mana);
                    return;
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 400, skyAccess = true)
    public static void solarCollectorSpeedUpgradeIncreasesCycles(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.SOLAR_MANA_COLLECTOR.get());

        helper.runAtTickTime(1, () -> {
            BlockEntity be = helper.getBlockEntity(pos);
            if (!(be instanceof SolarManaCollectorBlockEntity collector)) {
                helper.fail("找不到太陽魔法能收集器方塊實體");
                return;
            }
            // 放入多顆速度升級，確保可見提升
            collector.getUpgradeInventory().setItem(0, new ItemStack(ModItems.SPEED_UPGRADE.get()));
            collector.getUpgradeInventory().setItem(1, new ItemStack(ModItems.SPEED_UPGRADE.get()));
            collector.getUpgradeInventory().setItem(2, new ItemStack(ModItems.SPEED_UPGRADE.get()));
            collector.getUpgradeInventory().setItem(3, new ItemStack(ModItems.SPEED_UPGRADE.get()));
            collector.getUpgradeInventory().setItem(4, new ItemStack(ModItems.SPEED_UPGRADE.get()));

            ServerLevel level = helper.getLevel();
            level.setDayTime(1000L);
            level.setWeatherParameters(6000, 0, false, false);

            int delay = delayUntilNextCycle(level, SolarUpgradeManager.BASE_INTERVAL * 2);
            helper.runAtTickTime(delay, () -> {
                int mana = collector.getManaStorage().getManaStored();
                if (mana < SolarUpgradeManager.BASE_OUTPUT * 2) {
                    helper.fail("速度升級未提高產生次數，預期至少 " +
                            (SolarUpgradeManager.BASE_OUTPUT * 2) + "，實際=" + mana);
                    return;
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 400)
    public static void solarCollectorFlagLogicCoversEdgeCases(GameTestHelper helper) {
        if (SolarManaCollectorBlockEntity.shouldGenerate(true, false, true, true, false, false)) {
            helper.fail("非主世界不應產生魔力");
            return;
        }
        if (SolarManaCollectorBlockEntity.shouldGenerate(false, true, true, true, false, false)) {
            helper.fail("夜晚不應產生魔力");
            return;
        }
        if (SolarManaCollectorBlockEntity.shouldGenerate(true, true, false, true, false, false)) {
            helper.fail("無天空光時不應產生魔力");
            return;
        }
        if (SolarManaCollectorBlockEntity.shouldGenerate(true, true, true, false, false, false)) {
            helper.fail("無天空視野時不應產生魔力");
            return;
        }
        if (SolarManaCollectorBlockEntity.shouldGenerate(true, true, true, true, true, false)) {
            helper.fail("下雨時不應產生魔力");
            return;
        }
        if (SolarManaCollectorBlockEntity.shouldGenerate(true, true, true, true, false, true)) {
            helper.fail("打雷時不應產生魔力");
            return;
        }
        if (!SolarManaCollectorBlockEntity.shouldGenerate(true, true, true, true, false, false)) {
            helper.fail("晴天主世界應該產生魔力");
            return;
        }
        helper.succeed();
    }
}
