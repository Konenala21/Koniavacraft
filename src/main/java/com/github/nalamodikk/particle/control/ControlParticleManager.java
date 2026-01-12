package com.github.nalamodikk.particle.control;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ?堆撓????殉?????
 */
public class ControlParticleManager {
    private static final Map<UUID, ParticleControler> controls = new ConcurrentHashMap<>();

    public static ParticleControler getControl(UUID uuid) {
        return controls.get(uuid);
    }

    public static void removeControl(UUID uuid) {
        controls.remove(uuid);
    }

    public static ParticleControler createControl(UUID uuid) {
        ParticleControler controler = new ParticleControler(uuid);
        controls.put(uuid, controler);
        return controler;
    }
}
