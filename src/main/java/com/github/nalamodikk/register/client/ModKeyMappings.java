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

    public static final KeyMapping OPEN_UPGRADE_GUI = new KeyMapping(
        "key.koniava.open_upgrade_gui",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_U,
        CATEGORY_MOD
    );

    public static final KeyMapping NARA_SKIP = new KeyMapping(
        "key.koniava.nara_skip",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY_MOD
    );

    public static final KeyMapping DASH = new KeyMapping(
        "key.koniava.dash",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        CATEGORY_MOD
    );

    public static final KeyMapping CYCLE_SKILL = new KeyMapping(
        "key.koniava.cycle_skill",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        CATEGORY_MOD
    );

    public static final KeyMapping TOGGLE_HELMET = new KeyMapping(
        "key.koniava.toggle_helmet",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_N,
        CATEGORY_MOD
    );

    public static final KeyMapping TOGGLE_CHESTPLATE = new KeyMapping(
        "key.koniava.toggle_chestplate",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY_MOD
    );

    public static final KeyMapping TOGGLE_LEGGINGS = new KeyMapping(
        "key.koniava.toggle_leggings",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY_MOD
    );

    public static final KeyMapping TOGGLE_BOOTS = new KeyMapping(
        "key.koniava.toggle_boots",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY_MOD
    );

    private ModKeyMappings() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DEBUG_DETAILS);
        event.register(GHOST_LOCK);
        event.register(SKIP_ALTAR_ANIM);
        event.register(OPEN_UPGRADE_GUI);
        event.register(NARA_SKIP);
        event.register(DASH);
        event.register(CYCLE_SKILL);
        event.register(TOGGLE_HELMET);
        event.register(TOGGLE_CHESTPLATE);
        event.register(TOGGLE_LEGGINGS);
        event.register(TOGGLE_BOOTS);
    }
}
