package com.github.nalamodikk.client.renderer.armor;

import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyChestplateItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyHelmetItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyLeggingsItem;
import com.github.nalamodikk.common.item.equipment.boots.ManaAlloyBootsItem;
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
 * 把魔力合金盔甲的自訂 bedrock 模型畫在穿戴者身上。四件盔甲共用同一個 geometry,本層依各裝備槽
 * 穿了什麼,分別渲染對應的 bone 群組(頭/身手/腿/靴)。vanilla 盔甲由各 item 的
 * {@code getHumanoidArmorModel} 回傳空模型隱藏掉,真正的模型由這層畫。
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
        ItemStack chest  = entity.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs   = entity.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet   = entity.getItemBySlot(EquipmentSlot.FEET);

        boolean wearHead  = helmet.getItem() instanceof ManaAlloyHelmetItem;
        boolean wearChest = chest.getItem() instanceof ManaAlloyChestplateItem;
        boolean wearLegs  = legs.getItem() instanceof ManaAlloyLeggingsItem;
        boolean wearFeet  = feet.getItem() instanceof ManaAlloyBootsItem;

        if (!wearHead && !wearChest && !wearLegs && !wearFeet) return;

        HumanoidModel<T> parent = getParentModel();
        VertexConsumer vc = buffer.getBuffer(RenderType.armorCutoutNoCull(ManaAlloyArmorModel.TEXTURE));

        if (wearHead) {
            model.poseHead(parent);
            model.renderHead(pose, vc, light, OverlayTexture.NO_OVERLAY);
        }
        if (wearChest) {
            model.poseChest(parent);
            model.renderChest(pose, vc, light, OverlayTexture.NO_OVERLAY);
        }
        if (wearLegs) {
            model.poseLegs(parent);
            model.renderLegs(pose, vc, light, OverlayTexture.NO_OVERLAY);
        }
        if (wearFeet) {
            model.poseBoots(parent);
            model.renderBoots(pose, vc, light, OverlayTexture.NO_OVERLAY);
        }
    }
}
