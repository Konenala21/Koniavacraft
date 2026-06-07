package com.github.nalamodikk.space.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 飛船座椅：組進飛船的乘客座位（核心是駕駛位）。
 * 有 FACING：放置時依玩家朝向轉，椅背朝玩家背面，椅子面向玩家。組進船後 blockstate 保留，
 * 渲染時模型跟著轉。
 */
public class ShipSeatBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // 碰撞/外框形狀對齊椅子模型（座板 + 椅背靠 z 高側），跟 datagen 的 element 同尺寸。
    // BASE = FACING NORTH（horizontalBlock 此時 rotationY=0=不轉），其餘方向用 rotateYcw 對齊模型旋轉。
    private static final VoxelShape BASE_SHAPE = Shapes.or(
            Shapes.box(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 7 / 16.0, 14 / 16.0),   // 座板
            Shapes.box(2 / 16.0, 7 / 16.0, 11 / 16.0, 14 / 16.0, 1.0, 14 / 16.0)); // 椅背
    private static final VoxelShape[] SHAPES = new VoxelShape[Direction.values().length];

    public ShipSeatBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 椅子視覺開口方向 = FACING（椅背在模型 z 高側、horizontalBlock 套 (toYRot+180)%360）。
        // 要讓坐姿/船頭 = 放置者看的方向，FACING 直接存看的方向（不要 getOpposite）。
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    /** 依 FACING 取對齊模型的形狀（快取）。旋轉量 = horizontalBlock 的 rotationY=(toYRot+180)%360。 */
    private static VoxelShape shapeFor(Direction facing) {
        int i = facing.ordinal();
        if (SHAPES[i] == null) {
            int times = ((((int) facing.toYRot()) + 180) % 360) / 90;
            SHAPES[i] = rotateYcw(BASE_SHAPE, times);
        }
        return SHAPES[i];
    }

    /** 把形狀繞 Y 軸順時針(從上看)轉 times 個 90°。(x,z) -> (1-z, x)。 */
    private static VoxelShape rotateYcw(VoxelShape shape, int times) {
        VoxelShape result = shape;
        for (int t = 0; t < times; t++) {
            final VoxelShape src = result;
            final VoxelShape[] acc = { Shapes.empty() };
            src.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    acc[0] = Shapes.or(acc[0], Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            result = acc[0];
        }
        return result;
    }

    // HorizontalDirectionalBlock 預設不會讓 FACING 跟著 rotate/mirror。飛船拆解時方塊要套旋轉，
    // 沒這兩個覆寫椅子朝向就不會跟船一起轉。
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
