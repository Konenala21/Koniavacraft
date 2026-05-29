package com.github.nalamodikk.common.block.blockentity.conduit;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 測試 VirtualNetwork 的容量邏輯：
 * - 加入導管時容量按 tier 增加
 * - 移除導管時容量縮小
 * - 魔力在容量縮小時被截斷
 * - Tier 升級正確更新網路容量
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class VirtualNetworkGameTests {

    // 兩個相鄰位置，確保它們會加入同一個 VirtualNetwork
    private static final BlockPos POS_A = new BlockPos(1, 2, 1);
    private static final BlockPos POS_B = new BlockPos(2, 2, 1);

    // -------------------------------------------------------------------------
    // 1. 單導管容量 = BASIC tier 容量
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 50)
    public static void singleBasicConduitHasBasicCapacity(GameTestHelper helper) {
        helper.setBlock(POS_A, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());

        helper.runAtTickTime(5, () -> {
            ArcaneConduitBlockEntity conduit = getConduit(helper, POS_A);
            int expected = ConduitTier.BASIC.getBufferCapacity();
            helper.assertTrue(
                    conduit.getMaxManaStored() == expected,
                    "單個 BASIC 導管容量應為 " + expected + "，實際=" + conduit.getMaxManaStored()
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 2. 兩個相鄰 BASIC 導管共享網路，容量 = BASIC * 2
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 50)
    public static void twoAdjacentBasicConduitsShareCapacity(GameTestHelper helper) {
        helper.setBlock(POS_A, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());
        helper.setBlock(POS_B, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());

        helper.runAtTickTime(5, () -> {
            ArcaneConduitBlockEntity a = getConduit(helper, POS_A);
            ArcaneConduitBlockEntity b = getConduit(helper, POS_B);
            int expected = ConduitTier.BASIC.getBufferCapacity() * 2;

            helper.assertTrue(
                    a.getMaxManaStored() == expected,
                    "兩個 BASIC 導管的網路容量應為 " + expected + "，A 顯示=" + a.getMaxManaStored()
            );
            helper.assertTrue(
                    a.getMaxManaStored() == b.getMaxManaStored(),
                    "A 和 B 在同一個網路，應顯示相同容量。A=" + a.getMaxManaStored() + " B=" + b.getMaxManaStored()
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 3. 移除一個導管後，剩餘導管的網路容量縮小
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 80)
    public static void removingConduitReducesNetworkCapacity(GameTestHelper helper) {
        helper.setBlock(POS_A, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());
        helper.setBlock(POS_B, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());

        helper.runAtTickTime(5, () -> {
            // 確認兩個導管已加入同一網路
            ArcaneConduitBlockEntity a = getConduit(helper, POS_A);
            helper.assertTrue(
                    a.getMaxManaStored() == ConduitTier.BASIC.getBufferCapacity() * 2,
                    "移除前網路容量應為 2x BASIC，實際=" + a.getMaxManaStored()
            );
            helper.destroyBlock(POS_B);
        });

        helper.runAtTickTime(10, () -> {
            ArcaneConduitBlockEntity a = getConduit(helper, POS_A);
            int expected = ConduitTier.BASIC.getBufferCapacity();
            helper.assertTrue(
                    a.getMaxManaStored() == expected,
                    "移除 B 後 A 的容量應縮回 " + expected + "，實際=" + a.getMaxManaStored()
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 4. 網路內魔力超過縮減後的容量時，應自動截斷
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 80)
    public static void manaIsTruncatedWhenCapacityShrinks(GameTestHelper helper) {
        helper.setBlock(POS_A, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());
        helper.setBlock(POS_B, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());

        helper.runAtTickTime(5, () -> {
            ArcaneConduitBlockEntity a = getConduit(helper, POS_A);
            // 塞比單個導管容量更多的魔力（利用兩個導管的共享容量）
            int fillAmount = ConduitTier.BASIC.getBufferCapacity() + 50;
            a.receiveMana(fillAmount, ManaAction.EXECUTE);
            helper.assertTrue(
                    a.getManaStored() == fillAmount,
                    "填入前魔力應為 " + fillAmount + "，實際=" + a.getManaStored()
            );
            helper.destroyBlock(POS_B);
        });

        helper.runAtTickTime(10, () -> {
            ArcaneConduitBlockEntity a = getConduit(helper, POS_A);
            int cap = ConduitTier.BASIC.getBufferCapacity();
            helper.assertTrue(
                    a.getManaStored() <= cap,
                    "移除 B 後魔力應被截斷到 " + cap + "，實際=" + a.getManaStored()
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 5. 混合 tier：BASIC + ADVANCED，容量 = 256 + 1024
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 50)
    public static void mixedTierConduitsStackCorrectly(GameTestHelper helper) {
        helper.setBlock(POS_A, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());
        helper.setBlock(POS_B, ModBlocks.ADVANCED_ARCANE_CONDUIT.get().defaultBlockState());

        helper.runAtTickTime(5, () -> {
            ArcaneConduitBlockEntity a = getConduit(helper, POS_A);
            int expected = ConduitTier.BASIC.getBufferCapacity() + ConduitTier.ADVANCED.getBufferCapacity();
            helper.assertTrue(
                    a.getMaxManaStored() == expected,
                    "BASIC+ADVANCED 網路容量應為 " + expected + "，實際=" + a.getMaxManaStored()
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 6. 導管 tier 升級後，網路容量正確更新
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 50)
    public static void tierUpgradeUpdatesNetworkCapacity(GameTestHelper helper) {
        helper.setBlock(POS_A, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());
        helper.setBlock(POS_B, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());

        helper.runAtTickTime(5, () -> {
            // 升級 B 到 ADVANCED
            ArcaneConduitBlockEntity b = getConduit(helper, POS_B);
            b.setTier(ConduitTier.ADVANCED);
        });

        helper.runAtTickTime(8, () -> {
            ArcaneConduitBlockEntity a = getConduit(helper, POS_A);
            int expected = ConduitTier.BASIC.getBufferCapacity() + ConduitTier.ADVANCED.getBufferCapacity();
            helper.assertTrue(
                    a.getMaxManaStored() == expected,
                    "B 升級後網路容量應為 " + expected + "，實際=" + a.getMaxManaStored()
            );
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 7. NBT 存讀 round-trip：buffer 魔力 + tier 應還原
    //    保護 saveAdditional / loadAdditional（即將抽出網路成員管理時，NBT 仍留在 BE）
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void nbtRoundTripPreservesBufferAndTier(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        BlockState state = ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState();

        ArcaneConduitBlockEntity src = new ArcaneConduitBlockEntity(POS_A, state);
        src.setTier(ConduitTier.ADVANCED);
        src.setBufferMana(777);

        CompoundTag tag = new CompoundTag();
        src.saveAdditional(tag, registries);

        ArcaneConduitBlockEntity dst = new ArcaneConduitBlockEntity(POS_A, state);
        dst.loadAdditional(tag, registries);

        helper.assertTrue(dst.getTier() == ConduitTier.ADVANCED,
                "load 後 tier 應還原為 ADVANCED，實際=" + dst.getTier());
        helper.assertTrue(dst.getBufferManaStored() == 777,
                "load 後 buffer 魔力應還原為 777，實際=" + dst.getBufferManaStored());
        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // 8. 扳手右鍵循環 IO 設定（DISABLED→OUTPUT→INPUT→BOTH→DISABLED）
    //    保護 onUse / showConduitInfo（即將抽到 ConduitInteractionHandler）
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 30)
    public static void wandRightClickCyclesIoConfig(GameTestHelper helper) {
        helper.setBlock(POS_A, ModBlocks.BASIC_ARCANE_CONDUIT.get().defaultBlockState());

        helper.runAtTickTime(5, () -> {
            ArcaneConduitBlockEntity conduit = getConduit(helper, POS_A);
            Direction face = Direction.NORTH;
            IOHandlerUtils.IOType before = conduit.getIOConfig(face);

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack wand = new ItemStack(ModItems.BASIC_TECH_WAND.get());
            ((BasicTechWandItem) wand.getItem()).setMode(wand, BasicTechWandItem.TechWandMode.DIRECTION_CONFIG);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);

            BlockPos abs = helper.absolutePos(POS_A);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), face, abs, false);
            conduit.onUse(conduit.getBlockState(), helper.getLevel(), abs, player, hit);

            IOHandlerUtils.IOType after = conduit.getIOConfig(face);
            helper.assertTrue(after == IOHandlerUtils.nextIOType(before),
                    "扳手右鍵應把 " + face + " 的 IO 從 " + before + " 切到 "
                            + IOHandlerUtils.nextIOType(before) + "，實際=" + after);
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 輔助方法
    // -------------------------------------------------------------------------
    private static ArcaneConduitBlockEntity getConduit(GameTestHelper helper, BlockPos pos) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof ArcaneConduitBlockEntity conduit)) {
            helper.fail("預期 ArcaneConduitBlockEntity at " + pos + "，實際=" + be);
            throw new AssertionError("unreachable");
        }
        return conduit;
    }
}
