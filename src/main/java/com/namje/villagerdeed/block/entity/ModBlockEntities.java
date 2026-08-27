package com.namje.villagerdeed.block.entity;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.ModBlocks;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, VillagerDeed.MODID);

    public static final Supplier<BlockEntityType<VillagerDeedBlockEntity>> VILLAGERDEED_BE =
            BLOCK_ENTITES.register("villagerdeed_be", () -> new BlockEntityType<>(
                    VillagerDeedBlockEntity::new, ModBlocks.VILLAGERDEED_BLOCK.get()
            ));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITES.register(eventBus);
    }
}
