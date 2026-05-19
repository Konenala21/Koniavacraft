package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.worldgen.structure.AbandonedAltarPiece;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStructurePieceTypes {

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(BuiltInRegistries.STRUCTURE_PIECE, KoniavacraftMod.MOD_ID);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ABANDONED_ALTAR =
            STRUCTURE_PIECE_TYPES.register("abandoned_altar",
                    () -> (StructurePieceType.StructureTemplateType)(manager, tag) -> new AbandonedAltarPiece(manager, tag));

    public static void register(IEventBus bus) {
        STRUCTURE_PIECE_TYPES.register(bus);
    }
}
