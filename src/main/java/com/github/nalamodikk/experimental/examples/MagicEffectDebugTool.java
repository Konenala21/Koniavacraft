package com.github.nalamodikk.experimental.examples;

import com.github.nalamodikk.experimental.effects.MagicEffectRegistry;
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
 * 魔法效果除錯工具
 * 
 * 顯示當前活躍的魔法效果統計資訊和除錯數據
 */
public class MagicEffectDebugTool extends Item {
    
    public MagicEffectDebugTool(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide) {
            showDebugInfo(player);
        }
        
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }
    
    /**
     * 顯示除錯資訊給玩家
     */
    @OnlyIn(Dist.CLIENT)
    private void showDebugInfo(Player player) {
        // 獲取除錯資訊
        String registryInfo = MagicEffectRegistry.getInstance().getDebugInfo();
        String helperInfo = MagicEffectHelper.getDebugInfo();
        int activeCount = MagicEffectHelper.getActiveEffectCount();
        
        // 發送除錯訊息到聊天
        player.sendSystemMessage(Component.literal("=== Magic Effect Debug Info ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        
        player.sendSystemMessage(Component.literal("Registry: " + registryInfo)
            .withStyle(ChatFormatting.YELLOW));
        
        player.sendSystemMessage(Component.literal("Helper: " + helperInfo)
            .withStyle(ChatFormatting.AQUA));
        
        player.sendSystemMessage(Component.literal("Active Effects: " + activeCount)
            .withStyle(ChatFormatting.GREEN));
        
        // 如果有活躍效果，顯示詳細資訊
        if (activeCount > 0) {
            player.sendSystemMessage(Component.literal("Effect Details:")
                .withStyle(ChatFormatting.GRAY));
            
            // 獲取具體效果資訊
            MagicEffectRegistry.getInstance().getActiveEffects().forEach(effect -> {
                player.sendSystemMessage(Component.literal("  - " + effect.getDebugInfo())
                    .withStyle(ChatFormatting.WHITE));
            });
        }
        
        player.sendSystemMessage(Component.literal("=====================================")
            .withStyle(ChatFormatting.GOLD));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        tooltipComponents.add(Component.literal("Right-click to show debug info")
            .withStyle(ChatFormatting.GRAY));
        
        tooltipComponents.add(Component.literal("Shows active magic effects")
            .withStyle(ChatFormatting.DARK_GRAY));
        
        if (context.level() != null && context.level().isClientSide) {
            int activeCount = MagicEffectHelper.getActiveEffectCount();
            tooltipComponents.add(Component.literal("Current active: " + activeCount)
                .withStyle(activeCount > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
    }
}