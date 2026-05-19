package com.github.nalamodikk.client.renderer.deployer;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ManaDeployerBEWLR extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KoniavacraftMod.MOD_ID, "textures/block/mana_deployer_texture.png");

    private static final ResourceLocation MODEL_LOC = ResourceLocation.fromNamespaceAndPath(
        KoniavacraftMod.MOD_ID, "models/block/mana_deployer.model.json");

    private BedrockGeoData model;
    private boolean loadAttempted = false;

    public ManaDeployerBEWLR() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx,
                             PoseStack ps, MultiBufferSource buffers,
                             int light, int overlay) {
        if (!loadAttempted) {
            loadAttempted = true;
            model = BedrockLoader.loadModel(Minecraft.getInstance().getResourceManager(), MODEL_LOC);
        }
        if (model == null) return;

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEXTURE));

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        for (BedrockGeoData.Bone root : model.roots) {
            ManaDeployerRenderer.renderBone(root, ps, vc, light, overlay,
                0f, model.texW, model.texH, null);
        }

        ps.popPose();
    }
}
