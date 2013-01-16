package ic2.core;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

/**
 * Stub for accessing IC2 advanced shapeless recipes
 */
public class AdvShapelessRecipe implements IRecipe {
    public ItemStack output;


    @Override
    public boolean matches(InventoryCrafting var1, World var2) {
        return false;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting var1) {
        return null;
    }

    @Override
    public int getRecipeSize() {
        return 0;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return null;
    }
}
