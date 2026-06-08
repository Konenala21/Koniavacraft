package com.github.nalamodikk.client.renderer.armor;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.model.bedrock.BedrockGeoParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 魔力合金盔甲的 bedrock 模型(用專案自己的 {@link BedrockGeoParser},非 GeckoLib)。
 * 一個 geometry 含全部四件的 bone,依 bone 名稱拆成四組:
 * <ul>
 *   <li>頭盔(HEAD):{@code armorHead}(父 {@code bipedHead})</li>
 *   <li>胸甲(CHEST):{@code armorBody} + {@code armorRightArm} + {@code armorLeftArm}</li>
 *   <li>護腿(LEGS):{@code armorRightLeg} + {@code armorLeftLeg}</li>
 *   <li>靴子(FEET):{@code armorRightBoot} + {@code armorLeftBoot}</li>
 * </ul>
 *
 * 渲染方式:由 {@link ManaAlloyArmorLayer} 把玩家模型對應部件(head/body/arm/leg)的姿勢 copy 到
 * 這裡的對應 bone,再用合金盔甲貼圖畫出來(蓋掉被 item 隱藏的 vanilla 盔甲)。
 */
public class ManaAlloyArmorModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_alloy_armor"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "textures/models/armor/mana_alloy_armor.png");

    // 頭
    private final ModelPart head;
    // 身/手
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    // 腿
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    // 靴(在 geo 裡掛在 leg bone 下,跟著腿動)
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;

    public ManaAlloyArmorModel(ModelPart root) {
        // top-level bipedXxx bones 是骨架,真正的 cube 在它們底下的 armorXxx child。
        ModelPart bipedHead     = root.getChild("bipedHead");
        ModelPart bipedBody     = root.getChild("bipedBody");
        ModelPart bipedRightArm = root.getChild("bipedRightArm");
        ModelPart bipedLeftArm  = root.getChild("bipedLeftArm");
        ModelPart bipedRightLeg = root.getChild("bipedRightLeg");
        ModelPart bipedLeftLeg  = root.getChild("bipedLeftLeg");

        this.head     = bipedHead;
        this.body     = bipedBody;
        this.rightArm = bipedRightArm;
        this.leftArm  = bipedLeftArm;
        this.rightLeg = bipedRightLeg;
        this.leftLeg  = bipedLeftLeg;
        // boot 是獨立的 top-level bone(pivot 與 leg 相同),跟 leg 分開以免穿護腿時連靴一起畫。
        this.rightBoot = root.getChild("armorRightBoot");
        this.leftBoot  = root.getChild("armorLeftBoot");
    }

    public static LayerDefinition createLayer() {
        return BedrockGeoParser.parse(
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_alloy_armor.geo.json"));
    }

    // ── 姿勢同步:把 vanilla humanoid 部件的姿勢 copy 到對應 bone ────────────────

    /** 頭盔:跟 vanilla head 同動。 */
    public void poseHead(HumanoidModel<? extends LivingEntity> parent) {
        head.copyFrom(parent.head);
    }

    /** 胸甲:身體跟 body,左右手臂跟對應 arm。 */
    public void poseChest(HumanoidModel<? extends LivingEntity> parent) {
        body.copyFrom(parent.body);
        rightArm.copyFrom(parent.rightArm);
        leftArm.copyFrom(parent.leftArm);
    }

    /** 護腿:左右腿跟對應 leg。 */
    public void poseLegs(HumanoidModel<? extends LivingEntity> parent) {
        rightLeg.copyFrom(parent.rightLeg);
        leftLeg.copyFrom(parent.leftLeg);
    }

    /** 靴子:跟 vanilla leg 同動(boot bone 掛在 leg 骨架下)。 */
    public void poseBoots(HumanoidModel<? extends LivingEntity> parent) {
        rightBoot.copyFrom(parent.rightLeg);
        leftBoot.copyFrom(parent.leftLeg);
    }

    // ── 渲染:只畫該 slot 屬於的 bone ────────────────────────────────────────────

    public void renderHead(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        head.render(pose, buffer, light, overlay);
    }

    public void renderChest(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        body.render(pose, buffer, light, overlay);
        rightArm.render(pose, buffer, light, overlay);
        leftArm.render(pose, buffer, light, overlay);
    }

    public void renderLegs(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        rightLeg.render(pose, buffer, light, overlay);
        leftLeg.render(pose, buffer, light, overlay);
    }

    public void renderBoots(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        rightBoot.render(pose, buffer, light, overlay);
        leftBoot.render(pose, buffer, light, overlay);
    }
}
