package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.network.packet.client.turret.TrainingDummyStatsPacket;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 訓練假人：站著給玩家測傷害用，不能被推、不會死。
 *
 * 戰鬥 session 依攻擊者 UUID 分開記錄（累積傷害 / 命中數 / 起始與最後命中 tick）。玩家一打到就
 * 開始計時，超過 {@link #SESSION_TIMEOUT_TICKS}（10 秒）沒再打就自動結束該 session 並送出結算。
 * 傷害累積由 {@link com.github.nalamodikk.common.event.TrainingDummySessionHandler} 在
 * LivingDamageEvent.Post 用最終傷害值呼叫 {@link #recordHit}，保證跟畫面上的浮動傷害數字一致。
 */
public class TrainingDummyEntity extends Mob {

    // 沒人能一擊打掉這麼多，加上每 tick 回滿，等於永不死
    private static final float DUMMY_MAX_HEALTH = 1_000_000.0F;
    // 10 秒沒再打就退出戰鬥狀態
    public static final int SESSION_TIMEOUT_TICKS = 200;

    private static final class Session {
        double totalDamage = 0;
        int hitCount = 0;
        final long startTick;
        long lastHitTick;

        Session(long now) {
            this.startTick = now;
            this.lastHitTick = now;
        }
    }

    private final Map<UUID, Session> sessions = new HashMap<>();

    public TrainingDummyEntity(EntityType<? extends TrainingDummyEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, DUMMY_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0); // 推不動、打不退
    }

    @Override
    protected void registerGoals() {
        // 無 AI：站著不動
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // 潛行 + 右鍵：收回成物品（攻擊保留給測傷害，所以收回走潛行右鍵）
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    ItemStack stack = new ItemStack(ModItems.TRAINING_DUMMY.get());
                    if (!player.addItem(stack)) player.drop(stack, false);
                }
                this.discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // 無視環境傷害（岩漿 / 火 / 仙人掌 / 掉落 / 溺水等）：這些來源沒有攻擊者（getEntity()==null）。
    // 只對有攻擊者的傷害（玩家近戰 / 投射物 / 浮游砲）反應，純粹用來測傷害。
    // 每次受傷取消無敵冷卻，讓連打（含浮游砲連發）每一下都算進 DPS。
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() == null) return false;
        this.invulnerableTime = 0;
        boolean result = super.hurt(source, amount);
        this.setHealth(this.getMaxHealth()); // 永遠回滿，不會死
        return result;
    }

    // 不在岩漿 / 火裡冒火燃燒
    @Override
    public boolean fireImmune() {
        return true;
    }

    /** 由傷害事件 handler 用最終傷害值呼叫，累積 session 並把更新推給該玩家。 */
    public void recordHit(ServerPlayer player, float finalDamage) {
        if (finalDamage <= 0) return;
        long now = level().getGameTime();
        Session s = sessions.computeIfAbsent(player.getUUID(), uuid -> new Session(now));
        s.totalDamage += finalDamage;
        s.hitCount++;
        s.lastHitTick = now;
        sendStats(player, s, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (sessions.isEmpty()) return;

        long now = level().getGameTime();
        Iterator<Map.Entry<UUID, Session>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            Session s = entry.getValue();
            if (now - s.lastHitTick > SESSION_TIMEOUT_TICKS) {
                if (serverLevel.getPlayerByUUID(entry.getKey()) instanceof ServerPlayer p) {
                    sendStats(p, s, true);
                }
                it.remove();
            }
        }
    }

    // 實體被移除（拿起 / 死亡 / 卸載）時，把所有進行中的 session 收尾，
    // 否則 client HUD 收不到結束封包，計時器會一直跑。
    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel serverLevel && !sessions.isEmpty()) {
            for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
                if (serverLevel.getPlayerByUUID(entry.getKey()) instanceof ServerPlayer p) {
                    sendStats(p, entry.getValue(), true);
                }
            }
            sessions.clear();
        }
        super.remove(reason);
    }

    private void sendStats(ServerPlayer player, Session s, boolean ended) {
        long now = level().getGameTime();
        int durationTicks = (int) (s.lastHitTick - s.startTick); // 結算用最後命中時間
        if (!ended) durationTicks = (int) (now - s.startTick);   // 進行中用當下
        PacketDistributor.sendToPlayer(player,
                new TrainingDummyStatsPacket(ended, s.totalDamage, s.hitCount, durationTicks));
    }
}
