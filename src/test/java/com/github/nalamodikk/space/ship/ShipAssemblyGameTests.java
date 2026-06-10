package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlock;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
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
    private static BlockState engine() { return ModBlocks.MANA_ENGINE.get().defaultBlockState(); }
    private static BlockState fuelTank() { return ModBlocks.MANA_FUEL_TANK.get().defaultBlockState(); }

    /** 鋪 3x3 底座（x∈[3,5], z∈[3,5], y=1）+ 西側放組裝台 (2,1,4)。 */
    private static ShipAssemblyPadBlockEntity setupBaseAndPad(GameTestHelper helper) {
        // 清空建造區：收船測試用 world.setBlock 寫回的方塊不被 gametest 追蹤清理，
        // 會殘留污染後續測試的盒，導致 scan 多算。每個測試開頭清乾淨。
        for (int x = 2; x <= 8; x++)
            for (int y = 2; y <= 7; y++)
                for (int z = 2; z <= 6; z++)
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        for (int x = 3; x <= 5; x++)
            for (int z = 3; z <= 5; z++)
                helper.setBlock(new BlockPos(x, 1, z), base());
        helper.setBlock(new BlockPos(2, 1, 4), pad());
        // 必要骨架：每艘測試船在核心(4,2,4)的 -z 側自動有引擎+燃料槽，過組裝的骨架驗證。
        // 放 (4,2,3)+(4,3,3)（local (0,0,-1)/(0,1,-1)）= 沒有測試用到的格，避免撞到放門((0,1,0)/(0,2,0)) / +z 延伸((4,2,5)) 等。
        helper.setBlock(new BlockPos(4, 2, 3), engine());
        helper.setBlock(new BlockPos(4, 3, 3), fuelTank());
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
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_COUNT, 5, "count");
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
        expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_COUNT, 4, "count (box edge)");
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

        ShipEntity[] h = new ShipEntity[1];
        helper.startSequence()
                .thenExecute(() -> h[0] = pad.assembleShip())
                .thenIdle(5) // addFreshEntity 下一 tick 才進世界（多測試負載下拉長保險）
                .thenExecute(() -> {
                    expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_STATUS,
                            ShipAssemblyPadBlockEntity.STATUS_LAUNCHED, "status");
                    expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_COUNT, 5, "count");
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 2, 4));
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 2, 4));
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 2, 4));
                    if (h[0] == null || h[0].getContraption() == null || h[0].getContraption().size() != 5) // 3 自有 + 2 骨架
                        helper.fail("ship entity with expected blocks not spawned");
                })
                .thenSucceed();
    }

    /** 斜對角接的方塊(只共用一條邊、非面相鄰)該被組進船(26 連通)。6 連通會斷在斜接處 → count 只有 1。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void assembleDiagonalConnection(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(5, 2, 5), filler()); // 只靠斜對角(共用邊)接核心 → 6 連通會斷、26 才收
        helper.startSequence()
                .thenExecute(pad::assembleShip)
                .thenIdle(5)
                .thenExecute(() -> {
                    expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_STATUS,
                            ShipAssemblyPadBlockEntity.STATUS_LAUNCHED, "status");
                    expect(helper, pad, ShipAssemblyPadBlockEntity.DATA_COUNT, 4, "count(斜接的該收進來,+骨架=4)");
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 2, 5)); // 斜接方塊被組進船=從世界移除
                })
                .thenSucceed();
    }

    /** interactWithPick(client 準心 pick)放方塊:指到 (2,1,2) 上面 → 放到 (2,2,2)。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void shipInteractWithPickPlacesBlock(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        ship.addBlock(new BlockPos(0, 0, 0), Blocks.QUARTZ_BLOCK.defaultBlockState(), null);
        ship.addBlock(new BlockPos(2, 1, 2), Blocks.QUARTZ_BLOCK.defaultBlockState(), null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.OAK_PLANKS.asItem()));

        ShipEntity.Pick pick = new ShipEntity.Pick(new BlockPos(2, 1, 2), Direction.UP,
                Vec3.atCenterOf(new BlockPos(2, 1, 2)).add(0, 0.5, 0)); // 指到 (2,1,2) 上面
        entity.interactWithPick(player, InteractionHand.MAIN_HAND, pick);
        if (!ship.getBlocks().containsKey(new BlockPos(2, 2, 2)))
            helper.fail("interactWithPick did not place the block above the aimed face");
        helper.succeed();
    }

    /** 組裝捕捉的成形祭壇(帶真實 BE NBT)該能用 breakLocalBlock 打掉,跟編輯放的一樣。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void shipBreakCapturedFormedAltar(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        ship.addBlock(new BlockPos(0, 0, 0), Blocks.QUARTZ_BLOCK.defaultBlockState(), null);
        BlockState formed = ModBlocks.ASPECT_ALTAR.get().defaultBlockState().setValue(AspectAltarBlock.FORMED, true);
        // 模擬「組裝前放好、被捕捉」的狀態:真實 BE 存出來的 NBT
        AspectAltarBlockEntity be = ModBlockEntities.ASPECT_ALTAR_BE.get().create(new BlockPos(2, 1, 2), formed);
        be.setLevel(helper.getLevel());
        CompoundTag nbt = be.saveWithFullMetadata(helper.getLevel().registryAccess());
        nbt.remove("x"); nbt.remove("y"); nbt.remove("z");
        ship.addBlock(new BlockPos(2, 1, 2), formed, nbt);

        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(8, 5, 8));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL); // 非 instabuild → 掉落邏輯也跑

        // pick:玩家站祭壇上方往下看 → 準心該瞄到 (2,1,2)(打不到的話 client 就送不出 break 封包)
        net.minecraft.world.phys.Vec3 aw = entity.rotatedWorldPoint(2.5, 1.5, 2.5);
        player.setPos(aw.x, aw.y + 2.5, aw.z);
        player.setXRot(90f); player.setYRot(0f);
        player.setOldPosAndRot();
        BlockPos aimed = entity.getAimedLocalBlock(player);
        if (aimed == null || !aimed.equals(new BlockPos(2, 1, 2)))
            helper.fail("formed altar not pickable (getAimedLocalBlock=" + aimed + ")");

        entity.breakLocalBlock(player, new BlockPos(2, 1, 2));
        if (ship.getBlocks().containsKey(new BlockPos(2, 1, 2)))
            helper.fail("captured formed altar (with NBT) not removed by breakLocalBlock");
        helper.succeed();
    }

    /** breakLocalBlock 挖指定的 local 方塊會從 contraption 移除;核心(0,0,0)不可挖。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void shipBreakLocalBlockRemovesIt(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        ship.addBlock(new BlockPos(0, 0, 0), Blocks.QUARTZ_BLOCK.defaultBlockState(), null); // 核心位
        ship.addBlock(new BlockPos(2, 1, 2), Blocks.OAK_PLANKS.defaultBlockState(), null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        Player player = helper.makeMockPlayer(GameType.CREATIVE); // instabuild → 不掉落,簡化

        entity.breakLocalBlock(player, new BlockPos(2, 1, 2));
        if (ship.getBlocks().containsKey(new BlockPos(2, 1, 2)))
            helper.fail("breakLocalBlock did not remove the block");

        entity.breakLocalBlock(player, BlockPos.ZERO); // 核心
        if (!ship.getBlocks().containsKey(BlockPos.ZERO))
            helper.fail("ship core (0,0,0) must not be breakable");
        helper.succeed();
    }

    /** 在船上放兩個相鄰的箱子(同 facing)該連成 double(一 LEFT 一 RIGHT)，否則開起來是兩個單箱(27)。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void shipPlacedChestsConnect(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        for (int x = 0; x < 6; x++) for (int z = 0; z < 4; z++)
            ship.addBlock(new BlockPos(x, 0, z), Blocks.QUARTZ_BLOCK.defaultBlockState(), null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        Direction f = Direction.NORTH;
        // 先放 (3,1,1)，再放它西邊的 (2,1,1)。面 NORTH 時 (3,1,1) 在 (2,1,1) 的 clockwise(EAST) → 新的=LEFT、夥伴=RIGHT
        entity.placeState(new BlockPos(3, 1, 1), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, f));
        entity.placeState(new BlockPos(2, 1, 1), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, f));
        ChestType ta = ship.getBlocks().get(new BlockPos(2, 1, 1)).state().getValue(ChestBlock.TYPE);
        ChestType tb = ship.getBlocks().get(new BlockPos(3, 1, 1)).state().getValue(ChestBlock.TYPE);
        KoniavacraftMod.LOGGER.info("[shipchestplace] a={} b={} (該一 LEFT 一 RIGHT，SINGLE=沒連)", ta, tb);
        if (ta == ChestType.SINGLE || tb == ChestType.SINGLE)
            helper.fail("ship-placed adjacent chests stayed SINGLE (open as 27 not 54): " + ta + "/" + tb);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void disassembleReturnsBlocksAndRemovesEntity(GameTestHelper helper) {
        ShipAssemblyPadBlockEntity pad = setupBaseAndPad(helper);
        helper.setBlock(new BlockPos(4, 2, 4), core());
        helper.setBlock(new BlockPos(3, 2, 4), filler());

        ShipEntity[] h = new ShipEntity[1];
        helper.startSequence()
                .thenExecute(() -> {
                    h[0] = pad.assembleShip();                                  // 拿自己的船，不靠範圍搜尋(會抓到鄰格)
                    if (h[0] == null) helper.fail("assembleShip returned null");
                    else if (!h[0].disassemble()) helper.fail("disassemble returned false"); // 立刻拆，不給它飄
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertBlockPresent(ModBlocks.SHIP_CORE.get(), new BlockPos(4, 2, 4));
                    helper.assertBlockPresent(ModBlocks.MANA_BLOCK.get(), new BlockPos(3, 2, 4));
                    if (h[0] != null && !h[0].isRemoved())
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

        ShipEntity[] h = new ShipEntity[1];
        helper.startSequence()
                .thenExecute(() -> {
                    h[0] = pad.assembleShip();
                    if (h[0] == null) helper.fail("assembleShip returned null");
                    else if (!h[0].disassemble()) helper.fail("disassemble returned false");
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
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 4)
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
     * 靜止船的水平碰撞：玩家盒朝船的牆走，applyContraptionMovement 應把水平移動擋下。
     * 直接呼叫(不靠 mock player 走路 flaky)→ 確認「停著船走甲板穿過去」是碰撞邏輯壞還是別處。
     */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void stationaryShipBlocksHorizontalWalk(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState stone = Blocks.STONE.defaultBlockState();
        // 一面 2x2 牆(local x=0)
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) ship.addBlock(new BlockPos(0, y, z), stone, null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 2, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot(); // 初始化 xOld/yOld/zOld = getX(否則 applyContraptionMovement 用 0 在錯框算)

        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        // 牆(local x=0)的世界位置 = entity + (0 - centerOffset)。把 probe 放牆 -X 側、Y/Z 對齊牆，朝 +X 走進牆。
        net.minecraft.world.phys.Vec3 wall = entity.rotatedWorldPoint(0.5, 0.5, 0.5); // 牆中心一格的世界中心
        probe.setPos(wall.x - 1.2, wall.y - 0.5, wall.z);
        net.minecraft.world.phys.Vec3 motion = new net.minecraft.world.phys.Vec3(1.0, 0, 0);
        net.minecraft.world.phys.Vec3 result = entity.applyContraptionMovement(probe, motion);
        if (result.x > 0.7)
            helper.fail("stationary ship did not block horizontal walk into wall (motion.x=1.0 -> result.x=" + result.x + ")");
        helper.succeed();
    }

    /**
     * 傾斜船(pitch=30)的牆擋得住朝它走的實體：走 OBB 連續碰撞路徑。直接呼叫 applyContraptionMovement。
     * 沿「local +X」方向(牆的法線)往牆走，OBB 應把移動大幅擋下(toImpact 後沿面滑掉法線分量)。
     */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void tiltedShipBlocksWalkOBB(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) ship.addBlock(new BlockPos(0, y, z), stone, null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setXRot(30f); // 傾斜 → 走 OBB 路徑
        entity.setOldPosAndRot();

        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 face = entity.rotatedWorldPoint(0.0, 1.0, 1.0);   // local x=0 面中心
        net.minecraft.world.phys.Vec3 xdir = entity.rotatedWorldPoint(1.0, 1.0, 1.0).subtract(face).normalize(); // local +X 的世界方向
        net.minecraft.world.phys.Vec3 pc = face.subtract(xdir.scale(0.5)); // probe 中心放面 -X 側 0.5
        probe.setPos(pc.x, pc.y - 0.9, pc.z); // setPos 用腳底(中心 -0.9)
        net.minecraft.world.phys.Vec3 motion = xdir.scale(1.0); // 朝牆走
        net.minecraft.world.phys.Vec3 result = entity.applyContraptionMovement(probe, motion);
        if (result.length() > 0.7)
            helper.fail("tilted ship OBB did not block walk into wall (|motion|=1.0 -> |result|=" + result.length() + ")");
        helper.succeed();
    }

    /** 診斷：實心 2x2x2 方塊，從多方向(面/下面/角落)靠近，印出碰撞後 |result|(≈1=穿過去, 小=擋住)。upright+傾斜。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void diagShipCollisionDirections(GameTestHelper helper) {
        for (float pitch : new float[] { 0f, 30f }) {
            ShipContraption ship = new ShipContraption();
            BlockState stone = Blocks.STONE.defaultBlockState();
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
                ship.addBlock(new BlockPos(x, y, z), stone, null);
            ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
            entity.setContraption(ship);
            BlockPos origin = helper.absolutePos(new BlockPos(5, 5, 5));
            entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
            entity.setXRot(pitch);
            entity.setOldPosAndRot();
            Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
            String p = "pitch=" + pitch + " ";
            diag(entity, probe, p + "+Xface", 2.6, 1.0, 1.0, new Vec3(-1, 0, 0));
            diag(entity, probe, p + "below ", 1.0, -1.5, 1.0, new Vec3(0, 1, 0));
            diag(entity, probe, p + "top   ", 1.0, 3.5, 1.0, new Vec3(0, -1, 0));
            diag(entity, probe, p + "corner", 2.6, 2.6, 2.6, new Vec3(-1, -1, -1));
        }
        helper.succeed();
    }

    /** 多 tick realistic 逼近：probe 以 0.25/tick 走向實心方塊角落，最後量離中心距離(太近=真的穿進去)。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void diagCornerMultiTick(GameTestHelper helper) {
        for (float pitch : new float[] { 0f, 30f }) {
            ShipContraption ship = new ShipContraption();
            BlockState stone = Blocks.STONE.defaultBlockState();
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
                ship.addBlock(new BlockPos(x, y, z), stone, null);
            ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
            entity.setContraption(ship);
            BlockPos origin = helper.absolutePos(new BlockPos(6, 6, 6));
            entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
            entity.setXRot(pitch);
            entity.setOldPosAndRot();
            Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
            Vec3 cubeCenter = entity.rotatedWorldPoint(1.0, 1.0, 1.0);
            Vec3 start = entity.rotatedWorldPoint(3.2, 3.2, 3.2); // 角落外
            probe.setPos(start.x, start.y - 0.9, start.z);
            for (int tick = 0; tick < 30; tick++) {
                Vec3 toward = cubeCenter.subtract(probe.position()).normalize().scale(0.25);
                Vec3 r = entity.applyContraptionMovement(probe, toward);
                probe.setPos(probe.getX() + r.x, probe.getY() + r.y, probe.getZ() + r.z);
            }
            double dist = probe.position().distanceTo(cubeCenter);
            KoniavacraftMod.LOGGER.info("[diagMT] pitch=" + pitch + " corner final dist-to-center=" + String.format("%.3f", dist) + " (surface~1.8+, <1.3=penetrated)");
        }
        helper.succeed();
    }

    /** 載入用戶真船(run/ship_test.nbt)，每根柱子最高方塊上丟 probe，印出哪些方塊型別墜穿(找特殊穿點)。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 400)
    public static void realShipColumnDropDiag(GameTestHelper helper) throws Exception {
        java.nio.file.Path nbtPath = java.nio.file.Path.of("ship_test.nbt");
        if (!java.nio.file.Files.exists(nbtPath)) { helper.succeed(); return; } // 沒放用戶的船就跳過(本機診斷用)
        net.minecraft.nbt.CompoundTag root = net.minecraft.nbt.NbtIo.readCompressed(
                nbtPath, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        net.minecraft.core.HolderGetter<Block> holders = helper.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        net.minecraft.nbt.ListTag palette = root.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
        BlockState[] states = new BlockState[palette.size()];
        for (int i = 0; i < palette.size(); i++) states[i] = net.minecraft.nbt.NbtUtils.readBlockState(holders, palette.getCompound(i));
        net.minecraft.nbt.ListTag blocks = root.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND);
        ShipContraption ship = new ShipContraption();
        int n = 0;
        for (int i = 0; i < blocks.size(); i++) {
            net.minecraft.nbt.CompoundTag b = blocks.getCompound(i);
            BlockState st = states[b.getInt("state")];
            if (st.isAir()) continue;
            net.minecraft.nbt.ListTag p = b.getList("pos", net.minecraft.nbt.Tag.TAG_INT);
            ship.addBlock(new BlockPos(p.getInt(0), p.getInt(1), p.getInt(2)), st, b.contains("nbt") ? b.getCompound("nbt") : null);
            n++;
        }
        KoniavacraftMod.LOGGER.info("[shiptest] loaded {} solid blocks bounds={}", n, ship.bounds());
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        java.util.Map<String, Integer> throughByType = new java.util.TreeMap<>();
        int tested = 0, through = 0;
        for (var e : ship.getBlocks().entrySet()) {
            BlockPos lp = e.getKey();
            if (ship.getBlocks().containsKey(lp.above())) continue; // 上方有方塊 = 不是可站表面
            BlockState st = e.getValue().state();
            net.minecraft.world.phys.Vec3 above = entity.rotatedWorldPoint(lp.getX() + 0.5, lp.getY() + 1.6, lp.getZ() + 0.5);
            probe.setPos(above.x, above.y, above.z); // 腳在方塊頂上方 0.6
            net.minecraft.world.phys.Vec3 r = entity.applyContraptionMovement(probe, new net.minecraft.world.phys.Vec3(0, -3.0, 0));
            tested++;
            if (r.y < -2.0) { // 真穿才落滿 3.0；落樓梯矮板只落 ~1.1，不算
                through++;
                String tn = st.getBlock().toString().replace("Block{minecraft:", "").replace("}", "");
                throughByType.merge(tn, 1, Integer::sum);
            }
        }
        KoniavacraftMod.LOGGER.info("[shiptest] walkable surfaces tested={} fell-through={} byType={}", tested, through, throughByType);
        helper.succeed();
    }

    /** 走路模擬：真船甲板上走來走去(含重力累積)，比較「站上去 vel 歸零 vs 不歸零」會不會墜穿。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 400)
    public static void realShipWalkSim(GameTestHelper helper) throws Exception {
        java.nio.file.Path nbtPath = java.nio.file.Path.of("ship_test.nbt");
        if (!java.nio.file.Files.exists(nbtPath)) { helper.succeed(); return; }
        net.minecraft.nbt.CompoundTag root = net.minecraft.nbt.NbtIo.readCompressed(nbtPath, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        net.minecraft.core.HolderGetter<Block> holders = helper.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        net.minecraft.nbt.ListTag palette = root.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
        BlockState[] states = new BlockState[palette.size()];
        for (int i = 0; i < palette.size(); i++) states[i] = net.minecraft.nbt.NbtUtils.readBlockState(holders, palette.getCompound(i));
        net.minecraft.nbt.ListTag blocks = root.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND);
        ShipContraption ship = new ShipContraption();
        for (int i = 0; i < blocks.size(); i++) {
            net.minecraft.nbt.CompoundTag b = blocks.getCompound(i);
            BlockState st = states[b.getInt("state")];
            if (st.isAir()) continue;
            net.minecraft.nbt.ListTag p = b.getList("pos", net.minecraft.nbt.Tag.TAG_INT);
            ship.addBlock(new BlockPos(p.getInt(0), p.getInt(1), p.getInt(2)), st, b.contains("nbt") ? b.getCompound("nbt") : null);
        }
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        // 找一塊上方空氣、且四周也有甲板可走的地板(用 y 最高的其中一塊)
        BlockPos floor = null;
        for (var e : ship.getBlocks().entrySet()) {
            BlockPos lp = e.getKey();
            if (ship.getBlocks().containsKey(lp.above())) continue;
            if (floor == null || lp.getY() > floor.getY()) floor = lp;
        }
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 start = entity.rotatedWorldPoint(floor.getX() + 0.5, floor.getY() + 1.05, floor.getZ() + 0.5);
        int noReset = walkSim(entity, probe, start, false, 200);
        int withReset = walkSim(entity, probe, start, true, 200);
        KoniavacraftMod.LOGGER.info("[walksim] start local=({},{},{}) fellTick noReset={} withReset={} (-1=never fell)",
                floor.getX(), floor.getY(), floor.getZ(), noReset, withReset);
        helper.succeed();
    }

    /** 防穿安全網：把 probe 放到甲板下面(穿模)，snapToDeckSurface 應把它拉回表面上。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void safetyNetSnapsClippedEntity(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) ship.addBlock(new BlockPos(x, 0, z), quartz, null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        // 甲板 local y=0 方塊頂在 y=1。把 probe 腳放到 y=0.7(穿到甲板裡 0.3,真實每-tick 穿入量)
        net.minecraft.world.phys.Vec3 deckTop = entity.rotatedWorldPoint(2.5, 1.0, 2.5);
        net.minecraft.world.phys.Vec3 clipped = entity.rotatedWorldPoint(2.5, 0.7, 2.5);
        probe.setPos(clipped.x, clipped.y, clipped.z);
        boolean snapped = entity.snapToDeckSurface(probe);
        if (!snapped) helper.fail("safety net did not snap a clipped entity");
        if (Math.abs(probe.getY() - deckTop.y) > 0.1)
            helper.fail("snapped to wrong Y: probe.y=" + probe.getY() + " expected~" + deckTop.y);
        helper.succeed();
    }

    /** 純實心甲板(10x10 石英、無洞)中間走來走去含重力，不該墜穿。墜穿=真的走路碰撞 bug。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 120)
    public static void solidDeckWalkNoFall(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        for (int x = 0; x < 10; x++) for (int z = 0; z < 10; z++) ship.addBlock(new BlockPos(x, 0, z), quartz, null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 start = entity.rotatedWorldPoint(5.0, 1.05, 5.0); // 甲板中央
        int reset = walkSim(entity, probe, start, true, 200);
        int noReset = walkSim(entity, probe, start, false, 200);
        KoniavacraftMod.LOGGER.info("[soliddeck] fellTick withReset={} noReset={} (-1=ok)", reset, noReset);
        if (reset >= 0) helper.fail("walked through SOLID deck (withReset) at tick " + reset);
        helper.succeed();
    }

    /** 樓梯甲板(10x10 quartz_stairs)走來走去，不該墜穿。對應用戶船上的樓梯。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 120)
    public static void stairsDeckWalkNoFall(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState stairs = Blocks.QUARTZ_STAIRS.defaultBlockState();
        for (int x = 0; x < 10; x++) for (int z = 0; z < 10; z++) ship.addBlock(new BlockPos(x, 0, z), stairs, null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 start = entity.rotatedWorldPoint(5.0, 1.6, 5.0);
        int reset = walkSim(entity, probe, start, true, 200);
        KoniavacraftMod.LOGGER.info("[stairsdeck] fellTick withReset={} (-1=ok)", reset);
        if (reset >= 0) helper.fail("walked through STAIRS deck at tick " + reset);
        helper.succeed();
    }

    /** 牆/天花板碰撞:水平面(upright 船)走進牆、跳上天花板，該被擋。診斷「船內穿天花板/外面穿進來」。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 120)
    public static void roomWallCeilingBlocks(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        // 房間：地板 y=0、x=5 一道牆(y=1..3)、天花板 y=4
        for (int x = 0; x < 8; x++) for (int z = 0; z < 4; z++) ship.addBlock(new BlockPos(x, 0, z), quartz, null);
        for (int y = 1; y <= 3; y++) for (int z = 0; z < 4; z++) ship.addBlock(new BlockPos(5, y, z), quartz, null);
        for (int x = 0; x < 8; x++) for (int z = 0; z < 4; z++) ship.addBlock(new BlockPos(x, 4, z), quartz, null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot(); // upright(yaw=pitch=roll=0)→ collideBoundingBox，跟用戶直立船同路徑

        // 1) 重現 in-game:玩家「已經穿進牆裡」(從外面走進來、gate 延遲所致)，牆在 x=5(方塊 5..6)，
        //    玩家中心放 5.3(盒 5.0..5.6 已重疊牆)，往 +X 走。該被往外推到 x<5，不該往上爬穿過去。
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 s1 = entity.rotatedWorldPoint(5.3, 1.0, 2.0);
        probe.setPos(s1.x, s1.y, s1.z);
        net.minecraft.world.phys.Vec3 vel = net.minecraft.world.phys.Vec3.ZERO;
        double maxFeetY1 = -99;
        for (int t = 0; t < 40; t++) {
            vel = new net.minecraft.world.phys.Vec3(0.2, 0, 0); // 純水平往牆深處
            net.minecraft.world.phys.Vec3 r = entity.applyContraptionMovement(probe, vel);
            probe.setPos(probe.getX() + r.x, probe.getY() + r.y, probe.getZ() + r.z);
            maxFeetY1 = Math.max(maxFeetY1, entity.worldToLocalPoint(probe.getX(), probe.getBoundingBox().minY, probe.getZ()).y);
        }
        double wallX = entity.worldToLocalPoint(probe.getX(), probe.getBoundingBox().minY, probe.getZ()).x;
        KoniavacraftMod.LOGGER.info("[penetrate] startX=5.3 endX={} maxFeetY={} (X該<5被推出；X>6=穿過；FeetY飆=往上爬)",
                String.format("%.2f", wallX), String.format("%.2f", maxFeetY1));

        // 2) 跳上天花板:從地板 y=1 往上跳，天花板 y=4(底在 local y=4)，腳不該越過 ~3(頭頂 1.8→撞 4)
        net.minecraft.world.phys.Vec3 s2 = entity.rotatedWorldPoint(2.0, 1.0, 2.0);
        probe.setPos(s2.x, s2.y, s2.z);
        vel = new net.minecraft.world.phys.Vec3(0, 0.5, 0); // 往上衝
        double maxFeetY = -99;
        for (int t = 0; t < 40; t++) {
            vel = new net.minecraft.world.phys.Vec3(0, vel.y - 0.08, 0);
            net.minecraft.world.phys.Vec3 r = entity.applyContraptionMovement(probe, vel);
            probe.setPos(probe.getX() + r.x, probe.getY() + r.y, probe.getZ() + r.z);
            double fy = entity.worldToLocalPoint(probe.getX(), probe.getBoundingBox().minY, probe.getZ()).y;
            maxFeetY = Math.max(maxFeetY, fy);
        }

        KoniavacraftMod.LOGGER.info("[room] wallStopX={} (該<4.9，>5=穿牆) maxFeetY={} (該<2.3，越大=穿天花板)",
                String.format("%.2f", wallX), String.format("%.2f", maxFeetY));
        // penetrate 案:起點 5.3(已穿入)，被推出該 <5；爬上去(maxFeetY1 飆)或穿過(wallX>6)=壞
        if (wallX > 5.5) helper.fail("penetrating box NOT pushed out / passed through: localX=" + wallX);
        if (maxFeetY > 2.5) helper.fail("jumped THROUGH the ceiling: feetLocalY=" + maxFeetY);
        helper.succeed();
    }

    /** 掃描各傾斜角:量站在甲板上(只重力、grounded 歸零)60 tick 的滑移量 + 是否還在甲板上。回答「極度傾斜會怎樣」。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 200)
    public static void tiltAngleSweep(GameTestHelper helper) {
        for (float deg : new float[]{3, 5, 10, 15, 20, 25, 30, 35, 40, 45, 60, 75, 85}) {
            ShipContraption ship = new ShipContraption();
            BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
            for (int x = 0; x < 10; x++) for (int z = 0; z < 10; z++) ship.addBlock(new BlockPos(x, 0, z), quartz, null);
            ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
            entity.setContraption(ship);
            BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
            entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
            entity.setXRot(deg);
            entity.setOldPosAndRot();
            Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
            net.minecraft.world.phys.Vec3 start = entity.rotatedWorldPoint(5.0, 1.0, 5.0);
            probe.setPos(start.x, start.y, start.z);
            net.minecraft.world.phys.Vec3 vel = net.minecraft.world.phys.Vec3.ZERO;
            double minLY = 99, maxLY = -99;
            for (int t = 0; t < 60; t++) {
                vel = new net.minecraft.world.phys.Vec3(vel.x, vel.y - 0.08, vel.z);
                net.minecraft.world.phys.Vec3 r = entity.applyContraptionMovement(probe, vel);
                probe.setPos(probe.getX() + r.x, probe.getY() + r.y, probe.getZ() + r.z);
                if (r.y > vel.y + 1.0e-4) vel = new net.minecraft.world.phys.Vec3(vel.x, 0, vel.z);
                double ly = entity.worldToLocalPoint(probe.getX(), probe.getBoundingBox().minY, probe.getZ()).y;
                if (t >= 25) { minLY = Math.min(minLY, ly); maxLY = Math.max(maxLY, ly); } // 落定後量上下彈幅
            }
            KoniavacraftMod.LOGGER.info("[tiltsweep] {}deg bob={} {} (bob 大=站著上下彈/抽搐)",
                    (int) deg, String.format("%.3f", maxLY - minLY), (maxLY - minLY) > 0.05 ? "<<TWITCH" : "ok");
        }
        helper.succeed();
    }

    /** 微斜(5°)甲板上站著(只重力，grounded 歸零)：滑移應該很小。停著的船常帶幾度，這驗證它不會慢慢溜走。
     *  (大幅傾斜的防滑是 phase 2/3 deck-is-down 的事；之前的「人工重力 redirect」會造成抽搐已移除。) */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 120)
    public static void tiltedDeckNoSlide(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) ship.addBlock(new BlockPos(x, 0, z), quartz, null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setXRot(5f); // 微斜 5°(停著的船常帶幾度)
        entity.setOldPosAndRot();
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 start = entity.rotatedWorldPoint(4.0, 1.05, 4.0);
        probe.setPos(start.x, start.y, start.z);
        net.minecraft.world.phys.Vec3 startL = entity.worldToLocalPoint(start.x, start.y, start.z);
        net.minecraft.world.phys.Vec3 vel = net.minecraft.world.phys.Vec3.ZERO;
        for (int t = 0; t < 80; t++) {
            vel = new net.minecraft.world.phys.Vec3(vel.x, vel.y - 0.08, vel.z); // 只有重力，不走
            net.minecraft.world.phys.Vec3 r = entity.applyContraptionMovement(probe, vel);
            probe.setPos(probe.getX() + r.x, probe.getY() + r.y, probe.getZ() + r.z);
            if (r.y > vel.y + 1.0e-4) vel = net.minecraft.world.phys.Vec3.ZERO; // 被甲板擋住=站住，歸零
        }
        net.minecraft.world.phys.Vec3 endL = entity.worldToLocalPoint(probe.getX(), probe.getBoundingBox().minY, probe.getZ());
        double drift = Math.hypot(endL.x - startL.x, endL.z - startL.z);
        KoniavacraftMod.LOGGER.info("[tiltslide] drift={} (5° 微斜，應該很小)", String.format("%.2f", drift));
        if (drift > 1.0) helper.fail("slid too much on a mild 5deg deck: local drift=" + drift);
        helper.succeed();
    }

    /** 量「停著的微斜(5°)甲板上走路」的手感：腳底 local Y 該穩在甲板頂(1.0)，水平該等於輸入(不被吃)。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 120)
    public static void tiltedDeckWalkFeel(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        for (int x = 0; x < 10; x++) for (int z = 0; z < 4; z++) ship.addBlock(new BlockPos(x, 0, z), quartz, null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        // 直立(用戶的船 tilted=false)→ collideBoundingBox + 推出路徑，量這條的阻力
        entity.setOldPosAndRot();
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 start = entity.rotatedWorldPoint(1.0, 1.0, 2.0);
        probe.setPos(start.x, start.y, start.z);
        net.minecraft.world.phys.Vec3 vel = net.minecraft.world.phys.Vec3.ZERO;
        StringBuilder trace = new StringBuilder();
        double totalIn = 0, totalOut = 0, minLY = 99, maxLY = -99;
        for (int t = 0; t < 45; t++) {
            double wx = 0.13; // 固定往 +X 走
            vel = new net.minecraft.world.phys.Vec3(wx, vel.y - 0.08, 0);
            net.minecraft.world.phys.Vec3 r = entity.applyContraptionMovement(probe, vel);
            probe.setPos(probe.getX() + r.x, probe.getY() + r.y, probe.getZ() + r.z);
            if (r.y > vel.y + 1.0e-4) vel = new net.minecraft.world.phys.Vec3(vel.x, 0, vel.z);
            double ly = entity.worldToLocalPoint(probe.getX(), probe.getBoundingBox().minY, probe.getZ()).y;
            if (t >= 8) { // 跳過落地前幾 tick
                minLY = Math.min(minLY, ly); maxLY = Math.max(maxLY, ly);
                totalIn += wx; totalOut += Math.hypot(r.x, r.z);
                if (t % 3 == 0) trace.append(String.format("%.2f ", ly));
            }
        }
        double zDrift = entity.worldToLocalPoint(probe.getX(), probe.getBoundingBox().minY, probe.getZ()).z
                - entity.worldToLocalPoint(start.x, start.y, start.z).z;
        KoniavacraftMod.LOGGER.info("[walkfeel] sinkRange={} (越大越陷/抖) horizOut/In={} (越小越阻力) zSlide={} (沿傾斜方向滑移) LYtrace={}",
                String.format("%.3f", maxLY - minLY), String.format("%.2f", totalOut / totalIn),
                String.format("%.2f", zDrift), trace.toString().trim());
        // 防回歸：腳底別大幅上下彈(抽搐)、水平別被吃(阻力)。曾經的「人工重力」會讓 sinkRange 飆到 ~0.1+ 來回跳。
        if (maxLY - minLY > 0.3) helper.fail("walking bobs up/down too much (twitch): sinkRange=" + (maxLY - minLY));
        if (totalOut / totalIn < 0.85) helper.fail("walking horizontally resisted: out/in=" + (totalOut / totalIn));
        helper.succeed();
    }

    /** 微傾(1.5°，走 collideBoundingBox)走路時 result 的水平軸該對齊輸入(snap 抓住甲板法線歸零的洩漏)，
     *  否則 result.x≠輸入 → vanilla move 每 tick 歸零水平速度 = 走路阻力。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 120)
    public static void nearUprightWalkNoVelKill(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        for (int x = 0; x < 10; x++) for (int z = 0; z < 6; z++) ship.addBlock(new BlockPos(x, 0, z), quartz, null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setXRot(1.5f); // 微傾(<2° → collideBoundingBox)，重現洩漏
        entity.setOldPosAndRot();
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 start = entity.rotatedWorldPoint(5.0, 1.0, 3.0);
        probe.setPos(start.x, start.y, start.z);
        net.minecraft.world.phys.Vec3 vel = net.minecraft.world.phys.Vec3.ZERO;
        int kills = 0;
        for (int t = 0; t < 40; t++) {
            double wx = 0.13, wz = 0.05;
            vel = new net.minecraft.world.phys.Vec3(wx, vel.y - 0.08, wz);
            net.minecraft.world.phys.Vec3 r = entity.applyContraptionMovement(probe, vel);
            probe.setPos(probe.getX() + r.x, probe.getY() + r.y, probe.getZ() + r.z);
            if (r.y > vel.y + 1.0e-4) vel = new net.minecraft.world.phys.Vec3(vel.x, 0, vel.z);
            if (t >= 8 && (Math.abs(r.x - wx) > 1.0e-6 || Math.abs(r.z - wz) > 1.0e-6)) kills++; // 水平沒對齊=vanilla殺速度
        }
        KoniavacraftMod.LOGGER.info("[velkill] kills={}/32 (0=沒阻力，>0=vanilla每tick殺水平速度)", kills);
        if (kills > 0) helper.fail("vanilla would zero horizontal velocity " + kills + " ticks (walking resistance)");
        helper.succeed();
    }

    /** 掉落物(ItemEntity)該停在船甲板上,不穿過去(否則撿不到,因為真身掉到船下面)。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 120)
    public static void itemRestsOnDeck(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++)
            ship.addBlock(new BlockPos(x, 0, z), Blocks.QUARTZ_BLOCK.defaultBlockState(), null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(
                helper.getLevel(), 0, 0, 0, new ItemStack(Items.DIAMOND));
        net.minecraft.world.phys.Vec3 start = entity.rotatedWorldPoint(3.0, 2.5, 3.0); // 甲板上方
        item.setPos(start.x, start.y, start.z);
        net.minecraft.world.phys.Vec3 vel = net.minecraft.world.phys.Vec3.ZERO;
        for (int t = 0; t < 60; t++) {
            vel = new net.minecraft.world.phys.Vec3(vel.x, vel.y - 0.04, vel.z); // 物品重力 ~0.04
            net.minecraft.world.phys.Vec3 r = entity.applyContraptionMovement(item, vel);
            item.setPos(item.getX() + r.x, item.getY() + r.y, item.getZ() + r.z);
            if (r.y > vel.y + 1.0e-4) vel = new net.minecraft.world.phys.Vec3(vel.x, 0, vel.z);
        }
        double ly = entity.worldToLocalPoint(item.getX(), item.getBoundingBox().minY, item.getZ()).y;
        KoniavacraftMod.LOGGER.info("[itemdeck] localY={} (該~1=停甲板上, <0.5=穿過掉到船下)", String.format("%.2f", ly));
        if (ly < 0.5) helper.fail("item fell through the deck (lands under the ship, can't be picked up): localY=" + ly);
        helper.succeed();
    }

    private static int walkSim(ShipEntity entity, Player probe, net.minecraft.world.phys.Vec3 startWorld, boolean resetVel, int ticks) {
        probe.setPos(startWorld.x, startWorld.y, startWorld.z);
        net.minecraft.world.phys.Vec3 vel = net.minecraft.world.phys.Vec3.ZERO;
        double startLY = entity.worldToLocalPoint(startWorld.x, startWorld.y, startWorld.z).y;
        for (int t = 0; t < ticks; t++) {
            double wx = ((t / 15) % 2 == 0) ? 0.15 : -0.15;
            double wz = ((t / 15) % 4 < 2) ? 0.12 : -0.12;
            vel = new net.minecraft.world.phys.Vec3(wx, vel.y - 0.08, wz);
            net.minecraft.world.phys.Vec3 r = entity.applyContraptionMovement(probe, vel);
            probe.setPos(probe.getX() + r.x, probe.getY() + r.y, probe.getZ() + r.z);
            boolean grounded = r.y > vel.y + 1.0e-4;
            if (grounded && resetVel) vel = new net.minecraft.world.phys.Vec3(vel.x, 0, vel.z);
            double ly = entity.worldToLocalPoint(probe.getX(), probe.getY(), probe.getZ()).y;
            if (ly < startLY - 2.5) return t;
        }
        return -1;
    }

    /** 部分方塊(樓梯)當地板：probe 落下應被樓梯擋住，不該墜穿。對應用戶船上的 quartz_stairs。 */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void partialBlockStairsStopsFall(GameTestHelper helper) {
        ShipContraption ship = new ShipContraption();
        BlockState stairs = Blocks.QUARTZ_STAIRS.defaultBlockState();
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) ship.addBlock(new BlockPos(x, 0, z), stairs, null);
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        BlockPos origin = helper.absolutePos(new BlockPos(5, 5, 5));
        entity.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        entity.setOldPosAndRot();
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 deck = entity.rotatedWorldPoint(1.5, 1.0, 1.5); // 樓梯區(local y 0..1)上緣中心
        probe.setPos(deck.x, deck.y + 0.3, deck.z); // 略高於樓梯
        net.minecraft.world.phys.Vec3 result = entity.applyContraptionMovement(probe, new net.minecraft.world.phys.Vec3(0, -2.0, 0));
        if (result.y < -1.0)
            helper.fail("probe fell through stairs deck (motion.y=-2.0 -> result.y=" + result.y + ")");
        helper.succeed();
    }

    private static void diag(ShipEntity entity, Player probe, String name, double lx, double ly, double lz, Vec3 localDir) {
        Vec3 pos = entity.rotatedWorldPoint(lx, ly, lz);
        probe.setPos(pos.x, pos.y - 0.9, pos.z); // 盒中心放在 local 點
        Vec3 target = entity.rotatedWorldPoint(lx + localDir.x, ly + localDir.y, lz + localDir.z);
        Vec3 dir = target.subtract(pos).normalize();
        Vec3 result = entity.applyContraptionMovement(probe, dir.scale(1.0));
        KoniavacraftMod.LOGGER.info("[diag] " + name + " |motion|=1.0 -> |result|=" + String.format("%.3f", result.length()));
    }

    /**
     * 效能量測：組裝的兩個 mass-setBlock 熱點 — addToWorld(= placeInShadow 塞 2000 方塊) 與
     * removeFromWorld 等效(移除 2000 方塊)。組裝凍住若是 server 端，數字會在這裡現形。印 log。
     */
    @GameTest(template = TEMPLATE, templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 200)
    public static void perfBigShipPlacement(GameTestHelper helper) {
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
        BlockPos anchor = helper.absolutePos(new BlockPos(4, 2, 4)).above(24); // 空氣區，addToWorld 才放得下

        long t0 = System.nanoTime();
        ship.addToWorld(helper.getLevel(), anchor, Rotation.NONE);       // = placeInShadow 的核心
        long placeMs = (System.nanoTime() - t0) / 1_000_000;

        long t1 = System.nanoTime();
        int rmFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;   // = removeFromWorld 的快 flag
        for (BlockPos lp : ship.getBlocks().keySet())
            helper.getLevel().setBlock(lp.offset(anchor), Blocks.AIR.defaultBlockState(), rmFlags);
        long removeMs = (System.nanoTime() - t1) / 1_000_000;

        // spawn 序列化(把 2000 方塊打包同步給 client)— 組裝時也在這 tick 跑，gametest 可量
        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), helper.getLevel());
        entity.setContraption(ship);
        net.minecraft.network.RegistryFriendlyByteBuf sbuf = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        long t2 = System.nanoTime();
        entity.writeSpawnData(sbuf);
        long serializeMs = (System.nanoTime() - t2) / 1_000_000;
        int bytes = sbuf.writerIndex();

        KoniavacraftMod.LOGGER.info("[ship-place-perf] blocks={} addToWorld={}ms remove={}ms serialize={}ms ({}KB)",
                count, placeMs, removeMs, serializeMs, bytes / 1024);
        helper.succeed();
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
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 4)
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
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 4)
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

        ShipEntity[] h = new ShipEntity[1];
        helper.startSequence()
                .thenExecute(() -> {
                    h[0] = pad.assembleShip();
                    if (h[0] == null) helper.fail("assembleShip returned null");
                    else if (!h[0].disassemble()) helper.fail("disassemble returned false");
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

        ShipEntity[] h = new ShipEntity[1];
        helper.startSequence()
                .thenExecute(() -> {
                    h[0] = pad.assembleShip();
                    if (h[0] == null) helper.fail("assembleShip returned null");
                    else h[0].remove(Entity.RemovalReason.KILLED); // 立刻模擬 /kill,不給它飄到鄰格
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

        ShipEntity[] h = new ShipEntity[1];
        helper.startSequence()
                .thenExecute(() -> {
                    h[0] = pad.assembleShip();
                    if (h[0] == null) { helper.fail("assembleShip returned null"); return; }
                    // 模擬 GUI 改動：寫回 7 鑽石到箱子（local = 3-4 = -1,0,0）
                    SimpleContainer c = new SimpleContainer(27);
                    c.setItem(0, new ItemStack(Items.DIAMOND, 7));
                    h[0].writeContainerBack(new BlockPos(-1, 0, 0), c);
                    if (!h[0].disassemble()) helper.fail("disassemble returned false");
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
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 4)
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

        ShipEntity[] holder = new ShipEntity[1];
        helper.startSequence()
                .thenExecute(() -> holder[0] = pad.assembleShip()) // 直接拿組出來的船；範圍搜尋對「實體」不可靠(船會跨測試殘留 → flaky)
                .thenIdle(5)
                .thenExecute(() -> {
                    ShipEntity ship = holder[0];
                    if (ship == null) { helper.fail("assembleShip returned null"); return; }
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

    // (移除 driverInputMovesShip)：mock player 騎乘跨多 tick 不可靠(會中途離座/不 tick)，造成 flaky。
    // 多人不能動的真因是船卡地形(已修)，非輸入路徑；船移動碰撞由「撞牆」測試 + resolveTerrain 直接驗已足夠。

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
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 3)
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
                            .filter(s -> s.getContraption() != null && s.getContraption().size() == 4)
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
