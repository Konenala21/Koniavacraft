package com.github.nalamodikk.common.item.wand.core;

import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.common.item.tool.structure.WandStructure;
import com.github.nalamodikk.common.item.tool.structure.WandStructureBuilder;
import com.github.nalamodikk.common.item.tool.structure.WandStructures;
import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeBehavior;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.multiblock.api.IWandActivatable;
import com.github.nalamodikk.common.network.packet.server.skill.CastSkillPacket;
import com.github.nalamodikk.common.screen.block.shared.UniversalConfigMenu;
import com.github.nalamodikk.research.skill.SkillRegistry;
import com.github.nalamodikk.common.network.packet.client.BlockHighlightPacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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

            BlockPos pos = ctx.getClickedPos();
            BlockState clicked = level.getBlockState(pos);
            BlockEntity be = level.getBlockEntity(pos);
            WandStructure structure = WandStructures.findMatching(clicked, be);
            if (structure == null) return InteractionResult.PASS;

            int stored = wand.getOrDefault(ModDataComponents.MANA_STORED, 0);
            if (stored <= 0) return InteractionResult.FAIL;
            if (level.isClientSide) return InteractionResult.SUCCESS;

            // 跟獨立結構杖共用 build loop,差別:每塊扣 MANA_PER_BLOCK 魔力,扣完就停。
            WandStructureBuilder.BuildOutcome out =
                    WandStructureBuilder.build(level, pos, player, be, structure, MANA_PER_BLOCK, stored);
            if (out.budgetUsed() > 0) wand.set(ModDataComponents.MANA_STORED, stored - out.budgetUsed());
            return out.result();
        }

        @Override
        public Component getDisplayName() { return Component.translatable("item.koniava.structure_build_core"); }

        @Override
        public int getColor() { return 0xFFAA66FF; } // 紫
    },

    SPELL {
        @Override public Component getDescription() { return Component.translatable("tooltip.koniava.core.spell.desc"); }

        @Override
        public InteractionResult useOn(UseOnContext ctx, ItemStack wand) {
            // Spell core does nothing on blocks; casting happens on air right-click (use()).
            return InteractionResult.PASS;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack wand) {
            // Per-skill cooldown is enforced server-side (SkillCooldowns); the client
            // just requests and the server rejects casts that are still cooling down.
            if (level.isClientSide) {
                // Generic: cast whichever skill the wand has selected. Defaults to the
                // demo fireball until a selection UI sets SELECTED_SKILL.
                ResourceLocation skillId = wand.getOrDefault(ModDataComponents.SELECTED_SKILL, SkillRegistry.FIREBALL);
                CastSkillPacket.send(skillId);
            }
            return InteractionResultHolder.success(wand);
        }

        @Override
        public Component getDisplayName() { return Component.translatable("item.koniava.spell_core"); }

        @Override
        public int getColor() { return 0xFFEE7722; } // 橘
    };

    // ── Abstract declarations ─────────────────────────────────────────────────

    public abstract InteractionResult useOn(UseOnContext ctx, ItemStack wand);

    /** Right-click in air. Default: do nothing. The SPELL core overrides this to cast. */
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack wand) {
        return InteractionResultHolder.pass(wand);
    }

    public abstract Component getDisplayName();

    public abstract int getColor();

    public abstract Component getDescription();
}
