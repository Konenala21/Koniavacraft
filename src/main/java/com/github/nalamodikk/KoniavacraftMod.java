package com.github.nalamodikk;

import com.github.nalamodikk.biome.BiomeTerrainRegistration;
import com.github.nalamodikk.client.renderer.ManaStrikeShaderRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarShockwaveRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarMagicCircleRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarT6ClimaxRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarT45OrbRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarFadeRenderer;
import com.github.nalamodikk.client.renderer.entity.FloatingTurretModel;
import com.github.nalamodikk.client.renderer.entity.FloatingTurretProjectileRenderer;
import com.github.nalamodikk.client.renderer.entity.FloatingTurretRenderer;
import com.github.nalamodikk.client.renderer.item.FloatingTurretBEWLR;
import com.github.nalamodikk.client.renderer.turret.TurretHitEffectRenderer;
import com.github.nalamodikk.register.ModEntities;
import com.github.nalamodikk.register.ModMobEffects;
import com.github.nalamodikk.common.config.ModClientConfig;
import com.github.nalamodikk.common.config.ModCommonConfig;
import com.github.nalamodikk.register.*;
import com.github.nalamodikk.register.ModStructurePieceTypes;
import com.github.nalamodikk.register.ModStructureTypes;
import com.github.nalamodikk.register.client.ModKeyMappings;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(KoniavacraftMod.MOD_ID)
public class KoniavacraftMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "koniava";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under
    // the "examplemod" namespace
    public static final boolean IS_PRODUCTION = FMLLoader.isProduction();
    public static final boolean IS_DEV = !IS_PRODUCTION;

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    // The constructor for the mod class is the first code that is run when your mod
    // is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and
    // pass them in automatically.
    public KoniavacraftMod(IEventBus modEventBus, ModContainer modContainer) {

        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC);

        LOGGER.debug("Koniavacraft mod constructor initialized.");
        // Register the commonSetup method for modloading

        // 🌟 註冊生物群落
        BiomeTerrainRegistration.registerAll();
        // 🌟 初始化生物群落世界生成

        ModItems.register(modEventBus);

        ModBlocks.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModDataAttachments.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeModTabs.register(modEventBus);
        ModStructureTypes.register(modEventBus);
        ModStructurePieceTypes.register(modEventBus);
        ModEntities.register(modEventBus);
        ModMobEffects.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ModKeyMappings::onRegisterKeyMappings);
            modEventBus.addListener(com.github.nalamodikk.register.client.ModColorHandlers::onRegisterItemColors);
            modEventBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
                    e.registerEntityRenderer(ModEntities.FLOATING_TURRET.get(), FloatingTurretRenderer::new);
                    e.registerEntityRenderer(ModEntities.FLOATING_TURRET_PROJECTILE.get(), FloatingTurretProjectileRenderer::new);
            });
            modEventBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) ->
                    e.registerLayerDefinition(FloatingTurretModel.LAYER_LOCATION, FloatingTurretModel::createBodyLayer));
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
                        TurretHitEffectRenderer.reload();
                    }));
            // ClientEffectEvents is auto-registered by @EventBusSubscriber; no manual register needed.
        }
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod)
        // to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in
        // this class, like onServerStarting() below.
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(BiomeTerrainRegistration.class);

        // Register the item to a creative tab

        // Register our mod's ModConfigSpec so that FML can create and load the config
        // file for us
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.debug("Running common setup.");
        KoniavacraftMod.LOGGER.info("Koniavacraft world generation initialized.");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("Server starting.");
    }

    // You can use EventBusSubscriber to automatically register all static methods
    // in the class annotated with @SubscribeEvent

}
