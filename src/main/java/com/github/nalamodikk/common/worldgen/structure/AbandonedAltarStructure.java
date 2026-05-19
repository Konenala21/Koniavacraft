package com.github.nalamodikk.common.worldgen.structure;

import com.github.nalamodikk.register.ModStructureTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class AbandonedAltarStructure extends Structure {

    public static final MapCodec<AbandonedAltarStructure> CODEC =
            RecordCodecBuilder.mapCodec(i -> i.group(
                    settingsCodec(i),
                    ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.templateId)
            ).apply(i, AbandonedAltarStructure::new));

    private final ResourceLocation templateId;

    public AbandonedAltarStructure(StructureSettings settings, ResourceLocation templateId) {
        super(settings);
        this.templateId = templateId;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        return onTopOfChunkCenter(ctx, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            int x = ctx.chunkPos().getMiddleBlockX();
            int z = ctx.chunkPos().getMiddleBlockZ();
            int y = ctx.chunkGenerator().getFirstOccupiedHeight(
                    x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState()) + 1;
            builder.addPiece(new AbandonedAltarPiece(
                    ctx.structureTemplateManager(),
                    new BlockPos(x, y, z),
                    templateId));
        });
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.ABANDONED_ALTAR.get();
    }
}
