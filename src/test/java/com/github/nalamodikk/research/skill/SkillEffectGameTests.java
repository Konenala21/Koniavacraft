package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.FloatingTurretProjectile;
import com.github.nalamodikk.common.entity.ReactionCloud;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * In-world GameTests for the skill fixes that pure JUnit can't reach (they need a
 * live server: explosions, area clouds, projectile hits). Uses real mobs so the
 * explosion/cloud actually targets them. Poison victims are pigs (zombies are
 * undead and immune to poison).
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class SkillEffectGameTests {

    private static final BlockPos CENTER = new BlockPos(2, 2, 2);

    /** Ranged/AoE carrier: the explosion must not damage its own caster (safeExplode guard). */
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void explosionShieldsRangedCaster(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            Zombie caster = helper.spawn(EntityType.ZOMBIE, CENTER);
            Zombie target = helper.spawn(EntityType.ZOMBIE, CENTER);
            caster.setHealth(caster.getMaxHealth());
            float before = caster.getHealth();
            // target != caster -> guard active; explosion is centred on target, caster is point-blank
            SkillEffectOp.COMBUSTION.apply(helper.getLevel(), target, caster, new Vec3(1, 0, 0), 5.0F);
            if (caster.getHealth() < before) {
                helper.fail("ranged-carrier explosion damaged its own caster");
            } else {
                helper.succeed();
            }
        });
    }

    /** Flight carrier puts the effect on the caster (target == caster): self-blast must still hurt. */
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void explosionHurtsFlightSelfCast(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            Zombie caster = helper.spawn(EntityType.ZOMBIE, CENTER);
            caster.setHealth(caster.getMaxHealth());
            float before = caster.getHealth();
            SkillEffectOp.COMBUSTION.apply(helper.getLevel(), caster, caster, new Vec3(1, 0, 0), 5.0F);
            if (caster.getHealth() < before) {
                helper.succeed();
            } else {
                helper.fail("flight self-cast explosion should damage the caster");
            }
        });
    }

    /** Hazard cloud poisons enemies in it but skips its owner (ReactionCloud reflection). */
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void hazardCloudSkipsOwner(GameTestHelper helper) {
        Pig owner = helper.spawn(EntityType.PIG, CENTER);
        Pig victim = helper.spawn(EntityType.PIG, CENTER);
        ReactionCloud cloud = new ReactionCloud(helper.getLevel(), owner.getX(), owner.getY(), owner.getZ());
        cloud.setRadius(3.0F);
        cloud.setDuration(60);
        cloud.setWaitTime(2);
        cloud.setRadiusOnUse(0.0F);
        cloud.setOwner(owner);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        helper.getLevel().addFreshEntity(cloud);

        helper.runAtTickTime(40, () -> {
            if (owner.hasEffect(MobEffects.POISON)) {
                helper.fail("owner was poisoned by their own hazard cloud");
            } else if (!victim.hasEffect(MobEffects.POISON)) {
                helper.fail("a non-owner in the cloud should be poisoned");
            } else {
                helper.succeed();
            }
        });
    }

    /** Machine carrier + a heal effect (0 damage) must not damage the ally it hits. */
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 60)
    public static void healTurretDoesNotDamageAlly(GameTestHelper helper) {
        Zombie shooter = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
        Pig ally = helper.spawn(EntityType.PIG, new BlockPos(4, 2, 2));
        ally.setHealth(ally.getMaxHealth() * 0.5F);
        float before = ally.getHealth();

        Vec3 spawnPos = new Vec3(shooter.getX(), shooter.getY() + shooter.getBbHeight() * 0.5, shooter.getZ());
        Vec3 aim = ally.position().add(0, ally.getBbHeight() * 0.5, 0);
        FloatingTurretProjectile bolt = FloatingTurretProjectile.shootAt(helper.getLevel(), shooter, spawnPos, aim);
        bolt.setSkillPayload(0.0F, List.of(SkillEffectOp.VITALITY)); // heal payload, 0 damage
        helper.getLevel().addFreshEntity(bolt);

        helper.runAtTickTime(50, () -> {
            if (ally.getHealth() < before) {
                helper.fail("heal turret bolt damaged the ally (support skill should deal 0 and heal)");
            } else {
                helper.succeed();
            }
        });
    }
}
