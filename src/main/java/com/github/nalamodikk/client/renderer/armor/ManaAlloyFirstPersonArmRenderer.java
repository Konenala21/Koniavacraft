package com.github.nalamodikk.client.renderer.armor;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyChestplateItem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * 第一人稱:穿魔力合金胸甲時，把胸甲的手臂 bone 疊畫在 first-person 手臂上。
 * 第三人稱由 {@link ManaAlloyArmorLayer} 處理；第一人稱手臂是另一條 renderer 路徑（RenderArmEvent），
 * 所以在這裡補畫。不取消事件 = 蓋在 vanilla 手臂上（盔甲袖子）。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ManaAlloyFirstPersonArmRenderer {

    private ManaAlloyFirstPersonArmRenderer() {}

    private static ManaAlloyArmorModel model;

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        if (!(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ManaAlloyChestplateItem)) return;

        if (model == null) {
            model = new ManaAlloyArmorModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ManaAlloyArmorModel.LAYER));
        }
        VertexConsumer vc = event.getMultiBufferSource()
                .getBuffer(RenderType.armorCutoutNoCull(ManaAlloyArmorModel.TEXTURE));
        model.renderFirstPersonArm(event.getArm(), event.getPoseStack(), vc,
                event.getPackedLight(), OverlayTexture.NO_OVERLAY);
    }
}
