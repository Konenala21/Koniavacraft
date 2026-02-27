package com.github.nalamodikk.common.block.blockentity.render;

import com.github.nalamodikk.common.config.ModClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public final class RenderAnimationLodUtils {
    private RenderAnimationLodUtils() {
    }

    public static float getAnimationTimeScale(BlockPos blockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) {
            return 0.0F;
        }

        double centerX = blockPos.getX() + 0.5D;
        double centerY = blockPos.getY() + 0.5D;
        double centerZ = blockPos.getZ() + 0.5D;
        double distanceSq = cameraEntity.distanceToSqr(centerX, centerY, centerZ);
        int fullDistance = Math.max(1, ModClientConfig.INSTANCE.fullAnimationDistance.get());
        int reducedDistance = Math.max(fullDistance, ModClientConfig.INSTANCE.reducedAnimationDistance.get());
        float reducedScale = ModClientConfig.INSTANCE.reducedAnimationScale.get().floatValue();

        double fullDistanceSq = (double) fullDistance * fullDistance;
        double reducedDistanceSq = (double) reducedDistance * reducedDistance;

        if (distanceSq <= fullDistanceSq) {
            return 1.0F;
        }
        if (distanceSq <= reducedDistanceSq) {
            return Math.max(0.0F, Math.min(1.0F, reducedScale));
        }
        return 0.0F;
    }
}
