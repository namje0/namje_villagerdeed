package com.namje.villagerdeed.block.custom;

import com.mojang.serialization.MapCodec;
import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.entity.ModBlockEntities;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class VillagerDeedBlock extends BaseEntityBlock {
    public static final MapCodec<VillagerDeedBlock> CODEC = simpleCodec(VillagerDeedBlock::new);

    public VillagerDeedBlock(Properties properties) {
        super(properties);
        //this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, false));
    }

    /*
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }
     */

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

    /*
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean hasBed = hasAdjacentBedHead(context.getLevel(), context.getClickedPos());
        return this.defaultBlockState().setValue(ACTIVE, hasBed);
    }
     */

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

    /*
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack stack, boolean willHarvest, FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedEntity) {
            deedEntity.onCleanup(level);
        }
        return super.onDestroyedByPlayer(state, level, pos, player, stack, willHarvest, fluid);
    }
     */

    /*
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof BedBlock) {
                level.destroyBlock(neighborPos, true);
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
     */
}