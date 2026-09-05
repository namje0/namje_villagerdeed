package com.namje.villagerdeed.block.custom;

import com.mojang.serialization.MapCodec;
import com.namje.villagerdeed.block.entity.ModBlockEntities;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class VillagerDeedBlock extends BaseEntityBlock {
    public static final MapCodec<VillagerDeedBlock> CODEC = simpleCodec(VillagerDeedBlock::new);
    public static final EnumProperty<Direction> FACING;

    public VillagerDeedBlock(Properties properties) {
        super(properties);
        BlockState defaultState = this.stateDefinition.any().setValue(FACING, Direction.NORTH);
        this.registerDefaultState(defaultState);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new VillagerDeedBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide()
                ? null
                : createTickerHelper(blockEntityType, ModBlockEntities.VILLAGERDEED_BE.get(), VillagerDeedBlockEntity::serverTick);
    }

    @Nullable
    public static BlockPos findAdjacentBedHead(LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof BedBlock && neighborState.hasProperty(BedBlock.PART)) {
                BedPart part = neighborState.getValue(BedBlock.PART);
                if (part == BedPart.HEAD) {
                    return neighborPos;
                } else if (part == BedPart.FOOT && neighborState.hasProperty(BedBlock.FACING)) {
                    Direction facing = neighborState.getValue(BedBlock.FACING);
                    BlockPos headPos = neighborPos.relative(facing);
                    BlockState headState = level.getBlockState(headPos);
                    if (headState.getBlock() instanceof BedBlock
                            && headState.hasProperty(BedBlock.PART)
                            && headState.getValue(BedBlock.PART) == BedPart.HEAD) {
                        return headPos;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedEntity) {
                deedEntity.updateBedPresence(findAdjacentBedHead(level, pos));
            }
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader levelReader, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (levelReader instanceof Level level && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedEntity) {
                deedEntity.updateBedPresence(findAdjacentBedHead(level, pos));
            }
        }
        return super.updateShape(state, levelReader, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity villagerDeedBlockEntity) {
                if (villagerDeedBlockEntity.getDeedState() == 2) {
                    player.openMenu(new SimpleMenuProvider(villagerDeedBlockEntity,
                            Component.translatable("block.villagerdeed.namje_villagerdeed")), pos);
                } else {
                    String message = switch (villagerDeedBlockEntity.getDeedState()) {
                        case 0 -> "no_bed";
                        case 1 -> "waiting";
                        case 3 -> "respawning";
                        default -> "invalid";
                    };
                    player.sendOverlayMessage(Component.translatable("block.villagerdeed.namje_villagerdeed." + message));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    static {
        FACING = HorizontalDirectionalBlock.FACING;
    }
}