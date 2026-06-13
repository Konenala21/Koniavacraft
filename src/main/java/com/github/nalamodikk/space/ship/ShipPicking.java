package com.github.nalamodikk.space.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 飛船的視線拾取（raycast 準心瞄準船上哪格方塊），從 ShipEntity 抽出的純幾何搬移。
 * 只讀 contraption + 船的姿勢（{@code ship.worldToLocalPoint}），無狀態。互動/外框/挖礦都用同一條 pick。
 */
public final class ShipPicking {

    private final ShipEntity ship;

    public ShipPicking(ShipEntity ship) {
        this.ship = ship;
    }

    /** raycast 視線進船的 local 方塊，回傳最近命中的方塊 + 命中面 + 命中點(local，放方塊朝向要用)。 */
    @Nullable
    public ShipEntity.Pick pick(Player player) {
        ShipContraption contraption = ship.getContraption();
        if (contraption == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(6.0));
        Vec3 ls = ship.worldToLocalPoint(eye.x, eye.y, eye.z);
        Vec3 le = ship.worldToLocalPoint(end.x, end.y, end.z);
        ShipEntity.Pick best = null;
        double bestDist = Double.MAX_VALUE;
        for (var e : contraption.getBlocks().entrySet()) {
            BlockPos lp = e.getKey();
            VoxelShape shape = e.getValue().state().getShape(EmptyBlockGetter.INSTANCE, lp);
            if (shape.isEmpty()) shape = Shapes.block(); // 空 outline 也補滿格 → 渲染得出來就指得到
            BlockHitResult hit = shape.clip(ls, le, lp);
            if (hit != null) {
                double d = hit.getLocation().distanceToSqr(ls);
                if (d < bestDist) { bestDist = d; best = new ShipEntity.Pick(lp, hit.getDirection(), hit.getLocation()); }
            }
        }
        return best;
    }

    /** 玩家瞄準的 local 方塊（沒指到回 null）。 */
    @Nullable
    public BlockPos pickBlock(Player player) {
        ShipEntity.Pick p = pick(player);
        return p == null ? null : p.local();
    }

    /** 命中面方向（放方塊用）；沒命中回 UP 當保底。 */
    public Direction pickFace(Player player) {
        ShipEntity.Pick p = pick(player);
        return p == null ? Direction.UP : p.face();
    }
}
