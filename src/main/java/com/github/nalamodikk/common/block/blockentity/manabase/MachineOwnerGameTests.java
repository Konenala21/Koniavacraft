package com.github.nalamodikk.common.block.blockentity.manabase;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.rpg.RPGManager;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class MachineOwnerGameTests {
    private static final int INTELLIGENCE_BONUS = 10;
    private static final BlockPos DEFAULT_POS = new BlockPos(1, 2, 1);

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID)
    public static void manaGeneratorOwnerAndMultiplier(GameTestHelper helper) {
        runOwnerPlacementTest(helper, ModBlocks.MANA_GENERATOR.get(), new ItemStack(ModBlocks.MANA_GENERATOR.get()));
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID)
    public static void manaInfuserOwnerAndMultiplier(GameTestHelper helper) {
        runOwnerPlacementTest(helper, ModBlocks.MANA_INFUSER.get(), new ItemStack(ModBlocks.MANA_INFUSER.get()));
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID)
    public static void oreGrinderOwnerAndMultiplier(GameTestHelper helper) {
        runOwnerPlacementTest(helper, ModBlocks.ORE_GRINDER.get(), new ItemStack(ModBlocks.ORE_GRINDER.get()));
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID)
    public static void solarCollectorOwnerAndMultiplier(GameTestHelper helper) {
        runOwnerPlacementTest(helper, ModBlocks.SOLAR_MANA_COLLECTOR.get(), new ItemStack(ModBlocks.SOLAR_MANA_COLLECTOR.get()));
    }

    private static void runOwnerPlacementTest(GameTestHelper helper, Block block, ItemStack stack) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        RPGManager.getPlayerData(player).getAttributes().setIntelligence(INTELLIGENCE_BONUS);
        helper.setBlock(DEFAULT_POS, block);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(DEFAULT_POS), helper.getBlockState(DEFAULT_POS), player, stack);

        helper.runAtTickTime(2, () -> {
            BlockPos worldPos = helper.absolutePos(DEFAULT_POS);
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(worldPos);
            if (!(blockEntity instanceof AbstractManaMachineEntityBlock machine)) {
                helper.fail("找不到機器方塊實體");
                return;
            }

            if (!player.getUUID().equals(machine.getOwnerId())) {
                helper.fail("機器擁有者沒有正確記錄");
                return;
            }

            if (machine.getOwnerGenerationMultiplier() <= 1.0f) {
                helper.fail("機器產能倍率未套用玩家屬性");
                return;
            }

            helper.succeed();
        });
    }
}
