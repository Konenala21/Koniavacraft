package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, KoniavacraftMod.MOD_ID);

    private static final String[] LOCALES = {"zh_tw", "en"};
    private static final String[][] DIALOGUE_KEYS = {
            {"first_login", "line1"}, {"first_login", "line2"}, {"first_login", "line3"},
            {"first_login", "line4"}, {"first_login", "line5"}, {"first_login", "line6"},
            {"first_login", "line7"}, {"first_login", "line8"},
            {"research_table", "line1"}, {"research_table", "line2"},
            {"research_table", "line3"}, {"research_table", "line4"},
            {"punishment", "line1"}, {"punishment", "line2"}, {"punishment", "forgiven"},
            {"angry", "line1"},
            {"altar_t6", "line1"}, {"altar_t6", "line2"},
            {"first_scan", "line1"}, {"first_scan", "line2"},
            {"first_watch_open", "line1"}, {"first_watch_open", "line2"}, {"first_watch_open", "line3"},
            {"mana_gen_craft", "line1"}, {"mana_gen_craft", "line2"},
            {"mana_gen_placed", "line1"}, {"mana_gen_placed", "line2"}, {"mana_gen_placed", "line3"},
            {"wand_rod", "no_items_line1"}, {"wand_rod", "no_items_line2"},
            {"wand_rod", "ready_line1"}, {"wand_rod", "got_core_line1"},
            {"first_research", "line1"}, {"first_research", "line2"},
            {"first_altar_formed", "line1"}, {"first_altar_formed", "line2"},
            {"mana_grinder", "line1"}, {"mana_grinder", "line2"}, {"mana_grinder", "line3"},
            {"mana_infuser", "line1"}, {"mana_infuser", "line2"}, {"mana_infuser", "line3"},
            {"mana_crafting", "line1"}, {"mana_crafting", "line2"}, {"mana_crafting", "line3"},
            {"aspect_synthesis", "line1"}, {"aspect_synthesis", "line2"}, {"aspect_synthesis", "line3"},
    };

    // key: "zh_tw.first_login.line1" → DeferredHolder
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> NARA =
            new HashMap<>();

    static {
        for (String locale : LOCALES) {
            for (String[] parts : DIALOGUE_KEYS) {
                String group = parts[0];
                String line  = parts[1];
                String mapKey = locale + "." + group + "." + line;
                String eventPath = "nara." + locale + "." + group + "." + line;
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, eventPath);
                NARA.put(mapKey, SOUND_EVENTS.register(eventPath,
                        () -> SoundEvent.createVariableRangeEvent(loc)));
            }
        }
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
