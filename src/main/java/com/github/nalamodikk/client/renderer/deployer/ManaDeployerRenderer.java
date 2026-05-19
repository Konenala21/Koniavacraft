package com.github.nalamodikk.client.renderer.deployer;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_deployer.ManaDeployerBlock;
import com.github.nalamodikk.common.block.blockentity.mana_deployer.ManaDeployerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.Map;

public class ManaDeployerRenderer implements BlockEntityRenderer<ManaDeployerBlockEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KoniavacraftMod.MOD_ID, "textures/block/mana_deployer_texture.png");

    private static final ResourceLocation MODEL_LOC = ResourceLocation.fromNamespaceAndPath(
        KoniavacraftMod.MOD_ID, "models/block/mana_deployer.model.json");

    private static final ResourceLocation ANIM_LOC = ResourceLocation.fromNamespaceAndPath(
        KoniavacraftMod.MOD_ID, "anime/mana_deployer.animation.json");

    private static final String ANIM_NAME = "click_animation";

    private BedrockGeoData  model;
    private BedrockAnimData anim;
    private boolean loadAttempted = false;

    public ManaDeployerRenderer(BlockEntityRendererProvider.Context ctx) {}

    private void ensureLoaded(Minecraft mc) {
        if (loadAttempted) return;
        loadAttempted = true;
        var rm = mc.getResourceManager();
        model = BedrockLoader.loadModel(rm, MODEL_LOC);
        Map<String, BedrockAnimData> anims = BedrockLoader.loadAnimations(rm, ANIM_LOC);
        anim  = anims.get(ANIM_NAME);
    }

    @Override
    public void render(ManaDeployerBlockEntity be, float partialTick,
                       PoseStack ps, MultiBufferSource buffers,
                       int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        ensureLoaded(mc);
        if (model == null) return;

        Direction facing  = be.getBlockState().getValue(ManaDeployerBlock.FACING);
        float     animSec = be.getAnimTime(partialTick);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEXTURE));

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(facingYRot(facing)));

        for (BedrockGeoData.Bone root : model.roots) {
            renderBone(root, ps, vc, light, overlay, animSec, model.texW, model.texH, anim);
        }

        ps.popPose();
    }

    static void renderBone(BedrockGeoData.Bone bone, PoseStack ps,
                           VertexConsumer vc, int light, int overlay,
                           float animSec, float tw, float th, BedrockAnimData anim) {
        ps.pushPose();

        float px = -bone.pivot[0] / 16f;
        float py =  bone.pivot[1] / 16f;
        float pz =  bone.pivot[2] / 16f;

        ps.translate(px, py, pz);

        if (anim != null) {
            BedrockAnimData.BoneAnim ba = anim.bones.get(bone.name);
            if (ba != null) {
                float[] pos = ba.position(animSec);
                float[] rot = ba.rotation(animSec);
                ps.translate(-pos[0] / 16f, pos[1] / 16f, pos[2] / 16f);
                // Geckolib convention: ZYX code, negate X+Y, Z unchanged
                ps.mulPose(Axis.ZP.rotationDegrees( rot[2]));
                ps.mulPose(Axis.YP.rotationDegrees(-rot[1]));
                ps.mulPose(Axis.XP.rotationDegrees(-rot[0]));
            }
        }

        ps.translate(-px, -py, -pz);

        for (BedrockGeoData.Cube cube : bone.cubes) {
            renderCube(cube, ps, vc, light, overlay, tw, th);
        }

        for (BedrockGeoData.Bone child : bone.children) {
            renderBone(child, ps, vc, light, overlay, animSec, tw, th, anim);
        }

        ps.popPose();
    }

    static void renderCube(BedrockGeoData.Cube cube, PoseStack ps,
                            VertexConsumer vc, int light, int overlay,
                            float tw, float th) {
        ps.pushPose();

        float cpx = -cube.pivot[0] / 16f;
        float cpy =  cube.pivot[1] / 16f;
        float cpz =  cube.pivot[2] / 16f;
        ps.translate(cpx, cpy, cpz);
        // Geckolib convention: ZYX code, negate X+Y, Z unchanged
        ps.mulPose(Axis.ZP.rotationDegrees( cube.rotation[2]));
        ps.mulPose(Axis.YP.rotationDegrees(-cube.rotation[1]));
        ps.mulPose(Axis.XP.rotationDegrees(-cube.rotation[0]));
        ps.translate(-cpx, -cpy, -cpz);

        // X-mirror: -(origin+size) → x0, -origin → x1 (matches Geckolib vertex layout)
        float x0 = -(cube.origin[0] + cube.size[0]) / 16f;
        float x1 = -cube.origin[0] / 16f;
        float y0 = cube.origin[1] / 16f;
        float z0 = cube.origin[2] / 16f;
        float y1 = y0 + cube.size[1] / 16f;
        float z1 = z0 + cube.size[2] / 16f;

        PoseStack.Pose pose = ps.last();

        if (cube.north != null) {
            float u0=cube.north.u0(tw), v0=cube.north.v0(th), u1=cube.north.u1(tw), v1=cube.north.v1(th);
            quad(vc,pose, x1,y1,z0, x0,y1,z0, x0,y0,z0, x1,y0,z0,
                 u0,v0, u1,v0, u1,v1, u0,v1, 0,0,-1, light,overlay);
        }
        if (cube.south != null) {
            float u0=cube.south.u0(tw), v0=cube.south.v0(th), u1=cube.south.u1(tw), v1=cube.south.v1(th);
            quad(vc,pose, x0,y1,z1, x1,y1,z1, x1,y0,z1, x0,y0,z1,
                 u0,v0, u1,v0, u1,v1, u0,v1, 0,0,1, light,overlay);
        }
        if (cube.east != null) {
            float u0=cube.east.u0(tw), v0=cube.east.v0(th), u1=cube.east.u1(tw), v1=cube.east.v1(th);
            quad(vc,pose, x1,y1,z1, x1,y1,z0, x1,y0,z0, x1,y0,z1,
                 u0,v0, u1,v0, u1,v1, u0,v1, 1,0,0, light,overlay);
        }
        if (cube.west != null) {
            float u0=cube.west.u0(tw), v0=cube.west.v0(th), u1=cube.west.u1(tw), v1=cube.west.v1(th);
            quad(vc,pose, x0,y1,z0, x0,y1,z1, x0,y0,z1, x0,y0,z0,
                 u0,v0, u1,v0, u1,v1, u0,v1, -1,0,0, light,overlay);
        }
        if (cube.up != null) {
            float u0=cube.up.u0(tw), v0=cube.up.v0(th), u1=cube.up.u1(tw), v1=cube.up.v1(th);
            quad(vc,pose, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1,
                 u0,v0, u1,v0, u1,v1, u0,v1, 0,1,0, light,overlay);
        }
        if (cube.down != null) {
            float u0=cube.down.u0(tw), v0=cube.down.v0(th), u1=cube.down.u1(tw), v1=cube.down.v1(th);
            quad(vc,pose, x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0,
                 u0,v0, u1,v0, u1,v1, u0,v1, 0,-1,0, light,overlay);
        }

        ps.popPose();
    }

    static void quad(VertexConsumer vc, PoseStack.Pose pose,
                             float x0,float y0,float z0,
                             float x1,float y1,float z1,
                             float x2,float y2,float z2,
                             float x3,float y3,float z3,
                             float u0,float v0, float u1,float v1,
                             float u2,float v2, float u3,float v3,
                             float nx,float ny,float nz,
                             int light, int overlay) {
        vc.addVertex(pose,x3,y3,z3).setColor(255,255,255,255).setUv(u3,v3).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
        vc.addVertex(pose,x2,y2,z2).setColor(255,255,255,255).setUv(u2,v2).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
        vc.addVertex(pose,x1,y1,z1).setColor(255,255,255,255).setUv(u1,v1).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
        vc.addVertex(pose,x0,y0,z0).setColor(255,255,255,255).setUv(u0,v0).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
    }

    /** Model built with arm pointing +Z (south). Rotate so arm points FACING direction. */
    private static float facingYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0f;
            case EAST  -> 90f;
            case NORTH -> 180f;
            case WEST  -> 270f;
            default    -> 0f;
        };
    }

    @Override
    public AABB getRenderBoundingBox(ManaDeployerBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                        pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
