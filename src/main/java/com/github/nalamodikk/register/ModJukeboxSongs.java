package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;

public class ModJukeboxSongs {

    public static final ResourceKey<JukeboxSong> QUANTIFIED_MANA_A   = key("quantified_mana_a");
    public static final ResourceKey<JukeboxSong> QUANTIFIED_MANA_B   = key("quantified_mana_b");
    public static final ResourceKey<JukeboxSong> MEMORY_TWO_SIDES_A  = key("memory_two_sides_a");
    public static final ResourceKey<JukeboxSong> MEMORY_TWO_SIDES_B  = key("memory_two_sides_b");
    public static final ResourceKey<JukeboxSong> KNOWLEDGE_SHORTCUT_A = key("knowledge_shortcut_a");
    public static final ResourceKey<JukeboxSong> KNOWLEDGE_SHORTCUT_B = key("knowledge_shortcut_b");
    public static final ResourceKey<JukeboxSong> U_KEY_CORE_A        = key("u_key_core_a");
    public static final ResourceKey<JukeboxSong> U_KEY_CORE_B        = key("u_key_core_b");

    private static ResourceKey<JukeboxSong> key(String id) {
        return ResourceKey.create(Registries.JUKEBOX_SONG,
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, id));
    }
}
