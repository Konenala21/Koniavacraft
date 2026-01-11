package com.github.nalamodikk.particle.effects;

import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 客戶端粒子效果管理器
 *
 * 管理所有活動的粒子效果
 */
@OnlyIn(Dist.CLIENT)
public class ClientEffectManager {

    private static final ClientEffectManager INSTANCE = new ClientEffectManager();

    private final List<ClientMagicCircleEffect> activeEffects = new ArrayList<>();

    private ClientEffectManager() {}

    public static ClientEffectManager getInstance() {
        return INSTANCE;
    }

    /**
     * 創建魔法陣效果
     */
    public void createMagicCircle(BlockPos pos, ClientMagicCircleEffect.Config config) {
        ClientMagicCircleEffect effect = new ClientMagicCircleEffect(pos, config);
        if (effect.isActive()) {
            activeEffects.add(effect);
        }
    }

    /**
     * 每 tick 更新所有效果
     */
    public void tick() {
        Iterator<ClientMagicCircleEffect> iterator = activeEffects.iterator();
        while (iterator.hasNext()) {
            ClientMagicCircleEffect effect = iterator.next();
            effect.tick();

            // 移除已結束的效果
            if (!effect.isActive()) {
                iterator.remove();
            }
        }
    }

    /**
     * 清空所有效果
     */
    public void clear() {
        for (ClientMagicCircleEffect effect : activeEffects) {
            effect.stop();
        }
        activeEffects.clear();
    }

    /**
     * 獲取活動效果數量
     */
    public int getActiveEffectCount() {
        return activeEffects.size();
    }
}
