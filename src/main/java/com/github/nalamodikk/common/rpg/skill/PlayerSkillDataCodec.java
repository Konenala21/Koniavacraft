package com.github.nalamodikk.common.rpg.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

/**
 * 💾 PlayerSkillData Codec 序列化器
 *
 * 用於 NeoForge Attachment 系統
 */
public class PlayerSkillDataCodec {

    public static final Codec<PlayerSkillData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            // 已學習技能列表
            Codec.list(Codec.STRING).fieldOf("learnedSkills").forGetter(data ->
                new ArrayList<>(data.getLearnedSkills())
            ),

            // 技能冷卻 (轉換為 List<Entry>)
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("cooldowns").forGetter(data -> {
                Map<String, Integer> cooldowns = new HashMap<>();
                for (String skillId : data.getLearnedSkills()) {
                    int cooldown = data.getSkillCooldown(skillId);
                    if (cooldown > 0) {
                        cooldowns.put(skillId, cooldown);
                    }
                }
                return cooldowns;
            })
        ).apply(instance, PlayerSkillDataCodec::createFromCodec)
    );

    /**
     * 從 Codec 數據創建 PlayerSkillData
     */
    private static PlayerSkillData createFromCodec(List<String> learnedSkills, Map<String, Integer> cooldowns) {
        PlayerSkillData data = new PlayerSkillData();

        // 恢復已學習技能
        for (String skillId : learnedSkills) {
            data.learnSkill(skillId);
        }

        // 恢復冷卻時間
        for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
            data.setSkillCooldown(entry.getKey(), entry.getValue());
        }

        return data;
    }
}
