package com.github.nalamodikk.common.utils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BlockSelectorUtils {

    @Nullable
    public static BlockPos getTargetBlock(Player player, double maxDistance) {
        HitResult hit = player.pick(maxDistance, 1.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hit).getBlockPos();
        }
        return null;
    }

    @Nullable
    public static Entity getTargetEntity(Player player, double maxDistance) {
        Level level = player.level();
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));
        AABB area = player.getBoundingBox()
                .expandTowards(lookVec.scale(maxDistance))
                .inflate(1.0D, 1.0D, 1.0D);

        Entity hitResult = getPickableEntity(player, eyePos, endPos, area, maxDistance);
        if (hitResult == null) {
            hitResult = getItemEntity(level, player, eyePos, endPos, area, maxDistance);
        }

        if (hitResult instanceof PartEntity<?> part) {
            return part.getParent();
        }
        return hitResult;
    }

    @Nullable
    private static Entity getPickableEntity(Player player, Vec3 eyePos, Vec3 endPos, AABB area, double maxDistance) {
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                eyePos,
                endPos,
                area,
                entity -> !entity.isSpectator() && (entity.isPickable() || entity instanceof PartEntity<?>),
                maxDistance * maxDistance);

        return entityHit != null ? entityHit.getEntity() : null;
    }

    @Nullable
    private static Entity getItemEntity(Level level, Player player, Vec3 eyePos, Vec3 endPos,
                                        AABB area, double maxDistance) {
        double closestDist = maxDistance * maxDistance;
        ItemEntity closest = null;

        for (Entity entity : level.getEntities(player, area)) {
            if (entity instanceof ItemEntity itemEntity) {
                Optional<Vec3> hit = itemEntity.getBoundingBox().inflate(0.3).clip(eyePos, endPos);
                if (hit.isPresent()) {
                    double dist = eyePos.distanceToSqr(hit.get());
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = itemEntity;
                    }
                }
            }
        }

        return closest;
    }
}
