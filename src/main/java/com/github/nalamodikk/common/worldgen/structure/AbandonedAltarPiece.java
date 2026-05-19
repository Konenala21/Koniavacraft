package com.github.nalamodikk.common.worldgen.structure;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModStructurePieceTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class AbandonedAltarPiece extends TemplateStructurePiece {

    private static final ResourceLocation TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "abandoned_altar_01");

    public AbandonedAltarPiece(StructureTemplateManager manager, BlockPos pos) {
        super(ModStructurePieceTypes.ABANDONED_ALTAR.get(), 0, manager,
                TEMPLATE, TEMPLATE.toString(), makeSettings(), pos);
    }

    public AbandonedAltarPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructurePieceTypes.ABANDONED_ALTAR.get(), tag, manager,
                id -> makeSettings());
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
    }
}
