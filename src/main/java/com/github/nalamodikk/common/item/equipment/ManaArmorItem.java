package com.github.nalamodikk.common.item.equipment;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.client.ModKeyMappings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

public abstract class ManaArmorItem extends ArmorItem {

    private final int baseMaxMana;
    private final int baseArmor;
    private final int maxUpgradeSlots;

    protected ManaArmorItem(Holder<ArmorMaterial> material, Type type,
                             int baseMaxMana, int baseArmor, int maxUpgradeSlots,
                             Properties properties) {
        super(material, type, properties.stacksTo(1));
        this.baseMaxMana = baseMaxMana;
        this.baseArmor = baseArmor;
        this.maxUpgradeSlots = maxUpgradeSlots;
    }

    public int getBaseMaxMana() { return baseMaxMana; }
    public int getBaseArmor()   { return baseArmor; }
    public int getMaxUpgradeSlots() { return maxUpgradeSlots; }

    // ── Mana helpers ─────────────────────────────────────────────────────────

    public static int getMana(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
    }

    public static void setMana(ItemStack stack, int mana) {
        stack.set(ModDataComponents.MANA_STORED, Mth.clamp(mana, 0, getMaxMana(stack)));
    }

    public static int getMaxMana(ItemStack stack) {
        int base = stack.getItem() instanceof ManaArmorItem m ? m.getBaseMaxMana() : 0;
        return stack.getOrDefault(ModDataComponents.MAX_MANA, base);
    }

    // ── Upgrade data helpers ─────────────────────────────────────────────────

    public static EquipmentUpgradeData getData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.EQUIPMENT_UPGRADE_DATA, EquipmentUpgradeData.empty());
    }

    public static void setData(ItemStack stack, EquipmentUpgradeData data) {
        stack.set(ModDataComponents.EQUIPMENT_UPGRADE_DATA, data);
    }

    /** Recalculate and store MAX_MANA based on installed CAPACITY upgrades. */
    public abstract void recalculateMaxMana(ItemStack armor);

    /** Returns true if the given item can be installed as an upgrade in this armor. */
    public abstract boolean isValidUpgradeItem(ItemStack stack);

    /**
     * Returns a string key identifying the behavior type of the given upgrade item.
     * Used for one-per-type enforcement: two upgrades with the same key cannot coexist.
     */
    public abstract String getUpgradeBehaviorKey(ItemStack upgradeStack);

    // ── Dynamic armor attribute ──────────────────────────────────────────────

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        int armor = baseArmor + getArmorBonus(stack);
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,
                                        "mana_armor_" + getType().getName()),
                                armor,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.bySlot(getEquipmentSlot()))
                .build();
    }

    /** Override to add bonus armor from installed upgrades. */
    protected int getArmorBonus(ItemStack stack) { return 0; }

    // ── Tooltip ──────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.koniava.mana_armor.mana",
                getMana(stack), getMaxMana(stack)));
        int count = (int) getData(stack).upgrades().values().stream()
                .filter(s -> !s.isEmpty()).count();
        if (count > 0) {
            lines.add(Component.translatable("tooltip.koniava.mana_armor.upgrades",
                    count, maxUpgradeSlots));
        }
        lines.add(Component.translatable("tooltip.koniava.mana_alloy_boots.open_upgrade",
                ModKeyMappings.OPEN_UPGRADE_GUI.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.GRAY));
    }
}
