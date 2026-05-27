package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 鏡中世界 boss 招式預兆：依同步來的 TELEGRAPH_SKILL 在招式目標位置畫亮白輪廓。
 * 灰階世界下靠形狀 + 高亮度白辨識（顏色會被灰階吃掉，但明暗/形狀仍清楚）。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class CloneTelegraphRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        var bosses = mc.level.getEntitiesOfClass(PlayerCloneEntity.class,
                mc.player.getBoundingBox().inflate(48));
        boolean any = false;
        for (PlayerCloneEntity b : bosses) {
            if (b.getTelegraphSkill() != 0 || b.isReflecting()) { any = true; break; }
        }
        if (!any) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());

        Player target = mc.player; // 1 對 1 假設：預兆畫在本機玩家（招式目標）
        for (PlayerCloneEntity boss : bosses) {
            if (boss.isReflecting()) drawReflectBox(vc, m, boss); // 鏡反：boss 身上鏡面框（此時打它會反傷）
            switch (boss.getTelegraphSkill()) {
                case 1 -> drawRamWall(vc, m, boss, target);   // 面前一道牆輪廓
                case 2 -> drawLiftCircle(vc, m, target);      // 腳下一個圈
                case 3 -> drawChargeLine(vc, m, boss, target); // 衝刺路徑線
                default -> { }
            }
        }

        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    // 玩家面前（朝 boss 那側）一道 3 寬 2 高的牆框
    private static void drawRamWall(VertexConsumer vc, Matrix4f m, PlayerCloneEntity boss, Player target) {
        Vec3 tp = target.position();
        Vec3 toBoss = horiz(boss.position().subtract(tp));
        Vec3 side = new Vec3(-toBoss.z, 0, toBoss.x);
        Vec3 center = tp.add(toBoss); // 玩家前方一格
        double y0 = tp.y, y1 = tp.y + 2.0;
        for (int s = -1; s <= 1; s++) {
            Vec3 c = center.add(side.scale(s));
            line(vc, m, c.x, y0, c.z, c.x, y1, c.z); // 三條豎線
        }
        Vec3 l = center.add(side.scale(-1.5)), r = center.add(side.scale(1.5));
        line(vc, m, l.x, y0, l.z, r.x, y0, r.z); // 底邊
        line(vc, m, l.x, y1, l.z, r.x, y1, r.z); // 頂邊
    }

    // 玩家腳下一個圈
    private static void drawLiftCircle(VertexConsumer vc, Matrix4f m, Player target) {
        Vec3 c = target.position();
        int seg = 28;
        double rad = 1.3, y = c.y + 0.05;
        double px = c.x + rad, pz = c.z;
        for (int i = 1; i <= seg; i++) {
            double a = i / (double) seg * Math.PI * 2.0;
            double x = c.x + Math.cos(a) * rad, z = c.z + Math.sin(a) * rad;
            line(vc, m, px, y, pz, x, y, z);
            px = x; pz = z;
        }
    }

    // boss → 玩家 的衝刺路徑線
    private static void drawChargeLine(VertexConsumer vc, Matrix4f m, PlayerCloneEntity boss, Player target) {
        Vec3 b = boss.position(), t = target.position();
        line(vc, m, b.x, b.y + 0.15, b.z, t.x, t.y + 0.15, t.z);
    }

    // 鏡反狀態：在 boss 身上畫一個鏡面方框（提示此時打它會反傷）
    private static void drawReflectBox(VertexConsumer vc, Matrix4f m, PlayerCloneEntity boss) {
        net.minecraft.world.phys.AABB box = boss.getBoundingBox().inflate(0.15);
        double x0 = box.minX, y0 = box.minY, z0 = box.minZ, x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        line(vc, m, x0, y0, z0, x1, y0, z0); line(vc, m, x1, y0, z0, x1, y0, z1);
        line(vc, m, x1, y0, z1, x0, y0, z1); line(vc, m, x0, y0, z1, x0, y0, z0); // 底
        line(vc, m, x0, y1, z0, x1, y1, z0); line(vc, m, x1, y1, z0, x1, y1, z1);
        line(vc, m, x1, y1, z1, x0, y1, z1); line(vc, m, x0, y1, z1, x0, y1, z0); // 頂
        line(vc, m, x0, y0, z0, x0, y1, z0); line(vc, m, x1, y0, z0, x1, y1, z0);
        line(vc, m, x1, y0, z1, x1, y1, z1); line(vc, m, x0, y0, z1, x0, y1, z1); // 豎
    }

    private static Vec3 horiz(Vec3 v) {
        double len = Math.sqrt(v.x * v.x + v.z * v.z);
        if (len < 1.0e-4) return new Vec3(0, 0, 1);
        return new Vec3(v.x / len, 0, v.z / len);
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                             double x1, double y1, double z1, double x2, double y2, double z2) {
        float nx = (float) (x2 - x1), ny = (float) (y2 - y1), nz = (float) (z2 - z1);
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l > 1.0e-4f) { nx /= l; ny /= l; nz /= l; } else { nx = 0; ny = 1; nz = 0; }
        vc.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(255, 255, 255, 255).setNormal(nx, ny, nz);
        vc.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(255, 255, 255, 255).setNormal(nx, ny, nz);
    }
}
