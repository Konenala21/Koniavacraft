package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.commands.IParticleCommand;
import com.github.nalamodikk.particle.utils.PerformanceMonitor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 粒子全局管理器
 *
 * 負責維護粒子與控制器之間的通訊
 */
public class ParticleManager {

    private static final ParticleManager INSTANCE = new ParticleManager();

    // UUID -> 指令隊列
    private final Map<UUID, List<IParticleCommand>> commandQueues = new ConcurrentHashMap<>();

    // UUID -> 粒子實例
    private final Map<UUID, ICooParticle> particles = new ConcurrentHashMap<>();
    
    // 生成順序（用於驅逐）
    private final Queue<UUID> particleOrder = new LinkedList<>();

    private ParticleManager() {}

    public static ParticleManager getInstance() {
        return INSTANCE;
    }

    /**
     * 註冊粒子
     */
    public void registerParticle(UUID particleId, ICooParticle particle) {
        synchronized (particleOrder) {
            // 檢查負載限制
            int limit = PerformanceMonitor.getInstance().getParticleLimit();
            while (particles.size() >= limit) {
                evictOldestParticle();
            }
            
            particles.put(particleId, particle);
            particleOrder.offer(particleId);
        }
        commandQueues.putIfAbsent(particleId, Collections.synchronizedList(new ArrayList<>()));
    }

    /**
     * 取消註冊粒子
     */
    public void unregisterParticle(UUID particleId) {
        particles.remove(particleId);
        commandQueues.remove(particleId);
        // 注意：從 particleOrder 移除比較慢 (O(n))，我們可以在驅逐時檢查是否已存在
        // 或者使用 LinkedHashMap 來實現 LRU
    }
    
    private void evictOldestParticle() {
        UUID oldestId = particleOrder.poll();
        if (oldestId != null) {
            ICooParticle p = particles.get(oldestId);
            if (p != null) {
                p.remove(); // 這會觸發 unregisterParticle
            } else {
                // 如果已經移除了，遞歸嘗試下一個
                if (!particles.isEmpty()) {
                    evictOldestParticle();
                }
            }
        }
    }

    /**
     * 添加指令到隊列
     */
    public void queueCommand(UUID particleId, IParticleCommand command) {
        List<IParticleCommand> queue = commandQueues.computeIfAbsent(
            particleId,
            id -> Collections.synchronizedList(new ArrayList<>())
        );
        queue.add(command);
    }

    /**
     * 執行粒子的所有待處理指令
     */
    public void executeCommands(UUID particleId, ICooParticle particle) {
        List<IParticleCommand> queue = commandQueues.get(particleId);
        if (queue != null && !queue.isEmpty()) {
            synchronized (queue) {
                for (IParticleCommand command : queue) {
                    try {
                        command.execute(particle);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                queue.clear();
            }
        }
    }

    /**
     * 獲取粒子實例（如果存在）
     */
    public Optional<ICooParticle> getParticle(UUID particleId) {
        return Optional.ofNullable(particles.get(particleId));
    }

    /**
     * 生成並獲取粒子控制器
     */
    public Optional<ParticleController> spawnParticle(net.minecraft.world.level.Level level, String type, double x, double y, double z, double vx, double vy, double vz) {
        // 在伺服器端不生成實體粒子，僅作為邏輯佔位 (未來可整合網路同步)
        if (!level.isClientSide()) {
            return Optional.empty();
        }

        // 這裡需要根據 type 找到對應的 ParticleOptions
        // 為了展示 Phase 3 邏輯，我先實作對 coo_particle 的支持
        if (type.contains("coo_particle")) {
            UUID particleId = UUID.randomUUID();
            CooParticleOptions options = new CooParticleOptions(1.0f, 0xFFFFFF, 1.0f, particleId);
            level.addParticle(options, x, y, z, vx, vy, vz);
            
            // 現在我們可以精確地返回與粒子對應的控制器
            return Optional.of(new ParticleController(particleId));
        }
        return Optional.empty();
    }

    /**
     * 清理過期的粒子引用
     */
    public void cleanup() {
        // 移除已經不存在的粒子的指令隊列
        Set<UUID> toRemove = new HashSet<>();
        for (UUID id : commandQueues.keySet()) {
            if (!particles.containsKey(id)) {
                toRemove.add(id);
            }
        }
        toRemove.forEach(commandQueues::remove);
    }
}