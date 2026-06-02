package com.github.nalamodikk.client.renderer.armor;

import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyHelmetItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 把魔力合金頭盔的自訂 bedrock 模型畫在穿戴者頭上。vanilla 頭盔由 item 的
 * {@code getHumanoidArmorModel} 回傳空模型隱藏掉,這層負責畫真正的新模型。
 */
public class ManaAlloyArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private final ManaAlloyArmorModel model;

    public ManaAlloyArmorLayer(RenderLayerParent<T, M> parent, EntityModelSet models) {
        super(parent);
        this.model = new ManaAlloyArmorModel(models.bakeLayer(ManaAlloyArmorModel.LAYER));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof ManaAlloyHelmetItem)) return;

        // 把玩家模型 head 的姿勢同步到合金頭盔 bone,讓它跟著頭轉。
        model.head().copyFrom(getParentModel().head);
        VertexConsumer vc = buffer.getBuffer(RenderType.armorCutoutNoCull(ManaAlloyArmorModel.TEXTURE));
        model.renderHead(pose, vc, light, OverlayTexture.NO_OVERLAY);
    }
}
