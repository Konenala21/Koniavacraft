package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.orbit.BeltDef;
import com.github.nalamodikk.space.orbit.PlanetDef;
import com.github.nalamodikk.space.orbit.StarDef;
import com.github.nalamodikk.space.orbit.StarSystem;
import com.github.nalamodikk.space.orbit.StarSystemRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 把 StarSystemRegistry.BUILTIN 匯出成 datapack JSON
 * （data/koniava/star_systems/*.json），供 StarSystemLoader 載入。
 *
 * <p>單一真實來源：在 Java 編寫 BUILTIN（型別安全），datagen 匯出 JSON（可擴充格式），
 * runtime 讀 JSON。玩家/模組包丟自己的 JSON 即可加星系。
 */
public class StarSystemProvider implements DataProvider {

    private final PackOutput.PathProvider path;

    public StarSystemProvider(PackOutput output) {
        this.path = output.createPathProvider(PackOutput.Target.DATA_PACK, "star_systems");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (StarSystem system : StarSystemRegistry.BUILTIN) {
            Path out = path.json(net.minecraft.resources.ResourceLocation
                    .fromNamespaceAndPath(KoniavacraftMod.MOD_ID, system.id()));
            futures.add(DataProvider.saveStable(cache, toJson(system), out));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject toJson(StarSystem s) {
        JsonObject o = new JsonObject();
        o.add("world_pos", vec(s.worldPos()));

        JsonArray stars = new JsonArray();
        for (StarDef st : s.stars()) {
            JsonObject j = new JsonObject();
            j.addProperty("id", st.id());
            j.add("color", vec(st.color()));
            j.addProperty("radius", st.radius());
            j.addProperty("orbit_radius", st.orbitRadius());
            j.addProperty("orbit_period_days", st.orbitPeriodDays());
            stars.add(j);
        }
        o.add("stars", stars);

        JsonArray planets = new JsonArray();
        for (PlanetDef p : s.planets()) {
            JsonObject j = new JsonObject();
            j.addProperty("id", p.id());
            j.addProperty("dimension", p.dimension().location().toString());
            j.addProperty("orbital_radius", p.orbitalRadius());
            j.addProperty("orbital_period_days", p.orbitalPeriodDays());
            j.addProperty("start_angle_deg", p.startAngleDeg());
            j.addProperty("physical_radius", p.physicalRadius());
            j.addProperty("shader_type", p.shaderType().name());
            j.add("color_a", vec(p.colorA()));
            j.add("color_b", vec(p.colorB()));
            j.add("heat_color", vec(p.heatColor()));
            j.addProperty("heat_amount", p.heatAmount());
            j.addProperty("atmo_density", p.atmoDensity());
            j.addProperty("atmo_height", p.atmoHeight());
            j.addProperty("self_rotation_days", p.selfRotationDays());
            j.addProperty("parent_id", p.parentId());
            j.addProperty("ring_inner", p.ringInner());
            j.addProperty("ring_outer", p.ringOuter());
            j.addProperty("ring_tilt_deg", p.ringTiltDeg());
            planets.add(j);
        }
        o.add("planets", planets);

        JsonArray belts = new JsonArray();
        for (BeltDef b : s.belts()) {
            JsonObject j = new JsonObject();
            j.addProperty("id", b.id());
            j.addProperty("inner_radius", b.innerRadius());
            j.addProperty("outer_radius", b.outerRadius());
            j.addProperty("thickness", b.thickness());
            j.addProperty("density", b.density());
            j.add("color", vec(b.color()));
            belts.add(j);
        }
        o.add("belts", belts);

        return o;
    }

    private static JsonArray vec(Vector3f v) {
        JsonArray a = new JsonArray();
        a.add(v.x); a.add(v.y); a.add(v.z);
        return a;
    }

    @Override
    public String getName() { return "Koniava Star Systems"; }
}
