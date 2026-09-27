package com.namje.villagerdeed;

import com.namje.villagerdeed.datagen.ModBlockLootTableProvider;
import com.namje.villagerdeed.datagen.ModBlockTagsProvider;
import com.namje.villagerdeed.datagen.ModModelProvider;
import com.namje.villagerdeed.datagen.ModRecipeProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = VillagerDeed.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class VillagerDeedDataGen {
    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent event) {
        if (!event.includeClient()) {
            return;
        }

        DataGenerator gen = event.getGenerator();
        PackOutput packOutput = gen.getPackOutput();
        var lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        gen.addProvider(true, new ModModelProvider(packOutput, existingFileHelper));
        gen.addProvider(true, new ModBlockTagsProvider(packOutput, lookupProvider, existingFileHelper));
        gen.addProvider(true, new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new,
                LootContextParamSets.BLOCK))));
        gen.addProvider(true, new ModRecipeProvider(packOutput));
    }
}
