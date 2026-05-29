package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModSounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModDatapackProvider extends DatapackBuiltinEntriesProvider {

    public ModDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, ModWorldgenRegistries.BUILDER, Set.of(KoniavacraftMod.MOD_ID));
    }

    public static class ModWorldgenRegistries {

        public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
                .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap)
                .add(Registries.STRUCTURE, ModStructures::bootstrap)
                .add(Registries.STRUCTURE_SET, ModStructureSets::bootstrap)
                .add(Registries.BIOME, ModDimensionProvider::bootstrapBiome)
                .add(Registries.DIMENSION_TYPE, ModDimensionProvider::bootstrapDimensionType)
                .add(Registries.LEVEL_STEM, ModDimensionProvider::bootstrapLevelStem)
                .add(Registries.DAMAGE_TYPE, ctx -> {
                    ctx.register(
                            ResourceKey.create(Registries.DAMAGE_TYPE,
                                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "floating_turret")),
                            new DamageType("floating_turret",
                                    DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, DamageEffects.HURT)
                    );
                })
                .add(Registries.JUKEBOX_SONG, ctx -> {
                    var sounds = ctx.lookup(Registries.SOUND_EVENT);
                    registerSong(ctx, sounds, "quantified_mana_a",   ModSounds.MUSIC_DISC_QUANTIFIED_MANA_A.getKey(),   181.0f, 1);
                    registerSong(ctx, sounds, "quantified_mana_b",   ModSounds.MUSIC_DISC_QUANTIFIED_MANA_B.getKey(),   195.0f, 2);
                    registerSong(ctx, sounds, "memory_two_sides_a",  ModSounds.MUSIC_DISC_MEMORY_TWO_SIDES_A.getKey(),  165.0f, 3);
                    registerSong(ctx, sounds, "memory_two_sides_b",  ModSounds.MUSIC_DISC_MEMORY_TWO_SIDES_B.getKey(),  178.0f, 4);
                    registerSong(ctx, sounds, "knowledge_shortcut_a",ModSounds.MUSIC_DISC_KNOWLEDGE_SHORTCUT_A.getKey(),127.0f, 5);
                    registerSong(ctx, sounds, "knowledge_shortcut_b",ModSounds.MUSIC_DISC_KNOWLEDGE_SHORTCUT_B.getKey(),122.0f, 6);
                    registerSong(ctx, sounds, "u_key_core_a",        ModSounds.MUSIC_DISC_U_KEY_CORE_A.getKey(),        185.0f, 7);
                    registerSong(ctx, sounds, "u_key_core_b",        ModSounds.MUSIC_DISC_U_KEY_CORE_B.getKey(),        204.0f, 8);
                });
    }

    private static void registerSong(
            net.minecraft.data.worldgen.BootstrapContext<JukeboxSong> ctx,
            net.minecraft.core.HolderGetter<net.minecraft.sounds.SoundEvent> sounds,
            String id, ResourceKey<net.minecraft.sounds.SoundEvent> soundKey,
            float length, int comparator) {
        ctx.register(
                ResourceKey.create(Registries.JUKEBOX_SONG,
                        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, id)),
                new JukeboxSong(
                        sounds.getOrThrow(soundKey),
                        Component.translatable("jukebox_song.koniava." + id),
                        length,
                        comparator
                )
        );
    }
}
