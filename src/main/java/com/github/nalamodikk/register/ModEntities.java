package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.FloatingTurretEntity;
import com.github.nalamodikk.common.entity.FloatingTurretProjectile;
import com.github.nalamodikk.common.entity.OrbitalOrbEntity;
import com.github.nalamodikk.common.entity.SpaceCrackEntity;
import com.github.nalamodikk.common.entity.SpellProjectileEntity;
import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import com.github.nalamodikk.common.entity.NaraPhantomEntity;
import com.github.nalamodikk.common.entity.TrainingDummyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static net.minecraft.core.registries.Registries.ENTITY_TYPE;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ENTITY_TYPE, KoniavacraftMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<FloatingTurretEntity>> FLOATING_TURRET =
            ENTITY_TYPES.register("floating_turret", () ->
                    EntityType.Builder.<FloatingTurretEntity>of(FloatingTurretEntity::new, MobCategory.MISC)
                            .sized(0.6F, 0.6F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(KoniavacraftMod.MOD_ID + ":floating_turret")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<FloatingTurretProjectile>> FLOATING_TURRET_PROJECTILE =
            ENTITY_TYPES.register("floating_turret_projectile", () ->
                    EntityType.Builder.<FloatingTurretProjectile>of(FloatingTurretProjectile::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build(KoniavacraftMod.MOD_ID + ":floating_turret_projectile")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<SpellProjectileEntity>> SPELL_PROJECTILE =
            ENTITY_TYPES.register("spell_projectile", () ->
                    EntityType.Builder.<SpellProjectileEntity>of(SpellProjectileEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build(KoniavacraftMod.MOD_ID + ":spell_projectile")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<OrbitalOrbEntity>> ORBITAL_ORB =
            ENTITY_TYPES.register("orbital_orb", () ->
                    EntityType.Builder.<OrbitalOrbEntity>of(OrbitalOrbEntity::new, MobCategory.MISC)
                            .sized(0.4F, 0.4F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build(KoniavacraftMod.MOD_ID + ":orbital_orb")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<SpaceCrackEntity>> SPACE_CRACK =
            ENTITY_TYPES.register("space_crack", () ->
                    EntityType.Builder.<SpaceCrackEntity>of(SpaceCrackEntity::new, MobCategory.MISC)
                            .sized(1.5F, 2.0F)
                            .clientTrackingRange(16)
                            .updateInterval(20)
                            .build(KoniavacraftMod.MOD_ID + ":space_crack")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<PlayerCloneEntity>> PLAYER_CLONE =
            ENTITY_TYPES.register("player_clone", () ->
                    EntityType.Builder.<PlayerCloneEntity>of(PlayerCloneEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(KoniavacraftMod.MOD_ID + ":player_clone")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<NaraPhantomEntity>> NARA_PHANTOM =
            ENTITY_TYPES.register("nara_phantom", () ->
                    EntityType.Builder.<NaraPhantomEntity>of(NaraPhantomEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(16)
                            .build(KoniavacraftMod.MOD_ID + ":nara_phantom")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<TrainingDummyEntity>> TRAINING_DUMMY =
            ENTITY_TYPES.register("training_dummy", () ->
                    EntityType.Builder.<TrainingDummyEntity>of(TrainingDummyEntity::new, MobCategory.MISC)
                            .sized(0.7F, 1.9F)
                            .clientTrackingRange(10)
                            .build(KoniavacraftMod.MOD_ID + ":training_dummy")
            );

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(FLOATING_TURRET.get(), FloatingTurretEntity.createAttributes().build());
        event.put(PLAYER_CLONE.get(), PlayerCloneEntity.createAttributes().build());
        event.put(NARA_PHANTOM.get(), NaraPhantomEntity.createAttributes().build());
        event.put(TRAINING_DUMMY.get(), TrainingDummyEntity.createAttributes().build());
    }

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
