package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

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

    /**
     * VM1+VM2：方塊放進影子維度 + 改影子方塊後鏡射回視覺 contraption。
     * 注意：GameTestServer 不會為自訂 datapack 維度建 ServerLevel，所以 ship_shadow 在測試 server 不存在，
     * 整套 shadow 邏輯 no-op。此時本測試「跳過」(直接 succeed)；真正驗證要 runClient 實機。
     */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 120)
    public static void shipBlocksLiveInShadowAndMirrorBack(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(3, 2, 4), filler()); // local (-1,0,0)

        ShipEntity[] shipRef = new ShipEntity[1];
        BlockPos[] anchorRef = new BlockPos[1];

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
                    ServerLevel shadow = helper.getLevel().getServer().getLevel(ModDimensions.SHIP_SHADOW);
                    BlockPos anchor = ship.getShadowAnchor();
                    if (shadow == null || anchor == null) return; // gametest server 無自訂維度 → 跳過(thenSucceed 仍會過)
                    anchorRef[0] = anchor;
                    // 方塊真的放進影子了？
                    if (!shadow.getBlockState(anchor).is(ModBlocks.SHIP_CORE.get()))
                        helper.fail("core not placed in shadow");
                    if (!shadow.getBlockState(anchor.offset(-1, 0, 0)).is(ModBlocks.MANA_BLOCK.get()))
                        helper.fail("filler not placed in shadow");
                    // 模擬影子裡的變化（機器運轉/作物生長那種 blockstate 變）
                    shadow.setBlock(anchor.offset(-1, 0, 0), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                })
                .thenIdle(34) // 等 tickServerMirror（每 16 tick）跑到
                .thenExecute(() -> {
                    if (anchorRef[0] == null) return; // 跳過（無自訂維度）
                    var info = shipRef[0].getContraption().getBlocks().get(new BlockPos(-1, 0, 0));
                    if (info == null || !info.state().is(Blocks.GOLD_BLOCK))
                        helper.fail("mirror did not reflect shadow change (got "
                                + (info == null ? "null" : info.state().getBlock()) + ")");
                })
                .thenSucceed();
    }

    /**
     * VM1：機器真的在影子維度裡 tick。組裝一台頂部有太陽能收集器的船（影子是固定正午+天光），
     * 等它在影子裡產魔力 → 影子收集器 mana > 0。headless GameTestServer 無維度 → 跳過；runClient `/test` 才真跑。
     */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 160)
    public static void shipMachineTicksInShadow(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(5, 2, 4), ModBlocks.SOLAR_MANA_COLLECTOR.get().defaultBlockState()); // local (1,0,0)，同層在組裝盒內、上方見天

        ShipEntity[] shipRef = new ShipEntity[1];
        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(6);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null).findFirst().orElse(null);
                    if (ship == null) { helper.fail("no ship"); return; }
                    shipRef[0] = ship;
                })
                .thenIdle(100) // 等收集器在影子裡產魔力
                .thenExecute(() -> {
                    ShipEntity ship = shipRef[0];
                    if (ship == null || ship.getShadowAnchor() == null) return; // 無維度(headless) → 跳過
                    ServerLevel shadow = helper.getLevel().getServer().getLevel(ModDimensions.SHIP_SHADOW);
                    if (shadow == null) return;
                    // 直接從 contraption 找收集器的真實 local，不猜座標
                    BlockPos collectorLocal = ship.getContraption().getBlocks().entrySet().stream()
                            .filter(e -> e.getValue().state().is(ModBlocks.SOLAR_MANA_COLLECTOR.get()))
                            .map(Map.Entry::getKey).findFirst().orElse(null);
                    if (collectorLocal == null) {
                        helper.fail("collector not in contraption (size=" + ship.getContraption().getBlocks().size() + ")");
                        return;
                    }
                    BlockPos sp = ship.getShadowAnchor().offset(collectorLocal.getX(), collectorLocal.getY(), collectorLocal.getZ());
                    BlockEntity be = shadow.getBlockEntity(sp);
                    if (!(be instanceof SolarManaCollectorBlockEntity solar)) {
                        helper.fail("collector at local " + collectorLocal + " -> shadow " + sp
                                + " = " + shadow.getBlockState(sp).getBlock() + " be=" + be);
                        return;
                    }
                    if (solar.getManaStored() <= 0)
                        helper.fail("collector produced no mana in shadow (mana=" + solar.getManaStored() + ")");
                })
                .thenSucceed();
    }

    /**
     * 效能量測：程式化建一台接近 MAX_BLOCKS 的實心船，量三個熱點：localCollisionShape 建構(合併 N 個盒)、
     * resolveTerrain(移動時每 tick 迭代全船算地形碰撞，最可疑)、deck 碰撞查詢。數字印到 log，過慢則 fail。
     * 不需影子維度，headless 也能跑。
     */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 200)
    public static void perfBigShipCollision(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState stone = Blocks.STONE.defaultBlockState();
        int count = 0;
        outer:
        for (int x = -6; x <= 5; x++)
            for (int y = 0; y < 12; y++)
                for (int z = -7; z <= 6; z++) {
                    if (count >= ShipContraption.MAX_BLOCKS) break outer;
                    ship.addBlock(new BlockPos(x, y, z), stone, null);
                    count++;
                }
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 2, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);

        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        probe.setPos(entity.getX(), entity.getY(), entity.getZ()); // 船中心，碰撞 gate 才會真的跑
        Vec3 motion = new Vec3(0.3, -0.1, 0.2);

        long t0 = System.nanoTime();
        entity.applyContraptionMovement(probe, motion);          // 第一次觸發 localCollisionShape 建構
        long buildUs = (System.nanoTime() - t0) / 1000;

        int n = 200;
        long t1 = System.nanoTime();
        for (int i = 0; i < n; i++) entity.resolveTerrain(motion);
        long terrainUs = (System.nanoTime() - t1) / 1000 / n;

        long t2 = System.nanoTime();
        for (int i = 0; i < n; i++) entity.applyContraptionMovement(probe, motion);
        long deckUs = (System.nanoTime() - t2) / 1000 / n;

        KoniavacraftMod.LOGGER.info("[ship-perf] blocks={} shapeBuild={}us resolveTerrain={}us/call deckCollide={}us/call",
                count, buildUs, terrainUs, deckUs);
        // 警戒線：一 tick 預算 50ms(20 TPS)；resolveTerrain 每 tick > 5ms 在大船移動時會明顯吃 tick
        if (terrainUs > 5000)
            helper.fail("resolveTerrain too slow: " + terrainUs + "us/call (blocks=" + count + ") — 需優化(只用外殼方塊算地形碰撞)");
        helper.succeed();
    }

    /** Phase 3 箱子：用 writeContainerBack 改船上箱子內容 → 拆解後箱子保留新物品（寫回持久化）。 */
    /** 停船編輯：放門要放上下兩半（雙方塊），HALF 正確。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void editPlacesDoorBothHalves(GameTestHelper helper) {
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
                    if (ship == null) { helper.fail("no ship"); return; }
                    ship.placeState(new BlockPos(0, 1, 0), Blocks.OAK_DOOR.defaultBlockState());
                    var lower = ship.getContraption().getBlocks().get(new BlockPos(0, 1, 0));
                    var upper = ship.getContraption().getBlocks().get(new BlockPos(0, 2, 0));
                    if (lower == null || upper == null) { helper.fail("door halves missing"); return; }
                    if (lower.state().getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER)
                        helper.fail("lower half wrong");
                    if (upper.state().getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.UPPER)
                        helper.fail("upper half wrong");
                })
                .thenSucceed();
    }

    /** 停船編輯：updateContraptionBlock 能加方塊(state≠air)與刪方塊(state=air)，size 跟著變。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void editAddsAndRemovesBlock(GameTestHelper helper) {
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
                    if (ship == null) { helper.fail("no ship"); return; }
                    int before = ship.getContraption().size();
                    BlockPos newLocal = new BlockPos(0, 1, 0); // 核心上方
                    ship.updateContraptionBlock(newLocal, Blocks.STONE.defaultBlockState());
                    if (ship.getContraption().size() != before + 1
                            || !ship.getContraption().getBlocks().containsKey(newLocal)) {
                        helper.fail("add block failed"); return;
                    }
                    ship.updateContraptionBlock(newLocal, Blocks.AIR.defaultBlockState());
                    if (ship.getContraption().size() != before
                            || ship.getContraption().getBlocks().containsKey(newLocal)) {
                        helper.fail("remove block failed");
                    }
                })
                .thenSucceed();
    }

    /** 正常拆解不該誤觸 remove() 的掉落回歸（intentionalDisassembly flag）：拆完地上不該有掉落物。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void disassembleDoesNotDropItems(GameTestHelper helper) {
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
                    if (ship == null) { helper.fail("no ship"); return; }
                    if (!ship.disassemble()) helper.fail("disassemble returned false");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertBlockPresent(ModBlocks.SHIP_CORE.get(), new BlockPos(4, 2, 4));
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(5);
                    int drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).size();
                    if (drops > 0) helper.fail("normal disassembly dropped " + drops + " items (recovery wrongly fired)");
                })
                .thenSucceed();
    }

    /** 保險：船被 /kill（KILLED）而非正常拆解時，contraption 方塊應寫回世界、不蒸發。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void killedShipRecoversBlocks(GameTestHelper helper) {
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
                    if (ship == null) { helper.fail("no ship"); return; }
                    ship.remove(Entity.RemovalReason.KILLED); // 模擬 /kill
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertBlockPresent(ModBlocks.SHIP_CORE.get(), new BlockPos(4, 2, 4));
                    helper.assertBlockPresent(ModBlocks.MANA_BLOCK.get(), new BlockPos(3, 2, 4));
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void shipChestEditPersists(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.CHEST.defaultBlockState()); // 空箱組裝

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 2)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("no ship"); return; }
                    // 模擬 GUI 改動：寫回 7 鑽石到箱子（local = 3-4 = -1,0,0）
                    SimpleContainer c = new SimpleContainer(27);
                    c.setItem(0, new ItemStack(Items.DIAMOND, 7));
                    ship.writeContainerBack(new BlockPos(-1, 0, 0), c);
                    if (!ship.disassemble()) helper.fail("disassemble returned false");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    if (!(helper.getBlockEntity(new BlockPos(3, 2, 4)) instanceof Container restored)) {
                        helper.fail("chest not restored"); return;
                    }
                    ItemStack s = restored.getItem(0);
                    if (!s.is(Items.DIAMOND) || s.getCount() != 7)
                        helper.fail("written chest contents lost (got " + s + ")");
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

    /**
     * 駕駛輸入要讓船動(server 端 tickServerMovement 全路徑)：mock 駕駛坐駕駛位 + setControlInput(上升)，
     * tick 後船應上升。測「輸入→移動」這條(多人不能動的回歸測試；mock player 蓋 server 端邏輯)。
     */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 80)
    public static void driverInputMovesShip(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(5, 2, 4), seat());
        java.util.concurrent.atomic.AtomicReference<ShipEntity> shipRef = new java.util.concurrent.atomic.AtomicReference<>();
        double[] startY = new double[1];
        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 2)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("ship (core+1 seat) not found"); return; }
                    Player driver = helper.makeMockPlayer(GameType.SURVIVAL);
                    driver.startRiding(ship, false);
                    int seat = ship.seatIndexOf(driver);
                    if (seat < 0 || seat >= ShipEntity.MAX_DRIVERS) {
                        helper.fail("driver not seated in a driver seat (seat=" + seat + ")"); return;
                    }
                    shipRef.set(ship);
                    startY[0] = ship.getY();
                    ship.setControlInput(seat, 0f, 0f, 1, 0f, 0f); // 按跳躍 = 上升
                })
                .thenIdle(20) // 船每 tick 跑 tickServerMovement，應持續上升
                .thenExecute(() -> {
                    ShipEntity ship = shipRef.get();
                    if (ship == null) { helper.fail("no ship captured"); return; }
                    double dy = ship.getY() - startY[0];
                    if (dy < 1.0) helper.fail("driver input did not move the ship up (dy=" + dy + ")");
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

    /** Phase 2 互動方塊：切換船上活板門的 OPEN 狀態（server 端邏輯）。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void interactTogglesTrapdoor(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(5, 2, 4), Blocks.OAK_TRAPDOOR.defaultBlockState());

        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    AABB area = new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(4);
                    ShipEntity ship = helper.getLevel().getEntitiesOfClass(ShipEntity.class, area).stream()
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 2)
                            .findFirst().orElse(null);
                    if (ship == null) { helper.fail("core+trapdoor ship not found"); return; }
                    BlockPos local = new BlockPos(1, 0, 0); // 活板門相對核心錨點
                    var info = ship.getContraption().getBlocks().get(local);
                    if (info == null) { helper.fail("trapdoor not in contraption"); return; }
                    boolean before = info.state().getValue(BlockStateProperties.OPEN);
                    ship.tryToggleBlock(local, info.state());
                    boolean after = ship.getContraption().getBlocks().get(local).state()
                            .getValue(BlockStateProperties.OPEN);
                    if (after == before) helper.fail("trapdoor OPEN did not toggle (" + before + ")");
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
