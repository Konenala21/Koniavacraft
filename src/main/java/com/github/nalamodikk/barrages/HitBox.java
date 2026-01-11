package com.github.nalamodikk.barrages;

import com.github.nalamodikk.particle.utils.Math3DUtil;
import com.github.nalamodikk.particle.utils.math.RelativeLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public class HitBox {
    public double x1, y1, z1;
    public double x2, y2, z2;

    public HitBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        replacePoint();
    }

    public static HitBox of(double dx, double dy, double dz) {
        return new HitBox(-dx / 2, -dy / 2, -dz / 2, dx / 2, dy / 2, dz / 2);
    }

    public AABB ofBox(Vec3 center) {
        return new AABB(
            x1 + center.x,
            y1 + center.y,
            z1 + center.z,
            x2 + center.x,
            y2 + center.y,
            z2 + center.z
        );
    }

    public void rotateTo(RelativeLocation axis, RelativeLocation to) {
        List<RelativeLocation> points = Arrays.asList(
            new RelativeLocation(x1, y1, z1),
            new RelativeLocation(x2, y2, z2)
        );
        Math3DUtil.rotatePointsToPoint(points, to, axis);
        
        RelativeLocation p1 = points.get(0);
        RelativeLocation p2 = points.get(1);
        
        x1 = p1.x; y1 = p1.y; z1 = p1.z;
        x2 = p2.x; y2 = p2.y; z2 = p2.z;
        
        replacePoint();
    }

    private void replacePoint() {
        double tx = x1;
        x1 = Math.min(x1, x2);
        x2 = Math.max(tx, x2);
        
        double ty = y1;
        y1 = Math.min(y1, y2);
        y2 = Math.max(ty, y2);
        
        double tz = z1;
        z1 = Math.min(z1, z2);
        z2 = Math.max(tz, z2);
    }
}
