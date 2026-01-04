package com.github.nalamodikk.common.rpg.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;

/**
 * 🎯 玩家技能數據
 *
 * 管理玩家已學習的技能和冷卻狀態
 */
public class PlayerSkillData {

    // 已學習的技能 ID 列表
    private final Set<String> learnedSkills = new HashSet<>();

    // 技能冷卻時間 (技能ID -> 剩餘ticks)
    private final Map<String, Integer> skillCooldowns = new HashMap<>();

    // ===== 學習技能 =====

    /**
     * 📖 學習技能
     */
    public boolean learnSkill(String skillId) {
        return learnedSkills.add(skillId);
    }

    /**
     * ❌ 遺忘技能
     */
    public boolean forgetSkill(String skillId) {
        skillCooldowns.remove(skillId);
        return learnedSkills.remove(skillId);
    }

    /**
     * ✅ 檢查是否已學習技能
     */
    public boolean hasLearnedSkill(String skillId) {
        return learnedSkills.contains(skillId);
    }

    /**
     * 📋 獲取所有已學習技能
     */
    public Set<String> getLearnedSkills() {
        return new HashSet<>(learnedSkills);
    }

    // ===== 冷卻管理 =====

    /**
     * 🕐 設置技能冷卻
     * @param skillId 技能 ID
     * @param ticks 冷卻時間 (ticks)
     */
    public void setSkillCooldown(String skillId, int ticks) {
        if (ticks > 0) {
            skillCooldowns.put(skillId, ticks);
        } else {
            skillCooldowns.remove(skillId);
        }
    }

    /**
     * 🕐 獲取技能剩餘冷卻時間
     * @return 剩餘 ticks，0 表示無冷卻
     */
    public int getSkillCooldown(String skillId) {
        return skillCooldowns.getOrDefault(skillId, 0);
    }

    /**
     * ✅ 檢查技能是否冷卻完畢
     */
    public boolean isSkillReady(String skillId) {
        return getSkillCooldown(skillId) <= 0;
    }

    /**
     * ⏱️ 更新所有技能冷卻 (每 tick 調用)
     */
    public void tickCooldowns() {
        Iterator<Map.Entry<String, Integer>> iterator = skillCooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;

            if (remaining <= 0) {
                iterator.remove(); // 冷卻完畢，移除
            } else {
                entry.setValue(remaining);
            }
        }
    }

    // ===== NBT 序列化 =====

    /**
     * 💾 保存到 NBT
     */
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();

        // 已學習技能
        ListTag learnedList = new ListTag();
        for (String skillId : learnedSkills) {
            learnedList.add(StringTag.valueOf(skillId));
        }
        tag.put("LearnedSkills", learnedList);

        // 冷卻時間
        CompoundTag cooldownsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : skillCooldowns.entrySet()) {
            cooldownsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Cooldowns", cooldownsTag);

        return tag;
    }

    /**
     * 📂 從 NBT 加載
     */
    public void loadFromNBT(CompoundTag tag) {
        // 已學習技能
        learnedSkills.clear();
        if (tag.contains("LearnedSkills")) {
            ListTag learnedList = tag.getList("LearnedSkills", Tag.TAG_STRING);
            for (int i = 0; i < learnedList.size(); i++) {
                learnedSkills.add(learnedList.getString(i));
            }
        }

        // 冷卻時間
        skillCooldowns.clear();
        if (tag.contains("Cooldowns")) {
            CompoundTag cooldownsTag = tag.getCompound("Cooldowns");
            for (String key : cooldownsTag.getAllKeys()) {
                skillCooldowns.put(key, cooldownsTag.getInt(key));
            }
        }
    }

    /**
     * 🔄 複製數據
     */
    public void copyFrom(PlayerSkillData other) {
        this.learnedSkills.clear();
        this.learnedSkills.addAll(other.learnedSkills);

        this.skillCooldowns.clear();
        this.skillCooldowns.putAll(other.skillCooldowns);
    }
}
