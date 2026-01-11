package com.github.nalamodikk.particle.style;

import com.github.nalamodikk.particle.ParticleController;
import com.github.nalamodikk.particle.ParticleManager;
import com.github.nalamodikk.particle.utils.builder.PointsBuilder;
import com.github.nalamodikk.particle.utils.math.RelativeLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 基於幾何形狀的粒子樣式
 */
public class ParticleShapeStyle implements ParticleStyle {
    private final UUID uuid = UUID.randomUUID();
    private final Map<PointsBuilder, Function<RelativeLocation, StyleData>> pointBuilders = new LinkedHashMap<>();
    private final List<ParticleController> activeParticles = new ArrayList<>();
    
    private Level world;
    private Vec3 basePos;
    private boolean isRemoved = false;

    @Override
    public UUID getUuid() {
        return uuid;
    }

    /**
     * 增加一個幾何構建器到樣式中
     */
    public ParticleShapeStyle appendBuilder(PointsBuilder builder, Function<RelativeLocation, StyleData> dataFactory) {
        this.pointBuilders.put(builder, dataFactory);
        return this;
    }

    @Override
    public Map<StyleData, RelativeLocation> getCurrentFrames() {
        Map<StyleData, RelativeLocation> frames = new HashMap<>();
        for (Map.Entry<PointsBuilder, Function<RelativeLocation, StyleData>> entry : pointBuilders.entrySet()) {
            List<RelativeLocation> points = entry.getKey().create();
            for (RelativeLocation loc : points) {
                frames.put(entry.getValue().apply(loc), loc);
            }
        }
        return frames;
    }

    @Override
    public void display(Level world, Vec3 pos) {
        this.world = world;
        this.basePos = pos;
        
        Map<StyleData, RelativeLocation> frames = getCurrentFrames();
        for (Map.Entry<StyleData, RelativeLocation> entry : frames.entrySet()) {
            RelativeLocation rel = entry.getValue();
            Vec3 spawnPos = pos.add(rel.toVector());
            
            // 使用現有的 ParticleManager 生成粒子
            // 這裡假設我們有一個方便的方法來獲取 Controller
            // 注意：這裡需要對齊 ModParticles 的註冊名
            // 我暫時用一個硬編碼的類型，Phase 5 會修正
            Optional<ParticleController> controller = ParticleManager.getInstance()
                .spawnParticle(world, "koniava:coo_particle", spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
            
            controller.ifPresent(activeParticles::add);
        }
    }

    @Override
    public void tick() {
        if (isRemoved) return;
        
        // 這裡可以實現旋轉等動態效果
        // 暫時保持靜態
    }

    @Override
    public void remove() {
        isRemoved = true;
        for (ParticleController controller : activeParticles) {
            if (controller.isAlive()) {
                controller.remove();
            }
        }
        activeParticles.clear();
    }
}
