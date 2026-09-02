package com.namje.villagerdeed.block.custom;

import com.mojang.serialization.MapCodec;
import com.namje.villagerdeed.VillagerDeed;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class VillagerDeedBlock extends BaseEntityBlock {
    public static final MapCodec<VillagerDeedBlock> CODEC = simpleCodec(VillagerDeedBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public VillagerDeedBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new VillagerDeedBlockEntity(blockPos, blockState);
    }

    private static boolean hasAdjacentBedHead(LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof BedBlock && neighborState.hasProperty(BedBlock.PART)) {
                if (neighborState.getValue(BedBlock.PART) == BedPart.HEAD) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean hasBed = hasAdjacentBedHead(context.getLevel(), context.getClickedPos());
        return this.defaultBlockState().setValue(ACTIVE, hasBed);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader levelReader, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        boolean hasBed = hasAdjacentBedHead(levelReader, pos);
        if (state.getValue(ACTIVE) != hasBed) {
            return state.setValue(ACTIVE, hasBed);
        }
        return super.updateShape(state, levelReader, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity villagerDeedBlockEntity) {
                if (state.getValue(ACTIVE)) {
                    player.openMenu(new SimpleMenuProvider(villagerDeedBlockEntity,
                            Component.translatable("block.villagerdeed.namje_villagerdeed")), pos);
                } else {
                    player.sendOverlayMessage(Component.translatable("block.villagerdeed.namje_villagerdeed.no_bed"));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack stack, boolean willHarvest, FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity) {
            VillagerDeed.LOGGER.info("DESTROYED VILLAGER DEED");
        }
        return super.onDestroyedByPlayer(state, level, pos, player, stack, willHarvest, fluid);
    }

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
}