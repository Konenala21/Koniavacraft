package com.github.nalamodikk.research.aspect;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A single aspect (面向) in the research system.
 * Primary aspects (水火木金土烏) have no components.
 * Compound aspects are composed of exactly two other aspects.
 *
 * Connection rule: two adjacent grid cells can connect if and only if
 * one aspect is a direct component of the other (one step in the hierarchy).
 */
public class Aspect {

    private final ResourceLocation id;
    private final int color;
    private final List<Aspect> components;

    public Aspect(ResourceLocation id, int color, List<Aspect> components) {
        this.id = id;
        this.color = color;
        this.components = List.copyOf(components);
    }

    public ResourceLocation getId() { return id; }

    public int getColor() { return color; }

    public List<Aspect> getComponents() { return components; }

    public boolean isPrimary() { return components.isEmpty(); }

    /**
     * Returns true if this aspect and {@code other} can connect on adjacent grid cells.
     *
     * Rules (any one is sufficient):
     *   1. Same aspect (A ↔ A) — lets players extend a fixed node with the same aspect.
     *   2. Direct parent-child: one aspect is a component of the other.
     *   3. Shared component: both are compounds that share at least one common component
     *      (e.g. MANA(WU,Water) ↔ RESONANCE(WU,Metal) because both contain WU).
     */
    public boolean canConnectTo(Aspect other) {
        if (this.equals(other)) return true;
        if (this.components.contains(other) || other.components.contains(this)) return true;
        for (Aspect c : this.components) {
            if (other.components.contains(c)) return true;
        }
        return false;
    }

    public Component getName() {
        return Component.translatable("aspect." + id.getNamespace() + "." + id.getPath());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Aspect other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() { return id.toString(); }
}
