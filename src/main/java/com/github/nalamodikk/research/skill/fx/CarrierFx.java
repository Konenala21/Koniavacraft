package com.github.nalamodikk.research.skill.fx;

import com.github.nalamodikk.common.network.packet.client.skill.CarrierFxPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 載體視覺(粒子)獨立層。gameplay({@code SkillCompiler})只負責算命中與套效果,
 * 把「長什麼樣」全部交給這裡 → 改載體外觀不必動 gameplay。
 *
 * 第 1 步只搬現有的 server 端粒子(行為不變)。之後要上 shader 的載體(衝擊波/軌道),
 * 在這層改成同時送 CarrierFxPacket 給 client 觸發對應 shader,gameplay 仍只呼叫這裡一行。
 */
public final class CarrierFx {

    private CarrierFx() {}

    /** 鋒刃:前方扇形 sweep 弧。 */
    public static void slash(ServerLevel level, Vec3 eye, Vec3 look) {
        Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
        for (int i = 0; i <= 14; i++) {
            double a = (i / 14.0 - 0.5) * Math.PI * 0.75;
            Vec3 dir = look.scale(Math.cos(a)).add(side.scale(Math.sin(a))).normalize();
            Vec3 p = eye.add(dir.scale(2.5));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** 管道:沿射線一串 END_ROD 光點。 */
    public static void beam(ServerLevel level, Vec3 start, Vec3 end) {
        for (int s = 0; s <= 24; s++) {
            Vec3 m = start.lerp(end, s / 24.0);
            level.sendParticles(ParticleTypes.END_ROD, m.x, m.y, m.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** 重力:向心 PORTAL 漩渦。 */
    public static void field(ServerLevel level, Vec3 center, double radius) {
        for (int i = 0; i < 40; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = radius * Math.sqrt(level.random.nextDouble());
            Vec3 p = center.add(Math.cos(a) * r, level.random.nextDouble() * 2 - 1, Math.sin(a) * r);
            level.sendParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 1,
                    (center.x - p.x) * 0.2, (center.y - p.y) * 0.2, (center.z - p.z) * 0.2, 0.3);
        }
    }

    /** 連鎖跳躍的電火花(從 from 跳到 le 時在 le 身上爆)。 */
    public static void chainSpark(ServerLevel level, LivingEntity le) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
    }

    /** 兌 NOVA:中心爆 + 一圈橫掃弧。 */
    public static void nova(ServerLevel level, Vec3 center, double radius) {
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        for (int i = 0; i < 24; i++) {
            double a = (Math.PI * 2 * i) / 24;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** 坤 SHOCKWAVE:地面擴散環。server 粒子當底,另送 packet 觸發 client 的 shader 擴散環。 */
    public static void shockwave(ServerLevel level, Vec3 center, double radius) {
        for (double rr : new double[]{radius * 0.6, radius}) {
            for (int i = 0; i < 36; i++) {
                double a = (Math.PI * 2 * i) / 36;
                level.sendParticles(ParticleTypes.POOF,
                        center.x + Math.cos(a) * rr, center.y - 0.4, center.z + Math.sin(a) * rr, 1, 0.0, 0.0, 0.0, 0.02);
            }
        }
        CarrierFxPacket.send(level, center, CarrierFxType.SHOCKWAVE); // shader 擴散環
    }

    /** 艮 EARTHWAVE:沿地面往前的一條碎裂帶。 */
    public static void earthwave(ServerLevel level, Vec3 origin, Vec3 dir, double range) {
        for (int s = 0; s <= 16; s++) {
            double t = s / 16.0;
            Vec3 p = origin.add(dir.scale(range * t));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y + 0.1, p.z, 3, 0.3, 0.1, 0.3, 0.05);
        }
    }
}
