package com.github.nalamodikk.common.rpg.data;

import com.github.nalamodikk.common.rpg.attributes.PlayerAttributes;
import com.github.nalamodikk.common.rpg.player.PlayerClass;
import net.minecraft.nbt.CompoundTag;

/**
 * 🎮 玩家 RPG 數據
 *
 * 包含:
 * - 等級/經驗系統
 * - 職業系統
 * - 屬性系統
 * - 未分配屬性點
 */
public class PlayerRPGData {

    // ===== 等級系統 =====
    private int level = 1;
    private int experience = 0;
    private int experienceToNextLevel = 100;

    // ===== 職業系統 =====
    private PlayerClass playerClass = PlayerClass.NONE;

    // ===== 屬性系統 =====
    private final PlayerAttributes attributes = new PlayerAttributes();
    private int unspentAttributePoints = 0;

    // ===== 等級配置 =====
    private static final int MAX_LEVEL = 100;
    private static final int ATTRIBUTE_POINTS_PER_LEVEL = 3; // 每級獲得 3 點屬性點

    // ===== Getter/Setter =====

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(level, MAX_LEVEL));
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
    }

    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    public void setExperienceToNextLevel(int experienceToNextLevel) {
        this.experienceToNextLevel = experienceToNextLevel;
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(PlayerClass playerClass) {
        this.playerClass = playerClass;
    }

    public PlayerAttributes getAttributes() {
        return attributes;
    }

    public int getUnspentAttributePoints() {
        return unspentAttributePoints;
    }

    public void setUnspentAttributePoints(int points) {
        this.unspentAttributePoints = Math.max(0, points);
    }

    // ===== 經驗/等級方法 =====

    /**
     * ⭐ 添加經驗值
     * @param amount 經驗值數量
     * @return 是否升級
     */
    public boolean addExperience(int amount) {
        if (level >= MAX_LEVEL) {
            return false;
        }

        this.experience += amount;
        boolean leveledUp = false;

        // 檢查是否升級
        while (this.experience >= experienceToNextLevel && level < MAX_LEVEL) {
            this.experience -= experienceToNextLevel;
            levelUp();
            leveledUp = true;
        }

        // 達到最大等級時清空多餘經驗
        if (level >= MAX_LEVEL) {
            this.experience = 0;
        }

        return leveledUp;
    }

    /**
     * 📈 升級處理
     */
    private void levelUp() {
        level++;
        unspentAttributePoints += ATTRIBUTE_POINTS_PER_LEVEL;
        experienceToNextLevel = calculateExperienceForLevel(level + 1);

        // TODO: 觸發升級事件/音效/粒子效果
    }

    /**
     * 計算指定等級所需的經驗值
     * 使用公式: baseExp * (level^1.5)
     */
    private int calculateExperienceForLevel(int targetLevel) {
        if (targetLevel <= 1) return 0;
        return (int) (100 * Math.pow(targetLevel, 1.5));
    }

    /**
     * 📊 獲取升級進度百分比
     * @return 0.0 - 1.0
     */
    public float getLevelProgress() {
        if (level >= MAX_LEVEL) {
            return 1.0f;
        }
        return (float) experience / experienceToNextLevel;
    }

    // ===== 屬性點分配 =====

    /**
     * 🔄 分配屬性點
     * @param attributeName 屬性名稱 (strength, intelligence, agility, vitality, perception)
     * @param amount 點數
     * @return 是否成功
     */
    public boolean allocateAttributePoint(String attributeName, int amount) {
        if (unspentAttributePoints < amount || amount <= 0) {
            return false;
        }

        switch (attributeName.toLowerCase()) {
            case "strength" -> attributes.addStrength(amount);
            case "intelligence" -> attributes.addIntelligence(amount);
            case "agility" -> attributes.addAgility(amount);
            case "vitality" -> attributes.addVitality(amount);
            case "perception" -> attributes.addPerception(amount);
            default -> {
                return false;
            }
        }

        unspentAttributePoints -= amount;
        return true;
    }

    /**
     * 🔄 重置屬性點 (需要消耗道具或貨幣)
     */
    public void resetAttributes() {
        int totalPoints = attributes.getTotalAttributePoints();
        unspentAttributePoints += totalPoints;

        // 重置所有屬性
        attributes.setStrength(0);
        attributes.setIntelligence(0);
        attributes.setAgility(0);
        attributes.setVitality(0);
        attributes.setPerception(0);

        // TODO: 消耗重置道具/貨幣
    }

    // ===== NBT 序列化 =====

    /**
     * 💾 保存到 NBT
     */
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();

        // 等級系統
        tag.putInt("Level", level);
        tag.putInt("Experience", experience);
        tag.putInt("ExperienceToNextLevel", experienceToNextLevel);

        // 職業
        tag.putString("PlayerClass", playerClass.getId());

        // 屬性
        CompoundTag attributesTag = new CompoundTag();
        attributesTag.putInt("Strength", attributes.getStrength());
        attributesTag.putInt("Intelligence", attributes.getIntelligence());
        attributesTag.putInt("Agility", attributes.getAgility());
        attributesTag.putInt("Vitality", attributes.getVitality());
        attributesTag.putInt("Perception", attributes.getPerception());
        tag.put("Attributes", attributesTag);

        // 未分配屬性點
        tag.putInt("UnspentAttributePoints", unspentAttributePoints);

        return tag;
    }

    /**
     * 📂 從 NBT 加載
     */
    public void loadFromNBT(CompoundTag tag) {
        // 等級系統
        level = tag.getInt("Level");
        experience = tag.getInt("Experience");
        experienceToNextLevel = tag.getInt("ExperienceToNextLevel");

        // 職業
        playerClass = PlayerClass.fromId(tag.getString("PlayerClass"));

        // 屬性
        if (tag.contains("Attributes")) {
            CompoundTag attributesTag = tag.getCompound("Attributes");
            attributes.setStrength(attributesTag.getInt("Strength"));
            attributes.setIntelligence(attributesTag.getInt("Intelligence"));
            attributes.setAgility(attributesTag.getInt("Agility"));
            attributes.setVitality(attributesTag.getInt("Vitality"));
            attributes.setPerception(attributesTag.getInt("Perception"));
        }

        // 未分配屬性點
        unspentAttributePoints = tag.getInt("UnspentAttributePoints");
    }

    /**
     * 🔄 複製數據
     */
    public void copyFrom(PlayerRPGData other) {
        this.level = other.level;
        this.experience = other.experience;
        this.experienceToNextLevel = other.experienceToNextLevel;
        this.playerClass = other.playerClass;
        this.unspentAttributePoints = other.unspentAttributePoints;

        // 複製屬性
        this.attributes.setStrength(other.attributes.getStrength());
        this.attributes.setIntelligence(other.attributes.getIntelligence());
        this.attributes.setAgility(other.attributes.getAgility());
        this.attributes.setVitality(other.attributes.getVitality());
        this.attributes.setPerception(other.attributes.getPerception());
    }
}
