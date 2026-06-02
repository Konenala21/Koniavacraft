package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.FloatingTurretProjectile;
import com.github.nalamodikk.common.entity.ReactionCloud;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * In-world GameTests for the skill fixes that pure JUnit can't reach (they need a
 * live server: explosions, area-cloud state, projectile hits). Uses pigs (no random
 * armor to skew explosion damage; not poison-immune like undead).
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class SkillEffectGameTests {

    private static final BlockPos CENTER = new BlockPos(2, 2, 2);

    /**
     * Self-AoE: the explosion hurts a point-blank target but never its own caster. The caster is
     * always the explosion source, and vanilla excludes the source from explosion damage
     * (Explosion.explode uses getEntities(except = source)), so the caster is shielded regardless
     * of the safeExplode invuln guard. Uses pigs (no random armor to skew explosion damage).
     */
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void selfAoeHitsTargetButSparesCaster(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            Pig caster = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
            Pig target = helper.spawn(EntityType.PIG, new BlockPos(4, 2, 2));
            caster.setNoAi(true);
            target.setNoAi(true);
            caster.setHealth(caster.getMaxHealth());
            target.setHealth(target.getMaxHealth());
            float casterBefore = caster.getHealth();
            float targetBefore = target.getHealth();
            SkillEffectOp.COMBUSTION.apply(helper.getLevel(), target, caster, new Vec3(1, 0, 0), 5.0F);
            if (caster.getHealth() < casterBefore) {
                helper.fail("self-AoE explosion damaged its own caster (the explosion source)");
            } else if (target.getHealth() >= targetBefore) {
                helper.fail("explosion did not hurt the point-blank target (test would be a false pass)");
            } else {
                helper.succeed();
            }
        });
    }

    /**
     * Flight self-cast (target == caster): the caster is still the explosion source, so vanilla
     * source-exclusion spares them: a self-centred COMBUSTION deals NO explosion damage to the
     * caster. This documents current behavior. NOTE: if Flight self-blast is meant to be a real
     * risk/cost, safeExplode needs to hurt the caster explicitly (the invuln "guard" alone cannot,
     * since source-exclusion already shields them). Design decision pending.
     */
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void selfCenteredExplosionSparesSourceCaster(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            Pig caster = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
            caster.setNoAi(true);
            caster.setHealth(caster.getMaxHealth());
            float before = caster.getHealth();
            SkillEffectOp.COMBUSTION.apply(helper.getLevel(), caster, caster, new Vec3(1, 0, 0), 5.0F);
            if (caster.getHealth() < before) {
                helper.fail("source caster took explosion damage (vanilla should exclude the source)");
            } else {
                helper.succeed();
            }
        });
    }

    /**
     * ReactionCloud owner-skip: the owner is pre-injected into the vanilla AreaEffectCloud {@code victims}
     * map so its effect application loop ({@code if (!victims.containsKey(e))}) always skips them, while a
     * non-owner stays out of the map and is hit normally.
     *
     * Asserted deterministically on the victims map instead of through actual poisoning: a headless
     * GameTest region does not reliably tick a freshly added AreaEffectCloud, so the in-world poison
     * pipeline can't be exercised here (the cloud is confirmed working in real play). One manual
     * {@code tick()} drives skipOwner(); the map state IS the fix.
     */
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void hazardCloudSkipsOwner(GameTestHelper helper) {
        Pig owner = helper.spawn(EntityType.PIG, CENTER);
        Pig nonOwner = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 2));
        owner.setNoAi(true);
        nonOwner.setNoAi(true);
        ReactionCloud cloud = new ReactionCloud(helper.getLevel(), owner.getX(), owner.getY(), owner.getZ());
        cloud.setRadius(4.0F);
        cloud.setOwner(owner);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        helper.getLevel().addFreshEntity(cloud); // so getOwner() can resolve the owner by UUID
        cloud.tick();                            // drives skipOwner(): owner gets injected into victims

        try {
            Field victimsField = AreaEffectCloud.class.getDeclaredField("victims");
            victimsField.setAccessible(true);
            Map<?, ?> victims = (Map<?, ?>) victimsField.get(cloud);
            if (!victims.containsKey(owner)) {
                helper.fail("owner was NOT injected into the cloud skip-set (victims); owner would be hit by their own cloud");
            } else if (victims.containsKey(nonOwner)) {
                helper.fail("a non-owner must not be pre-skipped (only the owner should be)");
            } else {
                helper.succeed();
            }
        } catch (ReflectiveOperationException e) {
            helper.fail("AreaEffectCloud.victims reflection failed (mapping changed?): " + e);
        }
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
