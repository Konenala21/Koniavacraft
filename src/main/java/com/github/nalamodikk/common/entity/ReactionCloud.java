package com.github.nalamodikk.common.entity;

import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 反應留在地上的危險雲。跟 vanilla 滯留藥水雲不同:**不會影響 owner(施放者)**。
 *
 * 作法:vanilla {@link AreaEffectCloud#tick()} 只把效果套在「不在 victims map 裡」的實體,套過的會被
 * 記進 victims 一段時間跳過。每 tick 先把 owner 以「永不到期」塞進 victims,owner 就永遠被跳過。
 * victims 是 private,用反射拿;欄位名在 NeoForge 正式 mapping(dev/prod 一致)下穩定為 "victims"。
 *
 * 註:沿用 vanilla 的 AREA_EFFECT_CLOUD entity type(沒另註冊),所以若世界在雲存活期間存檔重載,
 * 會還原成 vanilla 雲(失去 owner 跳過)。對戰鬥用的短命雲(~120-160 tick)可接受。
 */
public class ReactionCloud extends AreaEffectCloud {

    private static Field VICTIMS;

    public ReactionCloud(Level level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Override
    public void tick() {
        skipOwner();
        super.tick();
    }

    @SuppressWarnings("unchecked")
    private void skipOwner() {
        LivingEntity owner = getOwner();
        if (owner == null) return;
        try {
            if (VICTIMS == null) {
                VICTIMS = AreaEffectCloud.class.getDeclaredField("victims");
                VICTIMS.setAccessible(true);
            }
            ((Map<Entity, Integer>) VICTIMS.get(this)).put(owner, Integer.MAX_VALUE); // 永不到期 -> 永遠跳過 owner
        } catch (Exception ignored) {
            // 反射失敗就退回 vanilla 行為(會影響 owner),不讓它 crash
        }
    }
}
