package com.namje.villagerdeed.datagen;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModModelProvider extends BlockStateProvider {

    public ModModelProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, VillagerDeed.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        Block block = ModBlocks.VILLAGERDEED_BLOCK.get();
        ModelFile model = models().cube("namje_villagerdeed",
                        modLoc("block/namje_villagerdeed_down"),
                        modLoc("block/namje_villagerdeed_up"),
                        modLoc("block/namje_villagerdeed_north"),
                        modLoc("block/namje_villagerdeed_south"),
                        modLoc("block/namje_villagerdeed_east"),
                        modLoc("block/namje_villagerdeed_west"))
                .texture("particle", modLoc("block/namje_villagerdeed_east"));
        horizontalBlock(block, model);
        simpleBlockItem(block, model);
    }
}
