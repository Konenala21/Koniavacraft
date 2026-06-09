package com.github.nalamodikk.common.item.tool.structure;

import com.github.nalamodikk.common.block.blockentity.altar.AltarGeometry;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 本源祭壇的多方塊結構:未成形補柱子(mana_block)+ 底座(aspect_pedestal),成形後補下一個升級環(mana_block)。
 * 行為照搬自舊的 StructureBuildWandItem,只是抽成 {@link WandStructure} 讓杖通用。
 */
public class AltarWandStructure implements WandStructure {

    @Override
    public boolean matches(BlockState clicked, @Nullable BlockEntity be) {
        return be instanceof AspectAltarBlockEntity;
    }

    @Override
    public List<RequiredBlock> required(Level level, BlockPos anchor, BlockEntity be) {
        List<RequiredBlock> list = new ArrayList<>();
        if (!(be instanceof AspectAltarBlockEntity altar)) return list;

        if (!altar.isFormed()) {
            Predicate<BlockState> pillarOk = s ->
                    s.is(ModBlocks.MANA_BLOCK.get()) || s.is(ModBlocks.ALTAR_PILLAR.get());
            BlockState pillar = ModBlocks.MANA_BLOCK.get().defaultBlockState();
            Item pillarItem = ModBlocks.MANA_BLOCK.get().asItem();
            for (Vec3i off : AltarGeometry.PILLAR_BOTTOM) list.add(new RequiredBlock(anchor.offset(off), pillarOk, pillar, pillarItem));
            for (Vec3i off : AltarGeometry.PILLAR_TOP)    list.add(new RequiredBlock(anchor.offset(off), pillarOk, pillar, pillarItem));

            Predicate<BlockState> pedOk = s -> s.is(ModBlocks.ASPECT_PEDESTAL.get());
            BlockState ped = ModBlocks.ASPECT_PEDESTAL.get().defaultBlockState();
            Item pedItem = ModBlocks.ASPECT_PEDESTAL.get().asItem();
            for (Vec3i off : AltarGeometry.PEDESTAL_OFFSETS) list.add(new RequiredBlock(anchor.offset(off), pedOk, ped, pedItem));
        } else {
            int nextTier = altar.getUpgradeTier() + 1;
            if (nextTier <= AltarGeometry.ALL_RINGS.size()) {
                Predicate<BlockState> ringOk = s ->
                        s.is(ModBlocks.MANA_BLOCK.get()) || s.is(ModBlocks.RESONANCE_RING.get());
                BlockState ring = ModBlocks.MANA_BLOCK.get().defaultBlockState();
                Item ringItem = ModBlocks.MANA_BLOCK.get().asItem();
                for (Vec3i off : AltarGeometry.ALL_RINGS.get(nextTier - 1)) list.add(new RequiredBlock(anchor.offset(off), ringOk, ring, ringItem));
            }
        }
        return list;
    }

    @Override
    public void onPlaced(Level level, BlockPos anchor, BlockEntity be) {
        if (be instanceof AspectAltarBlockEntity altar) altar.refreshUpgradeTier();
    }

    @Override
    @Nullable
    public Component completeMessage(Level level, BlockPos anchor, BlockEntity be) {
        if (!(be instanceof AspectAltarBlockEntity altar)) return null;
        if (!altar.isFormed()) {
            return Component.translatable("message.koniava.build_wand.ready_to_form");
        } else if (altar.getUpgradeTier() >= AltarGeometry.ALL_RINGS.size()) {
            return Component.translatable("message.koniava.build_wand.all_rings_done", AltarGeometry.ALL_RINGS.size());
        } else {
            return Component.translatable("message.koniava.build_wand.ring_ready", altar.getUpgradeTier() + 1);
        }
    }
}
