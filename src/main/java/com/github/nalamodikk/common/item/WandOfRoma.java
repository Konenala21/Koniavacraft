package com.github.nalamodikk.common.item;

import com.github.nalamodikk.particle.style.examples.RomaMagicTestStyle;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class WandOfRoma extends Item {
    public WandOfRoma(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            Vec3 pos = player.position().add(0, 0.1, 0);
            RomaMagicTestStyle style = new RomaMagicTestStyle(UUID.randomUUID());
            style.spawn(level, pos);
            com.github.nalamodikk.client.event.ClientParticleTickEvent.registerStyle(style);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}