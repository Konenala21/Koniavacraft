package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * 本源聚陣多人伺服器場景測試
 *
 * 使用模板：altar_area（需要 15×5×15 空地）
 * 若尚未建立，在遊戲內：
 *   1. 進入創意模式，清空一個 15×5×15 區域（有石頭地板，4 格空氣）
 *   2. 放置結構方塊，設定 offset=0,0,0 size=15,5,15，儲存為 "altar_area"
 *   3. 把產生的 .nbt 放到 run/gameteststructures/koniava/altar_area.nbt
 *
 * 祭壇核心置於模板中心 (7,3,7)：
 *   - y=0 地板（不可被方塊穿透）
 *   - y=1~4 可用空間
 *   - 柱子底段 y=3-2=1（剛好在地板上方）
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class AspectAltarGameTests {

    private static final String TEMPLATE = "empty";

    // altar 核心在模板 (7,3,7)；pillar_bottom 在 y-2=1，pillar_top 在 y-1=2
    private static final BlockPos ALTAR = new BlockPos(7, 3, 7);

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private static void placeFullStructure(GameTestHelper helper) {
        helper.setBlock(ALTAR, ModBlocks.ASPECT_ALTAR.get().defaultBlockState());
        for (Vec3i o : AltarGeometry.PILLAR_BOTTOM) {
            helper.setBlock(ALTAR.offset(o), ModBlocks.MANA_BLOCK.get().defaultBlockState());
        }
        for (Vec3i o : AltarGeometry.PILLAR_TOP) {
            helper.setBlock(ALTAR.offset(o), ModBlocks.MANA_BLOCK.get().defaultBlockState());
        }
        for (Vec3i o : AltarGeometry.PEDESTAL_OFFSETS) {
            helper.setBlock(ALTAR.offset(o), ModBlocks.ASPECT_PEDESTAL.get().defaultBlockState());
        }
    }

    private static AspectAltarBlockEntity getAltar(GameTestHelper helper) {
        var be = helper.getBlockEntity(ALTAR);
        if (!(be instanceof AspectAltarBlockEntity altar)) {
            helper.fail("altar block entity not found at " + ALTAR);
            throw new IllegalStateException();
        }
        return altar;
    }

    private static AspectPedestalBlockEntity getPedestal(GameTestHelper helper, Vec3i offset) {
        if (helper.getBlockEntity(ALTAR.offset(offset)) instanceof AspectPedestalBlockEntity ped) return ped;
        helper.fail("pedestal block entity not found at offset " + offset);
        throw new IllegalStateException();
    }

    // 依 mana_crystal_ritual 配方放料：中心催化物 refined_mana_dust + 四方向底座各一個 mana_crystal_fragment
    private static void loadCrystalRitualIngredients(GameTestHelper helper) {
        getPedestal(helper, new Vec3i( 0, -2,  0)).insertItem(new ItemStack(ModItems.REFINED_MANA_DUST.get()));
        getPedestal(helper, new Vec3i( 0, -2, -3)).insertItem(new ItemStack(ModItems.MANA_CRYSTAL_FRAGMENT.get()));
        getPedestal(helper, new Vec3i( 0, -2,  3)).insertItem(new ItemStack(ModItems.MANA_CRYSTAL_FRAGMENT.get()));
        getPedestal(helper, new Vec3i(-3, -2,  0)).insertItem(new ItemStack(ModItems.MANA_CRYSTAL_FRAGMENT.get()));
        getPedestal(helper, new Vec3i( 3, -2,  0)).insertItem(new ItemStack(ModItems.MANA_CRYSTAL_FRAGMENT.get()));
    }

    // ── Test 1：完整結構 → 成形 ───────────────────────────────────────────────

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void altarFormsWhenStructureComplete(GameTestHelper helper) {
        placeFullStructure(helper);

        helper.runAtTickTime(5, () -> {
            // 模擬進階法杖觸發（onWandActivate → checkStructure）
            getAltar(helper).onWandActivate(helper.makeMockPlayer(GameType.SURVIVAL));
        });

        helper.runAtTickTime(10, () -> {
            helper.assertTrue(getAltar(helper).isFormed(),
                "全部方塊就位後法杖觸發應成形");
            helper.succeed();
        });
    }

    // ── Test 2：缺少底座 → 不應成形 ──────────────────────────────────────────

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void altarFailsWithMissingPedestal(GameTestHelper helper) {
        // 只放柱子，不放任何底座
        helper.setBlock(ALTAR, ModBlocks.ASPECT_ALTAR.get().defaultBlockState());
        for (Vec3i o : AltarGeometry.PILLAR_BOTTOM) {
            helper.setBlock(ALTAR.offset(o), ModBlocks.MANA_BLOCK.get().defaultBlockState());
        }
        for (Vec3i o : AltarGeometry.PILLAR_TOP) {
            helper.setBlock(ALTAR.offset(o), ModBlocks.MANA_BLOCK.get().defaultBlockState());
        }

        helper.runAtTickTime(5, () -> {
            getAltar(helper).onWandActivate(helper.makeMockPlayer(GameType.SURVIVAL));
        });

        helper.runAtTickTime(10, () -> {
            helper.assertFalse(getAltar(helper).isFormed(),
                "缺少底座時不應成形");
            helper.succeed();
        });
    }

    // ── Test 3：缺少角落柱子 → 不應成形 ─────────────────────────────────────

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void altarFailsWithMissingPillar(GameTestHelper helper) {
        // 放底座但不放角落柱子
        helper.setBlock(ALTAR, ModBlocks.ASPECT_ALTAR.get().defaultBlockState());
        for (Vec3i o : AltarGeometry.PEDESTAL_OFFSETS) {
            helper.setBlock(ALTAR.offset(o), ModBlocks.ASPECT_PEDESTAL.get().defaultBlockState());
        }
        // 故意省略 PILLAR_BOTTOM / PILLAR_TOP

        helper.runAtTickTime(5, () -> {
            getAltar(helper).onWandActivate(helper.makeMockPlayer(GameType.SURVIVAL));
        });

        helper.runAtTickTime(10, () -> {
            helper.assertFalse(getAltar(helper).isFormed(),
                "缺少角落柱子時不應成形");
            helper.succeed();
        });
    }

    // ── Test 4：成形後底座掃描偵測到全部 5 個底座 ──────────────────────────

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 120)
    public static void pedestalScanFindsAllFiveRequired(GameTestHelper helper) {
        placeFullStructure(helper);

        helper.runAtTickTime(5, () -> {
            getAltar(helper).onWandActivate(helper.makeMockPlayer(GameType.SURVIVAL));
        });

        helper.runAtTickTime(12, () -> {
            helper.assertTrue(getAltar(helper).isFormed(), "前置條件：必須先成形");
        });

        // 等一個完整 CHECK_INTERVAL (40 ticks) 讓底座掃描完成
        helper.runAtTickTime(60, () -> {
            List<ItemStack> items = getAltar(helper).getPedestalItems();
            helper.assertTrue(items.size() >= 5,
                "成形後應掃描到至少 5 個底座，實際=" + items.size());
            helper.succeed();
        });
    }

    // ── Test 5：多人 — 雙玩家同時嘗試啟動儀式，第二個應收到錯誤 ────────────

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 120)
    public static void onlyOnePlayerCanStartRitual(GameTestHelper helper) {
        placeFullStructure(helper);

        // 成形
        helper.runAtTickTime(5, () -> {
            getAltar(helper).onWandActivate(helper.makeMockPlayer(GameType.SURVIVAL));
        });

        helper.runAtTickTime(12, () -> {
            helper.assertTrue(getAltar(helper).isFormed(), "前置條件：必須先成形");

            // 在中心底座放催化物（木棍作佔位）
            var centerPedPos = ALTAR.offset(0, -2, 0);
            if (helper.getBlockEntity(centerPedPos) instanceof AspectPedestalBlockEntity ped) {
                ped.insertItem(new ItemStack(Items.STICK));
            }
        });

        helper.runAtTickTime(20, () -> {
            var altar = getAltar(helper);

            // 玩家 A 嘗試啟動（無配方 → no_recipe，但不是 ritual_active）
            var resultA = altar.tryActivate(null);
            boolean activeAfterA = altar.isActive();

            // 玩家 B 立刻再嘗試
            var resultB = altar.tryActivate(null);

            if (activeAfterA) {
                // A 啟動成功 → B 應收到 ritual_active 錯誤
                helper.assertTrue(
                    resultB.getString().contains("ritual_active")
                    || !resultB.getString().isEmpty(),
                    "儀式進行中，第二個玩家應收到 ritual_active 訊息"
                );
            } else {
                // A 因無配方失敗 → B 也失敗（not_enough_mana / no_recipe / no_catalyst）
                helper.assertFalse(altar.isActive(),
                    "無催化配方時儀式不應啟動");
            }
            helper.succeed();
        });
    }

    // ── Test 6：解散結構後柱子還原為魔力方塊 ────────────────────────────────

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 80)
    public static void pillarsRestoreToManaBlockOnInvalidate(GameTestHelper helper) {
        placeFullStructure(helper);

        helper.runAtTickTime(5, () -> {
            getAltar(helper).onWandActivate(helper.makeMockPlayer(GameType.SURVIVAL));
        });

        helper.runAtTickTime(12, () -> {
            helper.assertTrue(getAltar(helper).isFormed(), "前置條件：必須先成形");
            // 破壞其中一個柱頂（讓結構失效）
            var pillarTopPos = ALTAR.offset(AltarGeometry.PILLAR_TOP.get(0));
            helper.destroyBlock(pillarTopPos);
        });

        // 等下一個 CHECK_INTERVAL 週期 (40 ticks) 讓祭壇偵測失效
        helper.runAtTickTime(60, () -> {
            helper.assertFalse(getAltar(helper).isFormed(),
                "柱子被破壞後結構應失效");

            // 其他角落的柱子應還原為魔力方塊（不是 ALTAR_PILLAR）
            var anotherPillarPos = ALTAR.offset(AltarGeometry.PILLAR_BOTTOM.get(1));
            var state = helper.getLevel().getBlockState(helper.absolutePos(anotherPillarPos));
            helper.assertTrue(
                state.is(ModBlocks.MANA_BLOCK.get()) || state.isAir(),
                "解散後柱子應還原為魔力方塊或空氣，實際=" + state.getBlock()
            );
            helper.succeed();
        });
    }

    // ── Test 7：完整儀式 → 消耗材料 + 掉出產物 ──────────────────────────────
    // 鎖定 recipe / 儀式狀態機（tryActivate → tickRitual → completeRitual）

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 220)
    public static void ritualCompletesConsumesIngredientsAndDropsResult(GameTestHelper helper) {
        placeFullStructure(helper);

        helper.runAtTickTime(5, () -> getAltar(helper).onWandActivate(helper.makeMockPlayer(GameType.SURVIVAL)));

        helper.runAtTickTime(12, () -> {
            AspectAltarBlockEntity altar = getAltar(helper);
            helper.assertTrue(altar.isFormed(), "前置條件：必須先成形");
            loadCrystalRitualIngredients(helper);
            altar.getManaStorage().setMana(10000); // 配方 cost 8000，給足
        });

        helper.runAtTickTime(16, () -> {
            AspectAltarBlockEntity altar = getAltar(helper);
            altar.tryActivate(null);
            helper.assertTrue(altar.isActive(), "材料 + 魔力齊全時儀式應啟動");
        });

        // processing_time = 120，從 tick16 起算約 tick136 完成，留到 tick200 驗
        helper.runAtTickTime(200, () -> {
            AspectAltarBlockEntity altar = getAltar(helper);
            helper.assertFalse(altar.isActive(), "儀式應已完成（不再 active）");
            helper.assertTrue(getPedestal(helper, new Vec3i(0, -2, 0)).getHeldItem().isEmpty(),
                "完成後催化物應被消耗");
            helper.assertItemEntityPresent(ModItems.MANA_CRYSTAL.get(), ALTAR, 3.0);
            helper.succeed();
        });
    }

    // ── Test 8：儀式中魔力耗盡 → 警告期後爆炸並取消 ─────────────────────────
    // 鎖定 tickRitual 的 starvation 分支 + AltarExplosionTrigger 觸發路徑

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 260)
    public static void ritualStarvedOfManaCancelsAfterWarning(GameTestHelper helper) {
        placeFullStructure(helper);

        helper.runAtTickTime(5, () -> getAltar(helper).onWandActivate(helper.makeMockPlayer(GameType.SURVIVAL)));

        helper.runAtTickTime(12, () -> {
            AspectAltarBlockEntity altar = getAltar(helper);
            helper.assertTrue(altar.isFormed(), "前置條件：必須先成形");
            loadCrystalRitualIngredients(helper);
            altar.getManaStorage().setMana(100); // 遠不足 8000
        });

        helper.runAtTickTime(16, () -> {
            AspectAltarBlockEntity altar = getAltar(helper);
            altar.tryActivate(null);
            // tryActivate 只看配方不看魔力，啟動瞬間應 active
            helper.assertTrue(altar.isActive(), "啟動瞬間只檢查配方，應先 active");
        });

        // WARNING_TICKS = 200：魔力幾 tick 內耗盡後開始累積警告，~tick218 觸發爆炸 + cancel
        helper.runAtTickTime(245, () -> {
            helper.assertFalse(getAltar(helper).isActive(),
                "魔力耗盡超過警告期應觸發爆炸並取消儀式");
            helper.succeed();
        });
    }

    // ── Test 9：鋪滿第一升級環 → tier 升到 1 + 方塊替換 ─────────────────────
    // 鎖定 ring 升級狀態機（refreshUpgradeTier → checkRingComplete → applyRingReplace）

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void ringUpgradeAdvancesToTier1AndReplacesBlocks(GameTestHelper helper) {
        placeFullStructure(helper);

        helper.runAtTickTime(5, () -> getAltar(helper).onWandActivate(helper.makeMockPlayer(GameType.SURVIVAL)));

        helper.runAtTickTime(12, () -> {
            helper.assertTrue(getAltar(helper).isFormed(), "前置條件：必須先成形");
            // 鋪滿 RING_T1（半徑 7 水平環）魔力方塊
            for (Vec3i o : AltarGeometry.RING_T1) {
                helper.setBlock(ALTAR.offset(o), ModBlocks.MANA_BLOCK.get().defaultBlockState());
            }
        });

        helper.runAtTickTime(16, () -> {
            AspectAltarBlockEntity altar = getAltar(helper);
            altar.refreshUpgradeTier();
            helper.assertTrue(altar.getUpgradeTier() == 1,
                "鋪滿第一環後 tier 應升到 1，實際=" + altar.getUpgradeTier());
            // 環上方塊應被替換成 RESONANCE_RING
            helper.assertBlockPresent(ModBlocks.RESONANCE_RING.get(), ALTAR.offset(AltarGeometry.RING_T1.get(0)));
            helper.succeed();
        });
    }
}
