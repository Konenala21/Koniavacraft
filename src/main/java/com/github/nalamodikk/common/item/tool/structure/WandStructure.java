package com.github.nalamodikk.common.item.tool.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 一種「結構建造杖」能蓋的多方塊結構。新增結構 = 實作這個 + 在 {@link WandStructures} register，
 * 杖本體(StructureBuildWandItem)與飛船上的同步邏輯都不用改。祭壇是第一個實作({@link AltarWandStructure})。
 */
public interface WandStructure {

    /** 被點的方塊 + 它的 BE 是不是此結構的錨點(例如祭壇 BE)。 */
    boolean matches(BlockState clicked, @Nullable BlockEntity be);

    /** 此結構在目前狀態下需要的所有方塊(動態:祭壇看 isFormed / 升級 tier)。空 = 沒有要蓋的了。 */
    List<RequiredBlock> required(Level level, BlockPos anchor, BlockEntity be);

    /** 有放下方塊 / 全部到位後的 hook(例如 altar.refreshUpgradeTier())。 */
    default void onPlaced(Level level, BlockPos anchor, BlockEntity be) {}

    /** 全部到位(沒有缺、沒有被擋)時顯示給玩家的訊息。null = 不顯示。在 onPlaced 之後呼叫。 */
    @Nullable
    default Component completeMessage(Level level, BlockPos anchor, BlockEntity be) {
        return null;
    }
}
