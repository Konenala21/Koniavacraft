package com.github.nalamodikk.barrages;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BarrageManager {
    private static final BarrageManager INSTANCE = new BarrageManager();
    private final List<Barrage> barrages = new CopyOnWriteArrayList<>();

    public static BarrageManager getInstance() {
        return INSTANCE;
    }

    public void spawn(Barrage barrage) {
        barrages.add(barrage);
        barrage.launch();
    }

    public void tick() {
        // 使用 CopyOnWriteArrayList 避免 ConcurrentModificationException
        for (Barrage barrage : barrages) {
            if (barrage.isValid()) {
                barrage.tick();
            } else {
                barrages.remove(barrage);
            }
        }
    }

    public List<Barrage> collectClipBarrages(ServerLevel world, AABB box) {
        List<Barrage> result = new ArrayList<>();
        for (Barrage b : barrages) {
            if (b.isValid() && b.world == world) {
                // 簡單的 AABB 碰撞檢測
                if (box.contains(b.loc) || box.intersects(b.hitBox.ofBox(b.loc))) {
                    result.add(b);
                }
            }
        }
        return result;
    }
}
