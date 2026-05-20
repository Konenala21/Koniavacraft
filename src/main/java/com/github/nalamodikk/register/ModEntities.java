package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.FloatingTurretEntity;
import com.github.nalamodikk.common.entity.FloatingTurretProjectile;
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

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
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

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(FLOATING_TURRET.get(), FloatingTurretEntity.createAttributes().build());
    }

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
