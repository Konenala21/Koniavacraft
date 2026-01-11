package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.commands.IParticleCommand;

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

    // UUID -> 粒子實例（弱引用）
    private final Map<UUID, ICooParticle> particles = new ConcurrentHashMap<>();

    private ParticleManager() {}

    public static ParticleManager getInstance() {
        return INSTANCE;
    }

    /**
     * 註冊粒子
     */
    public void registerParticle(UUID particleId, ICooParticle particle) {
        particles.put(particleId, particle);
        commandQueues.putIfAbsent(particleId, Collections.synchronizedList(new ArrayList<>()));
    }

    /**
     * 取消註冊粒子
     */
    public void unregisterParticle(UUID particleId) {
        particles.remove(particleId);
        commandQueues.remove(particleId);
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