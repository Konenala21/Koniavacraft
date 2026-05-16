package com.github.nalamodikk.common.datagen.recipe;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_grinder.ManaGrinderBlockEntity;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * T1 基礎魔力電路板製造鏈 GameTest
 *
 * 磨粉機三個配方（①②③）：
 *   ① 精煉魔力粉 + 銅錠 → 魔力基板 (mana=2000, time=80)
 *   ② 壓縮魔力粉 + 金錠 → 魔力導線×4 (mana=2500, time=100)
 *   ③ 史萊姆球×2 + 魔力粉 → 魔力黏膠×3 (mana=1500, time=60)
 *
 * 使用 "empty" 模板（與其他機器測試相同）。
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class T1CircuitBoardRecipeGameTests {

    private static final BlockPos POS = new BlockPos(1, 2, 1);

    // ── 輔助 ──────────────────────────────────────────────────────────────────

    private static ManaGrinderBlockEntity getGrinder(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(POS);
        if (!(be instanceof ManaGrinderBlockEntity g)) {
            helper.fail("預期 ManaGrinderBlockEntity at " + POS + "，實際=" + be);
            throw new AssertionError("unreachable");
        }
        return g;
    }

    /** 放置磨粉機、補魔力、放入雙槽材料，等待 extraTicks 後回到 tick 0。 */
    private static void setupGrinder(GameTestHelper helper, ItemStack slot0, ItemStack slot1, int mana) {
        helper.setBlock(POS, ModBlocks.MANA_GRINDER.get());
        helper.runAtTickTime(2, () -> {
            ManaGrinderBlockEntity g = getGrinder(helper);
            g.getManaStorage().setMana(mana);
            g.getItemHandler().setStackInSlot(0, slot0);
            g.getItemHandler().setStackInSlot(1, slot1);
            g.markInputChanged();
        });
    }

    // ── Test 1：精煉魔力粉 + 銅錠 → 魔力基板 (time=80) ──────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 160)
    public static void refinedDustAndCopperYieldsManaSubstrate(GameTestHelper helper) {
        setupGrinder(helper,
            new ItemStack(ModItems.REFINED_MANA_DUST.get()),
            new ItemStack(Items.COPPER_INGOT),
            10000);

        // processing_time=80，+40 緩衝
        helper.runAtTickTime(125, () -> {
            ManaGrinderBlockEntity g = getGrinder(helper);
            ItemStack out = findOutput(g);
            helper.assertTrue(
                !out.isEmpty() && out.is(ModItems.MANA_SUBSTRATE.get()),
                "精煉魔力粉 + 銅錠 → 應輸出魔力基板，實際=" + out
            );
            int manaLeft = g.getManaStorage().getManaStored();
            helper.assertTrue(manaLeft < 10000, "魔力應被消耗，剩餘=" + manaLeft);
            helper.succeed();
        });
    }

    // ── Test 2：壓縮魔力粉 + 金錠 → 魔力導線×4 (time=100) ───────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 200)
    public static void condensedDustAndGoldYieldsManaWire(GameTestHelper helper) {
        setupGrinder(helper,
            new ItemStack(ModItems.CONDENSED_MANA_DUST.get()),
            new ItemStack(Items.GOLD_INGOT),
            10000);

        // processing_time=100，+40 緩衝
        helper.runAtTickTime(145, () -> {
            ManaGrinderBlockEntity g = getGrinder(helper);
            ItemStack out = findOutput(g);
            helper.assertTrue(
                !out.isEmpty() && out.is(ModItems.MANA_WIRE.get()),
                "壓縮魔力粉 + 金錠 → 應輸出魔力導線，實際=" + out
            );
            helper.assertTrue(out.getCount() == 4, "魔力導線應產出 4 個，實際=" + out.getCount());
            helper.succeed();
        });
    }

    // ── Test 3：史萊姆球×2 + 魔力粉 → 魔力黏膠×3 (time=60) ─────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 140)
    public static void slimeBallsAndManaDustYieldsManaAdhesive(GameTestHelper helper) {
        // slot0=史萊姆球（數量 2），slot1=魔力粉
        ItemStack slimes = new ItemStack(Items.SLIME_BALL, 2);
        setupGrinder(helper, slimes, new ItemStack(ModItems.MANA_DUST.get()), 10000);

        // processing_time=60，+40 緩衝
        helper.runAtTickTime(105, () -> {
            ManaGrinderBlockEntity g = getGrinder(helper);
            ItemStack out = findOutput(g);
            helper.assertTrue(
                !out.isEmpty() && out.is(ModItems.MANA_ADHESIVE.get()),
                "史萊姆球×2 + 魔力粉 → 應輸出魔力黏膠，實際=" + out
            );
            helper.assertTrue(out.getCount() == 3, "魔力黏膠應產出 3 個，實際=" + out.getCount());
            helper.succeed();
        });
    }

    // ── Test 4：材料不符 → 不產出任何電路板中間材料 ─────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 80)
    public static void wrongInputsProduceNoCircuitMaterial(GameTestHelper helper) {
        // 放入無效組合（木頭 + 木頭）
        setupGrinder(helper,
            new ItemStack(Items.OAK_LOG),
            new ItemStack(Items.OAK_LOG),
            10000);

        helper.runAtTickTime(70, () -> {
            ManaGrinderBlockEntity g = getGrinder(helper);
            // 所有電路板中間材料的輸出槽都應為空
            boolean hasCircuitMaterial = false;
            for (int i = 2; i <= 5; i++) {
                ItemStack s = g.getItemHandler().getStackInSlot(i);
                if (!s.isEmpty()
                    && (s.is(ModItems.MANA_SUBSTRATE.get())
                     || s.is(ModItems.MANA_WIRE.get())
                     || s.is(ModItems.MANA_ADHESIVE.get()))) {
                    hasCircuitMaterial = true;
                    break;
                }
            }
            helper.assertFalse(hasCircuitMaterial, "無效材料組合不應產出任何電路板中間材料");
            helper.succeed();
        });
    }

    // ── Test 5：魔力不足 → 不推進進度 ───────────────────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 80)
    public static void noManaStopsCircuitRecipe(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MANA_GRINDER.get());
        helper.runAtTickTime(2, () -> {
            ManaGrinderBlockEntity g = getGrinder(helper);
            // 魔力 = 0
            g.getItemHandler().setStackInSlot(0, new ItemStack(ModItems.REFINED_MANA_DUST.get()));
            g.getItemHandler().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT));
            g.markInputChanged();
        });

        helper.runAtTickTime(65, () -> {
            ManaGrinderBlockEntity g = getGrinder(helper);
            helper.assertTrue(g.getProgress() == 0, "魔力為 0 時進度應維持 0，實際=" + g.getProgress());
            helper.assertTrue(findOutput(g).isEmpty(), "魔力為 0 時不應有輸出，實際=" + findOutput(g));
            helper.succeed();
        });
    }

    /** 掃描輸出槽（index 2-5），回傳第一個非空的 ItemStack。 */
    private static ItemStack findOutput(ManaGrinderBlockEntity g) {
        for (int i = 2; i <= 5; i++) {
            ItemStack s = g.getItemHandler().getStackInSlot(i);
            if (!s.isEmpty()) return s;
        }
        return ItemStack.EMPTY;
    }
}
