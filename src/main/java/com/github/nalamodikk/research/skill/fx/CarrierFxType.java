package com.github.nalamodikk.research.skill.fx;

/**
 * 需要用 shader 的載體視覺種類。server 端 {@link CarrierFx} 觸發時把這個的 ordinal 塞進
 * CarrierFxPacket 送給 client,client 端對照表導到對應的 shader renderer。
 * 加新 shader 載體就在這加一項。
 */
public enum CarrierFxType {
    SHOCKWAVE // 坤:衝擊波環 -> ManaStrikeShaderRenderer 的擴散環
}
