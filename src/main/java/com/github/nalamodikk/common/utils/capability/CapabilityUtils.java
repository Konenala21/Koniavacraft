package com.github.nalamodikk.common.utils.capability;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;


public class CapabilityUtils {

    /**
     * 嘗試從鄰近的方塊查詢魔力能力（Mana Capability）。
     * 會自動將查詢方向反轉，用於向鄰居方塊取得其「面對本方塊」方向的能力。
     *
     * <p><b>使用時機：</b>
     * <ul>
     *   <li>機器從周圍鄰居吸收魔力</li>
     *   <li>魔力輸送器（如魔力導管）查詢對象能否接收/提供魔力</li>
     * </ul>
     *
     * @param level 世界實例
     * @param neighborPos 鄰居的方塊位置
     * @param fromDirection 本方塊朝向鄰居的方向（會自動轉為鄰居的對應面）
     * @return 鄰居的魔力處理器，若該方向無能力則為 null
     *
     * 從鄰近方塊查詢 mana 能力（方向會自動反向）
     */
    @Nullable
    public static IUnifiedManaHandler getNeighborMana(Level level, BlockPos neighborPos, Direction fromDirection) {
        return level.getCapability(ModCapabilities.MANA, neighborPos, fromDirection.getOpposite());
    }

    /**
     * 安全 Optional 包裝版
     */
    public static Optional<IUnifiedManaHandler> getNeighborManaOptional(Level level, BlockPos neighborPos, Direction fromDirection) {
        return Optional.ofNullable(getNeighborMana(level, neighborPos, fromDirection));
    }



    /**
     * 從鄰近方塊查詢 energy 能力（方向會自動反向）
     */
    @Nullable
    public static IEnergyStorage getNeighborEnergy(Level level, BlockPos neighborPos, Direction fromDirection) {
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, fromDirection.getOpposite());
    }

    /**
     * 從鄰近方塊查詢 item handler 能力（方向會自動反向）
     */
    @Nullable
    public static IItemHandler getNeighborItemHandler(Level level, BlockPos neighborPos, Direction fromDirection) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, fromDirection.getOpposite());
    }


    /**
     * 嘗試從特定位置查詢魔力能力。
     * 若該方向失敗，將 fallback 查詢 null context（適用於 Void context 的能力）。
     *
     * <p><b>使用時機：</b>
     * <ul>
     *   <li>從自己的方塊中查詢內部魔力能力</li>
     *   <li>註冊時使用 {@link BlockCapability#create} 時 context 為 Void</li>
     * </ul>
     *
     * @param level 世界實例
     * @param pos 查詢方塊位置
     * @param dir 查詢方向，可為 null
     * @return 查詢到的魔力能力或 null
     */

    public static @Nullable IUnifiedManaHandler getMana(Level level, BlockPos pos, @Nullable Direction dir) {
        // 優先查詢給定方向
        IUnifiedManaHandler handler = level.getCapability(ModCapabilities.MANA, pos, dir);

        // 如果失敗，嘗試用 null fallback 查詢（有些註冊用 createVoid）
        if (handler == null) {
            handler = level.getCapability(ModCapabilities.MANA, pos, null);
        }

        return handler;
    }

    /**
     * 安全版本的 {@link #getMana}，使用 Optional 包裝結果。
     *
     * <p><b>使用時機：</b>
     * <ul>
     *   <li>想用 Optional 鏈式寫法處理魔力流動</li>
     *   <li>搭配 lambda 使用（如 ifPresent, map 等）</li>
     * </ul>
     *
     * @param level 世界實例
     * @param pos 查詢方塊位置
     * @param dir 查詢方向
     * @return Optional 包裝的魔力能力
     */
    public static Optional<IUnifiedManaHandler> getManaOptional(Level level, BlockPos pos, @Nullable Direction dir) {
        return Optional.ofNullable(getMana(level, pos, dir));
    }

    /**
     * 註冊具備方向性的 BlockCapability，對應一個或多個 BlockEntity 類別。
     *
     * <p>這適用於需要 Direction 作為 context 的能力，例如 NeoForge 內建的：
     * <ul>
     *   <li>{@code Capabilities.ItemHandler.BLOCK}</li>
     *   <li>{@code Capabilities.EnergyStorage.BLOCK}</li>
     * </ul>
     *
     * <p><b>使用時機：</b>
     * <ul>
     *   <li>需要根據方塊的不同方向提供不同邏輯的能力</li>
     *   <li>想簡化重複的註冊寫法</li>
     * </ul>
     *
     * @param event 能力註冊事件
     * @param capability 要註冊的 BlockCapability
     * @param providerFunc 根據 BlockEntity 與方向提供能力實例的邏輯
     * @param types 要註冊此能力的 BlockEntity 類別（可多個）
     * @param <T> 能力類型
     * @param <BE> BlockEntity 類型
     */

    // 支援 Direction 作為 context 的能力（像是 ITEM_HANDLER）
    @SafeVarargs
    public static <T, BE extends BlockEntity> void registerBlockEntities(
            RegisterCapabilitiesEvent event,
            BlockCapability<T, net.minecraft.core.Direction> capability,
            BiFunction<BE, net.minecraft.core.Direction, T> providerFunc,
            BlockEntityType<? extends BE>... types
    ) {
        ICapabilityProvider<BE, net.minecraft.core.Direction, T> provider = providerFunc::apply;
        for (BlockEntityType<? extends BE> type : types) {
            event.registerBlockEntity(capability, type, provider);
        }
    }


    /**
     * 註冊不具方向性的 BlockCapability（Void context），如自訂的魔力儲存能力。
     *
     * <p><b>使用時機：</b>
     * <ul>
     *   <li>能力與方向無關（如單一儲存空間）</li>
     *   <li>你在 Capability 註冊使用 {@code Void.class} 作為 context 類型</li>
     * </ul>
     *
     * @param event 註冊事件
     * @param capability 欲註冊的 BlockCapability
     * @param providerFunc 提供能力實例的函式（不考慮方向）
     * @param types 需要註冊此能力的方塊實體類型
     * @param <T> 能力介面
     * @param <BE> 方塊實體類別
     */

    // 支援 Void 作為 context 的能力（像是自訂 mana 能力）
    @SafeVarargs
    public static <T, BE extends BlockEntity> void registerBlockEntitiesVoidContext(
            RegisterCapabilitiesEvent event,
            BlockCapability<T, Void> capability,
            Function<BE, T> providerFunc,
            BlockEntityType<? extends BE>... types
    ) {
        ICapabilityProvider<BE, Void, T> provider = (be, unused) -> providerFunc.apply(be);
        for (BlockEntityType<? extends BE> type : types) {
            event.registerBlockEntity(capability, type, provider);
        }
    }


    /***
     *   獲取 娜拉系統的data
     */

//    public static Optional<INaraData> getNaraData(Player player) {
//        return Optional.ofNullable(player.getCapability(ModCapability.NARA, null));
//
//    }

}
