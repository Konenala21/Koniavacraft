package com.github.nalamodikk.experimental.examples;

import com.github.nalamodikk.common.utils.effects.MagicEffectHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 魔法效果清除工具
 * 
 * 清除所有活躍的魔法效果，用於測試和除錯
 */
public class MagicEffectCleaner extends Item {
    
    public MagicEffectCleaner(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide) {
            clearAllEffects(player);
        }
        
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }
    
    /**
     * 清除所有魔法效果
     */
    @OnlyIn(Dist.CLIENT)
    private void clearAllEffects(Player player) {
        // 獲取清除前的效果數量
        int beforeCount = MagicEffectHelper.getActiveEffectCount();
        
        // 清除所有效果
        MagicEffectHelper.clearAllEffects();
        
        // 發送確認訊息
        if (beforeCount > 0) {
            player.sendSystemMessage(Component.literal("Cleared " + beforeCount + " magic effects")
                .withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.literal("No magic effects to clear")
                .withStyle(ChatFormatting.YELLOW));
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        tooltipComponents.add(Component.literal("Right-click to clear all effects")
            .withStyle(ChatFormatting.GRAY));
        
        tooltipComponents.add(Component.literal("Removes all active magic effects")
            .withStyle(ChatFormatting.DARK_GRAY));
        
        if (context.level() != null && context.level().isClientSide) {
            int activeCount = MagicEffectHelper.getActiveEffectCount();
            if (activeCount > 0) {
                tooltipComponents.add(Component.literal("Will clear " + activeCount + " effects")
                    .withStyle(ChatFormatting.RED));
            } else {
                tooltipComponents.add(Component.literal("No effects to clear")
                    .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}