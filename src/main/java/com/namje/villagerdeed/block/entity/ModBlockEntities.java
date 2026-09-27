package com.namje.villagerdeed.block.entity;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.ModBlocks;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, VillagerDeed.MODID);

    public static final RegistryObject<BlockEntityType<VillagerDeedBlockEntity>> VILLAGERDEED_BE =
            BLOCK_ENTITES.register("villagerdeed_be", () -> BlockEntityType.Builder.of(
                    VillagerDeedBlockEntity::new, ModBlocks.VILLAGERDEED_BLOCK.get()
            ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITES.register(eventBus);
    }
}
