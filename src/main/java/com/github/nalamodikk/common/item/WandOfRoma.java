package com.github.nalamodikk.common.item;

import com.github.nalamodikk.particle.style.examples.RomaMagicTestStyle;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class WandOfRoma extends Item {
    public WandOfRoma(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            Vec3 pos = player.position().add(0, 0.1, 0); // 在腳下生成
            RomaMagicTestStyle style = new RomaMagicTestStyle();
            style.display(level, pos);
            
            // 為了讓動畫持續，我們需要一個地方 tick 它
            // 暫時的 Hack: 使用一個靜態列表或事件監聽器來 tick 所有活躍的 Style
            // (這在完整框架中是由 StyleManager 處理的)
            com.github.nalamodikk.client.event.ClientParticleTickEvent.registerStyle(style);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
