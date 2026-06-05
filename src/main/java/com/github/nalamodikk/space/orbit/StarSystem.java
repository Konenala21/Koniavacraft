package com.github.nalamodikk.space.orbit;

import org.joml.Vector3f;
import java.util.List;

/**
 * 代表一個恆星系：一到多顆恆星 + 一組行星。
 * 單星：stars.size() == 1，StarDef.orbitRadius = 0
 * 雙星：stars.size() == 2，各自繞質心互轉
 * 三星：stars.size() == 3，依此類推
 */
public record StarSystem(
    String id,
    Vector3f worldPos,      // 系統質心在太空維度的方塊座標
    List<StarDef> stars,
    List<PlanetDef> planets,
    List<BeltDef> belts     // 小行星帶 / 柯伊伯帶（可空）
) {
    /** 相容建構子：無帶系統。 */
    public StarSystem(String id, Vector3f worldPos, List<StarDef> stars, List<PlanetDef> planets) {
        this(id, worldPos, stars, planets, List.of());
    }

    /** 計算這個星系裡所有恆星的合成光方向（從某點看過去最亮的方向）。 */
    public Vector3f combinedLightDir(double gameTick, Vector3f observerPos) {
        Vector3f combined = new Vector3f();
        for (StarDef star : stars) {
            Vector3f starPos = star.worldPositionAt(gameTick, worldPos);
            Vector3f toStar  = new Vector3f(starPos).sub(observerPos);
            float    dist    = toStar.length();
            if (dist < 0.1f) continue;
            // 亮度按 1/r^2 加權
            float weight = (star.radius() * star.radius()) / (dist * dist);
            combined.add(new Vector3f(toStar).normalize().mul(weight));
        }
        float len = combined.length();
        return len > 0.001f ? combined.div(len) : new Vector3f(1, 0, 0);
    }
}
