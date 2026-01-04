package com.github.nalamodikk.register.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.misc";
    public static final KeyMapping DEBUG_DETAILS = new KeyMapping(
        "key.koniava.debug_details",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_SHIFT,
        CATEGORY
    );

    private ModKeyMappings() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DEBUG_DETAILS);
    }
}
