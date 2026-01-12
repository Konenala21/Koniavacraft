package com.github.nalamodikk.particle.effects;

import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ?堆撓????殉???謚祆???
 *
 * ???????斗??????????
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
     * ???啾???????
     */
    public void createMagicCircle(BlockPos pos, ClientMagicCircleEffect.Config config) {
        ClientMagicCircleEffect effect = new ClientMagicCircleEffect(pos, config);
        if (effect.isActive()) {
            activeEffects.add(effect);
        }
    }

    /**
     * ??tick ?皝????????
     */
    public void tick() {
        Iterator<ClientMagicCircleEffect> iterator = activeEffects.iterator();
        while (iterator.hasNext()) {
            ClientMagicCircleEffect effect = iterator.next();
            effect.tick();

            // ??謒????賹????
            if (!effect.isActive()) {
                iterator.remove();
            }
        }
    }

    /**
     * ?敺???????
     */
    public void clear() {
        for (ClientMagicCircleEffect effect : activeEffects) {
            effect.stop();
        }
        activeEffects.clear();
    }

    /**
     * ???????????鞈?
     */
    public int getActiveEffectCount() {
        return activeEffects.size();
    }
}
