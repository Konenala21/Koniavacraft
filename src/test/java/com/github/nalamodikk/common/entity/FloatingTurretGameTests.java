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

    // -------------------------------------------------------------------------
    // 6. 多人：兩個玩家輪流打同一個 clone 砲 → 全部 blocked
    //    驗 immunity 不是基於攻擊者身分，而是基於「攻擊源頭是 Player」這個一般性條件
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void cloneTurret_immuneToBothPlayersInMultiplayer(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            FloatingTurretEntity turret = spawnCloneTurret(helper);
            float fullHp = turret.getMaxHealth();
            Player p1 = helper.makeMockPlayer(GameType.SURVIVAL);
            Player p2 = helper.makeMockPlayer(GameType.SURVIVAL);

            turret.hurt(helper.getLevel().damageSources().playerAttack(p1), 10.0f);
            turret.hurt(helper.getLevel().damageSources().playerAttack(p2), 10.0f);
            // 第二個 player 用箭傷
            DamageSource arrowFromP2 = new DamageSource(
                    helper.getLevel().registryAccess()
                            .registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(DamageTypes.ARROW),
                    null, p2);
            turret.hurt(arrowFromP2, 10.0f);

            helper.assertTrue(turret.getHealth() == fullHp,
                    "clone turret hp changed under multi-player attacks: " + turret.getHealth() + "/" + fullHp);
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 7. 多人：兩個獨立 owner + 兩個 clone 砲，cross-attack 全部 blocked
    //    驗每個 turret 的 cloneCtrl 是獨立的，不會因為共用 controller 類別而互相干擾
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void twoCloneTurrets_independentOwners_bothImmuneToPlayers(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            // owner A + turret A
            Zombie ownerA = helper.spawn(EntityType.ZOMBIE, OWNER_POS);
            FloatingTurretEntity turretA = helper.spawn(ModEntities.FLOATING_TURRET.get(), TURRET_POS);
            turretA.setupAsCloneTurret(ownerA, ItemStack.EMPTY, 0);
            float fullA = turretA.getMaxHealth();

            // owner B + turret B
            Zombie ownerB = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1));
            FloatingTurretEntity turretB = helper.spawn(
                    ModEntities.FLOATING_TURRET.get(), new BlockPos(5, 2, 1));
            turretB.setupAsCloneTurret(ownerB, ItemStack.EMPTY, 0);
            float fullB = turretB.getMaxHealth();

            Player p1 = helper.makeMockPlayer(GameType.SURVIVAL);
            Player p2 = helper.makeMockPlayer(GameType.SURVIVAL);

            // p1 attack turret A, p2 attack turret B, p1 attack turret B, p2 attack turret A
            turretA.hurt(helper.getLevel().damageSources().playerAttack(p1), 10.0f);
            turretB.hurt(helper.getLevel().damageSources().playerAttack(p2), 10.0f);
            turretB.hurt(helper.getLevel().damageSources().playerAttack(p1), 10.0f);
            turretA.hurt(helper.getLevel().damageSources().playerAttack(p2), 10.0f);

            helper.assertTrue(turretA.getHealth() == fullA,
                    "turretA hp changed: " + turretA.getHealth() + "/" + fullA);
            helper.assertTrue(turretB.getHealth() == fullB,
                    "turretB hp changed: " + turretB.getHealth() + "/" + fullB);

            // 兩個 turret 都仍為 non-pickable
            helper.assertTrue(!turretA.isPickable(), "turretA should remain non-pickable");
            helper.assertTrue(!turretB.isPickable(), "turretB should remain non-pickable");
            helper.succeed();
        });
    }

    // -------------------------------------------------------------------------
    // 8. 多人：第一個 player 死亡後，第二個 player 仍然無法傷害 clone 砲
    //    驗免疫不依賴第一次攻擊者存活
    // -------------------------------------------------------------------------
    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void cloneTurret_immuneAfterFirstAttackerDies(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            FloatingTurretEntity turret = spawnCloneTurret(helper);
            float fullHp = turret.getMaxHealth();
            Player p1 = helper.makeMockPlayer(GameType.SURVIVAL);
            Player p2 = helper.makeMockPlayer(GameType.SURVIVAL);

            // p1 attack → blocked
            turret.hurt(helper.getLevel().damageSources().playerAttack(p1), 10.0f);
            // p1 dies
            p1.setHealth(0);
            // p2 attack → still blocked
            turret.hurt(helper.getLevel().damageSources().playerAttack(p2), 10.0f);

            helper.assertTrue(turret.getHealth() == fullHp,
                    "clone turret took damage after first attacker died: " + turret.getHealth() + "/" + fullHp);
            helper.succeed();
        });
    }
}
