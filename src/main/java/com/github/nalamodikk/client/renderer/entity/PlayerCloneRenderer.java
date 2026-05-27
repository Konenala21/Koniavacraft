package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class PlayerCloneRenderer extends HumanoidMobRenderer<PlayerCloneEntity, PlayerModel<PlayerCloneEntity>> {

    public PlayerCloneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerCloneEntity entity) {
        UUID id = entity.getSourceUUID().orElse(null);
        Minecraft mc = Minecraft.getInstance();
        if (id != null && mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(id);
            if (info != null) {
                return info.getSkin().texture();
            }
        }
        return DefaultPlayerSkin.get(id != null ? id : entity.getUUID()).texture();
    }
}
