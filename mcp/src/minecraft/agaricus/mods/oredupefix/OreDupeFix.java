package agaricus.mods.oredupefix;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.ItemData;
import cpw.mods.fml.relauncher.ReflectionHelper;
import ic2.api.Ic2Recipes;
import ic2.core.AdvRecipe;
import ic2.core.AdvShapelessRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import thermalexpansion.api.crafting.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod(modid = "OreDupeFix", name = "OreDupeFix", version = "1.0")
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public class OreDupeFix {
    /**
     * Map from ore name to mod ID preferred by user
     */
    public static HashMap<String, String> oreName2PreferredMod;

    /**
     * Map from ore name to ItemStack preferred by user, from their preferred mod
     */
    public static HashMap<String, ItemStack> oreName2PreferredItem;

    /**
     * Map from each ore ItemStack to
     */
    public static HashMap<ItemStack, ItemStack> oreItem2PreferredItem;

    @PostInit
    public static void postInit(FMLPostInitializationEvent event) {
        System.out.println("loading OreDupeFix!");

        loadPreferredOres();

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

            //System.out.println("craft output: " + output.getDisplayName() + " = " + output.itemID + ":" + output.getItemDamage() + " class " + iRecipe.getClass());

            int oreID = OreDictionary.getOreID(output);
            if (oreID == -1) {
                // this isn't an ore
                continue;
            }

            String oreName = OreDictionary.getOreName(oreID);

            ItemStack newOutput = oreName2PreferredItem.get(oreName);
            if (newOutput == null) {
                // no preference
                continue;
            }

            System.out.println("Modifying recipe "+iRecipe+" replacing "+output.itemID+":"+output.getItemDamage()+" -> "+newOutput.itemID+":"+newOutput.getItemDamage());
            setRecipeOutput(iRecipe, newOutput);
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

    public static void loadPreferredOres() {
         // TODO: load preferred ores from config
        oreName2PreferredMod = new HashMap<String, String>();
        oreName2PreferredMod.put("ingotCopper", "ThermalExpansion");
        oreName2PreferredMod.put("ingotTin", "ThermalExpansion");
        oreName2PreferredMod.put("ingotBronze", "IC2");
        oreName2PreferredMod.put("dustBronze", "ThermalExpansion");
        oreName2PreferredMod.put("dustIron", "ThermalExpansion");
        oreName2PreferredMod.put("dustTin", "ThermalExpansion");
        oreName2PreferredMod.put("dustSilver", "ThermalExpansion");
        oreName2PreferredMod.put("dustCopper", "ThermalExpansion");
        oreName2PreferredMod.put("dustGold", "ThermalExpansion");

        oreName2PreferredItem = new HashMap<String, ItemStack>();

        // Get registered ores and associated mods
        Map<Integer, ItemData> idMap = ReflectionHelper.getPrivateValue(GameData.class, null, "idMap");

        //dumpOreDict();

        // Map ore dict name to preferred item, given mod ID
        for (Map.Entry<String, String> entry : oreName2PreferredMod.entrySet()) {
            String oreName = entry.getKey();
            String preferredModID = entry.getValue();

            ArrayList<ItemStack> oreItems = OreDictionary.getOres(oreName);
            boolean found = false;
            for (ItemStack oreItem : oreItems) {
                ItemData itemData = idMap.get(oreItem.itemID);
                String modID = itemData.getModId();

                if (preferredModID.equals(modID)) {
                    oreName2PreferredItem.put(oreName, oreItem);
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("No mod '"+preferredModID+"' found for ore '"+oreName+"'! Skipping");
            }
        }

        // Show ore preferences
        for (String oreName : oreName2PreferredMod.keySet()) {
            String modID = oreName2PreferredMod.get(oreName);
            ItemStack itemStack = oreName2PreferredItem.get(oreName);
            if (itemStack != null) {
                System.out.println("Preferring ore name "+oreName+" from mod "+modID+" = "+itemStack.itemID+":"+ itemStack.getItemDamage());
            }
        }
    }


    /**
     * Dump ore dictionary for debugging
     */
    public static void dumpOreDict() {
        Map<Integer, ItemData> idMap = ReflectionHelper.getPrivateValue(GameData.class, null, "idMap");

        String[] oreNames = OreDictionary.getOreNames();
        for (String oreName : oreNames) {
            System.out.print("ore: " + oreName);
            ArrayList<ItemStack> oreItems = OreDictionary.getOres(oreName);
            for (ItemStack oreItem : oreItems) {
                ItemData itemData = idMap.get(oreItem.itemID);
                String modID = itemData.getModId();

                System.out.println(oreItem.itemID + "=" + modID + ", ");
            }
            System.out.println("");
        }
    }
}

