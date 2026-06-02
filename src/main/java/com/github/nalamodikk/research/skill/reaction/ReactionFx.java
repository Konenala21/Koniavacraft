package com.github.nalamodikk.research.skill.reaction;

import com.github.nalamodikk.research.skill.SkillEffectOp;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * 反應的「粒子簽名」(Tier A 視覺):每個反應命中時噴一組專屬粒子,讓 85 種反應彼此一眼可分。
 * 純 server 端 sendParticles,所有客戶端都看得到,不需要 shader(shader 是之後的 Tier B)。
 *
 * 形狀工具:burst(四散)、up(上噴)、ring(環)、cloud(低矮大範圍)、implode(向心吸入)。
 * sendParticles 在 count=0 時把 (dx,dy,dz) 當「速度向量」用,implode/directional 靠這個。
 */
public final class ReactionFx {

    private ReactionFx() {}

    public static void spawn(ServerLevel level, LivingEntity t, SkillEffectOp op) {
        double x = t.getX(), y = t.getY() + t.getBbHeight() * 0.5, z = t.getZ();
        switch (op) {
            // ── 英雄反應:專屬形狀 ──
            case IMPLOSION, MAGNETIC_STORM -> implode(level, ParticleTypes.PORTAL, x, y, z, 1.8, 28);
            case ANNIHILATION -> { ring(level, ParticleTypes.END_ROD, x, y, z, 1.3, 20); ring(level, ParticleTypes.SMOKE, x, y, z, 0.7, 16); }
            case MAELSTROM -> ring(level, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 1.6, 24);
            case SUPERHEATED_PLASMA -> { burst(level, ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 22); burst(level, ParticleTypes.ELECTRIC_SPARK, x, y, z, 22); }
            case THERMONUCLEAR -> { ring(level, ParticleTypes.FLAME, x, y, z, 2.2, 28); burst(level, ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1); }
            case HOLY_NOVA, RADIANT_SURGE -> { ring(level, ParticleTypes.END_ROD, x, y, z, 1.6, 24); burst(level, ParticleTypes.FLASH, x, y, z, 1); }

            // ── 火 ──
            case THERMAL_SHOCK, SOULFIRE, MELTDOWN, SMELT, WILDFIRE, CAUTERIZE, SPITE_BOLT ->
                    burst(level, ParticleTypes.FLAME, x, y, z, 28);
            case SOLAR_FLARE, COMBUSTION -> burst(level, ParticleTypes.LAVA, x, y, z, 24);
            case ERUPTION -> up(level, ParticleTypes.LAVA, x, y, z, 26);
            case THERMION -> { burst(level, ParticleTypes.FLAME, x, y, z, 14); burst(level, ParticleTypes.ELECTRIC_SPARK, x, y, z, 16); }

            // ── 爆炸 ──
            case KINETIC_BLAST, HYDROGEN_BLAST, OVERLOAD, VOID_SURGE, BIOHAZARD ->
                    { burst(level, ParticleTypes.EXPLOSION, x, y, z, 4); burst(level, ParticleTypes.LARGE_SMOKE, x, y, z, 14); }

            // ── 冰 ──
            case SHATTER, FROSTBIND, DEEP_FREEZE, WITHER_FROST, CRYO_VENOM, FROST_ETCH, MANA_FREEZE ->
                    burst(level, ParticleTypes.SNOWFLAKE, x, y, z, 28);
            case CRYOCRYSTAL -> { burst(level, ParticleTypes.SNOWFLAKE, x, y, z, 16); burst(level, ParticleTypes.CRIT, x, y, z, 14); }
            case PRISM -> ring(level, ParticleTypes.END_ROD, x, y, z, 1.0, 14);

            // ── 電 ──
            case SUPERCONDUCT, PHOTOELECTRIC, ELECTROLYSIS, ELECTRO_CORROSION, EMP, BIOELECTRIC, DEFIBRILLATE ->
                    burst(level, ParticleTypes.ELECTRIC_SPARK, x, y, z, 26);

            // ── 毒 / 酸 (綠) ──
            case TOXIC_BURN, STRONG_ACID, PLAGUE, SPORE_BURST, TOXIC_BURST, TAINTED_WATER, WILT, HEAVY_METAL,
                 ARCANE_VENOM, VIRULENCE, MIASMA, ACID_SPLASH, ACID_WASH, CORRODE, MANA_CORROSION, RUST,
                 DECAY, SEARING_ETCH ->
                    cloud(level, ParticleTypes.ITEM_SLIME, x, y, z);

            // ── 光 ──
            case SANCTIFY, MIRROR_FLASH, HOLY_WATER, PURGE, LIGHT_PRESSURE, PHOTOSYNTHESIS, MANA_SPRING ->
                    burst(level, ParticleTypes.END_ROD, x, y, z, 22);

            // ── 暗 ──
            case DEATH_SIPHON, ROT_BLOOM, CURSED_METAL -> burst(level, ParticleTypes.SCULK_SOUL, x, y, z, 22);

            // ── 治療 / 增益 (心 + 綠閃) ──
            case LIVING_SPRING, NEUTRALIZE, FLOURISH, SYMBIOSIS, ARCANE_MENDING, IRON_WARD, HIBERNATE ->
                    { burst(level, ParticleTypes.HEART, x, y, z, 8); burst(level, ParticleTypes.HAPPY_VILLAGER, x, y, z, 12); }

            // ── 水 / 力 ──
            case TORRENT -> burst(level, ParticleTypes.SPLASH, x, y, z, 26);
            case OVERGROWTH, VINE_LASH, PETRIFY, BLIGHT -> burst(level, ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 20);
            case STEAM_BURST, ASH_STORM -> cloud(level, ParticleTypes.CLOUD, x, y, z);
            case SHRAPNEL -> burst(level, ParticleTypes.CRIT, x, y, z, 24);
            case RESONANT_PULSE -> ring(level, ParticleTypes.POOF, x, y, z, 1.4, 18);
            case ELEMENTAL_STORM -> { burst(level, ParticleTypes.FLAME, x, y, z, 10); burst(level, ParticleTypes.SNOWFLAKE, x, y, z, 10); burst(level, ParticleTypes.ELECTRIC_SPARK, x, y, z, 10); }

            default -> burst(level, dust(0.8f, 0.8f, 0.9f), x, y, z, 16);
        }
    }

