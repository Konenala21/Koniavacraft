package com.github.nalamodikk.client.renderer.item;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WandRodBEWLR extends BlockEntityWithoutLevelRenderer {

    public static final ModelResourceLocation MODEL_LOCATION =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "item/wand_rod_geo"));

    // ── Core ──────────────────────────────────────────────────────────────
    private static final float CORE_X     = 0.75f    / 16f;
    private static final float CORE_Y     = 21f   / 16f;
    private static final float CORE_Z     = 0.15f / 16f;
    private static final float CORE_SCALE = 0.85f;

    // ── Upgrade slots → 6 prongs ──────────────────────────────────────────
    // slot0=left  slot1=right  slot2=front  slot3=back  (mid level, y≈9.57)
    // slot4=lower-left  slot5=lower-right               (lower level, y≈0)
    private static final float UPG_Y  = 2.0f / 16f;
    private static final float UPG_X0 = -5f    / 16f;  private static final float UPG_Z0 = 0f   / 16f;
    private static final float UPG_X1 = 5.75f  / 16f;  private static final float UPG_Z1 = 0f   / 16f;
    private static final float UPG_X2 = 0f     / 16f;  private static final float UPG_Z2 = -5f  / 16f;
    private static final float UPG_X3 = 0f     / 16f;  private static final float UPG_Z3 = 5f   / 16f;
    // slot4/5: lower prongs — X/Z/rot mirror slot0/1, Y offset = -9.6/16 from mid level
    private static final float UPG_Y45 = (2.0f - 9.6f) / 16f;
    private static final float UPG_X4 = -5f    / 16f;  private static final float UPG_Z4 = 0f   / 16f;
    private static final float UPG_X5 = 5.75f  / 16f;  private static final float UPG_Z5 = 0f   / 16f;
    // Y-axis rotation per slot
    private static final float UPG_ROT0 = 180f;
    private static final float UPG_ROT1 = 0f;
    private static final float UPG_ROT2 = 90f;
    private static final float UPG_ROT3 = -90f;
    private static final float UPG_ROT4 = 180f;
    private static final float UPG_ROT5 = 0f;
    private static final float UPGRADE_SCALE = 0.1f;

    // ── First-person (separate for independent tuning) ────────────────────
    private static final float FP_CORE_X     = 0f    / 16f;
    private static final float FP_CORE_Y     = 23f   / 16f;
    private static final float FP_CORE_Z     = -1.0f / 16f;
    private static final float FP_CORE_SCALE = 0.85f;

    // slot0/slot1 各自獨立 Y，補償第一人稱 25° X 旋轉造成的視覺高低差
    private static final float FP_UPG_Y0 = 2.0f / 16f;   // slot0 left  — tune independently
    private static final float FP_UPG_Y1 = 2.15f / 16f;   // slot1 right — tune independently
    private static final float FP_UPG_X0 = 4f     / 16f;
    private static final float FP_UPG_Z0 = 8f     / 16f;
    private static final float FP_UPG_X1 = 6.05f / 16f;
    private static final float FP_UPG_Z1 = 0.25f     / 16f;
    private static final float FP_UPG_X2 = 6.6f   / 16f;
    private static final float FP_UPG_Z2 = 3.6f   / 16f;
    private static final float FP_UPG_X3 = 8.6f   / 16f;
    private static final float FP_UPG_Z3 = 12.35f / 16f;
    private static final float FP_UPG_ROT0  =  -90f;
    private static final float FP_UPG_ROT1  =   0f;
    private static final float FP_UPG_ROT2  =   0f;
    private static final float FP_UPG_ROT3  = 180f;
    // X-axis tilt to flatten items that appear "standing upright" in first-person
    private static final float FP_UPG_ROTX0 =  0f;
    private static final float FP_UPG_ROTX1 =  0f;
    private static final float FP_UPGRADE_SCALE = 0.1f;

    private static WandRodBEWLR instance;

    public WandRodBEWLR(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    public static WandRodBEWLR getInstance() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new WandRodBEWLR(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context,
                             PoseStack ps, MultiBufferSource buf,
                             int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();

        ps.translate(0.5f, 0.5f, 0.5f);

        BakedModel wandModel = mc.getModelManager().getModel(MODEL_LOCATION);
        mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, ps, buf, packedLight, packedOverlay, wandModel);

        WandCoreData data = WandRodItem.getData(stack);

        boolean fp = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                  || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

        if (data.hasCore()) {
            renderAttached(data.core(),
                    fp ? FP_CORE_X : CORE_X,
                    fp ? FP_CORE_Y : CORE_Y,
                    fp ? FP_CORE_Z : CORE_Z,
                    fp ? FP_CORE_SCALE : CORE_SCALE,
                    0f, 0f, ps, buf, packedLight, packedOverlay);
        }

        float[] upgX   = fp ? new float[]{ FP_UPG_X0, FP_UPG_X1, FP_UPG_X2, FP_UPG_X3, UPG_X4,  UPG_X5  }
                            : new float[]{ UPG_X0,    UPG_X1,    UPG_X2,    UPG_X3,    UPG_X4,  UPG_X5  };
        float[] upgZ   = fp ? new float[]{ FP_UPG_Z0, FP_UPG_Z1, FP_UPG_Z2, FP_UPG_Z3, UPG_Z4,  UPG_Z5  }
                            : new float[]{ UPG_Z0,    UPG_Z1,    UPG_Z2,    UPG_Z3,    UPG_Z4,  UPG_Z5  };
        float[] upgY   = fp ? new float[]{ FP_UPG_Y0, FP_UPG_Y1, UPG_Y,     UPG_Y,     UPG_Y45, UPG_Y45 }
                            : new float[]{ UPG_Y,     UPG_Y,     UPG_Y,     UPG_Y,     UPG_Y45, UPG_Y45 };
        float[] upgRot  = fp ? new float[]{ FP_UPG_ROT0, FP_UPG_ROT1, FP_UPG_ROT2, FP_UPG_ROT3, UPG_ROT4, UPG_ROT5 }
                             : new float[]{ UPG_ROT0,    UPG_ROT1,    UPG_ROT2,    UPG_ROT3,    UPG_ROT4, UPG_ROT5 };
        float[] upgRotX = fp ? new float[]{ FP_UPG_ROTX0, FP_UPG_ROTX1, 0f, 0f, 0f, 0f }
                             : new float[]{ 0f, 0f, 0f, 0f, 0f, 0f };
        float upgS = fp ? FP_UPGRADE_SCALE : UPGRADE_SCALE;
        int slotCount = fp ? 2 : WandCoreData.UPGRADE_SLOTS;
        for (int i = 0; i < slotCount; i++) {
            ItemStack upgrade = data.getUpgrade(i);
            if (!upgrade.isEmpty()) {
                renderAttached(upgrade, upgX[i], upgY[i], upgZ[i], upgS, upgRot[i], upgRotX[i],
                        ps, buf, packedLight, packedOverlay);
            }
        }
    }

    private void renderAttached(ItemStack item, float x, float y, float z, float scale,
                                float rotY, float rotX,
                                PoseStack ps, MultiBufferSource buf,
                                int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        ps.pushPose();
        ps.translate(x, y, z);
        if (rotY != 0f) ps.mulPose(Axis.YP.rotationDegrees(rotY));
        if (rotX != 0f) ps.mulPose(Axis.XP.rotationDegrees(rotX));
        ps.scale(scale, scale, scale);
        mc.getItemRenderer().renderStatic(item, ItemDisplayContext.NONE,
                packedLight, packedOverlay, ps, buf, mc.level, 0);
        ps.popPose();
    }
}
