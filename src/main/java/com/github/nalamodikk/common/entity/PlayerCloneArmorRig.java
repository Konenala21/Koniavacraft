package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.event.VoidMirrorEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 二階段方塊機甲的「純視覺幾何」controller（從 PlayerCloneEntity 抽出）。
 *
 * 負責外殼長相（結構模板 / hardcoded 剪影）、變身飛入動畫、跟隨本體擺腿、剝落與清理。
 * 擁有所有 display 清單（不存盤、純視覺，reload 後由 rebuildArmor 重建）。
 *
 * 狀態樞紐（entityData ARMORED / bossEvent / 技能重置 / dimensions / armorHp）仍留在 PlayerCloneEntity，
 * 由 enterArmored / breakArmor / rebuildArmor 主導，這裡只負責被它們呼叫的視覺動作。
 */
class PlayerCloneArmorRig {

    private final PlayerCloneEntity clone;

    PlayerCloneArmorRig(PlayerCloneEntity clone) {
        this.clone = clone;
    }

    // 後接 boss UUID，避免多 boss 場景下 cleanup 清掉別的 boss 的活 display
    private static final String ARMOR_TAG_PREFIX = "koniava_mecha_armor_";
    // 結構模板（data/koniava/structure/mecha_shell.nbt）
    private static final ResourceLocation MECH_TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mecha_shell");
    private static final double ARMORED_ANCHOR_Y = 12.0; // 錨點對應的 world oy（boss 頭部會出現處）
    private static final int ARMOR_TRAVEL_TICKS = 50;    // 每塊方塊飛入移動時間
    private static final int LEG_TOP_ROW = 11;           // 剪影此列(含)以下視為腿
    private static final double LEG_HIP_Y = 3.0;         // 腿頂(髖)高度，繞此擺動

    // 機甲剪影（7 寬人形 + 寬肩窄腰）：o=外殼方塊（材質從 clonedInventory→環境取得）、b=本體核心(不放方塊)、'.'=空
    private static final String[] MECH_SHAPE = {
            "...o...",   // 0 頭頂天線
            "..ooo..",   // 1 頭頂
            ".o.b.o.",   // 2 眼+核心
            "..ooo..",   // 3 下顎
            ".ooooo.",   // 4 頸
            "ooooooo",   // 5 肩線 7 格
            "ooooooo",   // 6 肩甲
            ".ooooo.",   // 7 胸
            ".ooooo.",   // 8 腹
            "..ooo..",   // 9 腰收窄
            ".ooooo.",   // 10 髖
            ".oo.oo.",   // 11 大腿頂 (LEG_TOP_ROW)
            ".oo.oo.",   // 12 大腿
            ".oo.oo.",   // 13 小腿
            ".oo.oo.",   // 14 小腿（無腳板）
    };

    // display 清單（不存盤；變身時建立、跟隨期間更新、破殼/死亡時清理）
    private final List<Display.BlockDisplay> armorParts = new ArrayList<>();
    private final List<Vec3> armorOffsets = new ArrayList<>();      // local 偏移（右 x、上 y、前 z）
    private final List<Vec3> armorSpawnOffsets = new ArrayList<>(); // 變身時方塊的初始飛來位置（local offset）
    private final List<Integer> armorAssembleDelay = new ArrayList<>(); // 每塊方塊飛入的起飛延遲 tick（順序漸快）
    private final List<Integer> armorLegSide = new ArrayList<>();   // 與 armorParts 平行：0=非腿 1=左腿 2=右腿
    private final List<Vec3> turretMountOffsets = new ArrayList<>(); // 結構模板的 LIME_WOOL 砲位（左到右對應 slot index）

    private String armorTag() {
        return ARMOR_TAG_PREFIX + clone.getStringUUID();
    }

