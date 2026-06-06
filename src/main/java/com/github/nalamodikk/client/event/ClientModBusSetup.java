package com.github.nalamodikk.client.event;

import com.github.nalamodikk.client.particle.ClientInteractiveFormationManager;
import com.github.nalamodikk.client.renderer.ManaStrikeShaderRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarExplosionRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarFadeRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarMagicCircleRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarShockwaveRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarT45OrbRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarT6ClimaxRenderer;
import com.github.nalamodikk.client.renderer.armor.ManaAlloyArmorLayer;
import com.github.nalamodikk.client.renderer.armor.ManaAlloyArmorModel;
import com.github.nalamodikk.client.renderer.entity.FloatingTurretModel;
import com.github.nalamodikk.client.renderer.entity.ShipEntityRenderer;
import com.github.nalamodikk.client.renderer.entity.FloatingTurretProjectileRenderer;
import com.github.nalamodikk.client.renderer.entity.FloatingTurretRenderer;
import com.github.nalamodikk.client.renderer.entity.NaraPhantomRenderer;
import com.github.nalamodikk.client.renderer.entity.PlayerCloneRenderer;
import com.github.nalamodikk.client.renderer.entity.SpaceCrackRenderer;
import com.github.nalamodikk.client.renderer.entity.TrainingDummyModel;
import com.github.nalamodikk.client.renderer.entity.TrainingDummyRenderer;
import com.github.nalamodikk.client.renderer.item.FloatingTurretBEWLR;
import com.github.nalamodikk.client.renderer.turret.TurretHitEffectRenderer;
import com.github.nalamodikk.register.ModEntities;
import com.github.nalamodikk.register.ModParticles;
import com.github.nalamodikk.register.client.ModColorHandlers;
import com.github.nalamodikk.register.client.ModKeyMappings;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import com.github.nalamodikk.client.renderer.dimension.SpacePlanetManager;
import com.github.nalamodikk.client.renderer.dimension.SpaceSkyRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 所有 client 端 mod-bus 註冊(entity renderer / layer / armor layer / model / reload listener)。
 *
 * 為什麼獨立成一個類別:這些 lambda 直接引用 client-only 型別(例如 {@code new ManaAlloyArmorLayer<>(pr,...)}
 * 需要 {@code RenderLayerParent})。若放在 {@code @Mod} 主類別裡,即使包在 {@code Dist.CLIENT} 守衛內,
 * dedicated server 載入主類別做 class 驗證時仍會連帶載入那些 client 型別 -> RuntimeDistCleaner 擋下 -> crash。
 * 抽到這裡後,主類別位元組不含任何 client 型別,server 只會載入本類別的 header(不會驗證方法體),不再 crash。
 */
public final class ClientModBusSetup {

    private ClientModBusSetup() {}

    public static void init(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(ClientInteractiveFormationManager.class);
        modEventBus.addListener(ModParticles::registerProviders);
        modEventBus.addListener(ModKeyMappings::onRegisterKeyMappings);
        modEventBus.addListener(ModColorHandlers::onRegisterItemColors);

        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(ModEntities.FLOATING_TURRET.get(), FloatingTurretRenderer::new);
            e.registerEntityRenderer(ModEntities.FLOATING_TURRET_PROJECTILE.get(), FloatingTurretProjectileRenderer::new);
            e.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(), NoopRenderer::new);
            e.registerEntityRenderer(ModEntities.ORBITAL_ORB.get(), NoopRenderer::new);
            e.registerEntityRenderer(ModEntities.SPACE_CRACK.get(), SpaceCrackRenderer::new);
            e.registerEntityRenderer(ModEntities.PLAYER_CLONE.get(), PlayerCloneRenderer::new);
            e.registerEntityRenderer(ModEntities.NARA_PHANTOM.get(), NaraPhantomRenderer::new);
            e.registerEntityRenderer(ModEntities.TRAINING_DUMMY.get(), TrainingDummyRenderer::new);
            e.registerEntityRenderer(ModEntities.SHIP.get(), ShipEntityRenderer::new);
        });
        modEventBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> {
            e.registerLayerDefinition(FloatingTurretModel.LAYER_LOCATION, FloatingTurretModel::createBodyLayer);
            e.registerLayerDefinition(TrainingDummyModel.LAYER_LOCATION, TrainingDummyModel::createBodyLayer);
            e.registerLayerDefinition(ManaAlloyArmorModel.LAYER, ManaAlloyArmorModel::createLayer);
        });
        modEventBus.addListener((EntityRenderersEvent.AddLayers e) -> {
            for (PlayerSkin.Model skin : e.getSkins()) {
                if (e.getSkin(skin) instanceof PlayerRenderer pr) {
                    pr.addLayer(new ManaAlloyArmorLayer<>(pr, e.getEntityModels()));
                }
            }
        });
        modEventBus.addListener((ModelEvent.RegisterAdditional e) ->
                e.register(FloatingTurretBEWLR.GEO_MODEL_LOCATION));
        modEventBus.addListener((RegisterClientReloadListenersEvent e) ->
                e.registerReloadListener((ResourceManagerReloadListener) rm -> {
                    ManaStrikeShaderRenderer.reload();
                    AltarShockwaveRenderer.reload();
                    AltarMagicCircleRenderer.reload();
                    AltarT6ClimaxRenderer.reload();
                    AltarT45OrbRenderer.reload();
                    AltarFadeRenderer.reload();
                    AltarExplosionRenderer.reload();
                    TurretHitEffectRenderer.reload();
                    SpaceSkyRenderer.reload();
                    SpacePlanetManager.reload();
                }));
        // ClientEffectEvents is auto-registered by @EventBusSubscriber; no manual register needed.
    }
}
