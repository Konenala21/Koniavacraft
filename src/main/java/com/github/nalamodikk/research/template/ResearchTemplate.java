package com.github.nalamodikk.research.template;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Defines a single researchable entry in the knowledge tree.
 *
 * A template is immutable data. The live puzzle state is held in
 * {@link com.github.nalamodikk.research.grid.ResearchGrid}, generated
 * on demand from this template.
 */
public final class ResearchTemplate {

    private final ResourceLocation id;
    private final int tier;                          // 1–12
    private final List<Aspect> requiredAspects;      // FIXED nodes placed in the puzzle grid
    private final double holeRatio;                  // fraction of non-fixed cells to hole out (0.0–1.0)
    private final Set<ResourceLocation> prerequisites; // research IDs that must be completed first
    private final List<ResourceLocation> unlockedRecipes; // recipe IDs unlocked on completion
    private final ItemStack icon;                    // display icon in the knowledge book UI
    private final String titleKey;                   // translation key for the title

    private ResearchTemplate(Builder builder) {
        this.id = builder.id;
        this.tier = builder.tier;
        this.requiredAspects = List.copyOf(builder.requiredAspects);
        this.holeRatio = builder.holeRatio;
        this.prerequisites = Set.copyOf(builder.prerequisites);
        this.unlockedRecipes = List.copyOf(builder.unlockedRecipes);
        this.icon = builder.icon;
        this.titleKey = builder.titleKey;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public ResourceLocation getId()                  { return id; }
    public int getTier()                             { return tier; }
    public List<Aspect> getRequiredAspects()         { return requiredAspects; }
    public double getHoleRatio()                     { return holeRatio; }
    public Set<ResourceLocation> getPrerequisites()  { return prerequisites; }
    public List<ResourceLocation> getUnlockedRecipes() { return unlockedRecipes; }
    public ItemStack getIcon()                       { return icon.copy(); }
    public String getTitleKey()                      { return titleKey; }

    // ── Availability check ───────────────────────────────────────────────────

    /**
     * Returns true if the player meets all conditions to attempt this research:
     *   - player tier >= this research's tier
     *   - all prerequisite research entries are completed
     *   - player has discovered all required aspects
     */
    public boolean isAvailableTo(PlayerKnowledge knowledge) {
        if (knowledge.getCurrentTier() < tier) return false;

        for (ResourceLocation prereq : prerequisites) {
            if (!knowledge.hasCompleted(prereq)) return false;
        }

        for (Aspect aspect : requiredAspects) {
            if (!knowledge.hasDiscovered(aspect)) return false;
        }

        return true;
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private int tier = 1;
        private List<Aspect> requiredAspects = List.of();
        private double holeRatio = 0.35;
        private Set<ResourceLocation> prerequisites = Set.of();
        private List<ResourceLocation> unlockedRecipes = List.of();
        private ItemStack icon = ItemStack.EMPTY;
        private String titleKey;

        private Builder(ResourceLocation id) {
            this.id = id;
            this.titleKey = "research." + id.getNamespace() + "." + id.getPath();
        }

        public Builder tier(int tier)                                    { this.tier = tier; return this; }
        public Builder aspects(List<Aspect> aspects)                     { this.requiredAspects = aspects; return this; }
        public Builder aspects(Aspect... aspects)                        { this.requiredAspects = List.of(aspects); return this; }
        public Builder holeRatio(double ratio)                           { this.holeRatio = ratio; return this; }
        public Builder prerequisites(ResourceLocation... ids)            { this.prerequisites = Set.of(ids); return this; }
        public Builder unlocks(ResourceLocation... recipeIds)            { this.unlockedRecipes = List.of(recipeIds); return this; }
        public Builder icon(ItemStack icon)                              { this.icon = icon; return this; }
        public Builder titleKey(String key)                              { this.titleKey = key; return this; }

        public ResearchTemplate build() {
            if (requiredAspects.size() < 2)
                throw new IllegalStateException("Research " + id + " needs at least 2 required aspects");
            return new ResearchTemplate(this);
        }
    }
}
