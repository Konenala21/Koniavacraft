package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorScreen;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitConfigScreen;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingScreen;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorScreen;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserScreen;
import com.github.nalamodikk.common.block.blockentity.ore_grinder.OreGrinderScreen;
import com.github.nalamodikk.common.screen.block.shared.UniversalConfigScreen;
import com.github.nalamodikk.common.screen.block.shared.UpgradeScreen;
import com.github.nalamodikk.common.screen.player.ExtraEquipmentScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ModMenuScreens {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.MANA_CRAFTING_MENU.get(), ManaCraftingScreen::new);
        event.register(ModMenuTypes.MANA_GENERATOR_MENU.get(), ManaGeneratorScreen::new);
        event.register(ModMenuTypes.SOLAR_MANA_COLLECTOR_MENU.get(), SolarManaCollectorScreen::new);
        event.register(ModMenuTypes.UNIVERSAL_CONFIG_MENU.get(), UniversalConfigScreen::new);
        event.register(ModMenuTypes.UPGRADE_MENU.get(), UpgradeScreen::new);
        event.register(ModMenuTypes.EXTRA_EQUIPMENT_MENU.get(), ExtraEquipmentScreen::new);
        event.register(ModMenuTypes.CONDUIT_CONFIG_MENU.get(), ArcaneConduitConfigScreen::new);
        event.register(ModMenuTypes.MANA_INFUSER.get(), ManaInfuserScreen::new);
        event.register(ModMenuTypes.ORE_GRINDER_MENU.get(), OreGrinderScreen::new);

    }

}
