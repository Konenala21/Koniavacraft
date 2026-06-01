package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Datapack-driven aspect assignment: the highest-priority, explicit override for
 * which aspects an item/block carries. This is the scalable mod-compat surface:
 * the mod ships sensible defaults, and modpacks / addons / other mods drop their
 * own files to map their items, instead of the author hand-mapping thousands.
 *
 * <p>Format: {@code data/<namespace>/aspect_assignments/<any>.json}, a flat map of
 * item-or-block id to a list of aspect ids:
 * <pre>{@code
 * {
 *   "minecraft:diamond_sword": ["koniava:blade", "koniava:metal"],
 *   "create:cogwheel": ["koniava:mechanism"]
 * }
 * }</pre>
 * Later packs override earlier ones for the same id. Unknown aspect ids are skipped.
 * These win over tag / recipe / semantic / hash resolution and are deterministic
 * (same every world), so the seed only varies what is NOT explicitly assigned.
 */
@EventBusSubscriber
public class AspectOverrideLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AspectOverrideLoader.class);
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, List<Aspect>> OVERRIDES = new HashMap<>();

    public AspectOverrideLoader() {
        super(GSON, "aspect_assignments");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new AspectOverrideLoader());
    }

    /** Explicitly-assigned aspects for an id, or empty if none. */
    public static List<Aspect> get(ResourceLocation id) {
        return OVERRIDES.getOrDefault(id, List.of());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        OVERRIDES.clear();
        for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
            try {
                JsonObject root = GsonHelper.convertToJsonObject(file.getValue(), "aspect_assignments");
                for (Map.Entry<String, JsonElement> mapping : root.entrySet()) {
                    ResourceLocation itemId = ResourceLocation.parse(mapping.getKey());
                    List<Aspect> aspects = new ArrayList<>();
                    for (JsonElement el : GsonHelper.convertToJsonArray(mapping.getValue(), mapping.getKey())) {
                        Aspect a = ModAspects.get(ResourceLocation.parse(el.getAsString()));
                        if (a != null) aspects.add(a);
                        else LOGGER.warn("Aspect assignment '{}' references unknown aspect '{}'", itemId, el.getAsString());
                    }
                    if (!aspects.isEmpty()) OVERRIDES.put(itemId, List.copyOf(aspects)); // later packs override
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't parse aspect assignment file {}", file.getKey(), e);
            }
        }
        LOGGER.info("Loaded {} aspect overrides from {} datapack file(s).", OVERRIDES.size(), files.size());
    }
}
