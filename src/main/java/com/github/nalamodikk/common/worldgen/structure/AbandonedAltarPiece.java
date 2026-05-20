package com.github.nalamodikk.common.worldgen.structure;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModStructurePieceTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;

import javax.annotation.Nullable;

public class AbandonedAltarPiece extends TemplateStructurePiece {

    private static final ResourceKey<LootTable> CHEST_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "chests/abandoned_altar"));

    public AbandonedAltarPiece(StructureTemplateManager manager, BlockPos pos, ResourceLocation templateId) {
        super(ModStructurePieceTypes.ABANDONED_ALTAR.get(), 0, manager,
                templateId, templateId.toString(), makeSettings(), pos);
    }

    public AbandonedAltarPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructurePieceTypes.ABANDONED_ALTAR.get(), tag, manager,
                id -> makeSettings());
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false)
                .addProcessor(new BlockIgnoreProcessor(
                        ImmutableList.of(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR)))
                .addProcessor(new ChestLootProcessor(CHEST_LOOT));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        super.postProcess(level, structureManager, generator, random, box, chunkPos, pos);
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
    }

    private static class ChestLootProcessor extends StructureProcessor {
        private final ResourceKey<LootTable> lootTable;

        ChestLootProcessor(ResourceKey<LootTable> lootTable) {
            this.lootTable = lootTable;
        }

        @Override
        public StructureTemplate.StructureBlockInfo processBlock(
                LevelReader world, BlockPos offset, BlockPos pos,
                StructureTemplate.StructureBlockInfo blockInfoLocal,
                StructureTemplate.StructureBlockInfo blockInfoWorld,
                StructurePlaceSettings settings) {
            if (!(blockInfoWorld.state().getBlock() instanceof ChestBlock)) {
                return blockInfoWorld;
            }
            CompoundTag tag = blockInfoWorld.nbt() != null ? blockInfoWorld.nbt().copy() : new CompoundTag();
            tag.putString("LootTable", lootTable.location().toString());
            tag.putLong("LootTableSeed", RandomSource.create().nextLong());
            return new StructureTemplate.StructureBlockInfo(blockInfoWorld.pos(), blockInfoWorld.state(), tag);
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return StructureProcessorType.NOP;
        }
    }
}
