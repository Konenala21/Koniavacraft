package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 測試 AspectPedestalBlockEntity 的 IItemHandler capability
 * (為了支援漏斗 / 其他模組自動化而加入)
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class AspectPedestalItemHandlerGameTests {

    private static final BlockPos POS = new BlockPos(1, 2, 1);

    // ── 輔助 ──────────────────────────────────────────────────────────────────

    private static AspectPedestalBlockEntity getPedestal(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(POS);
        if (!(be instanceof AspectPedestalBlockEntity ped)) {
            helper.fail("預期 AspectPedestalBlockEntity at " + POS + "，實際=" + be);
            throw new AssertionError("unreachable");
        }
        return ped;
    }

    private static IItemHandler getHandler(GameTestHelper helper) {
        IItemHandler handler = helper.getLevel().getCapability(
            Capabilities.ItemHandler.BLOCK,
            helper.absolutePos(POS),
            Direction.UP
        );
        if (handler == null) {
            helper.fail("基座沒有 IItemHandler capability");
            throw new AssertionError("unreachable");
        }
        return handler;
    }

    // ── Test 1：Capability 存在 ───────────────────────────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void pedestalHasItemHandlerCapability(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ASPECT_PEDESTAL.get());
        helper.runAtTickTime(2, () -> {
            IItemHandler handler = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(POS),
                Direction.UP
            );
            helper.assertTrue(handler != null, "基座應暴露 IItemHandler capability");
            helper.assertTrue(handler.getSlots() == 1, "基座應只有 1 個 slot，實際=" + handler.getSlots());
            helper.succeed();
        });
    }

    // ── Test 2：插入物品 ──────────────────────────────────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void pedestalInsertItemViaHandler(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ASPECT_PEDESTAL.get());
        helper.runAtTickTime(2, () -> {
            IItemHandler handler = getHandler(helper);
            ItemStack remainder = handler.insertItem(0, new ItemStack(Items.DIAMOND), false);
            helper.assertTrue(remainder.isEmpty(), "鑽石應完全插入，剩餘=" + remainder);
            ItemStack held = handler.getStackInSlot(0);
            helper.assertTrue(held.is(Items.DIAMOND), "基座應持有鑽石，實際=" + held);
            helper.assertTrue(held.getCount() == 1, "基座應只持有 1 個，實際=" + held.getCount());
            helper.succeed();
        });
    }

    // ── Test 3：取出物品 ──────────────────────────────────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void pedestalExtractItemViaHandler(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ASPECT_PEDESTAL.get());
        helper.runAtTickTime(2, () -> {
            // 先直接放入（繞過 handler，測試已知狀態）
            getPedestal(helper).insertItem(new ItemStack(Items.EMERALD));

            IItemHandler handler = getHandler(helper);
            ItemStack extracted = handler.extractItem(0, 1, false);
            helper.assertTrue(extracted.is(Items.EMERALD), "取出的應為翡翠，實際=" + extracted);
            helper.assertTrue(handler.getStackInSlot(0).isEmpty(), "取出後 slot 應為空");
            helper.succeed();
        });
    }

    // ── Test 4：Simulate 不改變實際狀態 ──────────────────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void pedestalSimulateDoesNotModifyState(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ASPECT_PEDESTAL.get());
        helper.runAtTickTime(2, () -> {
            IItemHandler handler = getHandler(helper);

            // simulate insert → 不應真的插入
            ItemStack remainder = handler.insertItem(0, new ItemStack(Items.GOLD_INGOT), true);
            helper.assertTrue(remainder.isEmpty(), "模擬插入餘量應為空（代表可以插入）");
            helper.assertTrue(handler.getStackInSlot(0).isEmpty(),
                "模擬插入後 slot 應仍為空（未真的插入）");
            helper.succeed();
        });
    }

    // ── Test 5：已有物品時插入應被拒絕 ───────────────────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void pedestalRejectsInsertWhenFull(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ASPECT_PEDESTAL.get());
        helper.runAtTickTime(2, () -> {
            getPedestal(helper).insertItem(new ItemStack(Items.STICK));

            IItemHandler handler = getHandler(helper);
            // 嘗試再插入一個不同物品
            ItemStack toInsert = new ItemStack(Items.IRON_INGOT);
            ItemStack remainder = handler.insertItem(0, toInsert, false);
            helper.assertTrue(
                ItemStack.isSameItemSameComponents(remainder, toInsert),
                "基座已有物品時應完整退回插入的物品"
            );
            // slot 仍持有原來的物品
            helper.assertTrue(handler.getStackInSlot(0).is(Items.STICK), "原物品應仍在 slot 中");
            helper.succeed();
        });
    }

    // ── Test 6：空 slot 不能取出 ─────────────────────────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void pedestalExtractFromEmptySlotReturnsEmpty(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ASPECT_PEDESTAL.get());
        helper.runAtTickTime(2, () -> {
            IItemHandler handler = getHandler(helper);
            ItemStack extracted = handler.extractItem(0, 64, false);
            helper.assertTrue(extracted.isEmpty(), "空 slot 取出應回傳 empty，實際=" + extracted);
            helper.succeed();
        });
    }
}
