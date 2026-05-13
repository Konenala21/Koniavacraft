package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.knowledge.WorldAspectSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

import java.util.ArrayList;
import java.util.List;

public class EntityAspectResolver {

    public static List<Aspect> resolve(Entity entity, ServerLevel level) {
        if (entity == null) return List.of();

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        WorldAspectSavedData data = WorldAspectSavedData.get(level);
        List<Aspect> cached = data.getEntityMapping(id);
        if (cached != null) return cached;

        List<Aspect> aspects = new ArrayList<>();
        List<Aspect> candidates = new ArrayList<>();

        String path = id.getPath();
        addSpecificEntityAspects(path, aspects, candidates);
        if (aspects.isEmpty()) {
            addClassificationAspects(entity, aspects, candidates);
        }
        addKeywordCandidates(path, aspects, candidates);

        List<Aspect> result = AspectExpression.express(id, aspects, candidates, data.getGenomeSeed(), capacity(path));
        if (result.isEmpty()) {
            result = AspectExpression.fallback(id, data.getGenomeSeed(), 3);
        }
        data.putEntityMapping(id, result);
        return result;
    }

    // ── Specific entity mappings ──────────────────────────────────────────────

    private static void addSpecificEntityAspects(String path, List<Aspect> aspects, List<Aspect> candidates) {

        // ── Undead ────────────────────────────────────────────────────────────
        switch (path) {
            case "zombie" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.VITALITY);
                candidates.add(ModAspects.CORPUS); candidates.add(ModAspects.DEATH);
            }
            case "zombie_villager" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.HUMANITY);
                candidates.add(ModAspects.DEATH); candidates.add(ModAspects.TAINT);
            }
            case "husk" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.EARTH);
                candidates.add(ModAspects.FAMINE); candidates.add(ModAspects.DEATH);
            }
            case "drowned" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.WATER);
                candidates.add(ModAspects.DEATH); candidates.add(ModAspects.PRIMORDIAL);
            }
            case "skeleton" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.DEATH);
                candidates.add(ModAspects.EARTH); candidates.add(ModAspects.BLADE);
            }
            case "skeleton_horse" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.DEATH);
                candidates.add(ModAspects.MOMENTUM);
            }
            case "wither_skeleton" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.DEATH);
                candidates.add(ModAspects.SHADOW); candidates.add(ModAspects.TAINT);
            }
            case "stray" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.FROST);
                candidates.add(ModAspects.DEATH); candidates.add(ModAspects.WATER);
            }
            case "bogged" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.VENOM);
                candidates.add(ModAspects.DEATH); candidates.add(ModAspects.GROWTH);
            }
            case "zoglin" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.FIRE);
                candidates.add(ModAspects.BESTIA); candidates.add(ModAspects.DEATH);
            }
            case "zombified_piglin" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.FIRE);
                candidates.add(ModAspects.METAL); candidates.add(ModAspects.DEATH);
            }

            // ── Arthropod ─────────────────────────────────────────────────────
            case "spider" -> {
                aspects.add(ModAspects.VITALITY); aspects.add(ModAspects.BINDING);
                candidates.add(ModAspects.VENOM); candidates.add(ModAspects.SHADOW);
            }
            case "cave_spider" -> {
                aspects.add(ModAspects.VENOM); aspects.add(ModAspects.SHADOW);
                candidates.add(ModAspects.BINDING); candidates.add(ModAspects.INSTINCT);
            }
            case "bee" -> {
                aspects.add(ModAspects.VITALITY); aspects.add(ModAspects.HARVEST);
                candidates.add(ModAspects.VENOM); candidates.add(ModAspects.FLIGHT);
            }
            case "silverfish" -> {
                aspects.add(ModAspects.VITALITY); aspects.add(ModAspects.CORROSION);
                candidates.add(ModAspects.EXCAVATION); candidates.add(ModAspects.ELDRITCH);
            }
            case "endermite" -> {
                aspects.add(ModAspects.ELDRITCH); aspects.add(ModAspects.WU);
                candidates.add(ModAspects.VOID_ASPECT);
            }

            // ── Aquatic ───────────────────────────────────────────────────────
            case "cod", "salmon", "tropical_fish" -> {
                aspects.add(ModAspects.WATER); aspects.add(ModAspects.VITALITY);
                candidates.add(ModAspects.NOURISH); candidates.add(ModAspects.GROWTH);
            }
            case "pufferfish" -> {
                aspects.add(ModAspects.WATER); aspects.add(ModAspects.VENOM);
                candidates.add(ModAspects.VITALITY); candidates.add(ModAspects.PRIMORDIAL);
            }
            case "squid" -> {
                aspects.add(ModAspects.WATER); aspects.add(ModAspects.SHADOW);
                candidates.add(ModAspects.PRIMORDIAL); candidates.add(ModAspects.BINDING);
            }
            case "glow_squid" -> {
                aspects.add(ModAspects.WATER); aspects.add(ModAspects.RADIANCE);
                candidates.add(ModAspects.SHADOW); candidates.add(ModAspects.PRIMORDIAL);
            }
            case "dolphin" -> {
                aspects.add(ModAspects.WATER); aspects.add(ModAspects.VITALITY);
                candidates.add(ModAspects.MOMENTUM); candidates.add(ModAspects.SENSUS);
            }
            case "turtle" -> {
                aspects.add(ModAspects.VITALITY); aspects.add(ModAspects.WARDING);
                candidates.add(ModAspects.WATER); candidates.add(ModAspects.PROPAGATION);
            }
            case "axolotl" -> {
                aspects.add(ModAspects.PRIMORDIAL); aspects.add(ModAspects.WATER);
                candidates.add(ModAspects.VITALITY); candidates.add(ModAspects.MENDING);
            }
            case "guardian" -> {
                aspects.add(ModAspects.WATER); aspects.add(ModAspects.WARDING);
                candidates.add(ModAspects.RADIANCE); candidates.add(ModAspects.FORTIFY);
            }
            case "elder_guardian" -> {
                aspects.add(ModAspects.WATER); aspects.add(ModAspects.WARDING);
                candidates.add(ModAspects.WISDOM); candidates.add(ModAspects.BINDING);
            }
            case "frog" -> {
                aspects.add(ModAspects.VITALITY); aspects.add(ModAspects.WATER);
                candidates.add(ModAspects.PROPAGATION); candidates.add(ModAspects.PRIMORDIAL);
            }
            case "tadpole" -> {
                aspects.add(ModAspects.PRIMORDIAL); aspects.add(ModAspects.WATER);
                candidates.add(ModAspects.PROPAGATION);
            }

            // ── Illager ───────────────────────────────────────────────────────
            case "pillager" -> {
                aspects.add(ModAspects.ANIMA); aspects.add(ModAspects.BLADE);
                candidates.add(ModAspects.COGNITION); candidates.add(ModAspects.DESIRE);
            }
            case "vindicator" -> {
                aspects.add(ModAspects.ANIMA); aspects.add(ModAspects.BLADE);
                candidates.add(ModAspects.DESIRE); candidates.add(ModAspects.LAW);
            }
            case "evoker" -> {
                aspects.add(ModAspects.ANIMA); aspects.add(ModAspects.ARCANA);
                candidates.add(ModAspects.BINDING); candidates.add(ModAspects.FAITH);
            }
            case "vex" -> {
                aspects.add(ModAspects.ANIMA); aspects.add(ModAspects.FLIGHT);
                candidates.add(ModAspects.WU); candidates.add(ModAspects.BINDING);
            }
            case "witch" -> {
                aspects.add(ModAspects.ANIMA); aspects.add(ModAspects.ALCHEMY);
                candidates.add(ModAspects.VENOM); candidates.add(ModAspects.TAINT);
            }
            case "ravager" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.EARTH);
                candidates.add(ModAspects.MOMENTUM); candidates.add(ModAspects.FORTIFY);
            }

            // ── Specific hostile ──────────────────────────────────────────────
            case "creeper" -> {
                aspects.add(ModAspects.ENERGY); aspects.add(ModAspects.PHLOGISTON);
                candidates.add(ModAspects.RESONANCE); candidates.add(ModAspects.STORM);
            }
            case "enderman" -> {
                aspects.add(ModAspects.WU); aspects.add(ModAspects.GRAVITY);
                candidates.add(ModAspects.ANIMA); candidates.add(ModAspects.ELDRITCH);
                candidates.add(ModAspects.VOID_ASPECT);
            }
            case "blaze" -> {
                aspects.add(ModAspects.FIRE); aspects.add(ModAspects.ENERGY);
                candidates.add(ModAspects.ARCANA); candidates.add(ModAspects.RADIANCE);
            }
            case "ghast" -> {
                aspects.add(ModAspects.WU); aspects.add(ModAspects.FLIGHT);
                candidates.add(ModAspects.FIRE); candidates.add(ModAspects.DESIRE);
            }
            case "slime" -> {
                aspects.add(ModAspects.WATER); aspects.add(ModAspects.PRIMORDIAL);
                candidates.add(ModAspects.VITALITY); candidates.add(ModAspects.BINDING);
            }
            case "magma_cube" -> {
                aspects.add(ModAspects.FIRE); aspects.add(ModAspects.MAGMA);
                candidates.add(ModAspects.PRIMORDIAL); candidates.add(ModAspects.EARTH);
            }
            case "shulker" -> {
                aspects.add(ModAspects.BINDING); aspects.add(ModAspects.WU);
                candidates.add(ModAspects.WARDING); candidates.add(ModAspects.FLIGHT);
            }
            case "phantom" -> {
                aspects.add(ModAspects.UNDEAD); aspects.add(ModAspects.FLIGHT);
                candidates.add(ModAspects.SHADOW); candidates.add(ModAspects.DEATH);
            }
            case "warden" -> {
                aspects.add(ModAspects.WU); aspects.add(ModAspects.RESONANCE);
                candidates.add(ModAspects.BINDING); candidates.add(ModAspects.DEATH);
            }
            case "breeze" -> {
                aspects.add(ModAspects.FLIGHT); aspects.add(ModAspects.ENERGY);
                candidates.add(ModAspects.STORM); candidates.add(ModAspects.VAPOR);
            }
            case "wither" -> {
                aspects.add(ModAspects.DEATH); aspects.add(ModAspects.WU);
                candidates.add(ModAspects.BINDING); candidates.add(ModAspects.TAINT);
            }
            case "ender_dragon" -> {
                aspects.add(ModAspects.WU); aspects.add(ModAspects.VOID_ASPECT);
                candidates.add(ModAspects.ELDRITCH); candidates.add(ModAspects.FLIGHT);
                candidates.add(ModAspects.DEATH);
            }

            // ── Nether ────────────────────────────────────────────────────────
            case "piglin", "piglin_brute" -> {
                aspects.add(ModAspects.FIRE); aspects.add(ModAspects.METAL);
                candidates.add(ModAspects.WEALTH); candidates.add(ModAspects.COMMERCE);
            }
            case "hoglin" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.FIRE);
                candidates.add(ModAspects.CORPUS); candidates.add(ModAspects.INSTINCT);
            }
            case "strider" -> {
                aspects.add(ModAspects.FIRE); aspects.add(ModAspects.VITALITY);
                candidates.add(ModAspects.MAGMA); candidates.add(ModAspects.MOMENTUM);
            }

            // ── Passive fauna ─────────────────────────────────────────────────
            case "cow", "mooshroom" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.NOURISH);
                candidates.add(ModAspects.CORPUS); candidates.add(ModAspects.VITALITY);
            }
            case "pig" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.VITALITY);
                candidates.add(ModAspects.NOURISH); candidates.add(ModAspects.CORPUS);
            }
            case "sheep" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.VITALITY);
                candidates.add(ModAspects.HARVEST); candidates.add(ModAspects.NOURISH);
            }
            case "chicken" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.PROPAGATION);
                candidates.add(ModAspects.FLIGHT); candidates.add(ModAspects.VITALITY);
            }
            case "horse", "donkey", "mule", "camel" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.MOMENTUM);
                candidates.add(ModAspects.VITALITY); candidates.add(ModAspects.INSTRUMENT);
            }
            case "llama", "trader_llama" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.VITALITY);
                candidates.add(ModAspects.COMMERCE); candidates.add(ModAspects.MOMENTUM);
            }
            case "wolf" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.INSTINCT);
                candidates.add(ModAspects.VITALITY); candidates.add(ModAspects.BINDING);
            }
            case "cat", "ocelot" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.SHADOW);
                candidates.add(ModAspects.INSTINCT); candidates.add(ModAspects.DESIRE);
            }
            case "fox" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.DESIRE);
                candidates.add(ModAspects.INSTINCT); candidates.add(ModAspects.SHADOW);
            }
            case "rabbit" -> {
                aspects.add(ModAspects.INSTINCT); aspects.add(ModAspects.VITALITY);
                candidates.add(ModAspects.PROPAGATION); candidates.add(ModAspects.FLIGHT);
            }
            case "polar_bear" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.FROST);
                candidates.add(ModAspects.VITALITY); candidates.add(ModAspects.INSTINCT);
            }
            case "panda" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.VITALITY);
                candidates.add(ModAspects.GROWTH); candidates.add(ModAspects.NOURISH);
            }
            case "parrot" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.LANGUAGE);
                candidates.add(ModAspects.FLIGHT); candidates.add(ModAspects.VITALITY);
            }
            case "bat" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.SHADOW);
                candidates.add(ModAspects.FLIGHT); candidates.add(ModAspects.INSTINCT);
            }
            case "goat" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.EARTH);
                candidates.add(ModAspects.MOMENTUM); candidates.add(ModAspects.VITALITY);
            }
            case "sniffer" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.COGNITION);
                candidates.add(ModAspects.GROWTH); candidates.add(ModAspects.PRIMORDIAL);
            }
            case "armadillo" -> {
                aspects.add(ModAspects.BESTIA); aspects.add(ModAspects.WARDING);
                candidates.add(ModAspects.EARTH); candidates.add(ModAspects.INSTINCT);
            }

            // ── Golems ────────────────────────────────────────────────────────
            case "iron_golem" -> {
                aspects.add(ModAspects.METAL); aspects.add(ModAspects.FORTIFY);
                candidates.add(ModAspects.MECHANISM); candidates.add(ModAspects.ORDER);
            }
            case "snow_golem" -> {
                aspects.add(ModAspects.FROST); aspects.add(ModAspects.MECHANISM);
                candidates.add(ModAspects.WATER); candidates.add(ModAspects.FORTIFY);
            }

            // ── Villagers ─────────────────────────────────────────────────────
            case "villager" -> {
                aspects.add(ModAspects.HUMANITY); aspects.add(ModAspects.COMMERCE);
                candidates.add(ModAspects.COGNITION); candidates.add(ModAspects.ORDER);
            }
            case "wandering_trader" -> {
                aspects.add(ModAspects.HUMANITY); aspects.add(ModAspects.COMMERCE);
                candidates.add(ModAspects.WEALTH); candidates.add(ModAspects.LANGUAGE);
            }

            // ── Special passives ──────────────────────────────────────────────
            case "allay" -> {
                aspects.add(ModAspects.ANIMA); aspects.add(ModAspects.FLIGHT);
                candidates.add(ModAspects.LANGUAGE); candidates.add(ModAspects.RESONANCE);
            }

            // ── Vehicles ──────────────────────────────────────────────────────
            case "armor_stand" -> {
                aspects.add(ModAspects.METAL); aspects.add(ModAspects.CORPUS);
                candidates.add(ModAspects.MECHANISM);
            }
        }

        // Boat variants (oak_boat, spruce_boat, etc.)
        if (path.endsWith("_boat") || path.endsWith("_chest_boat")) {
            aspects.add(ModAspects.WOOD); aspects.add(ModAspects.WATER);
            candidates.add(ModAspects.MOMENTUM); candidates.add(ModAspects.COMMERCE);
        }
    }

    // ── Tag-based classification fallback ─────────────────────────────────────

    private static void addClassificationAspects(Entity entity, List<Aspect> aspects, List<Aspect> candidates) {
        if (entity instanceof LivingEntity livingEntity) {
            addLivingEntityAspects(livingEntity, aspects, candidates);
            return;
        }
        if (entity instanceof AbstractMinecart) {
            aspects.add(ModAspects.METAL); aspects.add(ModAspects.MECHANISM);
            candidates.add(ModAspects.MOMENTUM); candidates.add(ModAspects.EARTH);
            return;
        }
        if (entity instanceof Projectile) {
            aspects.add(ModAspects.ENERGY); aspects.add(ModAspects.MOMENTUM);
            candidates.add(ModAspects.WOOD);
            return;
        }
        aspects.add(ModAspects.EARTH);
        candidates.add(ModAspects.MECHANISM);
    }

    private static void addLivingEntityAspects(LivingEntity entity, List<Aspect> aspects, List<Aspect> candidates) {
        if (entity.getType().is(EntityTypeTags.UNDEAD)) {
            aspects.add(ModAspects.ANIMA); aspects.add(ModAspects.WU);
            candidates.add(ModAspects.UNDEAD); candidates.add(ModAspects.DEATH); candidates.add(ModAspects.CORROSION);
        } else if (entity.getType().is(EntityTypeTags.ARTHROPOD)) {
            aspects.add(ModAspects.VITALITY); aspects.add(ModAspects.CORROSION);
            candidates.add(ModAspects.VENOM); candidates.add(ModAspects.INSTINCT);
        } else if (entity.getType().is(EntityTypeTags.AQUATIC)) {
            aspects.add(ModAspects.WATER); aspects.add(ModAspects.VITALITY);
            candidates.add(ModAspects.PRIMORDIAL); candidates.add(ModAspects.GROWTH);
        } else if (entity.getType().is(EntityTypeTags.ILLAGER)) {
            aspects.add(ModAspects.ANIMA); aspects.add(ModAspects.COGNITION);
            candidates.add(ModAspects.ENERGY); candidates.add(ModAspects.DESIRE);
        } else if (entity instanceof Creeper) {
            aspects.add(ModAspects.ENERGY); aspects.add(ModAspects.PHLOGISTON);
            candidates.add(ModAspects.RESONANCE); candidates.add(ModAspects.STORM);
        } else if (entity instanceof EnderMan) {
            aspects.add(ModAspects.WU); aspects.add(ModAspects.GRAVITY);
            candidates.add(ModAspects.ELDRITCH); candidates.add(ModAspects.VOID_ASPECT);
        } else {
            aspects.add(ModAspects.VITALITY); aspects.add(ModAspects.EARTH);
            candidates.add(ModAspects.BESTIA); candidates.add(ModAspects.CORPUS); candidates.add(ModAspects.GROWTH);
        }
    }

    // ── Keyword supplements ───────────────────────────────────────────────────

    private static void addKeywordCandidates(String path, List<Aspect> aspects, List<Aspect> candidates) {
        if (path.contains("blaze") || path.contains("magma"))   { AspectExpression.addUnique(aspects, ModAspects.FIRE); AspectExpression.addUnique(candidates, ModAspects.ENERGY); }
        if (path.contains("slime"))                             { AspectExpression.addUnique(candidates, ModAspects.PRIMORDIAL); AspectExpression.addUnique(candidates, ModAspects.WATER); }
        if (path.contains("warden") || path.contains("sculk"))  { AspectExpression.addUnique(aspects, ModAspects.WU); AspectExpression.addUnique(candidates, ModAspects.RESONANCE); AspectExpression.addUnique(candidates, ModAspects.BINDING); }
        if (path.contains("dragon") || path.contains("ender"))  { AspectExpression.addUnique(aspects, ModAspects.WU); AspectExpression.addUnique(candidates, ModAspects.ELDRITCH); AspectExpression.addUnique(candidates, ModAspects.VOID_ASPECT); }
        if (path.contains("golem"))                             { AspectExpression.addUnique(aspects, ModAspects.MECHANISM); AspectExpression.addUnique(candidates, ModAspects.FORTIFY); }
        if (path.contains("phantom"))                           { AspectExpression.addUnique(candidates, ModAspects.FLIGHT); AspectExpression.addUnique(candidates, ModAspects.SHADOW); }
        if (path.contains("villager") || path.contains("trader")){ AspectExpression.addUnique(candidates, ModAspects.HUMANITY); AspectExpression.addUnique(candidates, ModAspects.COMMERCE); }
        if (path.contains("wither"))                            { AspectExpression.addUnique(candidates, ModAspects.DEATH); AspectExpression.addUnique(candidates, ModAspects.TAINT); }
        if (path.contains("frost") || path.contains("stray"))   { AspectExpression.addUnique(candidates, ModAspects.FROST); }
        if (path.contains("piglin"))                            { AspectExpression.addUnique(candidates, ModAspects.WEALTH); AspectExpression.addUnique(aspects, ModAspects.FIRE); }
    }

    // ── Capacity ──────────────────────────────────────────────────────────────

    private static int capacity(String path) {
        if (path.equals("ender_dragon") || path.equals("wither"))                       return 6;
        if (path.equals("elder_guardian") || path.equals("warden"))                     return 5;
        if (path.contains("blaze") || path.contains("ender") || path.contains("golem")) return 4;
        if (path.equals("evoker") || path.equals("witch") || path.equals("allay"))      return 4;
        return 3;
    }
}
