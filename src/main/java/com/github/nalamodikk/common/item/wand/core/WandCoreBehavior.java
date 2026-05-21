package com.github.nalamodikk.common.item.wand.core;

import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.multiblock.api.IWandActivatable;
import com.github.nalamodikk.common.screen.block.shared.UniversalConfigMenu;
import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public enum WandCoreBehavior {

    FORMATION {
        @Override
        public InteractionResult useOn(UseOnContext ctx, ItemStack wand) {
            Level level = ctx.getLevel();
            Player player = ctx.getPlayer();
            if (player == null) return InteractionResult.PASS;

            BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
            if (!(be instanceof IWandActivatable activatable)) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;

            int stored = wand.getOrDefault(ModDataComponents.MANA_STORED, 0);
            int cost = 500 + level.random.nextInt(1501);
            if (stored < cost) {
                player.displayClientMessage(
                        Component.translatable("message.koniava.wand.not_enough_mana", stored, cost), true);
                return InteractionResult.FAIL;
            }
            wand.set(ModDataComponents.MANA_STORED, stored - cost);
            player.displayClientMessage(activatable.onWandActivate(player), true);
            return InteractionResult.SUCCESS;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.koniava.formation_core");
        }

        @Override
        public int getColor() { return 0x5599FF; } // 藍
    },

    ACTIVATION {
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
        public int getColor() { return 0x44DD88; } // 綠
    },

    IO {
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
        public int getColor() { return 0xFFCC44; } // 黃
    },

    ROTATION {
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
        public int getColor() { return 0x44DDEE; } // 青
    },

    RITUAL {
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
        public int getColor() { return 0xFF5577; } // 紅粉
    };

    public abstract InteractionResult useOn(UseOnContext ctx, ItemStack wand);

    public abstract Component getDisplayName();

    public abstract int getColor();
}
