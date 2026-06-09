package com.github.nalamodikk.common.item.tool.structure;

import com.github.nalamodikk.common.network.packet.client.BlockHighlightPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 結構建造杖的共用 build loop。獨立的結構建造杖(StructureBuildWandItem)與魔杖的結構核心插件
 * (WandCoreBehavior.STRUCTURE_BUILD)都呼叫這裡,避免兩份一樣的祭壇放置邏輯。
 * 兩者唯一差別:核心版每塊扣 {@code manaPerBlock} 魔力(獨立版傳 0 = 不扣)。
 */
public final class WandStructureBuilder {
    private WandStructureBuilder() {}

    /** @param result 給 useOn 回傳;@param budgetUsed 實際消耗的預算(魔力),核心版據此扣魔力。 */
    public record BuildOutcome(InteractionResult result, int budgetUsed) {}

    /**
     * 跑完整建造流程:分類(已滿足/被擋/缺)→ 被擋橘色高亮停手 → 全到位顯示完成訊息 →
     * 否則放缺的(創造直接放、生存扣背包物品,且每塊扣 manaPerBlock 預算,預算不足就停)。
     * 訊息/高亮跟原本一致。server 端呼叫(放置/扣資源)。
     */
    public static BuildOutcome build(Level level, BlockPos anchor, Player player, BlockEntity be,
                                     WandStructure structure, int manaPerBlock, int budget) {
        List<RequiredBlock> required = structure.required(level, anchor, be);

        List<BlockPos> blocked = new ArrayList<>();
        List<RequiredBlock> missing = new ArrayList<>();
        for (RequiredBlock rb : required) {
            BlockState cur = level.getBlockState(rb.pos());
            if (rb.satisfied().test(cur)) continue;
            if (cur.isAir() || cur.canBeReplaced()) missing.add(rb);
            else blocked.add(rb.pos());
        }

        // 被佔用 → 橘色高亮,停止建造
        if (!blocked.isEmpty()) {
            if (player instanceof ServerPlayer sp) BlockHighlightPacket.sendToPlayer(sp, blocked, 1);
            player.displayClientMessage(
                    Component.translatable("message.koniava.build_wand.blocked", blocked.size()), true);
            return new BuildOutcome(InteractionResult.SUCCESS, 0);
        }

        // 全部就位 → 完成 hook + 狀態訊息
        if (missing.isEmpty()) {
            structure.onPlaced(level, anchor, be);
            Component msg = structure.completeMessage(level, anchor, be);
            if (msg != null) player.displayClientMessage(msg, true);
            return new BuildOutcome(InteractionResult.SUCCESS, 0);
        }

        // 放置缺的(預算 = 魔力,manaPerBlock=0 時不受限)
        int placed = 0;
        int budgetUsed = 0;
        List<BlockPos> stillMissing = new ArrayList<>();
        for (RequiredBlock rb : missing) {
            if (budget - budgetUsed < manaPerBlock) { stillMissing.add(rb.pos()); continue; } // 預算不足
            boolean placedOne;
            if (player.isCreative()) {
                level.setBlock(rb.pos(), rb.toPlace(), 3);
                placedOne = true;
            } else {
                int slot = findInInventory(player, rb.cost());
                if (slot >= 0) {
                    level.setBlock(rb.pos(), rb.toPlace(), 3);
                    player.getInventory().getItem(slot).shrink(1);
                    placedOne = true;
                } else {
                    stillMissing.add(rb.pos());
                    placedOne = false;
                }
            }
            if (placedOne) { placed++; budgetUsed += manaPerBlock; }
        }

        if (!stillMissing.isEmpty() && player instanceof ServerPlayer sp)
            BlockHighlightPacket.sendToPlayer(sp, stillMissing, 0); // 紅色

        if (placed > 0) structure.onPlaced(level, anchor, be);

        player.displayClientMessage(
                Component.translatable("message.koniava.build_wand.placed",
                        placed, placed + stillMissing.size()), true);
        return new BuildOutcome(InteractionResult.SUCCESS, budgetUsed);
    }

    private static int findInInventory(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) return i;
        }
        return -1;
    }
}
