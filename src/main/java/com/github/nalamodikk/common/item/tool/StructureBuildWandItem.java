package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.item.tool.structure.RequiredBlock;
import com.github.nalamodikk.common.item.tool.structure.WandStructure;
import com.github.nalamodikk.common.item.tool.structure.WandStructures;
import com.github.nalamodikk.common.network.packet.client.BlockHighlightPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 結構建造杖:對著結構錨點(目前只有祭壇)右鍵,自動補齊缺的方塊(創造直接放、生存從背包扣)。
 * 結構定義抽到 {@link WandStructure} / {@link WandStructures},杖本體只跑通用 build loop;
 * 加新結構不用改這個類別。飛船上用也行(ShipEntity 的 syncNewShadowBlocksToContraption 會把影子蓋的收回視覺船)。
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

        List<RequiredBlock> required = structure.required(level, anchor, be);

        // 分類:已滿足(略過)/ 被別的方塊佔住(blocked) / 缺且可放(missing)
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
            return InteractionResult.SUCCESS;
        }

        // 全部就位 → 完成 hook + 狀態訊息
        if (missing.isEmpty()) {
            structure.onPlaced(level, anchor, be);
            Component msg = structure.completeMessage(level, anchor, be);
            if (msg != null) player.displayClientMessage(msg, true);
            return InteractionResult.SUCCESS;
        }

        // 放置缺的(創造直接放,生存從背包扣對應物品),放不了的收集起來
        int placed = 0;
        List<BlockPos> stillMissing = new ArrayList<>();
        for (RequiredBlock rb : missing) {
            if (player.isCreative()) {
                level.setBlock(rb.pos(), rb.toPlace(), 3);
                placed++;
            } else {
                int slot = findInInventory(player, rb.cost());
                if (slot >= 0) {
                    level.setBlock(rb.pos(), rb.toPlace(), 3);
                    player.getInventory().getItem(slot).shrink(1);
                    placed++;
                } else {
                    stillMissing.add(rb.pos());
                }
            }
        }

        if (!stillMissing.isEmpty() && player instanceof ServerPlayer sp)
            BlockHighlightPacket.sendToPlayer(sp, stillMissing, 0); // 紅色

        if (placed > 0) structure.onPlaced(level, anchor, be);

        player.displayClientMessage(
                Component.translatable("message.koniava.build_wand.placed",
                        placed, placed + stillMissing.size()), true);
        return InteractionResult.SUCCESS;
    }

    private int findInInventory(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) return i;
        }
        return -1;
    }
}
