package com.github.nalamodikk.common.block.blockentity.research;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils.ModelElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ResearchTableRenderer implements BlockEntityRenderer<ResearchTableBlockEntity>,
        IBlockEntityRendererExtension<ResearchTableBlockEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/research_table.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/research_table_texture.png");

    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
    private boolean modelLoaded = false;

    public ResearchTableRenderer(BlockEntityRendererProvider.Context context) {
        loadAndParseModel();
    }

    private void loadAndParseModel() {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MODEL_LOCATION);
            if (resource.isEmpty()) {
                LOGGER.error("Missing research table model: {}", MODEL_LOCATION);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject modelData = JsonParser.parseReader(reader).getAsJsonObject();
                groupElements.clear();
                groupElements.putAll(BlockbenchModelRenderUtils.parseGroupedElements(modelData, true));
                modelLoaded = true;
                LOGGER.debug("Research table model loaded. groups={}", groupElements.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load research table model.", e);
        }
    }

    @Override
    public void render(ResearchTableBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!modelLoaded) return;

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        poseStack.pushPose();
        for (String group : groupElements.keySet()) {
            BlockbenchModelRenderUtils.renderGroup(
                    poseStack, vertexConsumer, packedLight, packedOverlay,
                    groupElements, group, 0f, 0f, 0f, 0f, this::getGroupOrigin
            );
        }
        poseStack.popPose();
    }

    private Vector3f getGroupOrigin(String groupName) {
        return new Vector3f(0.5f, 0.5f, 0.5f);
    }
}
