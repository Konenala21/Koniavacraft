package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.multiblock.AbstractMultiblockControllerBlockEntity;
import com.github.nalamodikk.common.multiblock.api.IWandActivatable;
import com.github.nalamodikk.common.multiblock.api.MultiblockPattern;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModRecipes;
import com.github.nalamodikk.common.network.packet.client.altar.AltarUpgradeAnimPacket;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class AspectAltarBlockEntity extends AbstractMultiblockControllerBlockEntity implements IWandActivatable {

    //   結構三層（核心在 y=0）：
    //
    //   y= 0：只有核心
    //   y=-1：四斜角(±3,±3) MANA_BLOCK / ALTAR_PILLAR，核心正下方空氣
    //   y=-2：四斜角(±3,±3) MANA_BLOCK / ALTAR_PILLAR
    //         + 核心正下(0,0)催化物底座
    //         + 東西南北(±3,0)/(0,±3)各一底座
    //         其餘 . 位置可選擇性放置底座（PEDESTAL_SCAN_RADIUS 內皆可）
    //
    //   成形時角落自動換成 ALTAR_PILLAR；解散時還原 MANA_BLOCK。
    private static final Predicate<BlockState> PILLAR_PRED =
            state -> state.is(ModBlocks.MANA_BLOCK.get()) || state.is(ModBlocks.ALTAR_PILLAR.get());

    // 必要底座位置（成形前需手動放置）
    public static final List<Vec3i> PEDESTAL_OFFSETS = List.of(
            new Vec3i( 0, -2,  0),  // 核心正下方（催化物）
            new Vec3i( 0, -2, -3),  // North
            new Vec3i( 0, -2,  3),  // South
            new Vec3i(-3, -2,  0),  // West
            new Vec3i( 3, -2,  0)   // East
    );

    // y=-2 = 底段（top=false），y=-1 = 頂段（top=true）；四斜角距中心 ±3
    public static final List<Vec3i> PILLAR_BOTTOM = List.of(
            new Vec3i(-3, -2, -3), new Vec3i(-3, -2, 3),
            new Vec3i( 3, -2, -3), new Vec3i( 3, -2, 3)
    );
    public static final List<Vec3i> PILLAR_TOP = List.of(
            new Vec3i(-3, -1, -3), new Vec3i(-3, -1, 3),
            new Vec3i( 3, -1, -3), new Vec3i( 3, -1, 3)
    );
    private static final List<Vec3i> PILLAR_OFFSETS;
    static {
        PILLAR_OFFSETS = new ArrayList<>();
        PILLAR_OFFSETS.addAll(PILLAR_BOTTOM);
        PILLAR_OFFSETS.addAll(PILLAR_TOP);
    }

    private static final MultiblockPattern PATTERN = MultiblockPattern.builder()
            // 催化物底座（核心正下方 y=-2）
            .requireBlock(new Vec3i( 0, -2,  0), ModBlocks.ASPECT_PEDESTAL.get())
            // 四方向底座 North/South/West/East
            .requireBlock(new Vec3i( 0, -2, -3), ModBlocks.ASPECT_PEDESTAL.get())
            .requireBlock(new Vec3i( 0, -2,  3), ModBlocks.ASPECT_PEDESTAL.get())
            .requireBlock(new Vec3i(-3, -2,  0), ModBlocks.ASPECT_PEDESTAL.get())
            .requireBlock(new Vec3i( 3, -2,  0), ModBlocks.ASPECT_PEDESTAL.get())
            // 四斜角柱子底段
            .require(new Vec3i(-3, -2, -3), PILLAR_PRED)
            .require(new Vec3i(-3, -2,  3), PILLAR_PRED)
            .require(new Vec3i( 3, -2, -3), PILLAR_PRED)
            .require(new Vec3i( 3, -2,  3), PILLAR_PRED)
            // 四斜角柱子頂段
            .require(new Vec3i(-3, -1, -3), PILLAR_PRED)
            .require(new Vec3i(-3, -1,  3), PILLAR_PRED)
            .require(new Vec3i( 3, -1, -3), PILLAR_PRED)
            .require(new Vec3i( 3, -1,  3), PILLAR_PRED)
            .build();

    private static final int CHECK_INTERVAL = 40;
    private static final int MAX_MANA = 50000;
    private static final int MANA_TRANSFER_RATE = 200;
    private static final int PEDESTAL_SCAN_RADIUS = 6;

    // ── 升級環位置（T1–T12：半徑 7/9/11/13，各3個平面）───────────────────────────
public static final List<Vec3i> RING_T1 = List.of(
            new Vec3i( -7,  0,  0), new Vec3i( -6,  0, -3), new Vec3i( -6,  0, -2), new Vec3i( -6,  0, -1),
            new Vec3i( -6,  0,  1), new Vec3i( -6,  0,  2), new Vec3i( -6,  0,  3), new Vec3i( -5,  0, -4),
            new Vec3i( -5,  0,  4), new Vec3i( -4,  0, -5), new Vec3i( -4,  0,  5), new Vec3i( -3,  0, -6),
            new Vec3i( -3,  0,  6), new Vec3i( -2,  0, -6), new Vec3i( -2,  0,  6), new Vec3i( -1,  0, -6),
            new Vec3i( -1,  0,  6), new Vec3i(  0,  0, -7), new Vec3i(  0,  0,  7), new Vec3i(  1,  0, -6),
            new Vec3i(  1,  0,  6), new Vec3i(  2,  0, -6), new Vec3i(  2,  0,  6), new Vec3i(  3,  0, -6),
            new Vec3i(  3,  0,  6), new Vec3i(  4,  0, -5), new Vec3i(  4,  0,  5), new Vec3i(  5,  0, -4),
            new Vec3i(  5,  0,  4), new Vec3i(  6,  0, -3), new Vec3i(  6,  0, -2), new Vec3i(  6,  0, -1),
            new Vec3i(  6,  0,  1), new Vec3i(  6,  0,  2), new Vec3i(  6,  0,  3), new Vec3i(  7,  0,  0)
    );
    public static final List<Vec3i> RING_T2 = List.of(
            new Vec3i( -7,  0,  0), new Vec3i( -6, -3,  0), new Vec3i( -6, -2,  0), new Vec3i( -6, -1,  0),
            new Vec3i( -6,  1,  0), new Vec3i( -6,  2,  0), new Vec3i( -6,  3,  0), new Vec3i( -5, -4,  0),
            new Vec3i( -5,  4,  0), new Vec3i( -4, -5,  0), new Vec3i( -4,  5,  0), new Vec3i( -3, -6,  0),
            new Vec3i( -3,  6,  0), new Vec3i( -2, -6,  0), new Vec3i( -2,  6,  0), new Vec3i( -1, -6,  0),
            new Vec3i( -1,  6,  0), new Vec3i(  0, -7,  0), new Vec3i(  0,  7,  0), new Vec3i(  1, -6,  0),
            new Vec3i(  1,  6,  0), new Vec3i(  2, -6,  0), new Vec3i(  2,  6,  0), new Vec3i(  3, -6,  0),
            new Vec3i(  3,  6,  0), new Vec3i(  4, -5,  0), new Vec3i(  4,  5,  0), new Vec3i(  5, -4,  0),
            new Vec3i(  5,  4,  0), new Vec3i(  6, -3,  0), new Vec3i(  6, -2,  0), new Vec3i(  6, -1,  0),
            new Vec3i(  6,  1,  0), new Vec3i(  6,  2,  0), new Vec3i(  6,  3,  0), new Vec3i(  7,  0,  0)
    );
    public static final List<Vec3i> RING_T3 = List.of(
            new Vec3i(  0, -7,  0), new Vec3i(  0, -6, -3), new Vec3i(  0, -6, -2), new Vec3i(  0, -6, -1),
            new Vec3i(  0, -6,  1), new Vec3i(  0, -6,  2), new Vec3i(  0, -6,  3), new Vec3i(  0, -5, -4),
            new Vec3i(  0, -5,  4), new Vec3i(  0, -4, -5), new Vec3i(  0, -4,  5), new Vec3i(  0, -3, -6),
            new Vec3i(  0, -3,  6), new Vec3i(  0, -2, -6), new Vec3i(  0, -2,  6), new Vec3i(  0, -1, -6),
            new Vec3i(  0, -1,  6), new Vec3i(  0,  0, -7), new Vec3i(  0,  0,  7), new Vec3i(  0,  1, -6),
            new Vec3i(  0,  1,  6), new Vec3i(  0,  2, -6), new Vec3i(  0,  2,  6), new Vec3i(  0,  3, -6),
            new Vec3i(  0,  3,  6), new Vec3i(  0,  4, -5), new Vec3i(  0,  4,  5), new Vec3i(  0,  5, -4),
            new Vec3i(  0,  5,  4), new Vec3i(  0,  6, -3), new Vec3i(  0,  6, -2), new Vec3i(  0,  6, -1),
            new Vec3i(  0,  6,  1), new Vec3i(  0,  6,  2), new Vec3i(  0,  6,  3), new Vec3i(  0,  7,  0)
    );
    public static final List<Vec3i> RING_T4 = List.of(
            new Vec3i( -9,  0,  0), new Vec3i( -8,  0, -4), new Vec3i( -8,  0, -3), new Vec3i( -8,  0, -2),
            new Vec3i( -8,  0, -1), new Vec3i( -8,  0,  1), new Vec3i( -8,  0,  2), new Vec3i( -8,  0,  3),
            new Vec3i( -8,  0,  4), new Vec3i( -7,  0, -5), new Vec3i( -7,  0,  5), new Vec3i( -6,  0, -6),
            new Vec3i( -6,  0,  6), new Vec3i( -5,  0, -7), new Vec3i( -5,  0,  7), new Vec3i( -4,  0, -8),
            new Vec3i( -4,  0,  8), new Vec3i( -3,  0, -8), new Vec3i( -3,  0,  8), new Vec3i( -2,  0, -8),
            new Vec3i( -2,  0,  8), new Vec3i( -1,  0, -8), new Vec3i( -1,  0,  8), new Vec3i(  0,  0, -9),
            new Vec3i(  0,  0,  9), new Vec3i(  1,  0, -8), new Vec3i(  1,  0,  8), new Vec3i(  2,  0, -8),
            new Vec3i(  2,  0,  8), new Vec3i(  3,  0, -8), new Vec3i(  3,  0,  8), new Vec3i(  4,  0, -8),
            new Vec3i(  4,  0,  8), new Vec3i(  5,  0, -7), new Vec3i(  5,  0,  7), new Vec3i(  6,  0, -6),
            new Vec3i(  6,  0,  6), new Vec3i(  7,  0, -5), new Vec3i(  7,  0,  5), new Vec3i(  8,  0, -4),
            new Vec3i(  8,  0, -3), new Vec3i(  8,  0, -2), new Vec3i(  8,  0, -1), new Vec3i(  8,  0,  1),
            new Vec3i(  8,  0,  2), new Vec3i(  8,  0,  3), new Vec3i(  8,  0,  4), new Vec3i(  9,  0,  0)
    );
    public static final List<Vec3i> RING_T5 = List.of(
            new Vec3i( -9,  0,  0), new Vec3i( -8, -4,  0), new Vec3i( -8, -3,  0), new Vec3i( -8, -2,  0),
            new Vec3i( -8, -1,  0), new Vec3i( -8,  1,  0), new Vec3i( -8,  2,  0), new Vec3i( -8,  3,  0),
            new Vec3i( -8,  4,  0), new Vec3i( -7, -5,  0), new Vec3i( -7,  5,  0), new Vec3i( -6, -6,  0),
            new Vec3i( -6,  6,  0), new Vec3i( -5, -7,  0), new Vec3i( -5,  7,  0), new Vec3i( -4, -8,  0),
            new Vec3i( -4,  8,  0), new Vec3i( -3, -8,  0), new Vec3i( -3,  8,  0), new Vec3i( -2, -8,  0),
            new Vec3i( -2,  8,  0), new Vec3i( -1, -8,  0), new Vec3i( -1,  8,  0), new Vec3i(  0, -9,  0),
            new Vec3i(  0,  9,  0), new Vec3i(  1, -8,  0), new Vec3i(  1,  8,  0), new Vec3i(  2, -8,  0),
            new Vec3i(  2,  8,  0), new Vec3i(  3, -8,  0), new Vec3i(  3,  8,  0), new Vec3i(  4, -8,  0),
            new Vec3i(  4,  8,  0), new Vec3i(  5, -7,  0), new Vec3i(  5,  7,  0), new Vec3i(  6, -6,  0),
            new Vec3i(  6,  6,  0), new Vec3i(  7, -5,  0), new Vec3i(  7,  5,  0), new Vec3i(  8, -4,  0),
            new Vec3i(  8, -3,  0), new Vec3i(  8, -2,  0), new Vec3i(  8, -1,  0), new Vec3i(  8,  1,  0),
            new Vec3i(  8,  2,  0), new Vec3i(  8,  3,  0), new Vec3i(  8,  4,  0), new Vec3i(  9,  0,  0)
    );
    public static final List<Vec3i> RING_T6 = List.of(
            new Vec3i(  0, -9,  0), new Vec3i(  0, -8, -4), new Vec3i(  0, -8, -3), new Vec3i(  0, -8, -2),
            new Vec3i(  0, -8, -1), new Vec3i(  0, -8,  1), new Vec3i(  0, -8,  2), new Vec3i(  0, -8,  3),
            new Vec3i(  0, -8,  4), new Vec3i(  0, -7, -5), new Vec3i(  0, -7,  5), new Vec3i(  0, -6, -6),
            new Vec3i(  0, -6,  6), new Vec3i(  0, -5, -7), new Vec3i(  0, -5,  7), new Vec3i(  0, -4, -8),
            new Vec3i(  0, -4,  8), new Vec3i(  0, -3, -8), new Vec3i(  0, -3,  8), new Vec3i(  0, -2, -8),
            new Vec3i(  0, -2,  8), new Vec3i(  0, -1, -8), new Vec3i(  0, -1,  8), new Vec3i(  0,  0, -9),
            new Vec3i(  0,  0,  9), new Vec3i(  0,  1, -8), new Vec3i(  0,  1,  8), new Vec3i(  0,  2, -8),
            new Vec3i(  0,  2,  8), new Vec3i(  0,  3, -8), new Vec3i(  0,  3,  8), new Vec3i(  0,  4, -8),
            new Vec3i(  0,  4,  8), new Vec3i(  0,  5, -7), new Vec3i(  0,  5,  7), new Vec3i(  0,  6, -6),
            new Vec3i(  0,  6,  6), new Vec3i(  0,  7, -5), new Vec3i(  0,  7,  5), new Vec3i(  0,  8, -4),
            new Vec3i(  0,  8, -3), new Vec3i(  0,  8, -2), new Vec3i(  0,  8, -1), new Vec3i(  0,  8,  1),
            new Vec3i(  0,  8,  2), new Vec3i(  0,  8,  3), new Vec3i(  0,  8,  4), new Vec3i(  0,  9,  0)
    );
    public static final List<Vec3i> RING_T7 = List.of(
            new Vec3i(-11,  0,  0), new Vec3i(-10,  0, -4), new Vec3i(-10,  0, -3), new Vec3i(-10,  0, -2),
            new Vec3i(-10,  0, -1), new Vec3i(-10,  0,  1), new Vec3i(-10,  0,  2), new Vec3i(-10,  0,  3),
            new Vec3i(-10,  0,  4), new Vec3i( -9,  0, -6), new Vec3i( -9,  0, -5), new Vec3i( -9,  0,  5),
            new Vec3i( -9,  0,  6), new Vec3i( -8,  0, -7), new Vec3i( -8,  0,  7), new Vec3i( -7,  0, -8),
            new Vec3i( -7,  0,  8), new Vec3i( -6,  0, -9), new Vec3i( -6,  0,  9), new Vec3i( -5,  0, -9),
            new Vec3i( -5,  0,  9), new Vec3i( -4,  0,-10), new Vec3i( -4,  0, 10), new Vec3i( -3,  0,-10),
            new Vec3i( -3,  0, 10), new Vec3i( -2,  0,-10), new Vec3i( -2,  0, 10), new Vec3i( -1,  0,-10),
            new Vec3i( -1,  0, 10), new Vec3i(  0,  0,-11), new Vec3i(  0,  0, 11), new Vec3i(  1,  0,-10),
            new Vec3i(  1,  0, 10), new Vec3i(  2,  0,-10), new Vec3i(  2,  0, 10), new Vec3i(  3,  0,-10),
            new Vec3i(  3,  0, 10), new Vec3i(  4,  0,-10), new Vec3i(  4,  0, 10), new Vec3i(  5,  0, -9),
            new Vec3i(  5,  0,  9), new Vec3i(  6,  0, -9), new Vec3i(  6,  0,  9), new Vec3i(  7,  0, -8),
            new Vec3i(  7,  0,  8), new Vec3i(  8,  0, -7), new Vec3i(  8,  0,  7), new Vec3i(  9,  0, -6),
            new Vec3i(  9,  0, -5), new Vec3i(  9,  0,  5), new Vec3i(  9,  0,  6), new Vec3i( 10,  0, -4),
            new Vec3i( 10,  0, -3), new Vec3i( 10,  0, -2), new Vec3i( 10,  0, -1), new Vec3i( 10,  0,  1),
            new Vec3i( 10,  0,  2), new Vec3i( 10,  0,  3), new Vec3i( 10,  0,  4), new Vec3i( 11,  0,  0)
    );
    public static final List<Vec3i> RING_T8 = List.of(
            new Vec3i(-11,  0,  0), new Vec3i(-10, -4,  0), new Vec3i(-10, -3,  0), new Vec3i(-10, -2,  0),
            new Vec3i(-10, -1,  0), new Vec3i(-10,  1,  0), new Vec3i(-10,  2,  0), new Vec3i(-10,  3,  0),
            new Vec3i(-10,  4,  0), new Vec3i( -9, -6,  0), new Vec3i( -9, -5,  0), new Vec3i( -9,  5,  0),
            new Vec3i( -9,  6,  0), new Vec3i( -8, -7,  0), new Vec3i( -8,  7,  0), new Vec3i( -7, -8,  0),
            new Vec3i( -7,  8,  0), new Vec3i( -6, -9,  0), new Vec3i( -6,  9,  0), new Vec3i( -5, -9,  0),
            new Vec3i( -5,  9,  0), new Vec3i( -4,-10,  0), new Vec3i( -4, 10,  0), new Vec3i( -3,-10,  0),
            new Vec3i( -3, 10,  0), new Vec3i( -2,-10,  0), new Vec3i( -2, 10,  0), new Vec3i( -1,-10,  0),
            new Vec3i( -1, 10,  0), new Vec3i(  0,-11,  0), new Vec3i(  0, 11,  0), new Vec3i(  1,-10,  0),
            new Vec3i(  1, 10,  0), new Vec3i(  2,-10,  0), new Vec3i(  2, 10,  0), new Vec3i(  3,-10,  0),
            new Vec3i(  3, 10,  0), new Vec3i(  4,-10,  0), new Vec3i(  4, 10,  0), new Vec3i(  5, -9,  0),
            new Vec3i(  5,  9,  0), new Vec3i(  6, -9,  0), new Vec3i(  6,  9,  0), new Vec3i(  7, -8,  0),
            new Vec3i(  7,  8,  0), new Vec3i(  8, -7,  0), new Vec3i(  8,  7,  0), new Vec3i(  9, -6,  0),
            new Vec3i(  9, -5,  0), new Vec3i(  9,  5,  0), new Vec3i(  9,  6,  0), new Vec3i( 10, -4,  0),
            new Vec3i( 10, -3,  0), new Vec3i( 10, -2,  0), new Vec3i( 10, -1,  0), new Vec3i( 10,  1,  0),
            new Vec3i( 10,  2,  0), new Vec3i( 10,  3,  0), new Vec3i( 10,  4,  0), new Vec3i( 11,  0,  0)
    );
    public static final List<Vec3i> RING_T9 = List.of(
            new Vec3i(  0,-11,  0), new Vec3i(  0,-10, -4), new Vec3i(  0,-10, -3), new Vec3i(  0,-10, -2),
            new Vec3i(  0,-10, -1), new Vec3i(  0,-10,  1), new Vec3i(  0,-10,  2), new Vec3i(  0,-10,  3),
            new Vec3i(  0,-10,  4), new Vec3i(  0, -9, -6), new Vec3i(  0, -9, -5), new Vec3i(  0, -9,  5),
            new Vec3i(  0, -9,  6), new Vec3i(  0, -8, -7), new Vec3i(  0, -8,  7), new Vec3i(  0, -7, -8),
            new Vec3i(  0, -7,  8), new Vec3i(  0, -6, -9), new Vec3i(  0, -6,  9), new Vec3i(  0, -5, -9),
            new Vec3i(  0, -5,  9), new Vec3i(  0, -4,-10), new Vec3i(  0, -4, 10), new Vec3i(  0, -3,-10),
            new Vec3i(  0, -3, 10), new Vec3i(  0, -2,-10), new Vec3i(  0, -2, 10), new Vec3i(  0, -1,-10),
            new Vec3i(  0, -1, 10), new Vec3i(  0,  0,-11), new Vec3i(  0,  0, 11), new Vec3i(  0,  1,-10),
            new Vec3i(  0,  1, 10), new Vec3i(  0,  2,-10), new Vec3i(  0,  2, 10), new Vec3i(  0,  3,-10),
            new Vec3i(  0,  3, 10), new Vec3i(  0,  4,-10), new Vec3i(  0,  4, 10), new Vec3i(  0,  5, -9),
            new Vec3i(  0,  5,  9), new Vec3i(  0,  6, -9), new Vec3i(  0,  6,  9), new Vec3i(  0,  7, -8),
            new Vec3i(  0,  7,  8), new Vec3i(  0,  8, -7), new Vec3i(  0,  8,  7), new Vec3i(  0,  9, -6),
            new Vec3i(  0,  9, -5), new Vec3i(  0,  9,  5), new Vec3i(  0,  9,  6), new Vec3i(  0, 10, -4),
            new Vec3i(  0, 10, -3), new Vec3i(  0, 10, -2), new Vec3i(  0, 10, -1), new Vec3i(  0, 10,  1),
            new Vec3i(  0, 10,  2), new Vec3i(  0, 10,  3), new Vec3i(  0, 10,  4), new Vec3i(  0, 11,  0)
    );
    public static final List<Vec3i> RING_T10 = List.of(
            new Vec3i(-13,  0,  0), new Vec3i(-12,  0, -5), new Vec3i(-12,  0, -4), new Vec3i(-12,  0, -3),
            new Vec3i(-12,  0, -2), new Vec3i(-12,  0, -1), new Vec3i(-12,  0,  1), new Vec3i(-12,  0,  2),
            new Vec3i(-12,  0,  3), new Vec3i(-12,  0,  4), new Vec3i(-12,  0,  5), new Vec3i(-11,  0, -6),
            new Vec3i(-11,  0,  6), new Vec3i(-10,  0, -8), new Vec3i(-10,  0, -7), new Vec3i(-10,  0,  7),
            new Vec3i(-10,  0,  8), new Vec3i( -9,  0, -9), new Vec3i( -9,  0,  9), new Vec3i( -8,  0,-10),
            new Vec3i( -8,  0, 10), new Vec3i( -7,  0,-10), new Vec3i( -7,  0, 10), new Vec3i( -6,  0,-11),
            new Vec3i( -6,  0, 11), new Vec3i( -5,  0,-12), new Vec3i( -5,  0, 12), new Vec3i( -4,  0,-12),
            new Vec3i( -4,  0, 12), new Vec3i( -3,  0,-12), new Vec3i( -3,  0, 12), new Vec3i( -2,  0,-12),
            new Vec3i( -2,  0, 12), new Vec3i( -1,  0,-12), new Vec3i( -1,  0, 12), new Vec3i(  0,  0,-13),
            new Vec3i(  0,  0, 13), new Vec3i(  1,  0,-12), new Vec3i(  1,  0, 12), new Vec3i(  2,  0,-12),
            new Vec3i(  2,  0, 12), new Vec3i(  3,  0,-12), new Vec3i(  3,  0, 12), new Vec3i(  4,  0,-12),
            new Vec3i(  4,  0, 12), new Vec3i(  5,  0,-12), new Vec3i(  5,  0, 12), new Vec3i(  6,  0,-11),
            new Vec3i(  6,  0, 11), new Vec3i(  7,  0,-10), new Vec3i(  7,  0, 10), new Vec3i(  8,  0,-10),
            new Vec3i(  8,  0, 10), new Vec3i(  9,  0, -9), new Vec3i(  9,  0,  9), new Vec3i( 10,  0, -8),
            new Vec3i( 10,  0, -7), new Vec3i( 10,  0,  7), new Vec3i( 10,  0,  8), new Vec3i( 11,  0, -6),
            new Vec3i( 11,  0,  6), new Vec3i( 12,  0, -5), new Vec3i( 12,  0, -4), new Vec3i( 12,  0, -3),
            new Vec3i( 12,  0, -2), new Vec3i( 12,  0, -1), new Vec3i( 12,  0,  1), new Vec3i( 12,  0,  2),
            new Vec3i( 12,  0,  3), new Vec3i( 12,  0,  4), new Vec3i( 12,  0,  5), new Vec3i( 13,  0,  0)
    );
    public static final List<Vec3i> RING_T11 = List.of(
            new Vec3i(-13,  0,  0), new Vec3i(-12, -5,  0), new Vec3i(-12, -4,  0), new Vec3i(-12, -3,  0),
            new Vec3i(-12, -2,  0), new Vec3i(-12, -1,  0), new Vec3i(-12,  1,  0), new Vec3i(-12,  2,  0),
            new Vec3i(-12,  3,  0), new Vec3i(-12,  4,  0), new Vec3i(-12,  5,  0), new Vec3i(-11, -6,  0),
            new Vec3i(-11,  6,  0), new Vec3i(-10, -8,  0), new Vec3i(-10, -7,  0), new Vec3i(-10,  7,  0),
            new Vec3i(-10,  8,  0), new Vec3i( -9, -9,  0), new Vec3i( -9,  9,  0), new Vec3i( -8,-10,  0),
            new Vec3i( -8, 10,  0), new Vec3i( -7,-10,  0), new Vec3i( -7, 10,  0), new Vec3i( -6,-11,  0),
            new Vec3i( -6, 11,  0), new Vec3i( -5,-12,  0), new Vec3i( -5, 12,  0), new Vec3i( -4,-12,  0),
            new Vec3i( -4, 12,  0), new Vec3i( -3,-12,  0), new Vec3i( -3, 12,  0), new Vec3i( -2,-12,  0),
            new Vec3i( -2, 12,  0), new Vec3i( -1,-12,  0), new Vec3i( -1, 12,  0), new Vec3i(  0,-13,  0),
            new Vec3i(  0, 13,  0), new Vec3i(  1,-12,  0), new Vec3i(  1, 12,  0), new Vec3i(  2,-12,  0),
            new Vec3i(  2, 12,  0), new Vec3i(  3,-12,  0), new Vec3i(  3, 12,  0), new Vec3i(  4,-12,  0),
            new Vec3i(  4, 12,  0), new Vec3i(  5,-12,  0), new Vec3i(  5, 12,  0), new Vec3i(  6,-11,  0),
            new Vec3i(  6, 11,  0), new Vec3i(  7,-10,  0), new Vec3i(  7, 10,  0), new Vec3i(  8,-10,  0),
            new Vec3i(  8, 10,  0), new Vec3i(  9, -9,  0), new Vec3i(  9,  9,  0), new Vec3i( 10, -8,  0),
            new Vec3i( 10, -7,  0), new Vec3i( 10,  7,  0), new Vec3i( 10,  8,  0), new Vec3i( 11, -6,  0),
            new Vec3i( 11,  6,  0), new Vec3i( 12, -5,  0), new Vec3i( 12, -4,  0), new Vec3i( 12, -3,  0),
            new Vec3i( 12, -2,  0), new Vec3i( 12, -1,  0), new Vec3i( 12,  1,  0), new Vec3i( 12,  2,  0),
            new Vec3i( 12,  3,  0), new Vec3i( 12,  4,  0), new Vec3i( 12,  5,  0), new Vec3i( 13,  0,  0)
    );
    public static final List<Vec3i> RING_T12 = List.of(
            new Vec3i(  0,-13,  0), new Vec3i(  0,-12, -5), new Vec3i(  0,-12, -4), new Vec3i(  0,-12, -3),
            new Vec3i(  0,-12, -2), new Vec3i(  0,-12, -1), new Vec3i(  0,-12,  1), new Vec3i(  0,-12,  2),
            new Vec3i(  0,-12,  3), new Vec3i(  0,-12,  4), new Vec3i(  0,-12,  5), new Vec3i(  0,-11, -6),
            new Vec3i(  0,-11,  6), new Vec3i(  0,-10, -8), new Vec3i(  0,-10, -7), new Vec3i(  0,-10,  7),
            new Vec3i(  0,-10,  8), new Vec3i(  0, -9, -9), new Vec3i(  0, -9,  9), new Vec3i(  0, -8,-10),
            new Vec3i(  0, -8, 10), new Vec3i(  0, -7,-10), new Vec3i(  0, -7, 10), new Vec3i(  0, -6,-11),
            new Vec3i(  0, -6, 11), new Vec3i(  0, -5,-12), new Vec3i(  0, -5, 12), new Vec3i(  0, -4,-12),
            new Vec3i(  0, -4, 12), new Vec3i(  0, -3,-12), new Vec3i(  0, -3, 12), new Vec3i(  0, -2,-12),
            new Vec3i(  0, -2, 12), new Vec3i(  0, -1,-12), new Vec3i(  0, -1, 12), new Vec3i(  0,  0,-13),
            new Vec3i(  0,  0, 13), new Vec3i(  0,  1,-12), new Vec3i(  0,  1, 12), new Vec3i(  0,  2,-12),
            new Vec3i(  0,  2, 12), new Vec3i(  0,  3,-12), new Vec3i(  0,  3, 12), new Vec3i(  0,  4,-12),
            new Vec3i(  0,  4, 12), new Vec3i(  0,  5,-12), new Vec3i(  0,  5, 12), new Vec3i(  0,  6,-11),
            new Vec3i(  0,  6, 11), new Vec3i(  0,  7,-10), new Vec3i(  0,  7, 10), new Vec3i(  0,  8,-10),
            new Vec3i(  0,  8, 10), new Vec3i(  0,  9, -9), new Vec3i(  0,  9,  9), new Vec3i(  0, 10, -8),
            new Vec3i(  0, 10, -7), new Vec3i(  0, 10,  7), new Vec3i(  0, 10,  8), new Vec3i(  0, 11, -6),
            new Vec3i(  0, 11,  6), new Vec3i(  0, 12, -5), new Vec3i(  0, 12, -4), new Vec3i(  0, 12, -3),
            new Vec3i(  0, 12, -2), new Vec3i(  0, 12, -1), new Vec3i(  0, 12,  1), new Vec3i(  0, 12,  2),
            new Vec3i(  0, 12,  3), new Vec3i(  0, 12,  4), new Vec3i(  0, 12,  5), new Vec3i(  0, 13,  0)
    );
    public static final List<List<Vec3i>> ALL_RINGS = List.of(
            RING_T1, RING_T2, RING_T3, RING_T4, RING_T5, RING_T6
    );


    private static final int MIN_COMPLETION_TICKS = 320;
    private static final int MAX_COMPLETION_TICKS = 700;

    private int ticker = 0;
    private int tickCounter = 0;
    private boolean active = false;
    private float progress = 0f;
    private int ritualTick = 0;
    private int ritualMaxTick = 0;
    private int upgradeTier = 0;
    private long ringPhaseStart = 0;
    private int completionAnimTick = 0;
    private int completionDuration = MIN_COMPLETION_TICKS;
    private UUID activatorUUID = null;

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

        if (active) tickRitual();

        if (completionAnimTick > 0) {
            completionAnimTick--;
            syncToClient();
        }
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

        // 優先嘗試放入六面相鄰的容器，放不完的才掉落
        result = tryOutputToAdjacentContainer(result);
        if (!result.isEmpty()) {
            Vec3 drop = Vec3.atCenterOf(worldPosition).add(0, 1.2, 0);
            ItemEntity entity = new ItemEntity(level, drop.x, drop.y, drop.z, result);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }

        completionDuration = Math.max(MIN_COMPLETION_TICKS,
                Math.min(MAX_COMPLETION_TICKS, ritualMaxTick / 3));
        completionAnimTick = completionDuration;

        if (level instanceof ServerLevel serverLevel && activatorUUID != null) {
            ServerPlayer sp = serverLevel.getServer().getPlayerList().getPlayer(activatorUUID);
            if (sp != null) {
                AdvancementHolder adv = serverLevel.getServer().getAdvancements()
                        .get(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "first_altar_ritual"));
                if (adv != null) {
                    var prog = sp.getAdvancements().getOrStartProgress(adv);
                    if (!prog.isDone()) {
                        for (String criterion : prog.getRemainingCriteria()) {
                            sp.getAdvancements().award(adv, criterion);
                        }
                    }
                }
            }
        }

        active = false;
        progress = 0f;
        ritualTick = 0;
        ritualMaxTick = 0;
        setChanged();
        syncToClient();

        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
    }

    private ItemStack tryOutputToAdjacentContainer(ItemStack stack) {
        for (Direction dir : Direction.values()) {
            if (stack.isEmpty()) break;
            IItemHandler handler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (handler == null) continue;
            stack = ItemHandlerHelper.insertItem(handler, stack, false);
        }
        return stack;
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

    public Component tryActivate(UUID playerUUID) {
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
        activatorUUID = playerUUID;
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
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.ALTAR_TYPE.get())
                .stream()
                .filter(h -> h.value().matches(input, level) && h.value().getMinTier() <= upgradeTier)
                .findFirst();
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
        // 環狀 RESONANCE_RING → 還原魔力方塊
        for (int i = 0; i < Math.min(upgradeTier, ALL_RINGS.size()); i++) restoreRingBlocks(ALL_RINGS.get(i));
        clearPedestals();
        if (active) cancelRitual();
        upgradeTier = 0;
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
                for (int i = 0; i < Math.min(upgradeTier, ALL_RINGS.size()); i++) restoreRingBlocks(ALL_RINGS.get(i));
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

    public void refreshUpgradeTier() {
        if (level == null || !isFormed()) return;
        // 夾緊舊值，防止存檔 tier 超出目前 ALL_RINGS 範圍（如從 T12 縮回 T6）
        upgradeTier = Math.min(upgradeTier, ALL_RINGS.size());
        int newTier = 0;
        for (List<Vec3i> ring : ALL_RINGS) {
            if (checkRingComplete(ring)) newTier++;
            else break;
        }
        if (newTier != upgradeTier) {
            if (newTier > upgradeTier) {
                for (int i = upgradeTier; i < newTier; i++) applyRingReplace(ALL_RINGS.get(i));
                if (level instanceof ServerLevel serverLevel) {
                    AltarUpgradeAnimPacket packet = new AltarUpgradeAnimPacket(worldPosition, newTier);
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                            serverLevel,
                            new net.minecraft.world.level.ChunkPos(worldPosition),
                            packet
                    );
                    if (newTier >= 5) {
                        String advId = newTier == 6 ? "altar_upgrade_t6" : "altar_upgrade_t5";
                        AdvancementHolder tierAdv = serverLevel.getServer().getAdvancements()
                                .get(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, advId));
                        if (tierAdv != null) {
                            for (ServerPlayer sp : serverLevel.players()) {
                                var prog = sp.getAdvancements().getOrStartProgress(tierAdv);
                                if (!prog.isDone()) {
                                    for (String c : prog.getRemainingCriteria()) sp.getAdvancements().award(tierAdv, c);
                                }
                            }
                        }
                    }
                }
            } else {
                for (int i = newTier; i < upgradeTier; i++) restoreRingBlocks(ALL_RINGS.get(i));
            }
            upgradeTier = newTier;
            setChanged();
            syncToClient();
        }
    }

    private boolean checkRingComplete(List<Vec3i> ring) {
        if (level == null) return false;
        for (Vec3i offset : ring) {
            BlockState s = level.getBlockState(worldPosition.offset(offset));
            // 接受 MANA_BLOCK（尚未替換）或 RESONANCE_RING（已替換）
            if (!s.is(ModBlocks.MANA_BLOCK.get()) && !s.is(ModBlocks.RESONANCE_RING.get())) return false;
        }
        return true;
    }

    private void applyRingReplace(List<Vec3i> ring) {
        if (level == null) return;
        for (Vec3i offset : ring) {
            BlockPos p = worldPosition.offset(offset);
            if (level.getBlockState(p).is(ModBlocks.MANA_BLOCK.get())) {
                level.setBlock(p, ModBlocks.RESONANCE_RING.get().defaultBlockState(), 3);
            }
        }
    }

    private void restoreRingBlocks(List<Vec3i> ring) {
        if (level == null) return;
        for (Vec3i offset : ring) {
            BlockPos p = worldPosition.offset(offset);
            if (level.getBlockState(p).is(ModBlocks.RESONANCE_RING.get())) {
                level.setBlock(p, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
            }
        }
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
        tag.putInt("UpgradeTier", upgradeTier);
        tag.putLong("RingPhaseStart", ringPhaseStart);
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
        completionAnimTick = tag.getInt("CompletionAnimTick");
        completionDuration = tag.contains("CompletionDuration") ? tag.getInt("CompletionDuration") : MIN_COMPLETION_TICKS;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        tag.putInt("CompletionAnimTick", completionAnimTick);
        tag.putInt("CompletionDuration", completionDuration);
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
