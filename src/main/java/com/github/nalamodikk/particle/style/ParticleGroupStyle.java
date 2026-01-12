package com.github.nalamodikk.particle.style;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.control.Controlable;
import com.github.nalamodikk.particle.network.ServerControler;
import com.github.nalamodikk.particle.network.buffer.ParticleControlerDataBuffer;
import com.github.nalamodikk.particle.utils.Math3DUtil;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 粒子組樣式
 * 客戶端渲染和服務端處理都用這個類
 */
public abstract class ParticleGroupStyle implements ParticleStyle, Controlable<ParticleGroupStyle>, ServerControler<ParticleGroupStyle> {
    protected final UUID uuid;
    protected Level world;
    protected Vec3 pos = Vec3.ZERO;
    protected boolean isClient = false;
    protected double rotate = 0.0;
    protected RelativeLocation axis = RelativeLocation.yAxis();
    protected double scale = 1.0;

    /** 可見範圍（用於網絡同步優化） */
    protected double visibleRange = 32.0;

    /** 上一次更新的遊戲時間（用於解決回放問題） */
    protected long lastUpdatedGameTime = 0L;

    /** 生成粒子樣式的時間 */
    protected long displayedTime = 0L;

    /** 自動發包同步到客戶端（可能占用大量帶寬） */
    protected boolean autoToggle = false;

    protected boolean displayed = false;
    protected boolean valid = true;

    /** Pre-tick 動作隊列 */
    protected final List<Consumer<ParticleGroupStyle>> invokeQueue = new ArrayList<>();

    protected final Map<UUID, Controlable<?>> particles = new ConcurrentHashMap<>();
    protected final Map<Controlable<?>, RelativeLocation> particleLocations = new ConcurrentHashMap<>();

    /** 粒子數據緩存（用於粒子死亡時重新生成） */
    protected final Map<UUID, StyleData> particleDataBuffers = new ConcurrentHashMap<>();

    /** 粒子組初始化時，存儲 1 倍縮放時粒子與原點的距離 */
    protected final Map<UUID, Double> particleDefaultLength = new ConcurrentHashMap<>();

    public ParticleGroupStyle(UUID uuid, double visibleRange) {
        this.uuid = uuid;
        this.visibleRange = visibleRange;
    }

    public ParticleGroupStyle(UUID uuid) {
        this(uuid, 32.0);
    }

