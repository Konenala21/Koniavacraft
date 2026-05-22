package com.github.nalamodikk.common.item.wand;

import com.github.nalamodikk.common.item.wand.core.IWandCore;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeBehavior;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class WandRodItem extends Item {

    public WandRodItem(Properties properties) {
        super(properties.component(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty()));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        WandCoreData data = stack.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());

        if (!data.hasCore()) {
            if (ctx.getPlayer() != null && !ctx.getLevel().isClientSide) {
                ctx.getPlayer().displayClientMessage(
                        Component.translatable("message.koniava.wand.no_core"), true);
            }
            return InteractionResult.FAIL;
        }

        IWandCore core = data.getCore();
        return core.coreUseOn(ctx, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        int stored = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        int max    = stack.getOrDefault(ModDataComponents.MAX_MANA, BASE_MAX_MANA);
        lines.add(Component.translatable("tooltip.koniava.wand.mana", stored, max));

        WandCoreData data = stack.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
        if (data.hasCore()) {
            lines.add(Component.translatable("tooltip.koniava.wand.core",
                    data.getCore().getCoreDisplayName()));
        } else {
            lines.add(Component.translatable("tooltip.koniava.wand.no_core"));
        }
    }

    public static WandCoreData getData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
    }

    public static void setData(ItemStack stack, WandCoreData data) {
        stack.set(ModDataComponents.WAND_CORE_DATA, data);
    }

    /** Recomputes MAX_MANA from base + installed capacity upgrades, clamps MANA_STORED. */
    public static void recalculateMaxMana(ItemStack stack, WandCoreData data) {
        int capacityCount = data.countUpgrade(WandUpgradeBehavior.CAPACITY);
        int newMax = BASE_MAX_MANA + capacityCount * WandUpgradeBehavior.CAPACITY.bonusPerSlot;
        stack.set(ModDataComponents.MAX_MANA, newMax);
        int stored = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (stored > newMax) stack.set(ModDataComponents.MANA_STORED, newMax);
    }

    public static final int BASE_MAX_MANA = 8000;

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int current = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        int max     = stack.getOrDefault(ModDataComponents.MAX_MANA, BASE_MAX_MANA);
        if (max <= 0) return 0;
        return Math.round(13.0f * current / max);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x4488FF;
    }
}
