package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypes {

    public static final ResourceKey<DamageType> FLOATING_TURRET = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "floating_turret"));
}
