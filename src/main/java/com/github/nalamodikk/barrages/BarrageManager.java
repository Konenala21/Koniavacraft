package com.github.nalamodikk.barrages;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BarrageManager {
    private static final BarrageManager INSTANCE = new BarrageManager();
    private final List<Barrage> barrages = new CopyOnWriteArrayList<>();

    public static BarrageManager getInstance() {
        return INSTANCE;
    }

    public void spawn(Barrage barrage, net.minecraft.world.phys.Vec3 pos, net.minecraft.world.phys.Vec3 direction) {
        barrages.add(barrage);
        barrage.launch(pos, direction);
    }

    public void tick() {
        for (Barrage barrage : barrages) {
            if (!barrage.isRemoved()) {
                barrage.tick();
            } else {
                barrages.remove(barrage);
            }
        }
    }

    public List<Barrage> collectClipBarrages(net.minecraft.world.level.Level world, net.minecraft.world.phys.AABB box) {
        List<Barrage> result = new ArrayList<>();
        for (Barrage b : barrages) {
            if (!b.isRemoved()) {
                // 簡單的 AABB 碰撞檢測
                if (box.contains(b.getPos()) || box.intersects(b.hitBox.ofBox(b.getPos()))) {
                    result.add(b);
                }
            }
        }
        return result;
    }
}
