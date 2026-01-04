package com.github.nalamodikk.common.coreapi.machine.logic.IO;


import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ManaInputHelper {

    /**
     * 嘗試從鄰近方塊抽取魔力，傳入裝置的位置、自己的儲存槽與最大輸入量。
     *
     * @param level        當前世界
     * @param pos          當前方塊位置
     * @param selfStorage  自身魔力儲存槽
     * @param maxAmount    每次最大抽取魔力量
     */
    public static void tryPullMana(Level level, BlockPos pos, IUnifiedManaHandler selfStorage, int maxAmount) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            // 檢查是否實作 IHasMana
            if (!(neighbor instanceof IHasMana manaProvider)) continue;

            // 檢查是否為 IConfigurableBlock 並且允許從該方向輸入
            if (neighbor instanceof IConfigurableBlock configurable) {
                if (!configurable.isOutput(dir.getOpposite())) continue;
            }

            // 取得對方 UnifiedManaHandler
            IUnifiedManaHandler otherStorage = manaProvider.getManaStorage();
            if (otherStorage == null || otherStorage.getManaStored() <= 0) continue;

            // 試圖抽取
            int extracted = otherStorage.extractMana(maxAmount, ManaAction.SIMULATE);
            if (extracted > 0) {
                int leftover = selfStorage.insertMana(extracted, ManaAction.EXECUTE);
                int accepted = extracted - leftover;
                if (accepted > 0) {
                    otherStorage.extractMana(accepted, ManaAction.EXECUTE);
                    break; // 成功就中斷
                }
            }
        }
    }
}
