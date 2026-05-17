package com.github.nalamodikk.common.block.blockentity.mana_deployer;

import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.UUID;

/** 魔力部署器 — 消耗魔力模擬玩家右鍵（放置/互動）或左鍵（破壞）。 */
public class ManaDeployerBlockEntity extends AbstractManaMachineEntityBlock {

    public enum Mode { RIGHT_CLICK, LEFT_CLICK }

    private static final int   MANA_COST     = 20;
    private static final int   SLOT_ITEM     = 0;
    private static final UUID  FAKE_UUID     =
            UUID.fromString("a5db8636-5bf7-4928-a5e4-e6fc2a18b97d");
    private Mode    mode      = Mode.RIGHT_CLICK;
    private int     tickTimer = 0;
    private boolean hasMana   = false;
    private float   animTimeSec = 0f;
    private boolean enabled   = true;
    private boolean wasRedstonePowered = false;

    private final EnumMap<Direction, IOHandlerUtils.IOType> directionConfig = new EnumMap<>(Direction.class);

    private final ContainerData deployerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> manaStorage != null ? Math.min(manaStorage.getManaStored(), 32767) : 0;
                case 1 -> manaStorage != null ? Math.min(manaStorage.getMaxManaStored(), 32767) : 0;
                case 2 -> mode == Mode.LEFT_CLICK ? 1 : 0;
                case 3 -> intervalTick;
                case 4 -> enabled ? 1 : 0;
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> mode = (value == 1) ? Mode.LEFT_CLICK : Mode.RIGHT_CLICK;
                case 3 -> intervalTick = value;
                case 4 -> enabled = (value == 1);
            }
        }
        @Override
        public int getCount() { return 5; }
    };

    public ContainerData getDeployerData() { return deployerData; }

    public ManaDeployerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_DEPLOYER_BE.get(), pos, state,
                false, 0, 1000, 20, MANA_COST);
        for (Direction dir : Direction.values())
            directionConfig.put(dir, IOHandlerUtils.IOType.INPUT); // 所有面接收魔力
    }

    // ── Item slot (1 slot, max 1 item) ────────────────────────────────────────

    @Override
    protected ItemStackHandler createHandler() {
        return new ItemStackHandler(1) {
            @Override public int getSlotLimit(int slot) { return 1; }
        };
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, ManaDeployerBlockEntity be) {
        if (level.isClientSide) {
            if (be.hasMana && be.enabled) {
                be.animTimeSec = (be.animTimeSec + 1f / be.intervalTick) % 6.75f;
            }
            return;
        }
        boolean currentHasMana = be.manaStorage != null
                && be.manaStorage.getManaStored() >= MANA_COST;
        if (currentHasMana != be.hasMana) {
            be.hasMana = currentHasMana;
            be.syncToClient();
        }
        if (!be.enabled) return;
        be.tickTimer++;
        if (be.tickTimer < be.intervalTick) return;
        be.tickTimer = 0;
        be.tryActivate((ServerLevel) level, state);
    }

    private void tryActivate(ServerLevel serverLevel, BlockState state) {
        if (manaStorage == null || manaStorage.getManaStored() < MANA_COST) return;
        if (itemHandler == null) return;

        Direction facing = state.getValue(ManaDeployerBlock.FACING);
        BlockPos target  = worldPosition.relative(facing);

        FakePlayer fp = FakePlayerFactory.get(serverLevel,
                new GameProfile(FAKE_UUID, "[koniava-deployer]"));
        fp.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        fp.setItemInHand(InteractionHand.MAIN_HAND,
                itemHandler.getStackInSlot(SLOT_ITEM).copy());

        boolean acted = mode == Mode.RIGHT_CLICK
                ? doRightClick(serverLevel, fp, facing, target)
                : doLeftClick(serverLevel, fp, target);

        if (acted) {
            manaStorage.extractMana(MANA_COST, ManaAction.EXECUTE);
            setChanged();
        }
    }

    private boolean doRightClick(ServerLevel level, FakePlayer fp, Direction facing, BlockPos target) {
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(target), facing.getOpposite(), target, false);
        ItemStack hand = fp.getMainHandItem();

        // 1. 嘗試物品的 useOn（放置方塊、使用工具等）
        if (!hand.isEmpty()) {
            UseOnContext ctx = new UseOnContext(level, fp, InteractionHand.MAIN_HAND, hand, hit);
            var result = hand.useOn(ctx);
            if (result != net.minecraft.world.InteractionResult.PASS) return true;
        }

        // 2. 嘗試方塊本身的互動（開門、按鈕、祭壇等）
        var result = level.getBlockState(target).useWithoutItem(level, fp, hit);
        return result != net.minecraft.world.InteractionResult.PASS;
    }

    private boolean doLeftClick(ServerLevel level, FakePlayer fp, BlockPos target) {
        if (level.getBlockState(target).isAir()) return false;
        level.destroyBlock(target, true, fp);
        return true;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public float getAnimTime(float partialTick) {
        if (!hasMana || !enabled) return animTimeSec;
        return (animTimeSec + partialTick / 20f) % 6.75f;
    }

    public Mode      getMode()        { return mode; }
    public boolean   isHasMana()     { return hasMana; }
    public int       getIntervalTick(){ return intervalTick; }
    public ItemStack getHeldItem()   { return itemHandler == null ? ItemStack.EMPTY : itemHandler.getStackInSlot(SLOT_ITEM); }
    public void      setHeldItem(ItemStack s){ if (itemHandler != null) { itemHandler.setStackInSlot(SLOT_ITEM, s); setChanged(); } }

    public void toggleMode() {
        mode = (mode == Mode.RIGHT_CLICK) ? Mode.LEFT_CLICK : Mode.RIGHT_CLICK;
        setChanged();
    }

    public void setIntervalTick(int ticks) {
        intervalTick = Math.clamp(ticks, 10, 12000);
        setChanged();
        syncToClient();
    }

    public void toggleEnabled() {
        enabled = !enabled;
        setChanged();
        syncToClient();
    }

    public boolean isEnabled() { return enabled; }

    public void onNeighborChanged(Level level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered && !wasRedstonePowered) {
            toggleEnabled();
        }
        wasRedstonePowered = powered;
        setChanged();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("DeployerMode", mode.name());
        tag.putBoolean("HasMana", hasMana);
        tag.putBoolean("Enabled", enabled);
        tag.putBoolean("WasRedstonePowered", wasRedstonePowered);
        CompoundTag ioTag = new CompoundTag();
        directionConfig.forEach((dir, type) -> ioTag.putString(dir.getName(), type.name()));
        tag.put("IOConfig", ioTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try { mode = Mode.valueOf(tag.getString("DeployerMode")); }
        catch (Exception ignored) { mode = Mode.RIGHT_CLICK; }
        hasMana = tag.getBoolean("HasMana");
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        wasRedstonePowered = tag.getBoolean("WasRedstonePowered");
        intervalTick = Math.clamp(intervalTick, 10, 12000);
        if (tag.contains("IOConfig")) {
            CompoundTag ioTag = tag.getCompound("IOConfig");
            for (Direction dir : Direction.values()) {
                String key = dir.getName();
                if (ioTag.contains(key)) {
                    try { directionConfig.put(dir, IOHandlerUtils.IOType.valueOf(ioTag.getString(key))); }
                    catch (Exception ignored) {}
                }
            }
        }
    }

    @Override protected boolean canGenerate() { return false; }

    // ── IConfigurableBlock ────────────────────────────────────────────────────

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        directionConfig.put(direction, type);
        onIOConfigChanged();
    }

    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return directionConfig.getOrDefault(direction, IOHandlerUtils.IOType.INPUT);
    }

    @Override
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return new EnumMap<>(directionConfig);
    }

    @Override
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
        directionConfig.clear();
        for (Direction dir : Direction.values())
            directionConfig.put(dir, map.getOrDefault(dir, IOHandlerUtils.IOType.INPUT));
        onIOConfigChanged();
    }

    private void onIOConfigChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ── MenuProvider (no GUI for now) ─────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.mana_deployer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ManaDeployerMenu(id, inv, this);
    }
}
