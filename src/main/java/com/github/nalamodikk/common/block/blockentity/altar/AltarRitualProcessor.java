package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.register.ModRecipes;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 本源聚陣的儀式生命週期：啟動、每 tick 消耗魔力推進進度、魔力耗盡的警告與爆炸、
 * 完成時消耗材料並輸出產物。狀態 field 仍存放在 {@link AspectAltarBlockEntity}（NBT 與
 * 客戶端同步集中於 BE），本類別只持有 BE reference 操作那些 field。
 */
final class AltarRitualProcessor {

    private final AspectAltarBlockEntity altar;

    AltarRitualProcessor(AspectAltarBlockEntity altar) {
        this.altar = altar;
    }

    void tick() {
        Level level = altar.getLevel();
        if (level == null || altar.ritualMaxTick <= 0) return;
        if (altar.cachedRecipe == null) {
            Optional<RecipeHolder<AltarRecipe>> holder = findMatchingRecipe();
            if (holder.isEmpty()) { cancelRitual(); return; }
            altar.cachedRecipe = holder.get().value();
        }

        // 計算本 tick 應消耗的魔力
        int totalCost  = altar.cachedRecipe.getManaCost();
        int remaining  = totalCost - altar.manaConsumedSoFar;
        int perTick    = Math.max(1, totalCost / altar.ritualMaxTick);
        int toExtract  = Math.min(perTick, remaining);

        if (toExtract > 0) {
            int extracted = altar.getManaStorage().extractMana(toExtract, ManaAction.EXECUTE);
            if (extracted < toExtract) {
                // 魔力不足，進入警告狀態，儀式暫停
                altar.warningTick++;
                if (altar.warningTick == 1 || altar.warningTick % 20 == 0) altar.syncToClient();
                // 電弧警告特效：每 10 tick 噴一次電火花 + 閃電音效
                if (level instanceof ServerLevel sl && altar.warningTick % 10 == 0) {
                    Vec3 wc = Vec3.atCenterOf(altar.getBlockPos());
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            wc.x, wc.y + 0.5, wc.z,
                            25, 0.4, 0.5, 0.4, 0.18);
                    float pitch = 1.4f + level.random.nextFloat() * 0.6f;
                    sl.playSound(null, altar.getBlockPos(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                            SoundSource.BLOCKS, 0.5f, pitch);
                }
                if (altar.warningTick >= AspectAltarBlockEntity.WARNING_TICKS) {
                    AltarExplosionTrigger.trigger(level, altar.getBlockPos(), altar.upgradeTier,
                            altar.activatorUUID, altar.activePedestals);
                    cancelRitual();
                }
                return;
            }
            altar.manaConsumedSoFar += extracted;
            altar.warningTick = 0;
        }

        altar.ritualTick++;
        altar.progress = (float) altar.ritualTick / altar.ritualMaxTick;
        if (altar.ritualTick >= altar.ritualMaxTick) { completeRitual(); return; }
        if (altar.ritualTick % 10 == 0) altar.syncToClient();
    }

    Component tryActivate(UUID playerUUID) {
        if (!altar.isFormed())
            return Component.translatable("block.koniava.aspect_altar.not_formed");
        if (altar.active)
            return Component.translatable("block.koniava.aspect_altar.ritual_active");
        if (altar.centerPedestal == null || altar.centerPedestal.getHeldItem().isEmpty())
            return Component.translatable("block.koniava.aspect_altar.no_catalyst");

        Optional<RecipeHolder<AltarRecipe>> holder = findMatchingRecipe();
        if (holder.isEmpty())
            return Component.translatable("block.koniava.aspect_altar.no_recipe");

        AltarRecipe recipe = holder.get().value();
        altar.active = true;
        altar.ritualTick = 0;
        altar.ritualMaxTick = recipe.getProcessingTime();
        altar.manaConsumedSoFar = 0;
        altar.warningTick = 0;
        altar.cachedRecipe = recipe;
        altar.progress = 0f;
        altar.activatorUUID = playerUUID;
        altar.setChanged();
        altar.syncToClient();
        return Component.translatable("block.koniava.aspect_altar.ritual_started");
    }

    void cancelRitual() {
        altar.active = false;
        altar.progress = 0f;
        altar.ritualTick = 0;
        altar.ritualMaxTick = 0;
        altar.manaConsumedSoFar = 0;
        altar.warningTick = 0;
        altar.cachedRecipe = null;
        altar.setChanged();
        altar.syncToClient();
    }

