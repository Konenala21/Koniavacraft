package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.research.skill.fx.CarrierFxType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * client 端載體 shader 派發:收到 {@code CarrierFxPacket} 後依載體種類觸發對應的 shader renderer。
 * 加新 shader 載體就在 switch 加一項;gameplay/server 完全不必碰。
 */
public final class CarrierFxClient {

    private CarrierFxClient() {}

    public static void play(int typeId, Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (typeId < 0 || typeId >= CarrierFxType.values().length) return;
        long tick = mc.level.getGameTime();
        switch (CarrierFxType.values()[typeId]) {
            case SHOCKWAVE -> ManaStrikeShaderRenderer.spawnEffect(pos, tick); // 內建擴散環當衝擊波
        }
    }
}
