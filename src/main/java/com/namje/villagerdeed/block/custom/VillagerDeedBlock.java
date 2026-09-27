package com.namje.villagerdeed.block.custom;

import com.namje.villagerdeed.block.entity.ModBlockEntities;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import com.namje.villagerdeed.menu.custom.VillagerDeedScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.UUID;

public class VillagerDeedBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING;

    public VillagerDeedBlock(Properties properties) {
        super(properties);
        BlockState defaultState = this.stateDefinition.any().setValue(FACING, Direction.NORTH);
        this.registerDefaultState(defaultState);
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedEntity) {
                deedEntity.updateBedPresence(deedEntity.findAdjacentBedHead(level, pos));
            }
        }
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction directionToNeighbour,
            BlockState neighbourState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos
    ) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedEntity) {
                deedEntity.updateBedPresence(deedEntity.findAdjacentBedHead((Level) level, pos));
            }
        }
        return super.updateShape(state, directionToNeighbour, neighbourState, level, pos, neighbourPos);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity villagerDeedBlockEntity) {
                Minecraft.getInstance().setScreen(
                        new VillagerDeedScreen(
                                Component.translatable("block.villagerdeed.namje_villagerdeed"),
                                villagerDeedBlockEntity
                        )
                );
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide() && placer instanceof Player player) {
            if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedEntity) {
                UUID playerUUID = player.getUUID();
                deedEntity.setOwnerUUID(playerUUID);
                deedEntity.subscribePlayer(playerUUID);
            }
        }
    }

    static {
        FACING = HorizontalDirectionalBlock.FACING;
    }
}