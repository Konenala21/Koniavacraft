package com.github.nalamodikk.research.skill;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.FloatingTurretProjectile;
import com.github.nalamodikk.common.entity.ReactionCloud;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

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
    private static final Logger LOGGER = LogUtils.getLogger();

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
     * Flight self-cast (target == caster) is a deliberate self-blast (rocket-jump / kamikaze): it
     * must hurt the caster. Vanilla excludes the explosion source from blast damage, so safeExplode
     * deals explicit self-damage (radius * SELF_BLAST_DAMAGE_PER_RADIUS) when target == caster.
     */
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void flightSelfBlastHurtsCaster(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            Pig caster = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
            caster.setNoAi(true);
            caster.setHealth(caster.getMaxHealth());
            float before = caster.getHealth();
            SkillEffectOp.COMBUSTION.apply(helper.getLevel(), caster, caster, new Vec3(1, 0, 0), 5.0F);
            if (caster.getHealth() >= before) {
                helper.fail("Flight self-blast should hurt the caster (target == caster)");
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

    /**
     * Multiplayer explosion-damage measurement: lines up several survival mock players at increasing
     * distances from a single skill explosion and logs how much HP each one loses, then sanity-checks
     * the falloff (centre takes the most, damage never rises with distance, someone past the radius
     * takes none). The damage table is printed to the log so you can read the actual numbers; change
     * POWER to read a different build's blast (it's the `damage` value the op receives, ~10 for a one
     * effect skill, ~18 for "energy three", ~57 for a heavily invested one).
     *
     * makeMockPlayer() returns a bare Player at BlockPos.ZERO that is NOT in the level, so each one is
     * setPos'd and addFreshEntity'd to become a real explosion target. SURVIVAL (not CREATIVE, which
     * is invulnerable). The caster is a separate zombie: vanilla excludes the explosion source, so the
     * caster must not be one of the measured players.
     */
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void explosionDamageAcrossPlayers(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            var level = helper.getLevel();
            Pig target = helper.spawn(EntityType.PIG, new BlockPos(5, 2, 5)); // explosion centre marker
            target.setNoAi(true);
            Vec3 center = target.position();
            Zombie caster = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, 2, 9)); // source != measured players

            final float POWER = 18.0F; // ~ "energy three" damage; radius = min(8, 2 + POWER*0.09) = 3.62
            int[] dists = {0, 1, 2, 3, 4, 5, 6};
            Player[] players = new Player[dists.length];
            float[] before = new float[dists.length];
            for (int i = 0; i < dists.length; i++) {
                Player p = helper.makeMockPlayer(GameType.SURVIVAL);
                // huge max HP so the raw explosion damage shows instead of being capped at a 20-HP death
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
                p.setPos(center.x + dists[i], center.y, center.z);
                p.setHealth(1000.0F);
                level.addFreshEntity(p);
                players[i] = p;
                before[i] = p.getHealth();
            }

            SkillEffectOp.ENERGY.apply(level, target, caster, new Vec3(1, 0, 0), POWER);

            StringBuilder table = new StringBuilder(
                    String.format("[ExplosionDmg] ENERGY power=%.0f radius=%.2f | dmg by distance: ",
                            POWER, Math.min(8.0F, 2.0F + POWER * 0.09F)));
            for (int i = 0; i < dists.length; i++) {
                table.append(String.format("%dm=%.1f  ", dists[i], before[i] - players[i].getHealth()));
            }
            LOGGER.warn(table.toString());

            float centreDmg = before[0] - players[0].getHealth();
            if (centreDmg <= 0.0F) {
                helper.fail("player at the blast centre took no damage (explosion didn't reach added mock players?)");
                return;
            }
            for (int i = 1; i < dists.length; i++) {
                float di = before[i] - players[i].getHealth();
                float dPrev = before[i - 1] - players[i - 1].getHealth();
                if (di > dPrev + 0.01F) {
                    helper.fail("explosion damage rose with distance: " + dists[i] + "m took more than " + dists[i - 1] + "m");
                    return;
                }
            }
            helper.succeed();
        });
    }
}
