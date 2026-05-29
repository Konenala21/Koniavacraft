package com.github.nalamodikk.common.item.wand.core;

import com.github.nalamodikk.common.block.blockentity.altar.AltarGeometry;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeBehavior;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.multiblock.api.IWandActivatable;
import com.github.nalamodikk.common.screen.block.shared.UniversalConfigMenu;
import com.github.nalamodikk.common.network.packet.client.BlockHighlightPacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

public enum WandCoreBehavior {

    FORMATION {
        @Override public Component getDescription() { return Component.translatable("tooltip.koniava.core.formation.desc"); }
        private static final int BASE_COOLDOWN = 20;

        @Override
        public InteractionResult useOn(UseOnContext ctx, ItemStack wand) {
            Level level = ctx.getLevel();
            Player player = ctx.getPlayer();
            if (player == null) return InteractionResult.PASS;

            BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
            if (!(be instanceof IWandActivatable activatable)) return InteractionResult.PASS;

            // 客戶端先做基本前置檢查，避免魔力不足時仍播揮手動畫
            if (player.getCooldowns().isOnCooldown(wand.getItem())) return InteractionResult.FAIL;
            int storedClient = wand.getOrDefault(ModDataComponents.MANA_STORED, 0);
            if (storedClient <= 0) return InteractionResult.FAIL;
            if (level.isClientSide) return InteractionResult.SUCCESS;

            WandCoreData data = WandRodItem.getData(wand);
            int stored = wand.getOrDefault(ModDataComponents.MANA_STORED, 0);
            int totalEfficiency = data.sumUpgradeBonus(WandUpgradeBehavior.EFFICIENCY);
            float costMult = Math.max(0.4f, 1.0f - totalEfficiency / 100f);
            int cost = Math.round((500 + level.random.nextInt(1501)) * costMult);
            if (stored < cost) {
                player.displayClientMessage(
                        Component.translatable("message.koniava.wand.not_enough_mana", stored, cost), true);
                return InteractionResult.FAIL;
            }
            wand.set(ModDataComponents.MANA_STORED, stored - cost);
            player.displayClientMessage(activatable.onWandActivate(player), true);

            int totalCooldown = data.sumUpgradeBonus(WandUpgradeBehavior.COOLDOWN);
            int cooldownTicks = Math.max(5, BASE_COOLDOWN - totalCooldown);
            player.getCooldowns().addCooldown(wand.getItem(), cooldownTicks);

            return InteractionResult.SUCCESS;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.koniava.formation_core");
        }

        @Override
        public int getColor() { return 0xFF5599FF; } // 藍
    },

    ACTIVATION {
        @Override public Component getDescription() { return Component.translatable("tooltip.koniava.core.activation.desc"); }
        @Override
        public InteractionResult useOn(UseOnContext ctx, ItemStack wand) {
            Level level = ctx.getLevel();
            Player player = ctx.getPlayer();
            if (player == null) return InteractionResult.PASS;

            BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
            if (!(be instanceof IWandActivatable activatable)) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;

            player.displayClientMessage(activatable.onWandActivate(player), true);
            return InteractionResult.SUCCESS;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.koniava.activation_core");
        }

        @Override
        public int getColor() { return 0xFF44DD88; } // 綠
    },

    IO {
        @Override public Component getDescription() { return Component.translatable("tooltip.koniava.core.io.desc"); }
        @Override
        public InteractionResult useOn(UseOnContext ctx, ItemStack wand) {
            Level level = ctx.getLevel();
            Player player = ctx.getPlayer();
            if (player == null || level.isClientSide) return InteractionResult.PASS;

            BlockPos pos = ctx.getClickedPos();
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof IConfigurableBlock configBlock)) return InteractionResult.PASS;

