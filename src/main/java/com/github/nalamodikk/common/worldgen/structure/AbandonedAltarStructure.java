package com.github.nalamodikk.common.worldgen.structure;

import com.github.nalamodikk.register.ModStructureTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class AbandonedAltarStructure extends Structure {

    public static final MapCodec<AbandonedAltarStructure> CODEC =
            simpleCodec(AbandonedAltarStructure::new);

    public AbandonedAltarStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        return onTopOfChunkCenter(ctx, Heightmap.Types.WORLD_SURFACE_WG, builder ->
                builder.addPiece(new AbandonedAltarPiece(
                        ctx.structureTemplateManager(),
                        ctx.chunkPos().getMiddleBlockPosition(0)))
        );
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.ABANDONED_ALTAR.get();
    }
}
