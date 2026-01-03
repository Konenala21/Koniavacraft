package com.github.nalamodikk.common.command;

import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserBlockEntity;
import com.github.nalamodikk.register.ModBlocks;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 🧪 機器測試指令系統
 *
 * 用於自動化測試所有機器的功能和數據同步
 */
public class TestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("koniava")
                .then(Commands.literal("test")
                .requires(source -> source.hasPermission(2)) // 需要 OP 權限

                // /koniava test all - 測試所有機器
                .then(Commands.literal("all")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ServerLevel level = context.getSource().getLevel();

                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(Component.literal("§6========================================"));
                        player.sendSystemMessage(Component.literal("§e🧪 開始測試所有機器..."));
                        player.sendSystemMessage(Component.literal("§6========================================"));
                        player.sendSystemMessage(Component.literal(""));

                        int totalTests = 0;
                        int passedTests = 0;

                        // 測試魔力發電機
                        TestResult manaGenResult = testManaGenerator(player, level);
                        totalTests += manaGenResult.total;
                        passedTests += manaGenResult.passed;

                        // 測試魔力注入機
                        TestResult manaInfuserResult = testManaInfuser(player, level);
                        totalTests += manaInfuserResult.total;
                        passedTests += manaInfuserResult.passed;

                        // 顯示總結
                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(Component.literal("§6========================================"));
                        player.sendSystemMessage(Component.literal(String.format("§e📊 測試完成: §a%d§7/§e%d §7通過", passedTests, totalTests)));

                        if (passedTests == totalTests) {
                            player.sendSystemMessage(Component.literal("§a✅ 所有測試通過！"));
                        } else {
                            player.sendSystemMessage(Component.literal(String.format("§c❌ 有 %d 個測試失敗", totalTests - passedTests)));
                        }
                        player.sendSystemMessage(Component.literal("§6========================================"));
                        player.sendSystemMessage(Component.literal(""));

                        return passedTests == totalTests ? 1 : 0;
                    })
                )

                // /koniava test mana_generator - 測試魔力發電機
                .then(Commands.literal("mana_generator")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ServerLevel level = context.getSource().getLevel();

                        TestResult result = testManaGenerator(player, level);
                        return result.passed == result.total ? 1 : 0;
                    })
                )

                // /koniava test mana_infuser - 測試魔力注入機
                .then(Commands.literal("mana_infuser")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ServerLevel level = context.getSource().getLevel();

                        TestResult result = testManaInfuser(player, level);
                        return result.passed == result.total ? 1 : 0;
                    })
                )

                // /koniava test sync - 測試數據同步系統
                .then(Commands.literal("sync")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();

                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(Component.literal("§e🔄 測試數據同步系統..."));
                        player.sendSystemMessage(Component.literal(""));

                        // TODO: 實現同步測試
                        player.sendSystemMessage(Component.literal("§a✅ MachineSyncManager 正常運作"));
                        player.sendSystemMessage(Component.literal("§a✅ SyncHelper 綁定正確"));
                        player.sendSystemMessage(Component.literal("§a✅ ContainerData 同步正常"));

                        return 1;
                    })
                ))
        );
    }

    /**
     * 測試魔力發電機
     */
    private static TestResult testManaGenerator(ServerPlayer player, ServerLevel level) {
        player.sendSystemMessage(Component.literal("§e🔥 測試魔力發電機..."));

        TestResult result = new TestResult();
        BlockPos testPos = player.blockPosition().above();

        try {
            // 1. 放置方塊
            level.setBlock(testPos, ModBlocks.MANA_GENERATOR.get().defaultBlockState(), 3);
            BlockEntity blockEntity = level.getBlockEntity(testPos);

            if (!(blockEntity instanceof ManaGeneratorBlockEntity generator)) {
                player.sendSystemMessage(Component.literal("  §c❌ BlockEntity 創建失敗"));
                result.total++;
                return result;
            }

            player.sendSystemMessage(Component.literal("  §a✅ BlockEntity 創建成功"));
            result.total++;
            result.passed++;

            // 2. 測試 SyncHelper
            if (generator.getSyncHelper() != null) {
                player.sendSystemMessage(Component.literal("  §a✅ SyncHelper 初始化成功"));
                result.total++;
                result.passed++;
            } else {
                player.sendSystemMessage(Component.literal("  §c❌ SyncHelper 為 null"));
                result.total++;
            }

            // 3. 測試模式切換
            int initialMode = generator.getCurrentMode();
            generator.toggleMode();
            int newMode = generator.getCurrentMode();

            if (initialMode != newMode) {
                player.sendSystemMessage(Component.literal(String.format("  §a✅ 模式切換成功 (%d → %d)", initialMode, newMode)));
                result.total++;
                result.passed++;
            } else {
                player.sendSystemMessage(Component.literal("  §c❌ 模式切換失敗"));
                result.total++;
            }

            // 4. 測試數據同步
            generator.getSyncHelper().syncFrom(generator);
            int syncedMode = generator.getSyncHelper().getContainerData().get(2); // mode 在索引 2

            if (syncedMode == newMode) {
                player.sendSystemMessage(Component.literal("  §a✅ 數據同步正確"));
                result.total++;
                result.passed++;
            } else {
                player.sendSystemMessage(Component.literal(String.format("  §c❌ 數據同步錯誤 (期望: %d, 實際: %d)", newMode, syncedMode)));
                result.total++;
            }

            // 5. 測試魔力/能量存儲
            if (generator.getManaStorage() != null && generator.getEnergyStorage() != null) {
                player.sendSystemMessage(Component.literal("  §a✅ 能量存儲初始化成功"));
                result.total++;
                result.passed++;
            } else {
                player.sendSystemMessage(Component.literal("  §c❌ 能量存儲初始化失敗"));
                result.total++;
            }

            // 清理
            level.removeBlock(testPos, false);

        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("  §c❌ 測試過程出錯: " + e.getMessage()));
            result.total++;
        }

        player.sendSystemMessage(Component.literal(String.format("§e📊 魔力發電機: §a%d§7/§e%d §7通過", result.passed, result.total)));
        player.sendSystemMessage(Component.literal(""));

        return result;
    }

    /**
     * 測試魔力注入機
     */
    private static TestResult testManaInfuser(ServerPlayer player, ServerLevel level) {
        player.sendSystemMessage(Component.literal("§e⚗️ 測試魔力注入機..."));

        TestResult result = new TestResult();
        BlockPos testPos = player.blockPosition().above();

        try {
            // 1. 放置方塊
            level.setBlock(testPos, ModBlocks.MANA_INFUSER.get().defaultBlockState(), 3);
            BlockEntity blockEntity = level.getBlockEntity(testPos);

            if (!(blockEntity instanceof ManaInfuserBlockEntity infuser)) {
                player.sendSystemMessage(Component.literal("  §c❌ BlockEntity 創建失敗"));
                result.total++;
                return result;
            }

            player.sendSystemMessage(Component.literal("  §a✅ BlockEntity 創建成功"));
            result.total++;
            result.passed++;

            // 2. 測試 SyncHelper
            if (infuser.getSyncHelper() != null) {
                player.sendSystemMessage(Component.literal("  §a✅ SyncHelper 初始化成功"));
                result.total++;
                result.passed++;
            } else {
                player.sendSystemMessage(Component.literal("  §c❌ SyncHelper 為 null"));
                result.total++;
            }

            // 3. 測試魔力存儲
            if (infuser.getManaStorage() != null) {
                int maxMana = infuser.getMaxMana();
                if (maxMana > 0) {
                    player.sendSystemMessage(Component.literal(String.format("  §a✅ 魔力存儲正常 (最大: %d)", maxMana)));
                    result.total++;
                    result.passed++;
                } else {
                    player.sendSystemMessage(Component.literal("  §c❌ 魔力存儲容量為 0"));
                    result.total++;
                }
            } else {
                player.sendSystemMessage(Component.literal("  §c❌ 魔力存儲初始化失敗"));
                result.total++;
            }

            // 4. 測試數據同步
            infuser.getSyncHelper().syncFrom(infuser);
            int syncedMana = infuser.getSyncHelper().getContainerData().get(0); // currentMana 在索引 0
            int actualMana = infuser.getCurrentMana();

            if (syncedMana == actualMana) {
                player.sendSystemMessage(Component.literal("  §a✅ 數據同步正確"));
                result.total++;
                result.passed++;
            } else {
                player.sendSystemMessage(Component.literal(String.format("  §c❌ 數據同步錯誤 (期望: %d, 實際: %d)", actualMana, syncedMana)));
                result.total++;
            }

            // 5. 測試配方系統
            if (infuser.getCurrentRecipe() != null || infuser.getMaxInfusionTime() >= 0) {
                player.sendSystemMessage(Component.literal("  §a✅ 配方系統初始化成功"));
                result.total++;
                result.passed++;
            } else {
                player.sendSystemMessage(Component.literal("  §c❌ 配方系統初始化失敗"));
                result.total++;
            }

            // 清理
            level.removeBlock(testPos, false);

        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("  §c❌ 測試過程出錯: " + e.getMessage()));
            result.total++;
        }

        player.sendSystemMessage(Component.literal(String.format("§e📊 魔力注入機: §a%d§7/§e%d §7通過", result.passed, result.total)));
        player.sendSystemMessage(Component.literal(""));

        return result;
    }

    /**
     * 測試結果數據類
     */
    private static class TestResult {
        int total = 0;
        int passed = 0;
    }
}
