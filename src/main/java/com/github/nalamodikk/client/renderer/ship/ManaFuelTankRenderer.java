package com.github.nalamodikk.client.renderer.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ManaFuelTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 燃料槽視覺液位：在槽的空玻璃窗內畫一個 cyan 魔力液體 box，高度 = 魔力 / 容量。
 * 一眼看全船哪槽滿/空。船上的槽靠鏡射同步真身 NBT(每 16 tick)，世界裡的槽靠 BE getUpdateTag/sendBlockUpdated。
 * 每面畫正反兩個 winding，免去背面剔除的繞序問題(box 很小，成本可忽略)。
 */
public class ManaFuelTankRenderer implements BlockEntityRenderer<ManaFuelTankBlockEntity> {
    private static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/misc/white.png");
    // 液窗內腔範圍(對齊蕎麥麵新模型 mana_reservoir 的玻璃儲液芯：內框 cube x/z 3~13、y 4~28，
    // 各內縮 0.5 不貼到框；模型是塔狀往上長到 2 格高，故 Y_SPAN 用 24/16)
    private static final float X0 = 3.5f / 16f, X1 = 12.5f / 16f;
    private static final float Z0 = 3.5f / 16f, Z1 = 12.5f / 16f;
    private static final float Y0 = 4f / 16f, Y_SPAN = 24f / 16f;
    private static final int COLOR = (205 << 24) | (40 << 16) | (200 << 8) | 224; // ARGB cyan, 半透明

    public ManaFuelTankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ManaFuelTankBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float fill = Mth.clamp(be.getManaStorage().getManaStored() / (float) ManaFuelTankBlockEntity.CAPACITY, 0f, 1f);
        if (fill <= 0.001f) return;
        float yT = Y0 + Y_SPAN * fill;

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentCull(WHITE));
        PoseStack.Pose p = poseStack.last();

        quad(vc, p, packedLight, X0, Y0, Z1, X0, yT, Z1, X1, yT, Z1, X1, Y0, Z1); // +Z
        quad(vc, p, packedLight, X1, Y0, Z0, X1, yT, Z0, X0, yT, Z0, X0, Y0, Z0); // -Z
        quad(vc, p, packedLight, X1, Y0, Z1, X1, yT, Z1, X1, yT, Z0, X1, Y0, Z0); // +X
        quad(vc, p, packedLight, X0, Y0, Z0, X0, yT, Z0, X0, yT, Z1, X0, Y0, Z1); // -X
        quad(vc, p, packedLight, X0, yT, Z0, X0, yT, Z1, X1, yT, Z1, X1, yT, Z0); // top
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, int light,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        v(vc, p, light, ax, ay, az, 0, 0); v(vc, p, light, bx, by, bz, 0, 1);
        v(vc, p, light, cx, cy, cz, 1, 1); v(vc, p, light, dx, dy, dz, 1, 0);
        v(vc, p, light, dx, dy, dz, 1, 0); v(vc, p, light, cx, cy, cz, 1, 1);
        v(vc, p, light, bx, by, bz, 0, 1); v(vc, p, light, ax, ay, az, 0, 0);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose p, int light, float x, float y, float z, float u, float vv) {
        vc.addVertex(p, x, y, z)
                .setColor((COLOR >> 16) & 0xFF, (COLOR >> 8) & 0xFF, COLOR & 0xFF, (COLOR >>> 24) & 0xFF)
                .setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
    }
}
