package com.github.nalamodikk.common.item.wand;

import com.github.nalamodikk.common.item.wand.core.IWandCore;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeBehavior;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class WandRodItem extends Item {

    public static final int BASE_MAX_MANA = 8000;

    private final int maxUpgradeMk;
    private final int maxUpgradeSlots;

    public WandRodItem(int maxUpgradeMk, int maxUpgradeSlots, Properties properties) {
        super(properties.component(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty()));
        this.maxUpgradeMk = maxUpgradeMk;
        this.maxUpgradeSlots = maxUpgradeSlots;
    }

    public int getMaxUpgradeMk() { return maxUpgradeMk; }
    public int getMaxUpgradeSlots() { return maxUpgradeSlots; }

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

    /**
     * Right-click in air delegates to the installed core's {@link IWandCore#coreUse}.
     * Only a Spell Core casts; other cores (or no core) do nothing here.
     * Block right-clicks still go through {@link #useOn}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        WandCoreData data = stack.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
        if (!data.hasCore()) {
            return InteractionResultHolder.pass(stack);
        }
        return data.getCore().coreUse(level, player, hand, stack);
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

        lines.add(Component.translatable("tooltip.koniava.wand.tier",
                maxUpgradeSlots, maxUpgradeMk));
    }

    public static WandCoreData getData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
    }

    public static void setData(ItemStack stack, WandCoreData data) {
        stack.set(ModDataComponents.WAND_CORE_DATA, data);
    }

    public static void recalculateMaxMana(ItemStack stack, WandCoreData data) {
        int bonus = data.sumUpgradeBonus(WandUpgradeBehavior.CAPACITY);
        int newMax = BASE_MAX_MANA + bonus;
        stack.set(ModDataComponents.MAX_MANA, newMax);
        int stored = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (stored > newMax) stack.set(ModDataComponents.MANA_STORED, newMax);
    }

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