    void buildShell(ServerLevel sl) {
        if (tryBuildArmorFromTemplate(sl)) return; // 找得到模板就用，找不到 fallback 下面的 hardcoded 剪影
        int rows = MECH_SHAPE.length;
        for (int row = 0; row < rows; row++) {
            String line = MECH_SHAPE[row];
            int center = line.length() / 2; // 奇數寬度的中央 col (7→3, 5→2)
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                if (ch != 'o') continue; // 只有 'o' 是外殼，b/'.' 跳過
                BlockState state = pickShellBlockState(sl);
                CompoundTag stateTag = new CompoundTag();
                stateTag.put("block_state", NbtUtils.writeBlockState(state));
                double ox = col - center;                // 中央對齊本體
                double oy = (rows - 1 - row);            // 最後一列=腳=本體腳高
                int leg = (row >= LEG_TOP_ROW) ? (col < center ? 1 : (col > center ? 2 : 0)) : 0;
                spawnArmorPart(sl, ox, oy, 0, stateTag, leg);
            }
        }
    }

    // 嘗試從結構模板載入機甲形狀；若找不到回 false fallback 到 hardcoded MECH_SHAPE
    // 模板標記方塊：AMETHYST_BLOCK = 本體錨點，WHITE_WOOL = 一般外殼，RED_WOOL = 左腿，BLUE_WOOL = 右腿
    private boolean tryBuildArmorFromTemplate(ServerLevel sl) {
        var templateOpt = sl.getStructureManager().get(MECH_TEMPLATE_ID);
        if (templateOpt.isEmpty()) return false;
        StructureTemplate template = templateOpt.get();
        StructurePlaceSettings settings =
                new StructurePlaceSettings();
        var anchors = template.filterBlocks(BlockPos.ZERO, settings, Blocks.AMETHYST_BLOCK);
        if (anchors.isEmpty()) return false; // 必須有錨點才能用
        BlockPos anchorPos = anchors.get(0).pos();
        placeArmorMarker(sl, template, settings, anchorPos, Blocks.WHITE_WOOL, 0);
        placeArmorMarker(sl, template, settings, anchorPos, Blocks.RED_WOOL, 1);
        placeArmorMarker(sl, template, settings, anchorPos, Blocks.BLUE_WOOL, 2);
        placeFixedArmorMarker(sl, template, settings, anchorPos, Blocks.YELLOW_WOOL,
                Blocks.SEA_LANTERN.defaultBlockState(), 0); // 機甲眼：固定 SEA_LANTERN 不被 inventory 替換
        collectTurretMounts(template, settings, anchorPos);
        return true;
    }

    private void collectTurretMounts(StructureTemplate template,
                                     StructurePlaceSettings settings,
                                     BlockPos anchorPos) {
        turretMountOffsets.clear();
        var blocks = template.filterBlocks(BlockPos.ZERO, settings, Blocks.LIME_WOOL);
        // 由 X 升序排（左到右），對應到 turret slotIdx 0..N
        blocks.sort(Comparator.comparingInt(info -> info.pos().getX()));
        for (var info : blocks) {
            BlockPos p = info.pos();
            double ox = p.getX() - anchorPos.getX();
            double oy = (p.getY() - anchorPos.getY()) + ARMORED_ANCHOR_Y;
            double oz = p.getZ() - anchorPos.getZ();
            turretMountOffsets.add(new Vec3(ox, oy, oz));
        }
    }

    /** 提供給 FloatingTurretEntity：取得 slotIdx 對應的砲位 local offset（模板定義）。null = 沒有對應砲位，砲走 fallback 軌道。 */
    @Nullable
    Vec3 getTurretMountOffset(int slotIdx) {
        if (slotIdx < 0 || slotIdx >= turretMountOffsets.size()) return null;
        return turretMountOffsets.get(slotIdx);
    }

    private void placeArmorMarker(ServerLevel sl,
                                  StructureTemplate template,
                                  StructurePlaceSettings settings,
                                  BlockPos anchorPos, Block marker, int legSide) {
        var blocks = template.filterBlocks(BlockPos.ZERO, settings, marker);
        for (var info : blocks) {
            BlockPos p = info.pos();
            double ox = p.getX() - anchorPos.getX();
            double oy = (p.getY() - anchorPos.getY()) + ARMORED_ANCHOR_Y;
            double oz = p.getZ() - anchorPos.getZ();
            BlockState state = pickShellBlockState(sl);
            CompoundTag stateTag = new CompoundTag();
            stateTag.put("block_state", NbtUtils.writeBlockState(state));
            spawnArmorPart(sl, ox, oy, oz, stateTag, legSide);
        }
    }

    // 固定材質標記：marker 位置 → 永遠生成 fixedState（不從 inventory/env 取代），用於「機甲眼」這類發光點綴
    private void placeFixedArmorMarker(ServerLevel sl,
                                       StructureTemplate template,
                                       StructurePlaceSettings settings,
                                       BlockPos anchorPos, Block marker,
                                       BlockState fixedState, int legSide) {
        var blocks = template.filterBlocks(BlockPos.ZERO, settings, marker);
        for (var info : blocks) {
            BlockPos p = info.pos();
            double ox = p.getX() - anchorPos.getX();
            double oy = (p.getY() - anchorPos.getY()) + ARMORED_ANCHOR_Y;
            double oz = p.getZ() - anchorPos.getZ();
            CompoundTag stateTag = new CompoundTag();
            stateTag.put("block_state", NbtUtils.writeBlockState(fixedState));
            spawnArmorPart(sl, ox, oy, oz, stateTag, legSide);
        }
    }

    // 機甲外殼方塊來源：優先從 clonedInventory（複製自玩家背包）扣除一個 BlockItem，否則從 boss 周圍環境取一格方塊（記入 modifiedBlocks 供離開時還原），最後 fallback 黑曜石
    private BlockState pickShellBlockState(ServerLevel sl) {
        for (ItemStack stack : clone.getClonedInventory()) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BlockItem bi) {
                stack.shrink(1);
                return bi.getBlock().defaultBlockState();
            }
        }
        BlockState env = takeNearbyTerrainBlock(sl);
        if (env != null) return env;
        return Blocks.OBSIDIAN.defaultBlockState();
    }

    // 從 boss 周圍掃描可用方塊（非空氣、非流體、非基岩、非已被本機改過），挖空並回傳狀態
    private BlockState takeNearbyTerrainBlock(ServerLevel sl) {
        BlockPos bp = clone.blockPosition();
        int r = 10;
        for (int attempts = 0; attempts < 48; attempts++) {
            int dx = sl.random.nextInt(2 * r + 1) - r;
            int dy = sl.random.nextInt(r + 1) - (r / 2); // 偏向腰部高度
            int dz = sl.random.nextInt(2 * r + 1) - r;
            BlockPos p = bp.offset(dx, dy, dz);
            BlockState state = sl.getBlockState(p);
            if (state.isAir()) continue;
            if (!state.getFluidState().isEmpty()) continue;
            if (state.getDestroySpeed(sl, p) < 0) continue; // 不可破壞（基岩等）
            // 避開被自己已經放過或挖過的紀錄方塊（VoidMirrorEvents.addModifiedBlock 用同樣 long key）
            // 註：放/挖都會被加進來，所以這個 check 兼任避開自己組的外殼源頭
            VoidMirrorEvents.addMinedTerrain(p.asLong(), state); // 記錄原始狀態，離開維度時還原
            sl.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            return state;
        }
        return null;
    }

    // Display.BlockDisplay.setBlockState 是 private，只能透過 NBT（block_state）設定外觀
    private void spawnArmorPart(ServerLevel sl, double ox, double oy, double oz, CompoundTag stateTag, int legSide) {
        Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(sl);
        if (d == null) return;
        d.load(stateTag.copy());
        d.addTag(armorTag());
        // 變身過場進行中：方塊先放在隨機遠處（上空 + 散布），tickAssemble 插值飛到正位
        Vec3 spawnOff = clone.phase2TransitionTicks > 0
                ? new Vec3(ox + (sl.random.nextDouble() - 0.5) * 14.0,
                           oy + 10.0 + sl.random.nextDouble() * 6.0,
                           oz + (sl.random.nextDouble() - 0.5) * 14.0)
                : new Vec3(ox, oy, oz);
        d.setPos(clone.getX() + spawnOff.x - 0.5, clone.getY() + spawnOff.y, clone.getZ() + spawnOff.z - 0.5);
        sl.addFreshEntity(d);
        armorParts.add(d);
        armorOffsets.add(new Vec3(ox, oy, oz)); // local 偏移（右 x、上 y、前 z），跟隨時依朝向旋轉
        armorSpawnOffsets.add(spawnOff);
        armorAssembleDelay.add(0); // 暫填，buildShell 結束後 assignAssembleDelays 改寫
        armorLegSide.add(legSide);
    }

    // 變身過場結束後或方塊組裝後，給每塊方塊指派飛入起飛 tick：順序隨機洗牌，間距用 sqrt 縮放 → 早期稀疏、後期密集（觀感由慢到快）
    void assignAssembleDelays(ServerLevel sl) {
        int n = armorParts.size();
        if (n == 0) return;
        List<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) order.add(i);
        java.util.Collections.shuffle(order, new java.util.Random(sl.random.nextLong()));
        int span = Math.max(1, PlayerCloneEntity.PHASE2_TRANSITION_LEN - ARMOR_TRAVEL_TICKS - 5); // 預留 5 tick 收尾
        for (int k = 0; k < n; k++) {
            float u = k / (float) Math.max(1, n - 1);
            float biased = (float) Math.pow(u, 1.8); // 後期更密 → 觀感加速
            int delay = Math.round(biased * span);
            armorAssembleDelay.set(order.get(k), delay);
        }
    }

    // 變身過場期間：每塊方塊各自有起飛延遲（從 spawnOffset 飛到 final offset），單塊用 ease-in（t²，慢→快）。tickFollow 在過場結束後接手。
    void tickAssemble() {
        if (armorParts.isEmpty()) return;
        int elapsed = PlayerCloneEntity.PHASE2_TRANSITION_LEN - clone.phase2TransitionTicks;
        float yawRad = clone.getYRot() * ((float) Math.PI / 180F);
        double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
        for (int i = 0; i < armorParts.size(); i++) {
            Display.BlockDisplay d = armorParts.get(i);
            if (d == null || !d.isAlive()) continue;
            Vec3 f = armorOffsets.get(i);
            Vec3 sp = i < armorSpawnOffsets.size() ? armorSpawnOffsets.get(i) : f;
            int delay = i < armorAssembleDelay.size() ? armorAssembleDelay.get(i) : 0;
            float local = Mth.clamp((elapsed - delay) / (float) ARMOR_TRAVEL_TICKS, 0F, 1F);
            float s = local * local; // ease-in 單塊由慢加速到位
            double ox = Mth.lerp(s, sp.x, f.x);
            double oy = Mth.lerp(s, sp.y, f.y);
            double oz = Mth.lerp(s, sp.z, f.z);
            double wx = ox * cosY - oz * sinY;
            double wz = ox * sinY + oz * cosY;
            d.setPos(clone.getX() + wx - 0.5, clone.getY() + oy, clone.getZ() + wz - 0.5);
        }
    }

    // 每 tick 讓外殼方塊跟著本體移動
    void tickFollow() {
        if (armorParts.isEmpty()) return;
        float yawRad = clone.getYRot() * ((float) Math.PI / 180F);
        double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
        for (int i = 0; i < armorParts.size(); i++) {
            Display.BlockDisplay d = armorParts.get(i);
            if (d == null || !d.isAlive()) continue;
            Vec3 o = armorOffsets.get(i);
            double lz = o.z;
            int leg = armorLegSide.get(i);
            if (leg != 0) { // 腿沿前後方向繞髖擺動（越靠腳擺幅越大），左右反相
                double phase = clone.walkPhase + (leg == 2 ? Math.PI : 0.0);
                lz += (LEG_HIP_Y - o.y) * Math.sin(phase) * 0.35;
            }
            // 依本體朝向把 local 偏移（右 x、前 z）旋轉到世界座標
            double wx = o.x * cosY - lz * sinY;
            double wz = o.x * sinY + lz * cosY;
            d.setPos(clone.getX() + wx - 0.5, clone.getY() + o.y, clone.getZ() + wz - 0.5);
        }
    }

    // 外殼被挖爆的視覺剝落：每塊外殼位置噴深邃石碎裂粒子，再移除（比單一爆炸更像機甲崩解）
    // 狀態切換（entityData / bossEvent / dimensions）由 PlayerCloneEntity.breakArmor 負責
    void breakVisual(ServerLevel sl) {
        BlockParticleOption crumble =
                new BlockParticleOption(ParticleTypes.BLOCK,
                        Blocks.OBSIDIAN.defaultBlockState());
        for (Display.BlockDisplay d : armorParts) {
            if (d == null || !d.isAlive()) continue;
            sl.sendParticles(crumble, d.getX(), d.getY() + 0.5, d.getZ(), 6, 0.2, 0.2, 0.2, 0.12);
        }
        clearParts();
    }

    // 清掉殘留的外殼方塊（死亡 / 離開時呼叫，不留孤兒 display）
    void clearParts() {
        for (Display.BlockDisplay d : armorParts) {
            if (d != null) d.discard();
        }
        armorParts.clear();
        armorOffsets.clear();
        armorSpawnOffsets.clear();
        armorAssembleDelay.clear();
        armorLegSide.clear();
    }

    // 額外掃描場地，清掉「自己」tag 的孤兒 display（fallback：pendingArmorRebuild 未消費就 die、或存盤殘留）
    void discardOwnedDisplays(ServerLevel sl) {
        String myTag = armorTag();
        for (Display.BlockDisplay d : sl.getEntitiesOfClass(Display.BlockDisplay.class,
                clone.getBoundingBox().inflate(24.0), e -> e.getTags().contains(myTag))) {
            d.discard();
        }
    }
}
