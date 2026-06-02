package com.github.nalamodikk.client.renderer.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * 魔力合金頭盔的 client 擴充:{@code getHumanoidArmorModel} 回傳一個「空的」HumanoidModel,
 * 讓 vanilla 盔甲層畫不出東西(頭盔被隱藏),真正的新模型由 {@link ManaAlloyArmorLayer} 畫。
 *
 * (放在 client 套件,只在 item 的 initializeClient 內被引用,不會在伺服端載入。)
 */
public final class ManaAlloyArmorClientExtension implements IClientItemExtensions {

    public static final ManaAlloyArmorClientExtension INSTANCE = new ManaAlloyArmorClientExtension();

    private HumanoidModel<?> emptyModel;

    private ManaAlloyArmorClientExtension() {}

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, ItemStack stack,
                                                  EquipmentSlot slot, HumanoidModel<?> original) {
        if (emptyModel == null) emptyModel = buildEmpty();
        return emptyModel; // 空模型:把 vanilla 頭盔渲染掉(實際模型走自訂 layer)
    }

    /** 建一個有標準 7 個 humanoid 部件但都沒有 cube 的空模型。 */
    private static HumanoidModel<LivingEntity> buildEmpty() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (String name : new String[]{"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        }
        ModelPart baked = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        return new HumanoidModel<>(baked);
    }
}
