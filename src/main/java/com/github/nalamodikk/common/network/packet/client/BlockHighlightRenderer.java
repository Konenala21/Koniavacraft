package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class BlockHighlightRenderer {

    private record Entry(BlockPos pos, int colorType, long expireMs) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final long DURATION_MS = 4000L;

    /** 接收來自伺服器的高亮請求 */
    public static void receive(List<BlockPos> positions, int colorType) {
        long expire = System.currentTimeMillis() + DURATION_MS;
        // 先移除同位置的舊條目，再加入新條目
        ENTRIES.removeIf(e -> positions.contains(e.pos()));
        for (BlockPos pos : positions) {
            ENTRIES.add(new Entry(pos, colorType, expire));
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ENTRIES.isEmpty()) return;

        long now = System.currentTimeMillis();
        ENTRIES.removeIf(e -> e.expireMs() < now);
        if (ENTRIES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        var bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        for (Entry entry : ENTRIES) {
            // 時間剩餘比例 → alpha 閃爍效果（最後 500ms 淡出）
            float remaining = (entry.expireMs() - now) / (float) DURATION_MS;
            float alpha = Math.min(1f, remaining * 4f);

            AABB box = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(entry.pos())).inflate(0.002);
            if (entry.colorType() == 1) {
                // 橘色 = 被佔用
                LevelRenderer.renderLineBox(poseStack, consumer, box, 1f, 0.5f, 0f, alpha);
            } else {
                // 紅色 = 材料不足
                LevelRenderer.renderLineBox(poseStack, consumer, box, 1f, 0.2f, 0.2f, alpha);
            }
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.LINES);
    }
}
