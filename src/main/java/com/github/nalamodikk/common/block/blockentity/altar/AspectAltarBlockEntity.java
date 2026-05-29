package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.multiblock.AbstractMultiblockControllerBlockEntity;
import com.github.nalamodikk.common.multiblock.api.IWandActivatable;
import com.github.nalamodikk.common.multiblock.api.MultiblockPattern;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.narasystem.nara.event.NaraServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class AspectAltarBlockEntity extends AbstractMultiblockControllerBlockEntity implements IWandActivatable {

    // 結構幾何（柱子/底座/升級環偏移量、成形 pattern）已抽出至 AltarGeometry。

    private static final int CHECK_INTERVAL = 40;
    private static final int MAX_MANA = 50000;
    private static final int MANA_TRANSFER_RATE = 200;
    private static final int PEDESTAL_SCAN_RADIUS = 6;

    // package-private：由 AltarRitualProcessor 讀取
    static final int MIN_COMPLETION_TICKS = 320;
    static final int MAX_COMPLETION_TICKS = 700;
    static final int WARNING_TICKS    = 200; // 10 秒無魔力後爆炸

    private int ticker = 0;
    private int tickCounter = 0;

    // ── 儀式 / 升級狀態（package-private：由 AltarRitualProcessor / AltarRingManager 共享操作）──
    // NBT 序列化與客戶端同步仍集中在本 BE，狀態機邏輯委派給兩個 manager。
    boolean active = false;
    float progress = 0f;
    int ritualTick = 0;
    int ritualMaxTick = 0;
    int upgradeTier = 0;
    long ringPhaseStart = 0;
    int completionAnimTick = 0;
    int completionDuration = MIN_COMPLETION_TICKS;
    UUID activatorUUID = null;
    int manaConsumedSoFar = 0;
    int warningTick = 0;    // 0 = 正常；> 0 = 警告中
    AltarRecipe cachedRecipe = null;

    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA, this::onManaChanged);
    private final EnumMap<Direction, IOHandlerUtils.IOType> directionConfig = new EnumMap<>(Direction.class);

    // package-private：由 AltarRitualProcessor 讀取（findMatchingRecipe / completeRitual）
    final List<AspectPedestalBlockEntity> activePedestals = new ArrayList<>();
    // 中心底座（y=-2 正下方），其物品為催化物
    AspectPedestalBlockEntity centerPedestal = null;

    private final AltarRitualProcessor ritualProcessor = new AltarRitualProcessor(this);
    private final AltarRingManager ringManager = new AltarRingManager(this);

    public AspectAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASPECT_ALTAR_BE.get(), pos, state);
        for (Direction d : Direction.values()) {
            directionConfig.put(d, IOHandlerUtils.IOType.INPUT);
        }
    }

    private void onManaChanged() {
        setChanged();
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public void tick() {
        if (level == null || level.isClientSide()) return;

        if (++ticker >= CHECK_INTERVAL) {
            ticker = 0;
            if (isFormed()) {
                // 驗證結構完整性；若柱子被打掉則自動解散（成形仍需法杖觸發）
                checkStructure();
            }
            if (isFormed()) {
                scanForPedestals();
                refreshUpgradeTier();
            }
        }

        if (tickCounter % 20 == 0) extractManaFromNeighbors();
        tickCounter++;

        if (active) ritualProcessor.tick();

        if (completionAnimTick > 0) {
            completionAnimTick--;
        }
    }

    private void extractManaFromNeighbors() {
        if (level == null || manaStorage.getManaStored() >= MAX_MANA) return;
        IOHandlerUtils.extractManaFromNeighbors(level, worldPosition, manaStorage, directionConfig, MANA_TRANSFER_RATE);
    }

    // ── 儀式觸發（委派給 AltarRitualProcessor）──────────────────────────────────

    public Component tryActivate(UUID playerUUID) {
        return ritualProcessor.tryActivate(playerUUID);
    }

    // ── 結構生命週期 ──────────────────────────────────────────────────────────

    @Override
    public void onStructureFormed() {
        if (level == null) return;
        // 角落魔力方塊 → 祭壇柱（底段 top=false，頂段 top=true）
        // 每個角落依據 XZ 象限賦予對應旋轉角度
        BlockState pillarBase = ModBlocks.ALTAR_PILLAR.get().defaultBlockState();
        for (Vec3i offset : AltarGeometry.PILLAR_BOTTOM) {
            BlockPos p = worldPosition.offset(offset);
            if (level.getBlockState(p).is(ModBlocks.MANA_BLOCK.get())) {
                level.setBlock(p, pillarBase.setValue(AltarPillarBlock.TOP, false), 3);
                applyPillarRotation(p, offset.getX(), offset.getZ());
            }
        }
        for (Vec3i offset : AltarGeometry.PILLAR_TOP) {
            BlockPos p = worldPosition.offset(offset);
            if (level.getBlockState(p).is(ModBlocks.MANA_BLOCK.get())) {
                level.setBlock(p, pillarBase.setValue(AltarPillarBlock.TOP, true), 3);
                applyPillarRotation(p, offset.getX(), offset.getZ());
            }
        }
        // 核心切換成 formed 模型
        level.setBlock(worldPosition, getBlockState().setValue(AspectAltarBlock.FORMED, true), 2);
        scanForPedestals();
        refreshUpgradeTier();
        ringPhaseStart = level.getGameTime();
        syncToClient();
        // 成形音效：音符盒 harp 音色，上揚音階
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP.value(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 0.8f);
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP.value(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.0f);
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP.value(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.26f);
        syncToClient();
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.entity.player.Player nearest = serverLevel.getNearestPlayer(
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 64.0, false);
            if (nearest instanceof net.minecraft.server.level.ServerPlayer sp) {
                NaraServerEvents.scheduleFirstAltarFormedTutorial(sp);
            }
        }
    }

    @Override
    public void onStructureInvalid() {
        if (level == null) { clearPedestals(); if (active) ritualProcessor.cancelRitual(); return; }
        // 祭壇柱 → 還原魔力方塊
        for (Vec3i offset : AltarGeometry.PILLAR_OFFSETS) {
            BlockPos p = worldPosition.offset(offset);
            if (level.getBlockState(p).is(ModBlocks.ALTAR_PILLAR.get())) {
                level.setBlock(p, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
            }
        }
        // 核心切回未成形模型
        BlockState cs = getBlockState();
        if (cs.hasProperty(AspectAltarBlock.FORMED)) {
            level.setBlock(worldPosition, cs.setValue(AspectAltarBlock.FORMED, false), 2);
        }
        // 環狀 RESONANCE_RING → 還原魔力方塊
        for (int i = 0; i < Math.min(upgradeTier, AltarGeometry.ALL_RINGS.size()); i++) ringManager.restoreRingBlocks(AltarGeometry.ALL_RINGS.get(i));
        clearPedestals();
        if (active) ritualProcessor.cancelRitual();
        upgradeTier = 0;
        syncToClient();
    }

    // ── 動態底座偵測 ──────────────────────────────────────────────────────────

    private void scanForPedestals() {
        if (level == null) return;

        activePedestals.removeIf(ped -> {
            if (ped.isRemoved() || level.getBlockEntity(ped.getBlockPos()) != ped) {
                ped.removedFromController();
                if (ped == centerPedestal) centerPedestal = null;
                return true;
            }
            return false;
        });

        // Perf: 用 getBlockState 預過濾（chunk 陣列查找，~100ns）才打 getBlockEntity（chunk + BE map，~1-5µs）
        // 169 個位置原本全都 BE lookup 約 0.2-1ms/scan，預過濾後只有真正放底座的位置 (~0-8) 做 BE lookup
        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
        BlockPos basePos = worldPosition.below(2);
        var pedestalBlock = com.github.nalamodikk.register.ModBlocks.ASPECT_PEDESTAL.get();
        for (int x = -PEDESTAL_SCAN_RADIUS; x <= PEDESTAL_SCAN_RADIUS; x++) {
            for (int z = -PEDESTAL_SCAN_RADIUS; z <= PEDESTAL_SCAN_RADIUS; z++) {
                scanPos.set(basePos.getX() + x, basePos.getY(), basePos.getZ() + z);
                if (!level.getBlockState(scanPos).is(pedestalBlock)) continue;
                if (level.getBlockEntity(scanPos) instanceof AspectPedestalBlockEntity ped) {
                    if (x == 0 && z == 0) centerPedestal = ped;
                    if (!activePedestals.contains(ped)) {
                        activePedestals.add(ped);
                        ped.addedToController(this);
                    }
                }
            }
        }
    }

    private void clearPedestals() {
        for (AspectPedestalBlockEntity ped : activePedestals) ped.removedFromController();
        activePedestals.clear();
        centerPedestal = null;
    }

    // ── 公開存取 ──────────────────────────────────────────────────────────────

    public List<ItemStack> getPedestalItems() {
        List<ItemStack> items = new ArrayList<>();
        for (AspectPedestalBlockEntity ped : activePedestals) items.add(ped.getHeldItem());
        return items;
    }

    public ItemStack getCatalyst() {
        return centerPedestal != null ? centerPedestal.getHeldItem() : ItemStack.EMPTY;
    }

    public boolean isActive() { return active; }
    public float getProgress() { return progress; }
    public ManaStorage getManaStorage() { return manaStorage; }
    public int getManaStored() { return manaStorage.getManaStored(); }
    public int getMaxMana() { return MAX_MANA; }

    @Override
    public net.minecraft.network.chat.Component onWandActivate(net.minecraft.world.entity.player.Player player) {
        checkStructure();
        return isFormed()
                ? net.minecraft.network.chat.Component.translatable("message.koniava.altar.formed")
                : net.minecraft.network.chat.Component.translatable("message.koniava.altar.not_formed");
    }

    // package-private：AltarRitualProcessor / AltarRingManager 同步用
    void syncToClient() {
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void setRemoved() {
        // 世界關閉時 setRemoved 也會被呼叫，此時不能 setBlock 否則會卡死
        // 只有 server 還在運行（玩家手動打掉方塊）時才還原柱子
        if (level != null && !level.isClientSide() && isFormed()) {
            net.minecraft.server.MinecraftServer server = level.getServer();
            if (server != null && server.isRunning()) {
                for (Vec3i offset : AltarGeometry.PILLAR_OFFSETS) {
                    BlockPos p = worldPosition.offset(offset);
                    if (level.getBlockState(p).is(ModBlocks.ALTAR_PILLAR.get())) {
                        level.setBlock(p, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
                    }
                }
                for (int i = 0; i < Math.min(upgradeTier, AltarGeometry.ALL_RINGS.size()); i++) ringManager.restoreRingBlocks(AltarGeometry.ALL_RINGS.get(i));
            }
            clearPedestals();
            active = false;
        }
        super.setRemoved();
    }

    private void applyPillarRotation(BlockPos pos, int dx, int dz) {
        if (level == null) return;
        if (!(level.getBlockEntity(pos) instanceof AltarPillarBlockEntity pillar)) return;
        int rot;
        if (dx < 0 && dz > 0)      rot = 90;  // SW ✓
        else if (dx > 0 && dz > 0) rot = 0;   // SE (對調)
        else if (dx > 0 && dz < 0) rot = 270; // NE ✓
        else                        rot = 180; // NW (對調)
        pillar.setRotation(rot);
    }

    // ── 升級 Tier ────────────────────────────────────────────────────────────

    public int getUpgradeTier() { return upgradeTier; }
    public long getRingPhaseStart() { return ringPhaseStart; }
    public int getCompletionAnimTick() { return completionAnimTick; }
    public int getCompletionDuration() { return completionDuration; }

    // 委派給 AltarRingManager（升級環偵測 / 替換 / tier 推進）
    public void refreshUpgradeTier() {
        ringManager.refreshUpgradeTier();
    }

    // ── Pattern ───────────────────────────────────────────────────────────────

    @Override
    public MultiblockPattern getPattern() { return AltarGeometry.PATTERN; }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Active", active);
        tag.putInt("RitualTick", ritualTick);
        tag.putInt("RitualMaxTick", ritualMaxTick);
        tag.putInt("Mana", manaStorage.getManaStored());
        tag.putInt("UpgradeTier", upgradeTier);
        tag.putLong("RingPhaseStart", ringPhaseStart);
        tag.putInt("ManaConsumedSoFar", manaConsumedSoFar);
        if (activatorUUID != null) tag.putString("ActivatorUUID", activatorUUID.toString());
        // CompletionAnimTick intentionally not saved — cosmetic only, resets on world load
        // WarningTick intentionally not saved — always reset to 0 on load for a fresh grace window
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        active = tag.getBoolean("Active");
        ritualTick = tag.getInt("RitualTick");
        ritualMaxTick = tag.getInt("RitualMaxTick");
        progress = ritualMaxTick > 0 ? (float) ritualTick / ritualMaxTick : 0f;
        manaStorage.setMana(tag.getInt("Mana"));
        upgradeTier = tag.getInt("UpgradeTier");
        ringPhaseStart = tag.getLong("RingPhaseStart");
        warningTick = 0; // always reset on load — player gets a fresh warning window after reload
        manaConsumedSoFar = tag.getInt("ManaConsumedSoFar");
        activatorUUID = tag.contains("ActivatorUUID") ? UUID.fromString(tag.getString("ActivatorUUID")) : null;
        // cachedRecipe not serializable; re-resolved lazily in tickRitual() on first tick
        // completionAnimTick not loaded — always starts at 0 on world load
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static <T extends BlockEntity>
    BlockEntityTicker<T> getTicker(Level level, BlockEntityType<T> type) {
        return (lvl, pos, state, be) -> {
            if (be instanceof AspectAltarBlockEntity altar) altar.tick();
        };
    }
}
