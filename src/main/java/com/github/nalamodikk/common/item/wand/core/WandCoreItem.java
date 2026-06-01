package com.github.nalamodikk.common.item.wand.core;

import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.research.skill.SkillEncoding;
import com.github.nalamodikk.research.skill.StoredSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class WandCoreItem extends Item implements IWandCore {

    private final WandCoreBehavior behavior;

    public WandCoreItem(WandCoreBehavior behavior, Properties properties) {
        super(properties);
        this.behavior = behavior;
    }

    public WandCoreBehavior getBehavior() {
        return behavior;
    }

    @Override
    public InteractionResult coreUseOn(UseOnContext ctx, ItemStack wandStack) {
        return behavior.useOn(ctx, wandStack);
    }

    @Override
    public InteractionResultHolder<ItemStack> coreUse(Level level, Player player, InteractionHand hand, ItemStack wandStack) {
        return behavior.use(level, player, hand, wandStack);
    }

    @Override
    public Component getCoreDisplayName() {
        return behavior.getDisplayName();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.koniava.wand_core.type", behavior.getDisplayName()));
        lines.add(behavior.getDescription());
        Component compatible = Component.translatable("item.koniava.wand_rod")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("item.koniava.wand_rod_advanced")
                        .withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("tooltip.koniava.upgrade.compatible", compatible)
                .withStyle(ChatFormatting.GRAY));

        appendEncodedSkills(stack, lines);
    }

    /** Spell cores list the skills encoded onto them, highlighting the selected one. */
    private void appendEncodedSkills(ItemStack stack, List<Component> lines) {
        if (behavior != WandCoreBehavior.SPELL) return;

        List<StoredSkill> skills = SkillEncoding.getSkills(stack);
        if (skills.isEmpty()) {
            lines.add(Component.translatable("tooltip.koniava.spell_core.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        int selected = stack.getOrDefault(ModDataComponents.SELECTED_SKILL_INDEX, 0);
        lines.add(Component.translatable("tooltip.koniava.spell_core.skills")
                .withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < skills.size(); i++) {
            String name = skills.get(i).name().isBlank()
                    ? Component.translatable("gui.koniava.skill_encoder.unnamed").getString()
                    : skills.get(i).name();
            boolean isSelected = i == selected;
            lines.add(Component.literal((isSelected ? " ▶ " : "    ") + name)
                    .withStyle(isSelected ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        }
    }
}
