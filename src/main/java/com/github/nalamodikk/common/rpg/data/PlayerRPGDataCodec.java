package com.github.nalamodikk.common.rpg.data;

import com.github.nalamodikk.common.rpg.player.PlayerClass;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 💾 PlayerRPGData Codec 序列化器
 *
 * 用於 NeoForge Attachment 系統
 */
public class PlayerRPGDataCodec {

    public static final Codec<PlayerRPGData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            // 等級系統
            Codec.INT.fieldOf("level").forGetter(PlayerRPGData::getLevel),
            Codec.INT.fieldOf("experience").forGetter(PlayerRPGData::getExperience),
            Codec.INT.fieldOf("experienceToNextLevel").forGetter(PlayerRPGData::getExperienceToNextLevel),

            // 職業
            Codec.STRING.fieldOf("playerClass").forGetter(data -> data.getPlayerClass().getId()),

            // 屬性
            Codec.INT.fieldOf("strength").forGetter(data -> data.getAttributes().getStrength()),
            Codec.INT.fieldOf("intelligence").forGetter(data -> data.getAttributes().getIntelligence()),
            Codec.INT.fieldOf("agility").forGetter(data -> data.getAttributes().getAgility()),
            Codec.INT.fieldOf("vitality").forGetter(data -> data.getAttributes().getVitality()),
            Codec.INT.fieldOf("perception").forGetter(data -> data.getAttributes().getPerception()),

            // 未分配屬性點
            Codec.INT.fieldOf("unspentAttributePoints").forGetter(PlayerRPGData::getUnspentAttributePoints)
        ).apply(instance, PlayerRPGDataCodec::createFromCodec)
    );

    /**
     * 從 Codec 數據創建 PlayerRPGData
     */
    private static PlayerRPGData createFromCodec(
            int level, int experience, int experienceToNextLevel,
            String playerClassId,
            int strength, int intelligence, int agility, int vitality, int perception,
            int unspentAttributePoints
    ) {
        PlayerRPGData data = new PlayerRPGData();

        // 等級系統
        data.setLevel(level);
        data.setExperience(experience);
        data.setExperienceToNextLevel(experienceToNextLevel);

        // 職業
        data.setPlayerClass(PlayerClass.fromId(playerClassId));

        // 屬性
        data.getAttributes().setStrength(strength);
        data.getAttributes().setIntelligence(intelligence);
        data.getAttributes().setAgility(agility);
        data.getAttributes().setVitality(vitality);
        data.getAttributes().setPerception(perception);

        // 未分配屬性點
        data.setUnspentAttributePoints(unspentAttributePoints);

        return data;
    }
}
