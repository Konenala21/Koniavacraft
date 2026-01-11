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
    
    // 動畫控制
    private com.github.nalamodikk.particle.utils.helper.ScaleHelper scaleHelper;
    private double currentScale = 1.0;
    private final List<Consumer<ParticleShapeStyle>> displayInvokes = new ArrayList<>();
    private final List<Consumer<ParticleShapeStyle>> preTickActions = new ArrayList<>();

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
        
        // 執行顯示時的初始化動作
        for (Consumer<ParticleShapeStyle> action : displayInvokes) {
            action.accept(this);
        }
        
        Map<StyleData, RelativeLocation> frames = getCurrentFrames();
        for (Map.Entry<StyleData, RelativeLocation> entry : frames.entrySet()) {
            RelativeLocation rel = entry.getValue();
            
            // 應用當前縮放
            RelativeLocation scaledRel = rel.multiplyClone(currentScale);
            
            Vec3 spawnPos = pos.add(scaledRel.toVector());
            
            Optional<ParticleController> controller = ParticleManager.getInstance()
                .spawnParticle(world, "koniava:coo_particle", spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
            
            controller.ifPresent(activeParticles::add);
        }
    }

    @Override
    public void tick() {
        if (isRemoved) return;
        
        // 執行 Pre-tick actions (例如動畫更新)
        for (Consumer<ParticleShapeStyle> action : preTickActions) {
            action.accept(this);
        }
        
        // 如果有 ScaleHelper，執行縮放
        if (scaleHelper != null) {
            scaleHelper.doScale();
        }
    }

    public void setScale(double scale) {
        this.currentScale = scale;
        // 如果已經顯示了，需要更新所有現存粒子的位置 (這裡簡化處理：先不即時更新位置，只影響新生成或下一幀)
        // 理想情況下應該遍歷 activeParticles 並重新計算相對位置
        // 但因為我們目前沒有保存每個粒子的原始相對位置 (只有 currentPosition)，所以縮放有點困難。
        // 不過如果我們只是每 tick 重新生成粒子 (像很多魔法陣特效一樣)，那只要改變 currentScale 就夠了。
        // 但我們的架構是生成一次粒子然後控制它。
        
        // TODO: 完整的縮放需要重新計算每個粒子的位置。
        // 這裡暫時只更新變數，留待 Phase 5 後續優化。
    }

    public ParticleShapeStyle loadScaleHelper(double min, double max, int ticks) {
        this.scaleHelper = new com.github.nalamodikk.particle.utils.helper.ScaleHelper(min, max, ticks);
        this.scaleHelper.setTarget(this);
        return this;
    }
    
    public ParticleShapeStyle toggleOnDisplay(Consumer<ParticleShapeStyle> action) {
        this.displayInvokes.add(action);
        return this;
    }
    
    public void addPreTickAction(Consumer<ParticleShapeStyle> action) {
        this.preTickActions.add(action);
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
