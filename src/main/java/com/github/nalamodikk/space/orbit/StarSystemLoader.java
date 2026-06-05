package com.github.nalamodikk.space.orbit;

import com.github.nalamodikk.client.renderer.dimension.PlanetRenderer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Datapack 驅動的星系定義：讀 {@code data/<ns>/star_systems/<name>.json}。
 * 載入後填進 StarSystemRegistry，太空渲染就用 JSON 定義的星系。
 * 沒有任何 JSON 時用 StarSystemRegistry.BUILTIN（內建太陽系）當 fallback。
 *
 * <p>這是「可擴充地基」：模組包/玩家丟 JSON 就能加星系，不用碰 Java。
 *
 * <p>JSON 格式（欄位對應 StarSystem/StarDef/PlanetDef/BeltDef）：
 * <pre>{@code
 * {
 *   "world_pos": [0, 64, 0],
 *   "stars": [
 *     { "id": "sun", "color": [1.0,0.95,0.72], "radius": 200, "orbit_radius": 0, "orbit_period_days": 0 }
 *   ],
 *   "planets": [
 *     { "id": "earth", "dimension": "koniava:moon", "orbital_radius": 1500,
 *       "orbital_period_days": 365, "start_angle_deg": 90, "physical_radius": 120,
 *       "shader_type": "ATMOSPHERE", "color_a": [0.18,0.42,0.68], "color_b": [0.35,0.62,1.0],
 *       "heat_color": [0,0,0], "heat_amount": 0, "atmo_density": 1.4, "atmo_height": 0.08,
 *       "self_rotation_days": 1.0, "parent_id": "", "ring_inner": 0, "ring_outer": 0, "ring_tilt_deg": 0 }
 *   ],
 *   "belts": [
 *     { "id": "asteroid_belt", "inner_radius": 3200, "outer_radius": 5400,
 *       "thickness": 180, "density": 0.93, "color": [0.45,0.40,0.34] }
 *   ]
 * }
 * }</pre>
 */
@EventBusSubscriber
public class StarSystemLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarSystemLoader.class);
    private static final Gson GSON = new Gson();

    public StarSystemLoader() {
        super(GSON, "star_systems");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new StarSystemLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        List<StarSystem> systems = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
            String id = file.getKey().getPath();
            try {
                systems.add(parseSystem(id, GsonHelper.convertToJsonObject(file.getValue(), "star_system")));
            } catch (Exception e) {
                LOGGER.error("Couldn't parse star system file {}", file.getKey(), e);
            }
        }
        StarSystemRegistry.setLoaded(systems);
        LOGGER.info("Loaded {} star system(s) from datapack ({} active).",
                systems.size(), StarSystemRegistry.getActive().size());
    }

    private static StarSystem parseSystem(String id, JsonObject o) {
        Vector3f worldPos = vec3(o, "world_pos", new Vector3f(0, 64, 0));

        List<StarDef> stars = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(o, "stars", new JsonArray())) {
            JsonObject s = el.getAsJsonObject();
            stars.add(new StarDef(
                GsonHelper.getAsString(s, "id"),
                vec3(s, "color", new Vector3f(1, 1, 1)),
                GsonHelper.getAsFloat(s, "radius", 100f),
                GsonHelper.getAsFloat(s, "orbit_radius", 0f),
                GsonHelper.getAsFloat(s, "orbit_period_days", 0f)
            ));
        }

        List<PlanetDef> planets = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(o, "planets", new JsonArray())) {
            JsonObject p = el.getAsJsonObject();
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.parse(GsonHelper.getAsString(p, "dimension", "koniava:space")));
            planets.add(new PlanetDef(
                GsonHelper.getAsString(p, "id"),
                dim,
                GsonHelper.getAsFloat(p, "orbital_radius"),
                GsonHelper.getAsFloat(p, "orbital_period_days"),
                GsonHelper.getAsFloat(p, "start_angle_deg", 0f),
                GsonHelper.getAsFloat(p, "physical_radius"),
                PlanetRenderer.Type.valueOf(GsonHelper.getAsString(p, "shader_type", "ROCKY")),
                vec3(p, "color_a", new Vector3f(0.6f, 0.6f, 0.6f)),
                vec3(p, "color_b", new Vector3f(0.3f, 0.3f, 0.3f)),
                vec3(p, "heat_color", new Vector3f(0, 0, 0)),
                GsonHelper.getAsFloat(p, "heat_amount", 0f),
                GsonHelper.getAsFloat(p, "atmo_density", 0f),
                GsonHelper.getAsFloat(p, "atmo_height", 0f),
                GsonHelper.getAsFloat(p, "self_rotation_days", 1f),
                GsonHelper.getAsString(p, "parent_id", ""),
                GsonHelper.getAsFloat(p, "ring_inner", 0f),
                GsonHelper.getAsFloat(p, "ring_outer", 0f),
                GsonHelper.getAsFloat(p, "ring_tilt_deg", 0f)
            ));
        }

        List<BeltDef> belts = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(o, "belts", new JsonArray())) {
            JsonObject b = el.getAsJsonObject();
            belts.add(new BeltDef(
                GsonHelper.getAsString(b, "id"),
                GsonHelper.getAsFloat(b, "inner_radius"),
                GsonHelper.getAsFloat(b, "outer_radius"),
                GsonHelper.getAsFloat(b, "thickness", 150f),
                GsonHelper.getAsFloat(b, "density", 0.93f),
                vec3(b, "color", new Vector3f(0.45f, 0.40f, 0.34f))
            ));
        }

        return new StarSystem(id, worldPos, stars, planets, belts);
    }

    private static Vector3f vec3(JsonObject o, String key, Vector3f def) {
        if (!o.has(key)) return def;
        JsonArray a = GsonHelper.convertToJsonArray(o.get(key), key);
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }
}
