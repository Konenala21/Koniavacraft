package com.github.nalamodikk.experimental.particle.item;

import com.github.nalamodikk.particle.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 粒子效果測試工具
 *
 * 使用方法：
 * - 右鍵方塊：生成當前模式的 HelperDemo 粒子
 * - 空手右鍵（空氣）：切換測試模式
 * - 蹲下右鍵方塊：生成魔法陣（舊效果）
 */
public class DebugParticleItem extends Item {

    private static final Map<UUID, Integer> playerModes = new HashMap<>();

    public enum TestMode {
        BASIC_EFFECTS("§b基礎效果", "淡入淡出、縮放脈衝、顏色過渡"),
        SPIRAL_MOTION("§a螺旋運動", "螺旋上升、縮放動畫"),
        PHYSICS("§6物理效果", "重力、速度限制、粒子軌跡"),
        ATTRACTOR("§c吸引力", "漩渦吸引、透明度脈衝"),
        FULL_DEMO("§d完整演示", "組合所有效果"),
        OLD_GUIDED_FLOW("§7導向魔力流", "舊版 GuidedFlowParticle"),
        OLD_MAGIC_CIRCLE("§5旋轉魔法陣", "舊版 MagicCircleParticle");

        private final String displayName;
        private final String description;

        TestMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }

    public DebugParticleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            UUID playerId = player.getUUID();
            int currentMode = playerModes.getOrDefault(playerId, 0);
            int nextMode = (currentMode + 1) % TestMode.values().length;
            playerModes.put(playerId, nextMode);

            TestMode mode = TestMode.values()[nextMode];
            player.displayClientMessage(Component.literal("§e[粒子測試] §f切換模式: " + mode.displayName), false);
            player.displayClientMessage(Component.literal("  §7" + mode.description), false);
        }
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().above();
        Player player = context.getPlayer();

        if (!level.isClientSide && player != null) {
            UUID playerId = player.getUUID();
            int modeIndex = playerModes.getOrDefault(playerId, 0);
            TestMode mode = TestMode.values()[modeIndex];

            if (player.isCrouching()) {
                // 蹲下右鍵：舊版魔法陣
                ParticleHelper.createSingleRotatingCircle(level, pos);
                player.displayClientMessage(Component.literal("§d生成單一 3D 旋轉魔法陣 (舊版)"), true);
                return InteractionResult.SUCCESS;
            }

            // 根據當前模式生成粒子
            switch (mode) {
                case BASIC_EFFECTS -> {
                    ParticleHelper.createHelperDemo_BasicEffects(level, pos);
                    player.displayClientMessage(Component.literal("§b[基礎效果] §f淡入淡出、縮放脈衝、顏色過渡"), true);
                }
                case SPIRAL_MOTION -> {
                    ParticleHelper.createHelperDemo_SpiralMotion(level, pos);
                    player.displayClientMessage(Component.literal("§a[螺旋運動] §f螺旋上升、縮放動畫"), true);
                }
                case PHYSICS -> {
                    ParticleHelper.createHelperDemo_Physics(level, pos);
                    player.displayClientMessage(Component.literal("§6[物理效果] §f重力、速度限制、粒子軌跡"), true);
                }
                case ATTRACTOR -> {
                    ParticleHelper.createHelperDemo_Attractor(level, pos);
                    player.displayClientMessage(Component.literal("§c[吸引力] §f漩渦吸引、透明度脈衝"), true);
                }
                case FULL_DEMO -> {
                    ParticleHelper.createHelperDemo_FullDemo(level, pos);
                    player.displayClientMessage(Component.literal("§d[完整演示] §f組合所有效果"), true);
                }
                case OLD_GUIDED_FLOW -> {
                    BlockPos target = pos.offset(5, 5, 5);
                    ParticleHelper.createGuidedFlow(level, pos, target);
                    player.displayClientMessage(Component.literal("§7[舊版] §f導向魔力流"), true);
                }
                case OLD_MAGIC_CIRCLE -> {
                    ParticleHelper.createSingleRotatingCircle(level, pos);
                    player.displayClientMessage(Component.literal("§5[舊版] §f旋轉魔法陣"), true);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }
}
