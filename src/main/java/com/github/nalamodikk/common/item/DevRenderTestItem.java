package com.github.nalamodikk.common.item;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.renderer.ManaStrikeShaderRenderer;
import com.github.nalamodikk.client.renderer.OrbitalTestShaderRenderer;
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

public class DevRenderTestItem extends Item {

    public DevRenderTestItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.koniava.dev_render_test.tooltip.1"));
        tooltipComponents.add(Component.translatable("item.koniava.dev_render_test.tooltip.2"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.8f);

        if (level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                KoniavacraftMod.LOGGER.info("[DevRenderTest] spawning OrbitalTest effect at {}", player.position());
                OrbitalTestShaderRenderer.spawnEffect(player.position(), level.getGameTime());
            } else {
                KoniavacraftMod.LOGGER.info("[DevRenderTest] spawning ManaStrike effect at {}", player.position());
                ManaStrikeShaderRenderer.spawnEffect(player.position(), level.getGameTime());
            }
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
