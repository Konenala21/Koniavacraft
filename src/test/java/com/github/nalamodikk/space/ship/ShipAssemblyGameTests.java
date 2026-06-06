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

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 80)
    public static void shipFliesForwardWithInput(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(4, 2, 5), filler());

        ShipEntity[] shipRef = new ShipEntity[1];
        double[] startZ = new double[1];

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 2)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("no ship spawned"); return; }
                    shipRef[0] = ship;
                    Player p = helper.makeMockPlayer(GameType.SURVIVAL);
                    p.setPos(ship.getX() + 0.5, ship.getY() + 1, ship.getZ() + 0.5);
                    p.startRiding(ship, true);
                    startZ[0] = ship.getZ();
                    ship.setControlInput(1f, 0f, 0, 0f); // yaw=0 → forward = +Z
                })
                .thenIdle(25)
                .thenExecute(() -> {
                    ShipEntity ship = shipRef[0];
                    if (ship == null) { helper.fail("ship lost"); return; }
                    double dz = ship.getZ() - startZ[0];
                    if (dz < 1.0) helper.fail("ship did not fly forward (dz=" + dz + ")");
                    if (!(ship.getControllingPassenger() instanceof Player p)) {
                        helper.fail("rider lost");
                        return;
                    }
                    if (Math.abs(p.getZ() - (ship.getZ() + 0.5)) > 1.5)
                        helper.fail("rider did not move with the ship");
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

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 80)
    public static void shipStopsAtTerrain(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(4, 2, 5), filler()); // 船朝 +z 延伸
        // 前方 +z 放一道牆（z=7，在 setupBaseAndPad 清空範圍 z<=6 之外）
        helper.setBlock(new BlockPos(4, 2, 7), Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(4, 3, 7), Blocks.STONE.defaultBlockState());

        ShipEntity[] shipRef = new ShipEntity[1];

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 2)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("no ship"); return; }
                    shipRef[0] = ship;
                    Player p = helper.makeMockPlayer(GameType.SURVIVAL);
                    p.startRiding(ship, true);
                    ship.setControlInput(1f, 0f, 0, 0f); // yaw=0 → 朝 +z 開
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    ShipEntity ship = shipRef[0];
                    if (ship == null) { helper.fail("ship lost"); return; }
                    // 牆在 z=7，船不該穿過去（核心起點 z=4，無碰撞會飛到 z>10）
                    if (ship.getZ() > 6.5)
                        helper.fail("ship flew through the wall (z=" + ship.getZ() + ")");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void seatsAllowMultiplePassengersOneDriver(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(5, 2, 4), seat()); // 1 個座椅 → 共 2 座位（核心+座椅）

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    // 過濾出本測試的船（core+座椅=2 塊），避開鄰測試殘留的船
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 2)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("ship with core+seat not found"); return; }
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
