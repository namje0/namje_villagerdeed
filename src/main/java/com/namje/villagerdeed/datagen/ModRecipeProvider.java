package com.namje.villagerdeed.datagen;

import com.namje.villagerdeed.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.VILLAGERDEED_BLOCK.get())
                .pattern("ADA")
                .pattern("ABA")
                .pattern("ACA")
                .define('A', ItemTags.PLANKS)
                .define('B', Items.BOOK)
                .define('C', Items.EMERALD)
                .define('D', Items.GOLD_INGOT)
                .unlockedBy(getHasName(Items.EMERALD), has(Items.EMERALD))
                .unlockedBy(getHasName(Items.GOLD_INGOT), has(Items.GOLD_INGOT))
                .unlockedBy(getHasName(Items.BOOK), has(Items.BOOK))
                .group("villagerdeed")
                .save(output, "villagerdeed:villagerdeed");
    }
}
