package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.multiblock.api.IWandActivatable;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class AdvancedTechWandItem extends BasicTechWandItem {

    public static final int MAX_MANA = 8000;

    public AdvancedTechWandItem(Properties properties) {
        super(properties
                .component(ModDataComponents.MANA_STORED, 0)
                .component(ModDataComponents.MAX_MANA, MAX_MANA));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int current = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        int max     = stack.getOrDefault(ModDataComponents.MAX_MANA, MAX_MANA);
        return max > 0 ? Math.round(13.0f * current / max) : 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x4488FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        int current = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        int max     = stack.getOrDefault(ModDataComponents.MAX_MANA, MAX_MANA);
        lines.add(Component.translatable("tooltip.koniava.mana_stored", current, max));
    }

    @Override
    public TechWandMode[] getSupportedModes() {
        return new TechWandMode[]{
                TechWandMode.CONFIGURE_IO,
                TechWandMode.DIRECTION_CONFIG,
                TechWandMode.ROTATE,
                TechWandMode.MULTIBLOCK
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // MULTIBLOCK 模式 + 蹲下：觸發結構驗證（需消耗魔力）
        if (level.getBlockEntity(pos) instanceof IWandActivatable activatable
                && player != null && player.isCrouching()
                && getMode(stack) == TechWandMode.MULTIBLOCK) {
            if (!level.isClientSide()) {
                int stored = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
                int cost = 500 + level.random.nextInt(1501); // 500~2000
                if (stored < cost) {
                    player.displayClientMessage(
                            Component.translatable("message.koniava.wand.not_enough_mana", stored, cost), true);
                    return InteractionResult.FAIL;
                }
                stack.set(ModDataComponents.MANA_STORED, stored - cost);
                Component result = activatable.onWandActivate(player);
                player.displayClientMessage(result, true);
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }
}
