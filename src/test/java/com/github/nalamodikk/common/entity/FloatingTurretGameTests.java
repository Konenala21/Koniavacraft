package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for FloatingTurretEntity clone-mode immunity (Phase 1 refactor verification).
 *
 * 驗證 CloneTurretController 抽出後行為與原本一致：
 * - 玩家無法傷害 clone 砲
 * - 玩家 ray-cast 無法選中 clone 砲（isPickable=false）
 * - 非玩家來源（環境傷害）仍能傷害 clone 砲
 * - 一般玩家砲（無 cloneCtrl）的 hurt 路徑不受影響
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class FloatingTurretGameTests {

    private static final BlockPos TURRET_POS = new BlockPos(1, 2, 1);
    private static final BlockPos OWNER_POS = new BlockPos(2, 2, 1);

    /** 建一個 FloatingTurretEntity 並 setup 成 clone 砲，owner 用 zombie 當 fake LivingEntity。 */
    private static FloatingTurretEntity spawnCloneTurret(GameTestHelper helper) {
        Zombie owner = helper.spawn(EntityType.ZOMBIE, OWNER_POS);
        FloatingTurretEntity turret = helper.spawn(ModEntities.FLOATING_TURRET.get(), TURRET_POS);
        turret.setupAsCloneTurret(owner, ItemStack.EMPTY, 0);
        return turret;
    }

    // -------------------------------------------------------------------------
    // 1. 玩家近戰：clone 砲血量完全不變
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void cloneTurret_immuneToPlayerMelee(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            FloatingTurretEntity turret = spawnCloneTurret(helper);
            float fullHp = turret.getMaxHealth();
            Player fakePlayer = helper.makeMockPlayer(GameType.SURVIVAL);

            DamageSource src = helper.getLevel().damageSources().playerAttack(fakePlayer);
            turret.hurt(src, 10.0f);

            helper.assertTrue(
                    turret.getHealth() == fullHp,
                    "clone turret should ignore player melee, got " + turret.getHealth() + "/" + fullHp);
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 2. 玩家為 source 的箭傷：clone 砲血量不變
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void cloneTurret_immuneToPlayerProjectile(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            FloatingTurretEntity turret = spawnCloneTurret(helper);
            float fullHp = turret.getMaxHealth();
            Player fakePlayer = helper.makeMockPlayer(GameType.SURVIVAL);

            // 模擬箭傷：DamageTypes.ARROW + player 為 causing entity（source.getEntity() == player）
            DamageSource src = new DamageSource(
                    helper.getLevel().registryAccess()
                            .registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(DamageTypes.ARROW),
                    null, fakePlayer);
            turret.hurt(src, 10.0f);

            helper.assertTrue(
                    turret.getHealth() == fullHp,
                    "clone turret should ignore player arrow, got " + turret.getHealth() + "/" + fullHp);
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 3. 環境傷害（無 entity 來源）：clone 砲血量確實有掉
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void cloneTurret_takesEnvironmentalDamage(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            FloatingTurretEntity turret = spawnCloneTurret(helper);
            float fullHp = turret.getMaxHealth();

            DamageSource src = helper.getLevel().damageSources().outOfBorder();
            turret.hurt(src, 10.0f);

            helper.assertTrue(
                    turret.getHealth() < fullHp,
                    "clone turret should take environmental damage, hp unchanged at " + turret.getHealth());
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 4. clone 模式 isPickable 為 false（ray-cast 跳過）
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void cloneTurret_isPickableFalse(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            FloatingTurretEntity cloneTurret = spawnCloneTurret(helper);
            FloatingTurretEntity playerTurret = helper.spawn(
                    ModEntities.FLOATING_TURRET.get(), new BlockPos(3, 2, 1));

            helper.assertTrue(!cloneTurret.isPickable(),
                    "clone turret should be non-pickable");
            helper.assertTrue(playerTurret.isPickable(),
                    "non-clone turret should remain pickable");
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 5. 沒有 cloneCtrl 的玩家砲：hurt 路徑維持原行為（怪物可以打到）
    //    重構不應誤動玩家路徑。
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void playerTurret_hurtFromMobStillWorks(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            FloatingTurretEntity turret = helper.spawn(
                    ModEntities.FLOATING_TURRET.get(), TURRET_POS);
            float fullHp = turret.getMaxHealth();
            Zombie attacker = helper.spawn(EntityType.ZOMBIE, OWNER_POS);

            DamageSource src = helper.getLevel().damageSources().mobAttack(attacker);
            turret.hurt(src, 5.0f);

            helper.assertTrue(turret.getHealth() < fullHp,
                    "non-clone turret should take mob damage, hp unchanged at " + turret.getHealth());
            helper.succeed();
        });
    }
}
