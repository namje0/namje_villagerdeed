package com.namje.villagerdeed.block.custom;

import com.mojang.serialization.MapCodec;
import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.function.BiPredicate;
import java.util.function.Function;

public class VillagerDeedBlock extends BaseEntityBlock {
    public static final MapCodec<VillagerDeedBlock> CODEC = simpleCodec(VillagerDeedBlock::new);

    public VillagerDeedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new VillagerDeedBlockEntity(blockPos, blockState);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       ItemStack stack, boolean willHarvest, FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity villagerDeedBlockEntity) {
            VillagerDeed.LOGGER.info("DESTROYED VILLAGER DEED");
        }

        return super.onDestroyedByPlayer(state, level, pos, player, stack, willHarvest, fluid);
    };

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity villagerDeedBlockEntity) {
            player.openMenu(new SimpleMenuProvider(villagerDeedBlockEntity,
                    Component.translatable("block.villagerdeed.namje_villagerdeed")), pos);
            return InteractionResult.SUCCESS;
            /*
            VillagerDeed.LOGGER.info("USED VILLAGER DEED");
            level.playSound(player, pos, SoundEvents.GLOW_ITEM_FRAME_ADD_ITEM,
                    SoundSource.BLOCKS, 1f, 1f);
             */
        }
        return InteractionResult.SUCCESS;
    };

    private static @Nullable BlockPos findAdjacentBedHead(LevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof BedBlock && neighborState.hasProperty(BedBlock.PART)) {
                if (neighborState.getValue(BedBlock.PART) == BedPart.HEAD) {
                    return neighborPos;
                }
            }
        }
        return null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos bedHeadPos = findAdjacentBedHead(level, pos);

        if (bedHeadPos != null) {
            VillagerDeed.LOGGER.info("BED DETECTED");
        }

        return super.getStateForPlacement(context);
    }

    /*
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        if () {

        }
        return null;
        //return super.getTicker(level, blockState, type);
    }
    */
}
