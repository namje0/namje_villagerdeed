package com.namje.villagerdeed.datagen;

import com.namje.villagerdeed.VillagerDeed;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output,
                                CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, VillagerDeed.MODID);
    }

    protected void addTags(HolderLookup.Provider provider) {

    }
}
