package com.github.nalamodikk.register.client;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;

public class DeveloperModeConfigSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {

    public DeveloperModeConfigSectionScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
        super(parent, type, modConfig, title);
    }

    @Override
    protected Element createBooleanValue(
            String key,
            ModConfigSpec.ValueSpec spec,
            Supplier<Boolean> source,
            Consumer<Boolean> target
    ) {
        return new Element(
                getTranslationComponent(key),
                getTooltipComponent(key, null),
                new OptionInstance<>(
                        getTranslationKey(key),
                        getTooltip(key, null),
                        OptionInstance.BOOLEAN_TO_STRING,
                        Custom.BOOLEAN_VALUES_NO_PREFIX,
                        source.get(),
                        newValue -> {
                            boolean currentValue = source.get();
                            if (isDeveloperModeToggleKey(key) && !currentValue && newValue) {
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(new DeveloperModeConfirmScreen(this, confirmed -> {
                                        if (confirmed) {
                                            undoManager.add(v -> {
                                                target.accept(v);
                                                onChanged(key);
                                            }, true, v -> {
                                                target.accept(v);
                                                onChanged(key);
                                            }, source.get());
                                        }
                                        rebuild();
                                    }));
                                }
                                return;
                            }

                            undoManager.add(v -> {
                                target.accept(v);
                                onChanged(key);
                            }, newValue, v -> {
                                target.accept(v);
                                onChanged(key);
                            }, currentValue);
                        }
                )
        );
    }

    private static boolean isDeveloperModeToggleKey(String key) {
        return "developerModeEnabled".equals(key) || key.endsWith(".developerModeEnabled");
    }
}
