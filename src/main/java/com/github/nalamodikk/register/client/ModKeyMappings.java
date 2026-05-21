package com.github.nalamodikk.register.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY     = "key.categories.misc";
    public static final String CATEGORY_MOD = "key.categories.koniava";

    public static final KeyMapping DEBUG_DETAILS = new KeyMapping(
        "key.koniava.debug_details",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_SHIFT,
        CATEGORY
    );

    public static final KeyMapping GHOST_LOCK = new KeyMapping(
        "key.koniava.ghost_lock",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        CATEGORY_MOD
    );

    public static final KeyMapping SKIP_ALTAR_ANIM = new KeyMapping(
        "key.koniava.skip_altar_anim",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY_MOD
    );

    private ModKeyMappings() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DEBUG_DETAILS);
        event.register(GHOST_LOCK);
        event.register(SKIP_ALTAR_ANIM);
    }
}
