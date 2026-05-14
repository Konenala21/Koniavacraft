package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.multiblock.AbstractMultiblockControllerBlockEntity;
import com.github.nalamodikk.common.multiblock.api.IWandActivatable;
import com.github.nalamodikk.common.multiblock.api.MultiblockPattern;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class AspectAltarBlockEntity extends AbstractMultiblockControllerBlockEntity implements IWandActivatable {

    //   結構三層（核心在 y=0）：
    //
    //   y= 0：只有核心
    //   y=-1：四斜角 MANA_BLOCK / ALTAR_PILLAR，中間空氣（中空層）
    //   y=-2：四斜角 MANA_BLOCK / ALTAR_PILLAR，中間必要底座（催化物槽）
    //
    //   成形時角落自動換成 ALTAR_PILLAR；解散時還原 MANA_BLOCK。
    private static final Predicate<BlockState> PILLAR_PRED =
            state -> state.is(ModBlocks.MANA_BLOCK.get()) || state.is(ModBlocks.ALTAR_PILLAR.get());

    // y=-2 = 底段（top=false），y=-1 = 頂段（top=true）
    private static final List<Vec3i> PILLAR_BOTTOM = List.of(
            new Vec3i(-1, -2, -1), new Vec3i(-1, -2, 1),
            new Vec3i( 1, -2, -1), new Vec3i( 1, -2, 1)
    );
    private static final List<Vec3i> PILLAR_TOP = List.of(
            new Vec3i(-1, -1, -1), new Vec3i(-1, -1, 1),
            new Vec3i( 1, -1, -1), new Vec3i( 1, -1, 1)
    );
    private static final List<Vec3i> PILLAR_OFFSETS;
    static {
        PILLAR_OFFSETS = new ArrayList<>();
        PILLAR_OFFSETS.addAll(PILLAR_BOTTOM);
        PILLAR_OFFSETS.addAll(PILLAR_TOP);
    }

    private static final MultiblockPattern PATTERN = MultiblockPattern.builder()
            .requireBlock(new Vec3i( 0, -2,  0), ModBlocks.ASPECT_PEDESTAL.get())
            .require(new Vec3i(-1, -2, -1), PILLAR_PRED)
            .require(new Vec3i(-1, -2,  1), PILLAR_PRED)
            .require(new Vec3i( 1, -2, -1), PILLAR_PRED)
            .require(new Vec3i( 1, -2,  1), PILLAR_PRED)
            .require(new Vec3i(-1, -1, -1), PILLAR_PRED)
            .require(new Vec3i(-1, -1,  1), PILLAR_PRED)
            .require(new Vec3i( 1, -1, -1), PILLAR_PRED)
            .require(new Vec3i( 1, -1,  1), PILLAR_PRED)
            .build();

    private static final int CHECK_INTERVAL = 40;
    private static final int MAX_MANA = 50000;
    private static final int MANA_TRANSFER_RATE = 200;
    private static final int PEDESTAL_SCAN_RADIUS = 6;

    private int ticker = 0;
    private int tickCounter = 0;
    private boolean active = false;
    private float progress = 0f;
    private int ritualTick = 0;
    private int ritualMaxTick = 0;

    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA, this::onManaChanged);
    private final EnumMap<Direction, IOHandlerUtils.IOType> directionConfig = new EnumMap<>(Direction.class);

    private final List<AspectPedestalBlockEntity> activePedestals = new ArrayList<>();
    // 中心底座（y=-2 正下方），其物品為催化物
    private AspectPedestalBlockEntity centerPedestal = null;

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
            checkStructure();
            if (isFormed()) scanForPedestals();
        }

        if (tickCounter % 20 == 0) extractManaFromNeighbors();
        tickCounter++;

        if (active) tickRitual();
    }

    private void extractManaFromNeighbors() {
        if (level == null || manaStorage.getManaStored() >= MAX_MANA) return;
        IOHandlerUtils.extractManaFromNeighbors(level, worldPosition, manaStorage, directionConfig, MANA_TRANSFER_RATE);
    }

    private void tickRitual() {
        if (level == null || ritualMaxTick <= 0) return;
        ritualTick++;
        progress = (float) ritualTick / ritualMaxTick;
        if (ritualTick >= ritualMaxTick) completeRitual();
        syncToClient();
    }

    private void completeRitual() {
        if (level == null) return;

        Optional<RecipeHolder<AltarRecipe>> holder = findMatchingRecipe();
        if (holder.isEmpty()) { cancelRitual(); return; }

        AltarRecipe recipe = holder.get().value();
        if (manaStorage.getManaStored() < recipe.getManaCost()) { cancelRitual(); return; }
        manaStorage.extractMana(recipe.getManaCost(), ManaAction.EXECUTE);

        for (AspectPedestalBlockEntity ped : activePedestals) {
            if (!ped.getHeldItem().isEmpty()) ped.consumeItem();
        }

        ItemStack result = recipe.getResult().copy();
        Vec3 drop = Vec3.atCenterOf(worldPosition).add(0, 1.2, 0);
        ItemEntity entity = new ItemEntity(level, drop.x, drop.y, drop.z, result);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);

        active = false;
        progress = 0f;
        ritualTick = 0;
        ritualMaxTick = 0;
        setChanged();
        syncToClient();
    }

    private void cancelRitual() {
        active = false;
        progress = 0f;
        ritualTick = 0;
        ritualMaxTick = 0;
        setChanged();
        syncToClient();
    }

    // ── 儀式觸發 ─────────────────────────────────────────────────────────────

    public Component tryActivate() {
        if (!isFormed())
            return Component.translatable("block.koniava.aspect_altar.not_formed");
        if (active)
            return Component.translatable("block.koniava.aspect_altar.ritual_active");
        if (centerPedestal == null || centerPedestal.getHeldItem().isEmpty())
            return Component.translatable("block.koniava.aspect_altar.no_catalyst");

        Optional<RecipeHolder<AltarRecipe>> holder = findMatchingRecipe();
        if (holder.isEmpty())
            return Component.translatable("block.koniava.aspect_altar.no_recipe");

        AltarRecipe recipe = holder.get().value();
        if (manaStorage.getManaStored() < recipe.getManaCost())
            return Component.translatable("block.koniava.aspect_altar.not_enough_mana",
                    recipe.getManaCost(), manaStorage.getManaStored());

        active = true;
        ritualTick = 0;
        ritualMaxTick = recipe.getProcessingTime();
        progress = 0f;
        setChanged();
        syncToClient();
        return Component.translatable("block.koniava.aspect_altar.ritual_started");
    }

    private Optional<RecipeHolder<AltarRecipe>> findMatchingRecipe() {
        if (level == null || centerPedestal == null) return Optional.empty();
        ItemStack catalyst = centerPedestal.getHeldItem();
        List<ItemStack> ingredients = activePedestals.stream()
                .filter(p -> p != centerPedestal)
                .map(AspectPedestalBlockEntity::getHeldItem)
                .toList();
        AltarRecipe.AltarInput input = new AltarRecipe.AltarInput(catalyst, ingredients);
        return level.getRecipeManager().getRecipeFor(ModRecipes.ALTAR_TYPE.get(), input, level);
    }

    // ── 結構生命週期 ──────────────────────────────────────────────────────────

    @Override
    public void onStructureFormed() {
        if (level == null) return;
        // 角落魔力方塊 → 祭壇柱（底段 top=false，頂段 top=true）
        // 每個角落依據 XZ 象限賦予對應旋轉角度
        BlockState pillarBase = ModBlocks.ALTAR_PILLAR.get().defaultBlockState();
        for (Vec3i offset : PILLAR_BOTTOM) {
            BlockPos p = worldPosition.offset(offset);
            if (level.getBlockState(p).is(ModBlocks.MANA_BLOCK.get())) {
                level.setBlock(p, pillarBase.setValue(AltarPillarBlock.TOP, false), 3);
                applyPillarRotation(p, offset.getX(), offset.getZ());
            }
        }
        for (Vec3i offset : PILLAR_TOP) {
            BlockPos p = worldPosition.offset(offset);
            if (level.getBlockState(p).is(ModBlocks.MANA_BLOCK.get())) {
                level.setBlock(p, pillarBase.setValue(AltarPillarBlock.TOP, true), 3);
                applyPillarRotation(p, offset.getX(), offset.getZ());
            }
        }
        // 核心切換成 formed 模型
        level.setBlock(worldPosition, getBlockState().setValue(AspectAltarBlock.FORMED, true), 2);
        scanForPedestals();
        syncToClient();
    }

    @Override
    public void onStructureInvalid() {
        if (level == null) { clearPedestals(); if (active) cancelRitual(); return; }
        // 祭壇柱 → 還原魔力方塊
        for (Vec3i offset : PILLAR_OFFSETS) {
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
        clearPedestals();
        if (active) cancelRitual();
        syncToClient();
    }

    // ── 動態底座偵測 ──────────────────────────────────────────────────────────

    private void scanForPedestals() {
        if (level == null) return;

        activePedestals.removeIf(ped -> {
            if (ped.isRemoved() || level.getBlockEntity(ped.getBlockPos()) != ped) {
                ped.removedFromController();
                return true;
            }
            return false;
        });

        BlockPos basePos = worldPosition.below(2);
        for (int x = -PEDESTAL_SCAN_RADIUS; x <= PEDESTAL_SCAN_RADIUS; x++) {
            for (int z = -PEDESTAL_SCAN_RADIUS; z <= PEDESTAL_SCAN_RADIUS; z++) {

                BlockPos scanPos = basePos.offset(x, 0, z);
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

    private void syncToClient() {
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
                for (Vec3i offset : PILLAR_OFFSETS) {
                    BlockPos p = worldPosition.offset(offset);
                    if (level.getBlockState(p).is(ModBlocks.ALTAR_PILLAR.get())) {
                        level.setBlock(p, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
                    }
                }
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

    // ── Pattern ───────────────────────────────────────────────────────────────

    @Override
    public MultiblockPattern getPattern() { return PATTERN; }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Active", active);
        tag.putInt("RitualTick", ritualTick);
        tag.putInt("RitualMaxTick", ritualMaxTick);
        tag.putInt("Mana", manaStorage.getManaStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        active = tag.getBoolean("Active");
        ritualTick = tag.getInt("RitualTick");
        ritualMaxTick = tag.getInt("RitualMaxTick");
        progress = ritualMaxTick > 0 ? (float) ritualTick / ritualMaxTick : 0f;
        manaStorage.setMana(tag.getInt("Mana"));
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
