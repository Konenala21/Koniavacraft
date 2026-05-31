package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A player-authored skill "recipe" stored on a spell core: a name plus the aspect
 * ids assigned to each role, plus an optional custom icon (an item id). It is not a
 * compiled skill; {@link #compile()} turns it into an executable {@link SkillEffect}
 * on demand (at cast time) via {@link SkillCompiler}, so storage stays small.
 *
 * The dual cost model means fuel aspects are gone: aspects are a gate, mana is the
 * consumable, so a recipe is just carrier + effects + modifiers (+ cosmetic icon).
 */
public record StoredSkill(String name,
                          ResourceLocation carrier,
                          List<ResourceLocation> effects,
                          List<ResourceLocation> modifiers,
                          Optional<ResourceLocation> icon) {

    /** Convenience: a recipe with no custom icon (icon is auto-generated). */
    public StoredSkill(String name, ResourceLocation carrier,
                       List<ResourceLocation> effects, List<ResourceLocation> modifiers) {
        this(name, carrier, effects, modifiers, Optional.empty());
    }

    public static final Codec<StoredSkill> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(StoredSkill::name),
            ResourceLocation.CODEC.fieldOf("carrier").forGetter(StoredSkill::carrier),
            ResourceLocation.CODEC.listOf().fieldOf("effects").forGetter(StoredSkill::effects),
            ResourceLocation.CODEC.listOf().fieldOf("modifiers").forGetter(StoredSkill::modifiers),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(StoredSkill::icon)
    ).apply(i, StoredSkill::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoredSkill> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StoredSkill::name,
            ResourceLocation.STREAM_CODEC, StoredSkill::carrier,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), StoredSkill::effects,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), StoredSkill::modifiers,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), StoredSkill::icon,
            StoredSkill::new);

    /** Resolve the aspect ids and compile to an executable skill (icon is cosmetic). */
    public SkillEffect compile() {
        Aspect carrierAspect = ModAspects.get(carrier);
        List<Aspect> effectAspects = effects.stream().map(ModAspects::get).filter(Objects::nonNull).toList();
        List<Aspect> modifierAspects = modifiers.stream().map(ModAspects::get).filter(Objects::nonNull).toList();
        return SkillCompiler.compile(carrierAspect, effectAspects, modifierAspects, Map.of());
    }
}
