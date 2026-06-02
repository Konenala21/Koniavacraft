package com.github.nalamodikk.client.renderer.armor;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.model.bedrock.BedrockGeoParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 魔力合金盔甲的 bedrock 模型(用專案自己的 {@link BedrockGeoParser},非 GeckoLib)。
 * 目前只有頭盔(armorHead)有實體 cube,所以只渲染頭部;之後夥伴補了身/腿/靴的模型再擴充。
 *
 * 渲染方式:由 {@link ManaAlloyArmorLayer} 把玩家模型的 head 姿勢 copy 到這裡的 head bone,
 * 再用合金盔甲貼圖畫出來(蓋掉被 item 隱藏的 vanilla 頭盔)。
 */
public class ManaAlloyArmorModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_alloy_armor"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "textures/models/armor/mana_alloy_armor.png");

    private final ModelPart head;

    public ManaAlloyArmorModel(ModelPart root) {
        this.head = root.getChild("bipedHead");
    }

    public static LayerDefinition createLayer() {
        return BedrockGeoParser.parse(
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_alloy_armor.geo.json"));
    }

    /** 頭部 bone(內含 armorHead 的頭盔 cube)。 */
    public ModelPart head() {
        return head;
    }

    public void renderHead(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        head.render(pose, buffer, light, overlay);
    }
}
