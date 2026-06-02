package com.github.nalamodikk;

import com.github.nalamodikk.biome.BiomeTerrainRegistration;
import com.github.nalamodikk.common.particle.FormationEffectManager;
import com.github.nalamodikk.client.event.ClientModBusSetup;
import com.github.nalamodikk.dimension.BoundedFlatChunkGenerator;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.register.ModEntities;
import com.github.nalamodikk.register.ModMobEffects;
import com.github.nalamodikk.register.ModVillagers;
import com.github.nalamodikk.register.ModSounds;
import com.github.nalamodikk.common.config.ModClientConfig;
import com.github.nalamodikk.common.config.ModCommonConfig;
import com.github.nalamodikk.register.*;
import com.github.nalamodikk.register.ModStructurePieceTypes;
import com.github.nalamodikk.register.ModStructureTypes;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
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
        ModArmorMaterials.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModSounds.register(modEventBus);
        ModParticles.register(modEventBus);
        ModChunkGenerators.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // 所有 client 端 mod-bus 註冊抽到 ClientModBusSetup(client-only 類別)。
            // 不可內聯在這:主類別含 client 型別位元組會讓 dedicated server 載入時 crash。見該類 javadoc。
            ClientModBusSetup.init(modEventBus);
        }
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod)
        // to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in
        // this class, like onServerStarting() below.
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(BiomeTerrainRegistration.class);
        NeoForge.EVENT_BUS.register(FormationEffectManager.class);

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

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        // 鏡中世界邊界已取消：未來規劃改為多 boss 共用維度，由各 boss 自行管理活動範圍。
        // 若舊存檔殘留 501 大小的 WorldBorder，重設回 vanilla 預設（59999968 ≈ 6e7）。
        // 地形仍由 BoundedFlatChunkGenerator 限制中央 501×501 平坦範圍，外圍是虛空。
        if (event.getLevel() instanceof ServerLevel serverLevel
                && serverLevel.dimension().equals(ModDimensions.VOID_MIRROR)) {
            WorldBorder border = serverLevel.getWorldBorder();
            if (border.getSize() < 1_000_000) {
                border.setCenter(0, 0);
                border.setSize(WorldBorder.MAX_SIZE);
            }
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods
    // in the class annotated with @SubscribeEvent

}
