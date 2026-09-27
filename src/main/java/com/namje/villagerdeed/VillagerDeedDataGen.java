package com.namje.villagerdeed;

import com.namje.villagerdeed.datagen.ModBlockLootTableProvider;
import com.namje.villagerdeed.datagen.ModBlockTagsProvider;
import com.namje.villagerdeed.datagen.ModModelProvider;
import com.namje.villagerdeed.datagen.ModRecipeProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;

@EventBusSubscriber(modid = VillagerDeed.MODID)
public class VillagerDeedDataGen {
    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        DataGenerator gen = event.getGenerator();
        PackOutput packOutput = gen.getPackOutput();
        var lookupProvider = event.getLookupProvider();

        gen.addProvider(true, new ModModelProvider(packOutput));
        gen.addProvider(true, new ModBlockTagsProvider(packOutput, lookupProvider));
        gen.addProvider(true, new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new,
                LootContextParamSets.BLOCK)), lookupProvider));
        gen.addProvider(true, new ModRecipeProvider.Runner(packOutput, lookupProvider));
    }
}
