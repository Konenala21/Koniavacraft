package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.commands.IParticleCommand;
import com.github.nalamodikk.particle.utils.PerformanceMonitor;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ???????? * ?滿?????賹???????????? */
public class ParticleManager {

    private static final ParticleManager INSTANCE = new ParticleManager();

    private final Map<UUID, List<IParticleCommand>> commandQueues = new ConcurrentHashMap<>();
    private final Map<UUID, ICooParticle> particles = new ConcurrentHashMap<>();
    private final Queue<UUID> particleOrder = new LinkedList<>();

    private ParticleManager() {}

    public static ParticleManager getInstance() {
        return INSTANCE;
    }

    public void registerParticle(UUID particleId, ICooParticle particle) {
        synchronized (particleOrder) {
            int limit = PerformanceMonitor.getInstance().getParticleLimit();
            while (particles.size() >= limit && !particleOrder.isEmpty()) {
                evictOldestParticle();
            }
            particles.put(particleId, particle);
            particleOrder.offer(particleId);
        }
        commandQueues.putIfAbsent(particleId, Collections.synchronizedList(new ArrayList<>()));
    }

    public void unregisterParticle(UUID particleId) {
        particles.remove(particleId);
        commandQueues.remove(particleId);
    }

    private void evictOldestParticle() {
        UUID oldestId = particleOrder.poll();
        if (oldestId != null) {
            ICooParticle p = particles.get(oldestId);
            if (p != null) {
                p.remove();
            }
        }
    }

    public void queueCommand(UUID particleId, IParticleCommand command) {
        List<IParticleCommand> queue = commandQueues.computeIfAbsent(
            particleId,
            id -> Collections.synchronizedList(new ArrayList<>())
        );
        queue.add(command);
    }

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

    public Optional<ICooParticle> getParticle(UUID particleId) {
        return Optional.ofNullable(particles.get(particleId));
    }

    public void cleanup() {
        Set<UUID> toRemove = new HashSet<>();
        for (UUID id : commandQueues.keySet()) {
            if (!particles.containsKey(id)) {
                toRemove.add(id);
            }
        }
        toRemove.forEach(commandQueues::remove);
    }
}