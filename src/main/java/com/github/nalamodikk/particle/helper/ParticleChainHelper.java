package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 粒子鏈助手
 * 提供粒子連接和鏈式效果（簡化版）
 */
public class ParticleChainHelper {

    /**
     * 在兩個粒子之間創建鏈接
     * @param particle1 粒子 1
     * @param particle2 粒子 2
     * @param level 世界
     * @param chainParticle 鏈接粒子類型
     * @param segments 鏈接段數
     */
    public static void createChain(ControlableParticle particle1, ControlableParticle particle2,
                                  ClientLevel level, ParticleOptions chainParticle, int segments) {
        Vec3 pos1 = particle1.getPosition();
        Vec3 pos2 = particle2.getPosition();

        for (int i = 1; i < segments; i++) {
            float progress = (float) i / segments;
            Vec3 pos = pos1.lerp(pos2, progress);
            level.addParticle(chainParticle, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    /**
     * 在多個粒子之間創建鏈接
     * @param particles 粒子列表
     * @param level 世界
     * @param chainParticle 鏈接粒子類型
     * @param segments 每段鏈接的段數
     */
    public static void createMultiChain(List<ControlableParticle> particles, ClientLevel level,
                                       ParticleOptions chainParticle, int segments) {
        for (int i = 0; i < particles.size() - 1; i++) {
            createChain(particles.get(i), particles.get(i + 1), level, chainParticle, segments);
        }
    }

    /**
     * 創建閉環鏈接
     * @param particles 粒子列表
     * @param level 世界
     * @param chainParticle 鏈接粒子類型
     * @param segments 每段鏈接的段數
     */
    public static void createClosedChain(List<ControlableParticle> particles, ClientLevel level,
                                        ParticleOptions chainParticle, int segments) {
        createMultiChain(particles, level, chainParticle, segments);

        // 連接最後一個和第一個
        if (particles.size() > 2) {
            createChain(particles.get(particles.size() - 1), particles.get(0), level, chainParticle, segments);
        }
    }
}
