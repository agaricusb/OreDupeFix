package agaricus.mods.oredupefix;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.ItemData;
import cpw.mods.fml.relauncher.ReflectionHelper;
import ic2.api.Ic2Recipes;
import ic2.core.AdvRecipe;
import ic2.core.AdvShapelessRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraftforge.common.ConfigCategory;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import thermalexpansion.api.crafting.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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


    private static boolean shouldDumpOreDict;

    @PostInit
    public static void postInit(FMLPostInitializationEvent event) {
        System.out.println("Loading OreDupeFix...");

        if (shouldDumpOreDict) {
            dumpOreDict();
        }

        loadPreferredOres();

        replaceCraftingRecipes();
        replaceFurnaceRecipes();
        replaceFurnaceRecipesInsensitive();



        // IC2 machines
        // TODO: make optional
        replaceIC2MachineRecipes(Ic2Recipes.getCompressorRecipes());
        replaceIC2MachineRecipes(Ic2Recipes.getExtractorRecipes());
        replaceIC2MachineRecipes(Ic2Recipes.getMaceratorRecipes());

        // TODO
        List<Map.Entry<ItemStack, Float>> scrapboxDrops = Ic2Recipes.getScrapboxDrops();

        // TODO: dungeon loot

        // TE machines
        /*
        // TODO
        ICrucibleRecipe[] iCrucibleRecipes = CraftingManagers.crucibleManager.getRecipeList();
        IFurnaceRecipe[] iFurnaceRecipes = CraftingManagers.furnaceManager.getRecipeList();
        IPulverizerRecipe[] iPulverizerRecipes = CraftingManagers.pulverizerManager.getRecipeList();
        ISawmillRecipe[] iSawmillRecipes = CraftingManagers.sawmillManager.getRecipeList();
        ISmelterRecipe[] iSmelterRecipes = CraftingManagers.smelterManager.getRecipeList();
        //ISmelterRecipe[] iFillRecipes F= CraftingManagers.transposerManager.getFillRecipeList(); // TODO
        */
    }

    public static void replaceFurnaceRecipes() {
         // Furnace recipes
        Map<List<Integer>, ItemStack> metaSmeltingList = FurnaceRecipes.smelting().getMetaSmeltingList(); // metadata-sensitive; (itemID,metadata) to ItemStack
        for (Map.Entry<List<Integer>, ItemStack> entry : metaSmeltingList.entrySet()) {
            List<Integer> inputItemPlusData = entry.getKey();
            ItemStack output = entry.getValue();

            ItemStack newOutput = getPreferredOre(output);

            if (newOutput != null) {
                entry.setValue(newOutput);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void replaceFurnaceRecipesInsensitive() {
        Map<Integer, ItemStack> smeltingList = (Map<Integer, ItemStack>)FurnaceRecipes.smelting().getSmeltingList(); // itemID to ItemStack

        for (Map.Entry<Integer, ItemStack> entry : smeltingList.entrySet()) {
            int itemID = entry.getKey();
            ItemStack output = entry.getValue();

            ItemStack newOutput = getPreferredOre(output);

            if (newOutput != null) {
                entry.setValue(newOutput);
            }
        }
    }

    public static void replaceCraftingRecipes() {
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

            ItemStack newOutput = getPreferredOre(output);
            if (newOutput == null) {
                // do not replace
                continue;
            }

            System.out.println("Modifying recipe "+iRecipe+" replacing "+output.itemID+":"+output.getItemDamage()+" -> "+newOutput.itemID+":"+newOutput.getItemDamage());
            setRecipeOutput(iRecipe, newOutput);
        }

    }

    public static void replaceIC2MachineRecipes(List<Map.Entry<ItemStack, ItemStack>> machineRecipes) {
         for (int i = 0; i < machineRecipes.size(); i += 1) {
            Map.Entry<ItemStack, ItemStack> entry = machineRecipes.get(i);
            ItemStack input = entry.getKey();
            ItemStack output = entry.getValue();

            ItemStack newOutput = getPreferredOre(output);
            if (newOutput != null) {
                entry.setValue(newOutput);
            }
        }
    }

    /**
     *
     * @param output The existing ore dictionary item
     * @return A new ore dictionary item, for the name ore but preferred by the user
     */
    public static ItemStack getPreferredOre(ItemStack output) {
        //System.out.println("craft output: " + output.getDisplayName() + " = " + output.itemID + ":" + output.getItemDamage() + " class " + iRecipe.getClass());

        int oreID = OreDictionary.getOreID(output);
        if (oreID == -1) {
            // this isn't an ore
            return null;
        }

        String oreName = OreDictionary.getOreName(oreID);

        ItemStack newOutput = oreName2PreferredItem.get(oreName);
        if (newOutput == null) {
            // no preference, do not replace
            return null;
        }

        return newOutput;
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

    @PreInit
    public static void preInit(FMLPreInitializationEvent event) {
        oreName2PreferredMod = new HashMap<String, String>();

        Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());

        FMLLog.log(Level.FINE, "OreDupeFix loading config");

        try {
            cfg.load();

            shouldDumpOreDict = cfg.get(Configuration.CATEGORY_GENERAL, "dumpOreDict", true).getBoolean(true);

            if (cfg.categories.size() == 0) {
                loadDefaults(cfg);
            }

            ConfigCategory category = cfg.getCategory("PreferredOres");

            for (Map.Entry<String, Property> entry : category.entrySet()) {
                String name = entry.getKey();
                Property property = entry.getValue();

                oreName2PreferredMod.put(name, property.value);
            }

        } catch (Exception e) {
            FMLLog.log(Level.SEVERE, e, "OreDupeFix had a problem loading it's configuration");
        } finally {
            cfg.save();
        }
    }

    public static void loadDefaults(Configuration cfg) {
        ConfigCategory category = cfg.getCategory("PreferredOres");

        FMLLog.log(Level.FINE, "OreDupeFix initializing defaults");

        HashMap<String, String> m = new HashMap<String, String>();
        // a reasonable set of defaults
        m.put("ingotCopper", "RedPowerBase");
        m.put("ingotTin", "RedPowerBase");
        m.put("ingotBronze", "IC2");
        m.put("dustBronze", "ThermalExpansion");
        m.put("dustIron", "ThermalExpansion");
        m.put("dustTin", "ThermalExpansion");
        m.put("dustSilver", "ThermalExpansion");
        m.put("dustCopper", "ThermalExpansion");
        m.put("dustGold", "ThermalExpansion");

        for (Map.Entry<String, String> entry : m.entrySet()) {
            String oreName = entry.getKey();
            String modID = entry.getValue();

            category.put(oreName, new Property(oreName, modID, Property.Type.STRING));
        }
    }

    public static void loadPreferredOres() {
        oreName2PreferredItem = new HashMap<String, ItemStack>();

        // Get registered ores and associated mods
        Map<Integer, ItemData> idMap = ReflectionHelper.getPrivateValue(GameData.class, null, "idMap");

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
     * Dump ore dictionary
     */
    public static void dumpOreDict() {
        Map<Integer, ItemData> idMap = ReflectionHelper.getPrivateValue(GameData.class, null, "idMap");

        String[] oreNames = OreDictionary.getOreNames();
        for (String oreName : oreNames) {
            System.out.print("ore: " + oreName + ": ");
            ArrayList<ItemStack> oreItems = OreDictionary.getOres(oreName);
            for (ItemStack oreItem : oreItems) {
                ItemData itemData = idMap.get(oreItem.itemID);
                String modID = itemData.getModId();

                System.out.print(oreItem.itemID + ":" + oreItem.getItemDamage() + "=" + modID + ", ");
            }
            System.out.println("");
        }
    }
}

