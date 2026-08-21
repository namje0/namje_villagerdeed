package com.namje.villagerdeed.block;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VillagerDeed.MODID);
    public static final DeferredBlock<Block> VILLAGERDEED_BLOCK = registerBlock("namje_villagerdeed",
            properties -> new Block(properties.sound(SoundType.WOOD)));

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.registerItem(name, properties -> new BlockItem(block.get(),
                properties.useBlockDescriptionPrefix()));
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    };

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
