package com.namje.villagerdeed.datagen;

import com.namje.villagerdeed.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "villagerdeed_recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        shaped(RecipeCategory.DECORATIONS, ModBlocks.VILLAGERDEED_BLOCK.get())
                .pattern("ABA")
                .pattern("AAA")
                .define('A', tag(ItemTags.PLANKS))
                .define('B', Items.GOLD_INGOT)
                .unlockedBy(getHasName(Items.GOLD_INGOT), has(Items.GOLD_INGOT))
                .group("villagerdeed")
                .save(output, "villagerdeed:villagerdeed");
    }
}
