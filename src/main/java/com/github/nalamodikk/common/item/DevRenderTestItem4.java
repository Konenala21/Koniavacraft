package com.github.nalamodikk.common.item;

import com.github.nalamodikk.common.particle.InteractiveFormationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class DevRenderTestItem4 extends Item {

    private static final String[] STAGE_LABELS = {
            "OFF",
            "Stage 1: Single",
            "Stage 2: Ring (instant)",
            "Stage 3: Ring (sequential)",
            "Stage 4: Double ring",
    };

    public DevRenderTestItem4(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.literal("Right-click: cycle formation"));
        tips.add(Component.literal("Particles face you, anchored in front of view"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResultHolder.success(player.getItemInHand(hand));

        int stage = InteractiveFormationManager.advance((net.minecraft.server.level.ServerPlayer) player);

        float pitch = 0.8f + stage * 0.15f;
        var sound = stage == InteractiveFormationManager.STAGE_OFF
                ? SoundEvents.BEACON_DEACTIVATE
                : SoundEvents.NOTE_BLOCK_BELL.value();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, 0.5f, pitch);

        player.displayClientMessage(
                Component.literal("[Dev4] " + STAGE_LABELS[stage]), true
        );

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
