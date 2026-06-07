package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * 飛船組裝台掃描邏輯的 GameTest（純 server 邏輯：底座 footprint、組裝架高度、找核心、
 * 建造盒邊界、組裝方塊數）。GUI/渲染不在此測。
 *
 * 佈局慣例（相對座標）：地板 y=0，底座/組裝台同層 y=1，飛船蓋在 y>=2。
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class ShipAssemblyGameTests {

    private static final String TEMPLATE = "empty";

    private static BlockState base()   { return ModBlocks.SHIP_ASSEMBLY_BASE.get().defaultBlockState(); }
    private static BlockState gantry() { return ModBlocks.SHIP_ASSEMBLY_GANTRY.get().defaultBlockState(); }
    private static BlockState pad()    { return ModBlocks.SHIP_ASSEMBLY_PAD.get().defaultBlockState(); }
    private static BlockState core()   { return ModBlocks.SHIP_CORE.get().defaultBlockState(); }
    private static BlockState filler() { return ModBlocks.MANA_BLOCK.get().defaultBlockState(); }
    private static BlockState seat()   { return ModBlocks.SHIP_SEAT.get().defaultBlockState(); }

    /** 鋪 3x3 底座（x∈[3,5], z∈[3,5], y=1）+ 西側放組裝台 (2,1,4)。 */
    private static ShipAssemblyPadBlockEntity setupBaseAndPad(GameTestHelper helper) {
        // 先清空建造區：收船測試用 world.setBlock 寫回的方塊不被 gametest 追蹤清理，
        // 會殘留污染後續測試的盒，導致 scan 多算。每個測試開頭清乾淨。
        for (int x = 2; x <= 8; x++)
            for (int y = 2; y <= 7; y++)
                for (int z = 2; z <= 6; z++)
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        for (int x = 3; x <= 5; x++)
            for (int z = 3; z <= 5; z++)
                helper.setBlock(new BlockPos(x, 1, z), base());
        helper.setBlock(new BlockPos(2, 1, 4), pad());
        var be = helper.getBlockEntity(new BlockPos(2, 1, 4));
        if (!(be instanceof ShipAssemblyPadBlockEntity pad)) {
            helper.fail("ship assembly pad BE not found");
            throw new IllegalStateException();
        }
        return pad;
    }

    private static void expect(GameTestHelper helper, ShipAssemblyPadBlockEntity pad, int slot, int want, String label) {
        int got = pad.getData().get(slot);
        if (got != want) helper.fail(label + " expected " + want + " but got " + got);
    }

    // ── 測試 ────────────────────────────────────────────────────────────────

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void noBaseReportsNoBase(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 4), pad());
        var be = helper.getBlockEntity(new BlockPos(2, 1, 4));
        if (!(be instanceof ShipAssemblyPadBlockEntity pad)) { helper.fail("pad BE missing"); return; }
        pad.scan();
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_STATUS,
                ShipAssemblyPadBlockEntity.STATUS_NO_BASE, "status");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void baseWithNoCoreReportsNoCore(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        pad.scan();
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_STATUS,
                ShipAssemblyPadBlockEntity.STATUS_NO_CORE, "status");
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_BOX_W, 3, "boxW");
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_BOX_D, 3, "boxD");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void singleCoreAssemblesAndCounts(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        // 飛船：核心 + 兩塊填充，連在一起（y=2，在底座上方、盒內）
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(3, 2, 4), filler());
        helper.setBlock(new BlockPos(5, 2, 4), filler());
        pad.scan();
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_STATUS,
                ShipAssemblyPadBlockEntity.STATUS_OK, "status");
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_CORES, 1, "cores");
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_COUNT, 3, "count");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void floodFillStopsAtBoxEdge(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        // 核心 + 沿 +x 連到盒外 (6,2,4)：盒邊界 maxX=5，(6,2,4) 不該被抓
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(5, 2, 4), filler());
        helper.setBlock(new BlockPos(6, 2, 4), filler()); // 盒外
        pad.scan();
        // 只抓到核心 + (5,2,4) = 2 塊，(6,2,4) 在盒外被擋
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_COUNT, 2, "count (box edge)");
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_STATUS,
                ShipAssemblyPadBlockEntity.STATUS_OK, "status");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void multipleCoresReportsMultiCore(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(3, 2, 3), core());
        helper.setBlock(new BlockPos(5, 2, 5), core());
        pad.scan();
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_STATUS,
                ShipAssemblyPadBlockEntity.STATUS_MULTI_CORE, "status");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void assembleShipRemovesBlocksAndSpawnsEntity(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(3, 2, 4), filler());
        helper.setBlock(new BlockPos(5, 2, 4), filler());

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5) // addFreshEntity 下一 tick 才進世界（多測試負載下拉長保險）
                .thenExecute(() -> {
                    expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_STATUS,
                            ShipAssemblyPadBlockEntity.STATUS_LAUNCHED, "status");
                    expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_COUNT, 3, "count");
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 2, 4));
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 2, 4));
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 2, 4));
                    // 直接查 level 的飛船實體，過濾出本測試的船（3 塊），避開鄰測試殘留
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 3)
                            .findFirst().orElse(null);
                    if (ship == null) helper.fail("ship entity with 3 blocks not spawned");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void disassembleReturnsBlocksAndRemovesEntity(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(3, 2, 4), filler());

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 2)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("no ship to disassemble"); return; }
                    if (!ship.disassemble()) helper.fail("disassemble returned false");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertBlockPresent(ModBlocks.SHIP_CORE.get(), new BlockPos(4, 2, 4));
                    helper.assertBlockPresent(ModBlocks.MANA_BLOCK.get(), new BlockPos(3, 2, 4));
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    if (!helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).isEmpty())
                        helper.fail("ship entity not removed after disassemble");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void disassemblePreservesChestContents(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.CHEST.defaultBlockState());
        if (helper.getBlockEntity(new BlockPos(3, 2, 4)) instanceof Container chest) {
            chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        } else {
            helper.fail("chest BE missing");
            return;
        }

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 2)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("no ship"); return; }
                    if (!ship.disassemble()) helper.fail("disassemble returned false");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    if (!(helper.getBlockEntity(new BlockPos(3, 2, 4)) instanceof Container restored)) {
                        helper.fail("chest not restored");
                        return;
                    }
                    ItemStack s = restored.getItem(0);
                    if (!s.is(Items.DIAMOND) || s.getCount() != 5)
                        helper.fail("chest contents lost (got " + s + ")");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void shipStopsAtTerrain(GameTestHelper helper) {
        // 直接測碰撞解算 resolveTerrain（不靠騎乘移動，避免 gametest mock player 騎乘不穩）。
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(4, 2, 5), filler());       // 船朝 +z 延伸（filler 在前）
        helper.setBlock(new BlockPos(4, 2, 7), Blocks.STONE.defaultBlockState()); // 前方牆
        helper.setBlock(new BlockPos(4, 3, 7), Blocks.STONE.defaultBlockState());

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 2)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("no ship"); return; }
                    double startZ = ship.getZ();
                    // 手動往 +z 推 12 步，每步靠 resolveTerrain 解算（撞牆那軸歸零）
                    for (int i = 0; i < 12; i++) {
                        Vec3 allowed = ship.resolveTerrain(new Vec3(0, 0, 0.5));
                        ship.setPos(ship.getX(), ship.getY(), ship.getZ() + allowed.z);
                    }
                    // 牆在核心前 3 格、filler 在前 1 格 → filler 撞牆，位移應 ~1 內停。無碰撞會走 6。
                    double dz = ship.getZ() - startZ;
                    if (dz > 2.5)
                        helper.fail("ship flew through the wall (dz=" + dz + ")");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void seatsAllowMultiplePassengersOneDriver(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(5, 2, 4), seat()); // 2 張座椅 → 2 座位（核心不再算座位，只當無椅後備）
        helper.setBlock(new BlockPos(3, 2, 4), seat());

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    // 過濾出本測試的船（core+2 座椅=3 塊），避開鄰測試殘留的船
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 3)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("ship with core+2 seats not found"); return; }
                    if (ship.getSeats().size() != 2)
                        helper.fail("expected 2 seats, got " + ship.getSeats().size());

                    Player p1 = helper.makeMockPlayer(GameType.SURVIVAL);
                    Player p2 = helper.makeMockPlayer(GameType.SURVIVAL);
                    Player p3 = helper.makeMockPlayer(GameType.SURVIVAL);
                    p1.startRiding(ship, false);
                    p2.startRiding(ship, false);
                    p3.startRiding(ship, false); // 第 3 人應被擋（只有 2 座位，canAddPassenger=false）

                    if (ship.getPassengers().size() != 2)
                        helper.fail("expected 2 passengers, got " + ship.getPassengers().size());
                    if (p3.isPassenger()) helper.fail("3rd passenger should be rejected (only 2 seats)");
                    if (ship.getControllingPassenger() != p1)
                        helper.fail("driver should be the first rider");

                    // 兩人對應不同座位（index + 座標）。直接驗指派邏輯，
                    // 不依賴 positionRider 自動執行（gametest mock player 不一定 tick）
                    int i1 = ship.getPassengers().indexOf(p1);
                    int i2 = ship.getPassengers().indexOf(p2);
                    if (i1 < 0 || i2 < 0 || i1 == i2) helper.fail("passengers share a seat index");
                    if (ship.getSeats().get(i1).equals(ship.getSeats().get(i2)))
                        helper.fail("two passengers mapped to the same seat position");
                })
                .thenSucceed();
    }

    /** 甲板碰撞地基：箱子落到船方塊上方時，restrictMotion 應把向下移動擋住（站得住）。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void deckCollisionStopsFall(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core()); // 單方塊船（核心，full cube 有碰撞）

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 1)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("single-block ship not found"); return; }

                    // 箱子置於船中心正上方（在方塊頂面 y≈ship.getY()+1 之上），向下掉 2 格
                    Vec3 center = new Vec3(ship.getX(), ship.getY() + 2.5, ship.getZ());
                    AABB box = AABB.ofSize(center, 0.6, 1.8, 0.6);
                    Vec3 allowed = ship.restrictMotion(box, new Vec3(0, -2, 0));
                    if (allowed.y < -1.0)
                        helper.fail("fall not stopped by deck, allowed.y=" + allowed.y + " (expected near 0)");

                    // 水平撞牆：箱子在方塊側面同高，往方塊方向移動應被擋（X 受限）
                    Vec3 sideCenter = new Vec3(ship.getX() + 1.2, ship.getY() + 0.5, ship.getZ());
                    AABB sideBox = AABB.ofSize(sideCenter, 0.6, 1.8, 0.6);
                    Vec3 sideAllowed = ship.restrictMotion(sideBox, new Vec3(-1.0, 0, 0));
                    if (sideAllowed.x < -0.95)
                        helper.fail("wall did not block sideways move, allowed.x=" + sideAllowed.x);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 20)
    public static void gantrySetsBoxHeight(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        // 在角落立 3 格高組裝架（top y=4 → height = 4 - baseY(1) = 3）
        helper.setBlock(new BlockPos(3, 2, 3), gantry());
        helper.setBlock(new BlockPos(3, 3, 3), gantry());
        helper.setBlock(new BlockPos(3, 4, 3), gantry());
        helper.setBlock(new BlockPos(4, 2, 4), core());
        pad.scan();
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_BOX_H, 3, "boxH (gantry)");
        helper.succeed();
    }
}
