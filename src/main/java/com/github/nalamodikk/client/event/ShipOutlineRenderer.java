package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 對著飛船時，在瞄準的那一塊上畫黑色選取外框（vanilla 對實體不畫方塊框，所以自己畫）。
 * 用跟 ShipEntityRenderer 一樣的變換（繞船中心轉 yaw + translate(-centerOffset)）把框放到旋轉後位置。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ShipOutlineRenderer {

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.crosshairPickEntity instanceof ShipEntity ship) || ship.getContraption() == null) return;

        BlockPos local = ship.getAimedLocalBlock(mc.player);
        if (local == null) return;
        var info = ship.getContraption().getBlocks().get(local);
        if (info == null) return;
        BlockState state = info.state();
        VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, local);
        if (shape.isEmpty()) return;

        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();
        double ex = Mth.lerp(pt, ship.xOld, ship.getX());
        double ey = Mth.lerp(pt, ship.yOld, ship.getY());
        double ez = Mth.lerp(pt, ship.zOld, ship.getZ());
        Vec3 co = ship.centerOffset();

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(ex - camPos.x, ey - camPos.y, ez - camPos.z);
        pose.mulPose(ship.orientation(pt)); // 完整姿勢(含 pitch/roll)，跟方塊渲染一致；舊的 yaw-only 在傾斜時外框會差開
        pose.translate(-co.x, -co.y, -co.z);

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.lines());
        Matrix4f mat = pose.last().pose();
        PoseStack.Pose p = pose.last();
        int lx = local.getX(), ly = local.getY(), lz = local.getZ();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
            float n = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (n < 1.0e-4f) return;
            dx /= n; dy /= n; dz /= n;
            vc.addVertex(mat, (float) (x1 + lx), (float) (y1 + ly), (float) (z1 + lz))
                    .setColor(0f, 0f, 0f, 0.4f).setNormal(p, dx, dy, dz);
            vc.addVertex(mat, (float) (x2 + lx), (float) (y2 + ly), (float) (z2 + lz))
                    .setColor(0f, 0f, 0f, 0.4f).setNormal(p, dx, dy, dz);
        });
        pose.popPose();
        buf.endBatch(RenderType.lines());
    }
}
