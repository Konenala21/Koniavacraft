package com.github.nalamodikk.common.rpg.skill;

/**
 * 🎯 技能類型枚舉
 */
public enum SkillType {
    MELEE("melee"),
    RANGED("ranged"),
    MAGIC("magic"),
    BUFF("buff"),
    DEBUFF("debuff"),
    UTILITY("utility");

    private final String id;

    SkillType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * 獲取翻譯鍵
     */
    public String getTranslationKey() {
        return "rpg.skill.type." + id;
    }
}