            if (player instanceof ServerPlayer sp) {
                var manaStorage = CapabilityUtils.getMana(sp.level(), pos, null);
                if (manaStorage != null) {
                    ManaUpdatePacket.sendManaUpdate(sp, pos, manaStorage.getManaStored());
                }
                sp.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("screen.koniava.configure_io");
                    }
                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new UniversalConfigMenu(id, inv, be, wand);
                    }
                }, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeWithCodec(NbtOps.INSTANCE, ItemStack.CODEC, wand);
                    buf.writeWithCodec(NbtOps.INSTANCE, CodecsLibrary.DIRECTION_IOTYPE_MAP, configBlock.getIOMap());
                });
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.koniava.io_core");
        }

        @Override
        public int getColor() { return 0xFFFFCC44; } // 黃
    },

    ROTATION {
        @Override public Component getDescription() { return Component.translatable("tooltip.koniava.core.rotation.desc"); }
        @Override
        public InteractionResult useOn(UseOnContext ctx, ItemStack wand) {
            Level level = ctx.getLevel();
            Player player = ctx.getPlayer();
            if (player == null || level.isClientSide) return InteractionResult.PASS;

            BlockPos pos = ctx.getClickedPos();
            BlockState state = level.getBlockState(pos);

            if (state.hasProperty(BlockStateProperties.FACING)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.FACING,
                        state.getValue(BlockStateProperties.FACING).getClockWise()), 3);
                player.displayClientMessage(
                        Component.translatable("message.koniava.block_rotated_facing"), true);
                return InteractionResult.SUCCESS;
            } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                        state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise()), 3);
                player.displayClientMessage(
                        Component.translatable("message.koniava.block_rotated_horizontal"), true);
                return InteractionResult.SUCCESS;
            }

            player.displayClientMessage(
                    Component.translatable("message.koniava.block_cannot_rotate"), true);
            return InteractionResult.CONSUME;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.koniava.rotation_core");
        }

        @Override
        public int getColor() { return 0xFF44DDEE; } // 青
    },

    RITUAL {
        @Override public Component getDescription() { return Component.translatable("tooltip.koniava.core.ritual.desc"); }
        @Override
        public InteractionResult useOn(UseOnContext ctx, ItemStack wand) {
            Level level = ctx.getLevel();
            Player player = ctx.getPlayer();
            if (player == null || level.isClientSide) return InteractionResult.PASS;

            BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
            if (!(be instanceof AspectAltarBlockEntity altar)) return InteractionResult.PASS;

            player.displayClientMessage(altar.tryActivate(player.getUUID()), true);
            return InteractionResult.SUCCESS;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.koniava.ritual_core");
        }

        @Override
        public int getColor() { return 0xFFFF5577; } // 紅粉
    },

    STRUCTURE_BUILD {
        private static final int MANA_PER_BLOCK = 100;

        @Override public Component getDescription() { return Component.translatable("tooltip.koniava.core.structure_build.desc"); }

        @Override
        public InteractionResult useOn(UseOnContext ctx, ItemStack wand) {
            Level level = ctx.getLevel();
            Player player = ctx.getPlayer();
            if (player == null) return InteractionResult.PASS;

            BlockPos altarPos = ctx.getClickedPos();
            BlockEntity be = level.getBlockEntity(altarPos);
            if (!(be instanceof AspectAltarBlockEntity altar)) return InteractionResult.PASS;

            int stored = wand.getOrDefault(ModDataComponents.MANA_STORED, 0);
            if (stored <= 0) return InteractionResult.FAIL;
            if (level.isClientSide) return InteractionResult.SUCCESS;

            List<BlockPos> missingPillar   = new ArrayList<>();
            List<BlockPos> missingPedestal = new ArrayList<>();
            List<BlockPos> blocked         = new ArrayList<>();
            collectAltarPositions(level, altarPos, altar, missingPillar, missingPedestal, blocked);

            if (!blocked.isEmpty()) {
                if (player instanceof ServerPlayer sp)
                    BlockHighlightPacket.sendToPlayer(sp, blocked, 1);
                player.displayClientMessage(
                        Component.translatable("message.koniava.build_wand.blocked", blocked.size()), true);
                return InteractionResult.SUCCESS;
            }

            if (missingPillar.isEmpty() && missingPedestal.isEmpty()) {
                altar.refreshUpgradeTier();
                Component msg;
                if (!altar.isFormed()) {
                    msg = Component.translatable("message.koniava.build_wand.ready_to_form");
                } else if (altar.getUpgradeTier() >= AltarGeometry.ALL_RINGS.size()) {
                    msg = Component.translatable("message.koniava.build_wand.all_rings_done",
                            AltarGeometry.ALL_RINGS.size());
                } else {
                    msg = Component.translatable("message.koniava.build_wand.ring_ready",
                            altar.getUpgradeTier() + 1);
                }
                player.displayClientMessage(msg, true);
                return InteractionResult.SUCCESS;
            }

            int mana = stored;
            int placed = 0;
            List<BlockPos> stillMissing = new ArrayList<>();

            for (BlockPos pos : missingPillar) {
                if (mana < MANA_PER_BLOCK) { stillMissing.add(pos); continue; }
                if (player.isCreative()) {
                    level.setBlock(pos, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
                    placed++; mana -= MANA_PER_BLOCK;
                } else {
                    int slot = findInInventory(player, ModBlocks.MANA_BLOCK.get().asItem());
                    if (slot >= 0) {
                        level.setBlock(pos, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
                        player.getInventory().getItem(slot).shrink(1);
                        placed++; mana -= MANA_PER_BLOCK;
                    } else { stillMissing.add(pos); }
                }
            }

            for (BlockPos pos : missingPedestal) {
                if (mana < MANA_PER_BLOCK) { stillMissing.add(pos); continue; }
                if (player.isCreative()) {
                    level.setBlock(pos, ModBlocks.ASPECT_PEDESTAL.get().defaultBlockState(), 3);
                    placed++; mana -= MANA_PER_BLOCK;
                } else {
                    int slot = findInInventory(player, ModBlocks.ASPECT_PEDESTAL.get().asItem());
                    if (slot >= 0) {
                        level.setBlock(pos, ModBlocks.ASPECT_PEDESTAL.get().defaultBlockState(), 3);
                        player.getInventory().getItem(slot).shrink(1);
                        placed++; mana -= MANA_PER_BLOCK;
                    } else { stillMissing.add(pos); }
                }
            }

            if (!stillMissing.isEmpty() && player instanceof ServerPlayer sp)
                BlockHighlightPacket.sendToPlayer(sp, stillMissing, 0);

            if (placed > 0) {
                altar.refreshUpgradeTier();
                wand.set(ModDataComponents.MANA_STORED, mana);
            }

            player.displayClientMessage(
                    Component.translatable("message.koniava.build_wand.placed",
                            placed, placed + stillMissing.size()), true);
            return InteractionResult.SUCCESS;
        }

        @Override
        public Component getDisplayName() { return Component.translatable("item.koniava.structure_build_core"); }

        @Override
        public int getColor() { return 0xFFAA66FF; } // 紫
    };

    // ── 結構建造核心 helper methods ───────────────────────────────────────────

    private static void collectAltarPositions(Level level, BlockPos altarPos, AspectAltarBlockEntity altar,
                                               List<BlockPos> missingPillar, List<BlockPos> missingPedestal,
                                               List<BlockPos> blocked) {
        if (!altar.isFormed()) {
            checkPillarPositions(level, altarPos, AltarGeometry.PILLAR_BOTTOM, missingPillar, blocked);
            checkPillarPositions(level, altarPos, AltarGeometry.PILLAR_TOP,   missingPillar, blocked);
            checkPedestalPositions(level, altarPos, AltarGeometry.PEDESTAL_OFFSETS, missingPedestal, blocked);
        } else {
            int nextTier = altar.getUpgradeTier() + 1;
            if (nextTier <= AltarGeometry.ALL_RINGS.size()) {
                checkRingPositions(level, altarPos, AltarGeometry.ALL_RINGS.get(nextTier - 1),
                        missingPillar, blocked);
            }
        }
    }

    private static void checkPillarPositions(Level level, BlockPos altarPos, List<Vec3i> offsets,
                                              List<BlockPos> missing, List<BlockPos> blocked) {
        for (Vec3i offset : offsets) {
            BlockPos p = altarPos.offset(offset);
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.MANA_BLOCK.get()) || s.is(ModBlocks.ALTAR_PILLAR.get())) continue;
            if (s.isAir() || s.canBeReplaced()) missing.add(p);
            else blocked.add(p);
        }
    }

    private static void checkPedestalPositions(Level level, BlockPos altarPos, List<Vec3i> offsets,
                                                List<BlockPos> missing, List<BlockPos> blocked) {
        for (Vec3i offset : offsets) {
            BlockPos p = altarPos.offset(offset);
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.ASPECT_PEDESTAL.get())) continue;
            if (s.isAir() || s.canBeReplaced()) missing.add(p);
            else blocked.add(p);
        }
    }

    private static void checkRingPositions(Level level, BlockPos altarPos, List<Vec3i> offsets,
                                            List<BlockPos> missing, List<BlockPos> blocked) {
        for (Vec3i offset : offsets) {
            BlockPos p = altarPos.offset(offset);
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.MANA_BLOCK.get()) || s.is(ModBlocks.RESONANCE_RING.get())) continue;
            if (s.isAir() || s.canBeReplaced()) missing.add(p);
            else blocked.add(p);
        }
    }

    private static int findInInventory(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.is(item)) return i;
        }
        return -1;
    }

    // ── Abstract declarations ─────────────────────────────────────────────────

    public abstract InteractionResult useOn(UseOnContext ctx, ItemStack wand);

    public abstract Component getDisplayName();

    public abstract int getColor();

    public abstract Component getDescription();
}
