package com.cleanroommc.groovyscript.compat.mods.immersiveengineering;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.crafting.SqueezerRecipe;
import com.cleanroommc.groovyscript.api.IIngredient;
import com.cleanroommc.groovyscript.compat.EnergyRecipeBuilder;
import com.cleanroommc.groovyscript.compat.mods.ModSupport;
import com.cleanroommc.groovyscript.helper.SimpleObjectStream;
import com.cleanroommc.groovyscript.registry.VirtualizedRegistry;
import com.cleanroommc.groovyscript.sandbox.GroovyLog;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Iterator;

public class Squeezer extends VirtualizedRegistry<SqueezerRecipe> {

    public Squeezer() {
        super("Squeezer", "squeezer");
    }

    public static RecipeBuilder recipeBuilder() {
        return new RecipeBuilder();
    }

    @Override
    public void onReload() {
        removeScripted().forEach(recipe -> SqueezerRecipe.recipeList.removeIf(r -> r == recipe));
        SqueezerRecipe.recipeList.addAll(restoreFromBackup());
    }

    public void add(SqueezerRecipe recipe) {
        if (recipe != null) {
            addScripted(recipe);
            SqueezerRecipe.recipeList.add(recipe);
        }
    }

    public SqueezerRecipe add(FluidStack fluidOutput, @Nonnull ItemStack itemOutput, Object input, int energy) {
        SqueezerRecipe recipe = create(fluidOutput, itemOutput, input, energy);
        addScripted(recipe);
        return recipe;
    }

    public void remove(SqueezerRecipe recipe) {
        if (SqueezerRecipe.recipeList.removeIf(r -> r == recipe)) addBackup(recipe);
    }

    public void removeByOutput(FluidStack fluidOutput) {
        for (Iterator<SqueezerRecipe> iterator = SqueezerRecipe.recipeList.iterator(); iterator.hasNext(); ) {
            SqueezerRecipe recipe = iterator.next();
            if ((fluidOutput != null && fluidOutput.isFluidEqual(recipe.fluidOutput)) || (fluidOutput == null && recipe.fluidOutput == null)) {
                addBackup(recipe);
                iterator.remove();
            }
        }
    }

    public void removeByOutput(FluidStack fluidOutput, ItemStack itemOutput) {
        for (Iterator<SqueezerRecipe> iterator = SqueezerRecipe.recipeList.iterator(); iterator.hasNext(); ) {
            SqueezerRecipe recipe = iterator.next();
            if (((fluidOutput != null && fluidOutput.isFluidEqual(recipe.fluidOutput)) || (fluidOutput == null && recipe.fluidOutput == null)) && ((recipe.input == null && itemOutput.isEmpty()) || (recipe.input != null && recipe.input.matches(itemOutput)))) {
                addBackup(recipe);
                iterator.remove();
            }
        }
    }

    public void removeByInput(ItemStack input) {
        SqueezerRecipe recipe = SqueezerRecipe.findRecipe(input);
        remove(recipe);
    }

    public SimpleObjectStream<SqueezerRecipe> stream() {
        return new SimpleObjectStream<>(SqueezerRecipe.recipeList).setRemover(recipe -> {
            SqueezerRecipe r = SqueezerRecipe.findRecipe(recipe.input.stack);
            if (r != null) {
                remove(r);
                return true;
            }
            return false;
        });
    }

    public void removeAll() {
        SqueezerRecipe.recipeList.forEach(this::addBackup);
        SqueezerRecipe.recipeList.clear();
    }

    private static SqueezerRecipe create(FluidStack fluidOutput, @Nonnull ItemStack itemOutput, Object input, int energy) {
        if (input instanceof IIngredient) input = ((IIngredient) input).getMatchingStacks();
        return SqueezerRecipe.addRecipe(fluidOutput, itemOutput, input, energy);
    }

    private static class RecipeBuilder extends EnergyRecipeBuilder<SqueezerRecipe> {

        protected ItemStack itemOutput = ItemStack.EMPTY;
        protected FluidStack fOutput = null;

        @Override
        public String getErrorMsg() {
            return "Error adding Immersive Engineering Refinery recipe";
        }

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg, 1, 1, 0, 1);
            validateFluids(msg, 0, 0, 0, 1);
            if (output.size() > 0) itemOutput = output.get(0);
            if (fluidOutput.size() > 0) fOutput = fluidOutput.get(0);
        }

        @Override
        public @Nullable SqueezerRecipe register() {
            return ModSupport.IMMERSIVE_ENGINEERING.get().squeezer.add(fOutput, itemOutput, input.get(0), energy);
        }
    }
}
