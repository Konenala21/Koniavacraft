package com.github.nalamodikk.common.item.tool.structure;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 結構建造杖認得的結構清單。加新結構在這裡 register 一次,杖就支援。
 */
public final class WandStructures {
    private static final List<WandStructure> REGISTERED = new ArrayList<>();

    static {
        register(new AltarWandStructure());
    }

    private WandStructures() {}

    public static void register(WandStructure structure) {
        REGISTERED.add(structure);
    }

    /** 找出點到的方塊對應的結構,沒有則 null。 */
    @Nullable
    public static WandStructure findMatching(BlockState clicked, @Nullable BlockEntity be) {
        for (WandStructure s : REGISTERED) {
            if (s.matches(clicked, be)) return s;
        }
        return null;
    }
}
