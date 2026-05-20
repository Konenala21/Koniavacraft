package com.github.nalamodikk.client.renderer.item;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FloatingTurretBEWLR extends BlockEntityWithoutLevelRenderer {

    // floating_turret_geo.json：用 BlockBench 匯出原始幾何體存到這個路徑
    // assets/koniava/models/item/floating_turret_geo.json
    public static final ModelResourceLocation GEO_MODEL_LOCATION =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "item/floating_turret_geo"));

    private static FloatingTurretBEWLR instance;

    public FloatingTurretBEWLR(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    public static FloatingTurretBEWLR getInstance() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new FloatingTurretBEWLR(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context,
                             PoseStack ps, MultiBufferSource buf,
                             int packedLight, int packedOverlay) {
        // 第一人稱：entity 負責渲染 ghost，手臂顯示為空手
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return;
        }
        // 第三人稱：照常渲染，讓玩家手上顯示小型控制器（JSON thirdperson 設定生效）

        // GUI / 地板 / 框架展示：使用 floating_turret_geo.json 的幾何體
        BakedModel geoModel = Minecraft.getInstance().getModelManager().getModel(GEO_MODEL_LOCATION);
        Minecraft.getInstance().getItemRenderer()
                .render(stack, context, false, ps, buf, packedLight, packedOverlay, geoModel);
    }
}