    // ── 形狀工具 ──
    private static void burst(ServerLevel level, ParticleOptions p, double x, double y, double z, int count) {
        level.sendParticles(p, x, y, z, count, 0.45, 0.45, 0.45, 0.06);
    }

    private static void up(ServerLevel level, ParticleOptions p, double x, double y, double z, int count) {
        level.sendParticles(p, x, y, z, count, 0.25, 0.5, 0.25, 0.2);
    }

    private static void cloud(ServerLevel level, ParticleOptions p, double x, double y, double z) {
        level.sendParticles(p, x, y - 0.3, z, 40, 1.7, 0.4, 1.7, 0.01);
    }

    private static void ring(ServerLevel level, ParticleOptions p, double x, double y, double z, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2 * i) / points;
            level.sendParticles(p, x + Math.cos(a) * radius, y, z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** 向心:在環上生成、速度指向中心(count=0 時 dx/dy/dz 當速度)。 */
    private static void implode(ServerLevel level, ParticleOptions p, double x, double y, double z, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2 * i) / points;
            double px = x + Math.cos(a) * radius, pz = z + Math.sin(a) * radius;
            level.sendParticles(p, px, y, pz, 0, (x - px) * 0.6, 0, (z - pz) * 0.6, 0.5);
        }
    }

    private static DustParticleOptions dust(float r, float g, float b) {
        return new DustParticleOptions(new Vector3f(r, g, b), 1.2f);
    }
}
