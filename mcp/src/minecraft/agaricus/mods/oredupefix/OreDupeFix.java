package agaricus.mods.oredupefix;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.relauncher.ReflectionHelper;
import ic2.api.Ic2Recipes;
import ic2.core.AdvRecipe;
import ic2.core.AdvShapelessRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import thermalexpansion.api.crafting.*;

import java.util.List;
import java.util.Map;

@Mod(modid = "OreDupeFix", name = "OreDupeFix", version = "1.0")
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public class OreDupeFix {
    @PostInit
    public static void postInit(FMLPostInitializationEvent event) {
        System.out.println("loading OreDupeFix!");

        // TODO: load preferred ores from config

        // Crafting recipes
        List recipeList = CraftingManager.getInstance().getRecipeList();
        for(Object recipe: recipeList) {
            if (!(recipe instanceof IRecipe)) {
                continue;
            }

            IRecipe iRecipe = (IRecipe)recipe;
            ItemStack output = iRecipe.getRecipeOutput();

            if (output == null) {
                // some recipes require computing the output with getCraftingResult()
                continue;
            }

            System.out.println("craft output: " + output.getDisplayName() + " = " + output.itemID + ":" + output.getItemDamage() + " class " + iRecipe.getClass());

            int itemID = output.itemID;
            int damage = output.getItemDamage();


            if (itemID == 5267 && damage == 5) {  // RP2 copper
                // TODO: set output
                ItemStack newOutput = new ItemStack(20257, 1, 64); // TE
                setRecipeOutput(iRecipe, newOutput);
            }
        }

        // Furnace recipes
        Map<List<Integer>, ItemStack> metaSmeltingList = FurnaceRecipes.smelting().getMetaSmeltingList(); // metadata-sensitive
        Map smeltingList = FurnaceRecipes.smelting().getSmeltingList();

        // IC2 machines
        List<Map.Entry<ItemStack, ItemStack>> compressorRecipes = Ic2Recipes.getCompressorRecipes();
        List<Map.Entry<ItemStack, ItemStack>> extractorRecipes = Ic2Recipes.getExtractorRecipes();
        List<Map.Entry<ItemStack, ItemStack>> maceratorRecipes = Ic2Recipes.getMaceratorRecipes();
        List<Map.Entry<ItemStack, Float>> scrapboxDrops = Ic2Recipes.getScrapboxDrops();

        // TE machines
        ICrucibleRecipe[] iCrucibleRecipes = CraftingManagers.crucibleManager.getRecipeList();
        IFurnaceRecipe[] iFurnaceRecipes = CraftingManagers.furnaceManager.getRecipeList();
        IPulverizerRecipe[] iPulverizerRecipes = CraftingManagers.pulverizerManager.getRecipeList();
        ISawmillRecipe[] iSawmillRecipes = CraftingManagers.sawmillManager.getRecipeList();
        ISmelterRecipe[] iSmelterRecipes = CraftingManagers.smelterManager.getRecipeList();
        //ISmelterRecipe[] iFillRecipes F= CraftingManagers.transposerManager.getFillRecipeList(); // TODO
    }

    public static void setRecipeOutput(IRecipe iRecipe, ItemStack output) {
        if (iRecipe instanceof ShapedRecipes) {
            ReflectionHelper.setPrivateValue(ShapedRecipes.class,(ShapedRecipes)iRecipe, output, "e"/*"recipeOutput"*/); // TODO: avoid hardcoding obf
        } else if (iRecipe instanceof ShapelessRecipes) {
            ReflectionHelper.setPrivateValue(ShapelessRecipes.class,(ShapelessRecipes)iRecipe, output, "a"/*"recipeOutput"*/);
        } else if (iRecipe instanceof ShapelessOreRecipe) {
            ReflectionHelper.setPrivateValue(ShapelessOreRecipe.class,(ShapelessOreRecipe)iRecipe, output, "output");
        } else if (iRecipe instanceof ShapedOreRecipe) {
            ReflectionHelper.setPrivateValue(ShapedOreRecipe.class,(ShapedOreRecipe)iRecipe, output, "output");

        // IndustrialCraft^2
        } else if (iRecipe instanceof AdvRecipe) {
            // thanks IC2 for making this field public.. even if recipes aren't in the API
            ((AdvRecipe)iRecipe).output = output;
        } else if (iRecipe instanceof AdvShapelessRecipe) {
            ((AdvShapelessRecipe)iRecipe).output = output;
        }

        // TODO: te
        // TODO: bc
    }
}

