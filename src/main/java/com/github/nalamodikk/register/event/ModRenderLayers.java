package com.github.nalamodikk.register.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.altar.AltarPillarRenderer;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarRenderer;
import com.github.nalamodikk.common.block.blockentity.altar.AspectPedestalRenderer;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableRenderer;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarCollectorRenderer;
import com.github.nalamodikk.common.block.blockentity.mana_generator.render.ManaGeneratorRenderer;
import com.github.nalamodikk.common.block.blockentity.mana_grinder.ManaGrinderRenderer;
import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)

public class ModRenderLayers {

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), SolarCollectorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_GRINDER_BE.get(), ManaGrinderRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ASPECT_ALTAR_BE.get(), AspectAltarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ASPECT_PEDESTAL_BE.get(), AspectPedestalRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ALTAR_PILLAR_BE.get(), AltarPillarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RESEARCH_TABLE_BE.get(), ResearchTableRenderer::new);
//        event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_CONDUIT_BE.get(), ArcaneConduitBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(new ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "block/resonance_ring"),
                "standalone"));
    }
}
