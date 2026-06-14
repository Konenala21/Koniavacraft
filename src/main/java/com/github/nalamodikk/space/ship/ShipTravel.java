package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.space.orbit.PlanetDef;
import com.github.nalamodikk.space.orbit.StarSystem;
import com.github.nalamodikk.space.orbit.StarSystemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 飛船的高度觸發跨維度旅行（主世界 <-> 太空），從 ShipEntity 抽出的純結構搬移。
 *
 * <p>進太空 = 主世界飛到 {@link #SPACE_ENTRY_Y} 且 tier ≥ {@link #ORBIT_MIN_TIER}；回地球 = 在太空裡
 * 飛進地球**當下公轉位置**的引力區（{@link #EARTH_GRAVITY_RADIUS}）內，不是 Y 門檻 → 地球會動，
 * 引力區跟著走（同渲染:StarSystemRegistry + getGameTime）。
 *
 * <p>船是 contraption entity，搬移用 {@code changeDimension(DimensionTransition)} 在目標維度重建
 * （save NBT 帶 contraption + shadowAnchor + launchX/Z，新船 tick 重接影子）。乘客先卸載 → 傳送 → 重騎。
 * 轉場後 {@link #TRANSITION_COOLDOWN} tick 冷卻防剛到就反彈回去。
 */
public final class ShipTravel {

    /** 主世界飛到這高度 → 進太空。client 端 AtmosphereTransition.Y_FADE_END 自動引用這個。 */
    public static final int SPACE_ENTRY_Y = 3000;
    /** 進軌道最低 tier:有燃料+引擎=tier 5 ≥ 1,基礎船就能上;星球之後要更高。 */
    public static final int ORBIT_MIN_TIER = 1;
    /** 引力區半徑(回地球的觸發距離)。注意:<地球視覺半徑 76 → 回程會先飛進地球球體才傳送(鑽進星球感)。 */
    public static final double EARTH_GRAVITY_RADIUS = 40.0;
    /** 進太空時落在引力區外緣再加這距離(地球外側)。抵達距離 = 半徑+margin,客戶端 renderAscentEarth 對齊這個。 */
    public static final double ARRIVE_MARGIN = 50.0;
    /** 回主世界的 Y(高空,接著往下飛)。 */
    public static final double OVERWORLD_RETURN_Y = 700.0;
    /** 轉場後冷卻 tick,防剛到就立刻反彈。 */
    public static final int TRANSITION_COOLDOWN = 100;

    private final ShipEntity ship;

    private boolean dimChanging;        // 正在跨維度轉場,防同 tick 重複觸發
    private int transitionCooldown;     // 轉場後冷卻,防剛到就反彈(不存檔,重建後手動設)
    private double launchX, launchZ;    // 從主世界哪裡發射的,回程傳回這(存 NBT 跨維度保留)

    public ShipTravel(ShipEntity ship) {
        this.ship = ship;
    }

    /** 是否正在跨維度轉場(轉場會卸乘客→傳送→重騎,removePassenger 別把這當「玩家高速下船」誤擋)。 */
    public boolean isTransitioning() {
        return dimChanging;
    }

    /**
     * 每 server tick 跑（由 tickServerMovement 呼叫）。主世界 Y >= SPACE_ENTRY_Y → tier 夠就進太空；
     * 太空飛進地球引力區 → 回主世界。
     */
    public void tick() {
        if (ship.level().isClientSide || dimChanging || ship.getContraption() == null) return;
        if (transitionCooldown > 0) { transitionCooldown--; return; }
        MinecraftServer server = ship.level().getServer();
        if (server == null) return;

        if (ship.level().dimension() == Level.OVERWORLD && ship.getY() >= SPACE_ENTRY_Y) {
            if (ship.getShipTier() < ORBIT_MIN_TIER) { // tier 不夠:離不開大氣層,節流提示,不傳送
                if (ship.tickCount % 40 == 0)
                    for (Entity p : ship.getPassengers())
                        if (p instanceof ServerPlayer sp)
                            sp.displayClientMessage(Component.translatable("message.koniava.ship.tier_too_low_orbit"), true);
                return;
            }
            ServerLevel space = server.getLevel(ModDimensions.SPACE);
            if (space != null) {
                launchX = ship.getX(); launchZ = ship.getZ(); // 記住發射點,回程傳回這
                // 落在地球(當下軌道位置)外側、引力區外:沿「遠離太陽」方向推開
                Vec3 earth = currentEarthSpacePos(space);
                Vector3f sysPos = StarSystemRegistry.SOLAR_SYSTEM.worldPos();
                Vec3 sun = new Vec3(sysPos.x, earth.y, sysPos.z);
                Vec3 outward = earth.subtract(sun);
                outward = outward.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : outward.normalize();
                Vec3 arrive = earth.add(outward.scale(EARTH_GRAVITY_RADIUS + ARRIVE_MARGIN));
                doTransition(space, arrive);
            }
        } else if (ship.level().dimension() == ModDimensions.SPACE) {
            Vec3 earth = currentEarthSpacePos((ServerLevel) ship.level()); // 公轉中,每 tick 重算
            if (ship.position().distanceToSqr(earth) <= EARTH_GRAVITY_RADIUS * EARTH_GRAVITY_RADIUS)
                doTransition(server.overworld(), new Vec3(launchX, OVERWORLD_RETURN_Y, launchZ));
        }
    }

    /** 地球在太空維度的當下位置(公轉)。跟渲染同一套:StarSystemRegistry 的軌道公式 + getGameTime。 */
    private static Vec3 currentEarthSpacePos(ServerLevel space) {
        StarSystem sys = StarSystemRegistry.SOLAR_SYSTEM;
        double t = space.getGameTime();
        Vector3f sunPos = sys.stars().get(0).worldPositionAt(t, sys.worldPos());
        for (PlanetDef p : sys.planets())
            if (p.id().equals("earth")) {
                Vector3f ep = p.worldPositionAt(t, sunPos);
                return new Vec3(ep.x, ep.y, ep.z);
            }
        return new Vec3(sunPos.x, sunPos.y, sunPos.z);
    }

    /** 連船帶乘客搬到 target 維度的 arrive 位置。changeDimension 重建船,乘客卸載→傳送→重騎。 */
    private void doTransition(ServerLevel target, Vec3 arrive) {
        dimChanging = true;
        List<ServerPlayer> riders = new ArrayList<>();
        for (Entity p : ship.getPassengers()) if (p instanceof ServerPlayer sp) riders.add(sp);
        for (ServerPlayer sp : riders) sp.stopRiding();
        DimensionTransition transition = new DimensionTransition(
                target, arrive, Vec3.ZERO, ship.getYRot(), ship.getXRot(), DimensionTransition.DO_NOTHING);
        Entity moved = ship.changeDimension(transition);
        if (moved instanceof ShipEntity newShip) {
            newShip.travel.dimChanging = false;
            newShip.travel.transitionCooldown = TRANSITION_COOLDOWN; // 剛到,冷卻防立刻反彈回去
            for (ServerPlayer sp : riders) {
                sp.teleportTo(target, arrive.x, arrive.y + 1.0, arrive.z, java.util.Set.of(), sp.getYRot(), sp.getXRot());
                sp.startRiding(newShip, true);
            }
        }
    }

    /** 發射點存 NBT，跨維度/重啟保留（回程傳回這）。 */
    public void writeNbt(CompoundTag tag) {
        tag.putDouble("LaunchX", launchX);
        tag.putDouble("LaunchZ", launchZ);
    }

    public void readNbt(CompoundTag tag) {
        launchX = tag.getDouble("LaunchX");
        launchZ = tag.getDouble("LaunchZ");
    }
}
