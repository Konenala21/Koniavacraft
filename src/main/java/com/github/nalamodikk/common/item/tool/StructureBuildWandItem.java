package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.item.tool.structure.WandStructure;
import com.github.nalamodikk.common.item.tool.structure.WandStructureBuilder;
import com.github.nalamodikk.common.item.tool.structure.WandStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 獨立的結構建造杖:對結構錨點(目前只有祭壇)右鍵,免費補齊缺的方塊(只扣背包物品,不耗魔力)。
 * 結構定義在 {@link WandStructure}/{@link WandStructures},放置流程共用 {@link WandStructureBuilder}
 * (魔杖的結構核心插件 WandCoreBehavior.STRUCTURE_BUILD 走同一套,只是會扣魔力)。
 */
public class StructureBuildWandItem extends Item {

    public StructureBuildWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos anchor = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null || level.isClientSide()) return InteractionResult.PASS;

        BlockState clicked = level.getBlockState(anchor);
        BlockEntity be = level.getBlockEntity(anchor);
        WandStructure structure = WandStructures.findMatching(clicked, be);
        if (structure == null) return InteractionResult.PASS;

        // manaPerBlock=0、budget 不限 → 不耗魔力,只受背包物品限制
        return WandStructureBuilder.build(level, anchor, player, be, structure, 0, Integer.MAX_VALUE).result();
    }
}
