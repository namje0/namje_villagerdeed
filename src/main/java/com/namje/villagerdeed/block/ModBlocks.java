package com.namje.villagerdeed.block;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.custom.VillagerDeedBlock;
import com.namje.villagerdeed.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, VillagerDeed.MODID);
    public static final RegistryObject<Block> VILLAGERDEED_BLOCK = registerBlock("namje_villagerdeed",
            properties -> new VillagerDeedBlock(properties.sound(SoundType.WOOD).strength(2f)));

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        RegistryObject<T> toReturn = BLOCKS.register(name, () -> function.apply(BlockBehaviour.Properties.of()));
        registerBlockItem(name, toReturn);
        return toReturn;
    };

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
