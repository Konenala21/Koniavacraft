package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.SpaceCrackEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 在 AFTER_PARTICLES 階段渲染所有 SpaceCrackEntity，繞開 entity dispatcher。
 * 這樣方塊、實體、BlockEntity（祭壇/箱子等）都會被裂縫穿透視覺覆蓋。
 * Perf: 用 ACTIVE static set 追蹤活動裂縫，沒裂縫時跳過整個 AABB scan + spatial 查找
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class SpaceCrackStageRenderer {

    // 客戶端追蹤目前場景中的 SpaceCrackEntity（由 entity 的 onAddedToLevel / onClientRemoval 維護）
    // 用 IdentityHashMap-backed set 避免每幀 getEntitiesOfClass 的 AABB + spatial 查找
    private static final java.util.Set<SpaceCrackEntity> ACTIVE = java.util.Collections.newSetFromMap(
            new java.util.WeakHashMap<>());

    public static void register(SpaceCrackEntity crack) { ACTIVE.add(crack); }
    public static void unregister(SpaceCrackEntity crack) { ACTIVE.remove(crack); }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (ACTIVE.isEmpty()) return; // 沒裂縫直接跳過，避免每幀 spatial 查找
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        // 直接 iterate ACTIVE set，O(N) 不需要 spatial 查找
        for (SpaceCrackEntity crack : ACTIVE) {
            if (!crack.isAlive() || crack.level() != mc.level) continue;
            double ex = crack.xOld + (crack.getX() - crack.xOld) * partialTick;
            double ey = crack.yOld + (crack.getY() - crack.yOld) * partialTick;
            double ez = crack.zOld + (crack.getZ() - crack.zOld) * partialTick;
            // 64 格距離過濾（粗略，避免遠處 hidden 裂縫白費渲染）
            double dx = ex - camPos.x, dy = ey - camPos.y, dz = ez - camPos.z;
            if (dx * dx + dy * dy + dz * dz > 64.0 * 64.0) continue;
            ps.pushPose();
            ps.translate(dx, dy, dz);
            SpaceCrackRenderer.renderCrack(crack, ps, buf, partialTick);
            ps.popPose();
        }
        buf.endBatch();
    }
}
