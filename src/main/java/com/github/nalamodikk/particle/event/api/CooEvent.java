package com.github.nalamodikk.particle.event.api;

/**
 * 所有自定義事件都要繼承 CooEvent
 *
 * 示例:
 * <pre>{@code
 * public class ParticleSpawnEvent extends CooEvent {
 *     private final UUID particleId;
 *     private final Vec3 position;
 *
 *     public ParticleSpawnEvent(UUID particleId, Vec3 position) {
 *         this.particleId = particleId;
 *         this.position = position;
 *     }
 *
 *     public UUID getParticleId() { return particleId; }
 *     public Vec3 getPosition() { return position; }
 * }
 * }</pre>
 */
public abstract class CooEvent {
}