    public ParticleGroupStyle() {
        this(UUID.randomUUID(), 32.0);
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    // ========== 抽象方法（子類必須實現） ==========

    /**
     * 獲取當前幀的粒子數據
     * @return 粒子數據和相對位置的映射
     */
    public abstract Map<StyleData, RelativeLocation> getCurrentFrames();

    /**
     * 當樣式被顯示時調用
     */
    public abstract void onDisplay();

    /**
     * 服務器同步到客戶端時，寫入需要同步的參數
     * 基類參數（pos, world, rotate, axis, scale, uuid）無需同步
     * @return 需要同步的參數映射
     */
    public abstract Map<String, ParticleControlerDataBuffer<?>> writePacketArgsMap();

    /**
     * 客戶端接收服務器同步時，讀取參數
     * 基類參數會自動處理，只需處理自定義參數
     * @param args 接收到的參數映射
     */
    public abstract void readPacketArgs(Map<String, ParticleControlerDataBuffer<?>> args);

    // ========== 生命週期方法 ==========

    /**
     * 顯示粒子組
     * @param world 世界
     * @param pos 位置
     */
    @Override
    public void display(Level world, Vec3 pos) {
        if (displayed) {
            return;
        }
        displayed = true;
        this.displayedTime = world.getGameTime();
        this.lastUpdatedGameTime = world.getGameTime();
        this.pos = pos;
        this.world = world;
        this.isClient = world.isClientSide();

        if (!isClient) {
            // 服務器只負責數據同步，不負責粒子生成
            onDisplay();
            return;
        }

        flush();
        onDisplay();
    }

    /**
     * 刷新粒子（清除並重新生成）
     */
    public void flush() {
        if (!particles.isEmpty()) {
            clear(true);
        }
        displayParticles();
    }

    /**
     * Tick 更新
     */
    public void tick() {
        if (!displayed || !valid) {
            clear(false);
            return;
        }

        long current = world.getGameTime();

        // 執行 pre-tick 動作
        for (Consumer<ParticleGroupStyle> action : invokeQueue) {
            action.accept(this);
        }

        // 更新所有粒子
        for (Controlable<?> controlable : particles.values()) {
            // TODO: 支持 ControlableParticleGroup 和嵌套 ParticleGroupStyle
            // if (controlable instanceof ControlableParticleGroup group) {
            //     group.tick();
            // } else if (controlable instanceof ParticleGroupStyle style) {
            //     style.tick();
            // }
        }

        this.lastUpdatedGameTime = current;
    }

    /**
     * 在顯示粒子前調用（可被子類覆蓋）
     * @param styles 粒子數據映射
     */
    protected void beforeDisplay(Map<StyleData, RelativeLocation> styles) {
        // 空實現，子類可覆蓋
    }

    /**
     * 顯示所有粒子
     */
    private void displayParticles() {
        Map<StyleData, RelativeLocation> locations = getCurrentFrames();
        beforeDisplay(locations);
        toggleScale(locations);

        // TODO: 實現粒子生成邏輯
        // 需要 ParticleDisplayer 和相關系統
        KoniavacraftMod.LOGGER.warn("displayParticles() not yet fully implemented - need ParticleDisplayer system");
    }

    /**
     * 清除所有粒子
     * @param keepData 是否保留數據（用於重新生成）
     */
    protected void clear(boolean keepData) {
        particles.values().forEach(Controlable::remove);
        particles.clear();
        particleLocations.clear();

        if (!keepData) {
            particleDataBuffers.clear();
            particleDefaultLength.clear();
        }
    }

    // ========== Pre-tick 動作 ==========

    /**
     * 添加 pre-tick 動作
     * @param action 要執行的動作
     */
    public void addPreTickAction(Consumer<ParticleGroupStyle> action) {
        invokeQueue.add(action);
    }

    // ========== 變換方法 ==========

    @Override
    public void rotateToPoint(RelativeLocation to) {
        Math3DUtil.rotatePointsToPoint(
            new ArrayList<>(particleLocations.values()), to, axis
        );
        this.axis = to;
        toggleRelative();

        if (!isClient) {
            // TODO: 同步到其他客戶端
            // change(Map.of("rotate_to", ParticleControlerDataBuffers.relative(to)));
        }
    }

    /**
     * 旋轉到指定方向並額外旋轉指定角度
     * @param to 目標方向
     * @param angle 額外旋轉角度（弧度）
     */
    public void rotateToWithAngle(RelativeLocation to, double angle) {
        Math3DUtil.rotateAsAxis(
            new ArrayList<>(particleLocations.values()), axis, angle
        );
        Math3DUtil.rotatePointsToPoint(
            new ArrayList<>(particleLocations.values()), to, axis
        );
        this.axis = to;
        this.rotate += angle;
        if (this.rotate >= 2 * Math.PI) {
            this.rotate -= 2 * Math.PI;
        }
        toggleRelative();

        if (!isClient) {
            // TODO: 同步到其他客戶端
        }
    }

    @Override
    public void rotateAsAxis(double radian) {
        Math3DUtil.rotateAsAxis(
            new ArrayList<>(particleLocations.values()), axis, radian
        );
        this.rotate += radian;
        if (this.rotate >= 2 * Math.PI) {
            this.rotate -= 2 * Math.PI;
        }
        toggleRelative();

        if (!isClient) {
            // TODO: 同步到其他客戶端
        }
    }

    @Override
    public void teleportTo(Vec3 pos) {
        this.pos = pos;
        toggleRelative();

        if (!isClient) {
            // TODO: 同步到其他客戶端
        }
    }

    @Override
    public void teleportTo(double x, double y, double z) {
        teleportTo(new Vec3(x, y, z));
    }

    /**
     * 更新所有粒子的相對位置
     */
    protected void toggleRelative() {
        if (!isClient) return;

        particleLocations.forEach((control, rel) -> {
            control.teleportTo(pos.add(rel.toVector()));
        });
    }

    // ========== 縮放方法 ==========

    /**
     * 設置縮放比例
     * @param newScale 新的縮放比例
     */
    public void scale(double newScale) {
        if (newScale < 0.0) {
            KoniavacraftMod.LOGGER.error("scale cannot be less than zero");
            return;
        }
        this.scale = newScale;

        if (displayed) {
            toggleScaleDisplayed();
        }

        if (!valid) {
            return;
        }

        if (!isClient) {
            // TODO: 同步到其他客戶端
        }
    }

    /**
     * 應用縮放到所有位置
     * @param locations 位置映射
     */
    protected void toggleScale(Map<StyleData, RelativeLocation> locations) {
        if (!valid) {
            return;
        }

        if (particleDefaultLength.isEmpty()) {
            for (Map.Entry<StyleData, RelativeLocation> entry : locations.entrySet()) {
                particleDefaultLength.put(entry.getKey().getUuid(), entry.getValue().length());
            }
        }

        for (Map.Entry<StyleData, RelativeLocation> entry : locations.entrySet()) {
            Double len = particleDefaultLength.get(entry.getKey().getUuid());
            if (len != null && len > 0.0) {
                entry.getValue().multiply(len * scale / entry.getValue().length());
            }
        }
    }

    /**
     * 當已顯示時更新縮放
     */
    protected void toggleScaleDisplayed() {
        particleLocations.forEach((control, rel) -> {
            UUID uuid = control.controlUUID();
            Double len = particleDefaultLength.get(uuid);
            if (len == null || Math.abs(len) < 1e-3) {
                return;
            }

            rel.multiply(len * scale / rel.length());
        });
        toggleRelative();
    }

    // ========== 網絡同步方法 ==========

    /**
     * 服務器端變更通知（TODO: 需要網絡包系統）
     * @param toggleMethod 變更方法
     * @param args 同步參數
     */
    public void change(Consumer<ParticleGroupStyle> toggleMethod, Map<String, ParticleControlerDataBuffer<?>> args) {
        if (isClient) {
            return;
        }

        toggleMethod.accept(this);

        // TODO: 發包到所有可見玩家
        KoniavacraftMod.LOGGER.warn("Network sync not yet implemented");
    }

    /**
     * 服務器端變更通知（僅發包）
     * @param args 同步參數
     */
    public void change(Map<String, ParticleControlerDataBuffer<?>> args) {
        change(style -> {}, args);
    }

    // ========== Controlable 接口實現 ==========

    @Override
    public void remove() {
        clear(false);
        valid = false;

        if (!isClient) {
            // TODO: 同步移除到客戶端
        }
    }

    @Override
    public void spawn(Level world, Vec3 pos) {
        display(world, pos);
    }

    @Override
    public UUID controlUUID() {
        return uuid;
    }

    @Override
    public ParticleGroupStyle getControlObject() {
        return this;
    }

    // ========== ServerControler 接口實現 ==========

    @Override
    public ParticleGroupStyle getValue() {
        return this;
    }

    // ========== Getter/Setter ==========

    public double getVisibleRange() {
        return visibleRange;
    }

    public void setVisibleRange(double visibleRange) {
        this.visibleRange = visibleRange;
    }

    public boolean isAutoToggle() {
        return autoToggle;
    }

    public void setAutoToggle(boolean autoToggle) {
        this.autoToggle = autoToggle;
    }

    public boolean isDisplayed() {
        return displayed;
    }

    public boolean isValid() {
        return valid;
    }

    public Level getWorld() {
        return world;
    }

    public Vec3 getPos() {
        return pos;
    }

    public double getScale() {
        return scale;
    }

    public RelativeLocation getAxis() {
        return axis;
    }

    public double getRotate() {
        return rotate;
    }
}