    private void completeRitual() {
        Level level = altar.getLevel();
        if (level == null) return;

        AltarRecipe recipe = altar.cachedRecipe;
        if (recipe == null) { cancelRitual(); return; }

        // Validate ingredient indices BEFORE consuming anything — defensive guard against
        // findMatchedIndices diverging from the earlier matches() check
        List<AspectPedestalBlockEntity> nonCenter = altar.activePedestals.stream()
                .filter(p -> p != altar.centerPedestal).toList();
        List<ItemStack> nonCenterItems = nonCenter.stream()
                .map(AspectPedestalBlockEntity::getHeldItem).toList();
        int[] matched = recipe.findMatchedIndices(nonCenterItems);
        if (matched == null) { cancelRitual(); return; }

        if (altar.centerPedestal != null) altar.centerPedestal.consumeItem();
        for (int idx : matched) nonCenter.get(idx).consumeItem();

        // 完成粒子
        if (level instanceof ServerLevel sl) {
            Vec3 center = Vec3.atCenterOf(altar.getBlockPos());
            sl.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1, center.z, 40, 0.6, 0.6, 0.6, 0.12);
            sl.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.5, center.z, 30, 1.0, 1.0, 1.0, 0.3);
        }

        ItemStack result = recipe.getResult().copy();

        // 優先嘗試放入六面相鄰的容器，放不完的才掉落
        result = tryOutputToAdjacentContainer(result);
        if (!result.isEmpty()) {
            Vec3 drop = Vec3.atCenterOf(altar.getBlockPos()).add(0, 1.2, 0);
            ItemEntity entity = new ItemEntity(level, drop.x, drop.y, drop.z, result);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }

        altar.completionDuration = Math.max(AspectAltarBlockEntity.MIN_COMPLETION_TICKS,
                Math.min(AspectAltarBlockEntity.MAX_COMPLETION_TICKS, altar.ritualMaxTick / 3));
        altar.completionAnimTick = altar.completionDuration;

        if (level instanceof ServerLevel serverLevel && altar.activatorUUID != null) {
            ServerPlayer sp = serverLevel.getServer().getPlayerList().getPlayer(altar.activatorUUID);
            if (sp != null) {
                AdvancementHolder adv = serverLevel.getServer().getAdvancements()
                        .get(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "first_altar_ritual"));
                if (adv != null) {
                    var prog = sp.getAdvancements().getOrStartProgress(adv);
                    if (!prog.isDone()) {
                        for (String criterion : prog.getRemainingCriteria()) {
                            sp.getAdvancements().award(adv, criterion);
                        }
                    }
                }
            }
        }

        altar.active = false;
        altar.progress = 0f;
        altar.ritualTick = 0;
        altar.ritualMaxTick = 0;
        altar.manaConsumedSoFar = 0;
        altar.warningTick = 0;
        altar.cachedRecipe = null;
        altar.setChanged();
        altar.syncToClient();

        level.playSound(null, altar.getBlockPos(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS, 1.0f, 1.2f);
    }

    private ItemStack tryOutputToAdjacentContainer(ItemStack stack) {
        Level level = altar.getLevel();
        for (Direction dir : Direction.values()) {
            if (stack.isEmpty()) break;
            IItemHandler handler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, altar.getBlockPos().relative(dir), dir.getOpposite());
            if (handler == null) continue;
            stack = ItemHandlerHelper.insertItem(handler, stack, false);
        }
        return stack;
    }

    private Optional<RecipeHolder<AltarRecipe>> findMatchingRecipe() {
        Level level = altar.getLevel();
        if (level == null || altar.centerPedestal == null) return Optional.empty();
        ItemStack catalyst = altar.centerPedestal.getHeldItem();
        List<ItemStack> ingredients = altar.activePedestals.stream()
                .filter(p -> p != altar.centerPedestal)
                .map(AspectPedestalBlockEntity::getHeldItem)
                .toList();
        AltarRecipe.AltarInput input = new AltarRecipe.AltarInput(catalyst, ingredients);
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.ALTAR_TYPE.get())
                .stream()
                .filter(h -> h.value().matches(input, level) && h.value().getMinTier() <= altar.upgradeTier)
                .findFirst();
    }
}
