package com.github.nalamodikk.common.utils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class BlockSelectorUtils {

    /**
     * Finds the block position being looked at.
     */
    public static BlockPos getTargetBlock(Player player, double maxDistance) {
        HitResult hit = player.pick(maxDistance, 1.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hit).getBlockPos();
        }
        return null;
    }

    /**
     * Finds the entity being looked at.
     */
    public static Entity getTargetEntity(Player player, double maxDistance) {
        Level level = player.level();
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));
        AABB area = player.getBoundingBox().expandTowards(lookVec.scale(maxDistance)).inflate(1.0D, 1.0D, 1.0D);

        return net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eyePos, endPos, area, (e) -> !e.isSpectator() && e.isPickable(), maxDistance * maxDistance
        ) instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }
}
