package com.github.nalamodikk.register.client;

import com.github.nalamodikk.common.item.wand.core.WandCoreItem;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeItem;
import com.github.nalamodikk.register.ModItems;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public class ModColorHandlers {

    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // 核心插件：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof WandCoreItem core) {
                        return core.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.FORMATION_CORE.get(),
                ModItems.ACTIVATION_CORE.get(),
                ModItems.IO_CORE.get(),
                ModItems.ROTATION_CORE.get(),
                ModItems.RITUAL_CORE.get()
        );

        // 升級物品：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof WandUpgradeItem upg) {
                        return upg.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.WAND_UPGRADE_CAPACITY.get(),
                ModItems.WAND_UPGRADE_EFFICIENCY.get(),
                ModItems.WAND_UPGRADE_RANGE.get(),
                ModItems.WAND_UPGRADE_COOLDOWN.get()
        );
    }
}
