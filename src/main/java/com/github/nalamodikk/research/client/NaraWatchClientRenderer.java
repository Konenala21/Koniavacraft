package com.github.nalamodikk.research.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.block.BlockSelectorUtils;
import com.github.nalamodikk.register.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class NaraWatchClientRenderer {
    private static final double WATCH_TARGET_RANGE = 4.5D;
    private static final int TARGET_NAME_COLOR = 0xD7F7FF;
    private static final int TARGET_NAME_SHADOW = 0x77001522;
    private static final float WATCH_MODEL_SCALE = 0.3F; // Bksesame 新模型約大 5 倍,縮小以免第一人稱擋視野
    private static final float WATCH_MODEL_X_ROT = 45.0F;
    private static final float WATCH_MODEL_Y_ROT = 181.0F;
    private static final float WATCH_MODEL_Z_ROT = 0.0F;
    private static final float WATCH_MODEL_X = -0.047F;
    private static final float WATCH_MODEL_Y = -0.065F;
    private static final float WATCH_MODEL_Z = 0.45F;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!event.getItemStack().is(ModItems.NARA_WATCH.get())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        event.setCanceled(true);
        if (event.getHand() == InteractionHand.OFF_HAND && player.getMainHandItem().is(ModItems.NARA_WATCH.get())) {
            return;
        }
        renderTwoHandedWatch(minecraft, player, event);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }
        if (!isHoldingWatch(player)) {
            return;
        }

        Component name = null;

        // 1. Try Entity (higher priority)
        Entity entity = BlockSelectorUtils.getTargetEntity(player, WATCH_TARGET_RANGE);
        if (entity != null) {
            if (entity instanceof ItemEntity itemEntity) {
                name = itemEntity.getItem().getHoverName();
            } else {
                name = entity.getDisplayName();
            }
        }

        // 2. Try Block
        if (name == null) {
            BlockPos targetPos = BlockSelectorUtils.getTargetBlock(player, WATCH_TARGET_RANGE);
            if (targetPos != null) {
                BlockState state = minecraft.level.getBlockState(targetPos);
                name = state.getBlock().getName();
            }
        }

        if (name == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = font.width(name);
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() / 2 - 34;

        graphics.fill(x - 6, y - 4, x + width + 6, y + 12, TARGET_NAME_SHADOW);
        graphics.drawString(font, name, x, y, TARGET_NAME_COLOR, true);
    }

    private static void renderTwoHandedWatch(Minecraft minecraft, LocalPlayer player, RenderHandEvent event) {
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        float swing = event.getSwingProgress();
        float swingRoot = Mth.sqrt(swing);
        float swingYOffset = -0.2F * Mth.sin(swing * (float) Math.PI);
        float swingZOffset = -0.4F * Mth.sin(swingRoot * (float) Math.PI);

        poseStack.pushPose();
        // 1. 全域基礎位置：只處理裝備進度與穩定位置
        poseStack.translate(0.0F, -0.10F + event.getEquipProgress() * -1.0F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F));

        // 2. 渲染手臂：採用與原版地圖一模一樣的「伸長」數學模型
        if (!player.isInvisible()) {
            poseStack.pushPose();
            
            // 基礎揮動
            float swingRotation = Mth.sin(swingRoot * (float) Math.PI);
            poseStack.mulPose(Axis.XP.rotationDegrees(swingRotation * 20.0F));

            // 計算原版地圖特有的 f3 係數 (決定伸長感)
            float pitch = Mth.lerp(event.getPartialTick(), player.xRotO, player.getXRot());
            float f_tilt = 1.0F - pitch / 45.0F + 0.1F;
            f_tilt = Mth.clamp(f_tilt, 0.0F, 1.0F);
            float f3 = -Mth.cos(f_tilt * (float) Math.PI) * 0.5F + 0.5F;

            // 應用原版地圖變換：增加位移權重來強化「伸長」感
            // f3 接近 0 時代表低頭，此時手臂會向前下方大幅延伸
            poseStack.translate(0.0F, 0.04F + f3 * -0.8F, (1.0F - f3) * -0.3F);
            poseStack.mulPose(Axis.XP.rotationDegrees(f3 * -85.0F));

            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            renderWatchHand(minecraft, poseStack, buffer, event.getPackedLight(), player, HumanoidArm.RIGHT);
            renderWatchHand(minecraft, poseStack, buffer, event.getPackedLight(), player, HumanoidArm.LEFT);
            poseStack.popPose();
        }

        // 3. 物品渲染：不跟隨手臂的伸長與揮動位移
        // 這裡移除了之前的 swingRotation 旋轉，符合 "物品不要運動" 的要求
        renderWatchModel(minecraft, player, event.getItemStack(), poseStack, buffer, event.getPackedLight());
        poseStack.popPose();
    }

    private static void renderWatchHand(Minecraft minecraft, PoseStack poseStack, MultiBufferSource buffer,
                                        int packedLight, AbstractClientPlayer player, HumanoidArm arm) {
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * -41.0F));
        poseStack.translate(side * 0.3F, -1.1F, 0.45F);
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poseStack, buffer, packedLight, player);
        } else {
            renderer.renderLeftHand(poseStack, buffer, packedLight, player);
        }
        poseStack.popPose();
    }

    private static void renderWatchModel(Minecraft minecraft, LocalPlayer player, ItemStack stack,
                                         PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(WATCH_MODEL_X, WATCH_MODEL_Y, WATCH_MODEL_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(WATCH_MODEL_X_ROT));
        poseStack.mulPose(Axis.YP.rotationDegrees(WATCH_MODEL_Y_ROT));
        poseStack.mulPose(Axis.ZP.rotationDegrees(WATCH_MODEL_Z_ROT));
        poseStack.scale(WATCH_MODEL_SCALE, WATCH_MODEL_SCALE, WATCH_MODEL_SCALE);
        minecraft.getItemRenderer().renderStatic(
                player,
                stack,
                net.minecraft.world.item.ItemDisplayContext.NONE,
                false,
                poseStack,
                buffer,
                minecraft.level,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                player.getId()
        );
        poseStack.popPose();
    }

    private static boolean isHoldingWatch(LocalPlayer player) {
        return player.getMainHandItem().is(ModItems.NARA_WATCH.get())
                || player.getOffhandItem().is(ModItems.NARA_WATCH.get());
    }

    private NaraWatchClientRenderer() {
    }
}
