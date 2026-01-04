package com.github.nalamodikk.common.rpg.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 📚 技能註冊表
 *
 * 管理所有技能的註冊和查詢
 */
public class SkillRegistry {

    private static final Map<String, Skill> SKILLS = new HashMap<>();

    /**
     * 📝 註冊技能
     */
    public static void register(Skill skill) {
        SKILLS.put(skill.getId(), skill);
    }

    /**
     * 🔍 根據 ID 獲取技能
     */
    public static Optional<Skill> getSkill(String id) {
        return Optional.ofNullable(SKILLS.get(id));
    }

    /**
     * 📋 獲取所有技能
     */
    public static Map<String, Skill> getAllSkills() {
        return new HashMap<>(SKILLS);
    }

    /**
     * 🗑️ 清空註冊表 (僅用於測試)
     */
    public static void clear() {
        SKILLS.clear();
    }

    // ===== 初始化方法 =====

    /**
     * 🚀 初始化所有技能
     */
    public static void init() {
        // TODO: 註冊所有技能
        // register(new HeavyStrikeSkill());
        // register(new FireballSkill());
        // etc.
    }
}
