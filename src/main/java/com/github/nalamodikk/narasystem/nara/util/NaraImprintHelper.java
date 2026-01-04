package com.github.nalamodikk.narasystem.nara.util;

import com.github.nalamodikk.narasystem.nara.api.INaraImprint;
import net.minecraft.world.item.ItemStack;

public class NaraImprintHelper implements INaraImprint {

    @Override
    public boolean hasNaraImprint(ItemStack stack) {
        // TODO: 改為檢查是否存在 DataComponent（例如 NARA_IMPRINT_COMPONENT）
        return false;
    }

    @Override
    public void setNaraImprint(ItemStack stack) {
        // TODO: 實作為 stack.set(DataComponents.YOUR_COMPONENT, new YourValue(...));
    }
}
