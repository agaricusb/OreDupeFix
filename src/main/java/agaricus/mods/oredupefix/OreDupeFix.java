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
import ic2.core.item.ItemScrapbox;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.ConfigCategory;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.*;
import java.util.logging.Level;

@Mod(modid = "OreDupeFix", name = "OreDupeFix", version = "3.1-SNAPSHOT") // TODO: version from resource
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

    private static boolean shouldDumpOreDict;

    private static boolean verbose;

    private static boolean replaceCrafting;
    private static boolean replaceFurnace;
    private static boolean replaceFurnaceInsensitive;
    private static boolean replaceDungeonLoot;
    private static boolean replaceIC2Compressor;
    private static boolean replaceIC2Extractor;
    private static boolean replaceIC2Macerator;
    private static boolean replaceIC2Scrapbox;

    @PreInit
    public static void preInit(FMLPreInitializationEvent event) {
        oreName2PreferredMod = new HashMap<String, String>();

        Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());

        FMLLog.log(Level.FINE, "OreDupeFix loading config");

        try {
            cfg.load();

            if (cfg.getCategoryNames().size() == 0) {
                loadDefaults(cfg);
            }

            ConfigCategory category = cfg.getCategory("PreferredOres");

            for (Map.Entry<String, Property> entry : category.entrySet()) {
                String name = entry.getKey();
                Property property = entry.getValue();

                if (property.getString().length() == 0) {
                    // not set
                    continue;
                }

                oreName2PreferredMod.put(name, property.getString());
            }

            shouldDumpOreDict = cfg.get(Configuration.CATEGORY_GENERAL, "dumpOreDict", true).getBoolean(true);
            verbose = cfg.get(Configuration.CATEGORY_GENERAL, "verbose", true).getBoolean(true);

            // TODO: refactor
            replaceCrafting = cfg.get(Configuration.CATEGORY_GENERAL, "replaceCrafting", true).getBoolean(true);
            replaceFurnace = cfg.get(Configuration.CATEGORY_GENERAL, "replaceFurnace", true).getBoolean(true);
            replaceFurnaceInsensitive = cfg.get(Configuration.CATEGORY_GENERAL, "replaceFurnaceInsensitive", true).getBoolean(true);
            replaceDungeonLoot = cfg.get(Configuration.CATEGORY_GENERAL, "replaceDungeonLoot", true).getBoolean(true);
            replaceIC2Compressor = cfg.get(Configuration.CATEGORY_GENERAL, "replaceIC2Compressor", true).getBoolean(true);
            replaceIC2Extractor = cfg.get(Configuration.CATEGORY_GENERAL, "replaceIC2Extractor", true).getBoolean(true);
            replaceIC2Macerator = cfg.get(Configuration.CATEGORY_GENERAL, "replaceIC2Macerator", true).getBoolean(true);
            replaceIC2Scrapbox = cfg.get(Configuration.CATEGORY_GENERAL, "replaceIC2Scrapbox", true).getBoolean(true);
        } catch (Exception e) {
            FMLLog.log(Level.SEVERE, e, "OreDupeFix had a problem loading it's configuration");
        } finally {
            cfg.save();
        }
    }

    @PostInit
    public static void postInit(FMLPostInitializationEvent event) {
        log("Loading OreDupeFix...");

        if (shouldDumpOreDict) {
            dumpOreDict();
        }

        loadPreferredOres();

        if (replaceCrafting) replaceCraftingRecipes();
        if (replaceFurnace) replaceFurnaceRecipes();
        if (replaceFurnaceInsensitive) replaceFurnaceRecipesInsensitive();
        if (replaceDungeonLoot) replaceDungeonLoot();

        // IC2 machines
        try {
            if (replaceIC2Compressor) replaceIC2MachineRecipes(Ic2Recipes.getCompressorRecipes());
            if (replaceIC2Extractor) replaceIC2MachineRecipes(Ic2Recipes.getExtractorRecipes());
            if (replaceIC2Macerator) replaceIC2MachineRecipes(Ic2Recipes.getMaceratorRecipes());
            if (replaceIC2Scrapbox) replaceIC2ScrapboxDrops();
        } catch (Throwable t) {
            t.printStackTrace();
            FMLLog.log(Level.WARNING, "Failed to replace IC2 machine recipes: "+t.getLocalizedMessage()+", fix this (update?) or turn off replaceIC2* in config");
        }

        // TE machines
        /*
        // TODO - check TE API for 'replaceable recipes' setting
        ICrucibleRecipe[] iCrucibleRecipes = CraftingManagers.crucibleManager.getRecipeList();
        IFurnaceRecipe[] iFurnaceRecipes = CraftingManagers.furnaceManager.getRecipeList();
        IPulverizerRecipe[] iPulverizerRecipes = CraftingManagers.pulverizerManager.getRecipeList();
        ISawmillRecipe[] iSawmillRecipes = CraftingManagers.sawmillManager.getRecipeList();
        ISmelterRecipe[] iSmelterRecipes = CraftingManagers.smelterManager.getRecipeList();
        //ISmelterRecipe[] iFillRecipes F= CraftingManagers.transposerManager.getFillRecipeList(); // TODO
        */
    }

    public static void log(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }

    public static void loadDefaults(Configuration cfg) {
        ConfigCategory category = cfg.getCategory("PreferredOres");

        FMLLog.log(Level.FINE, "OreDupeFix initializing defaults");

        HashMap<String, String> m = new HashMap<String, String>();

        // populate with all ore names for documentation purposes, no overrides
        Map<Integer, ItemData> idMap = ReflectionHelper.getPrivateValue(GameData.class, null, "idMap");
        List<String> oreNames = Arrays.asList(OreDictionary.getOreNames());
        Collections.sort(oreNames);
        for (String oreName : oreNames) {
            m.put(oreName, "");
        }

        // a reasonable set of defaults
        m.put("ingotCopper", "RedPowerBase");
        m.put("ingotTin", "RedPowerBase");
        m.put("ingotBronze", "IC2");
        m.put("ingotSilver", "RedPowerBase");
        m.put("ingotLead", "ThermalExpansion");
        m.put("dustBronze", "ThermalExpansion");
        m.put("dustIron", "ThermalExpansion");
        m.put("dustTin", "ThermalExpansion");
        m.put("dustSilver", "ThermalExpansion");
        m.put("dustCopper", "ThermalExpansion");
        m.put("dustGold", "ThermalExpansion");
        m.put("dustObsidian", "ThermalExpansion");
        m.put("nuggetTin", "ThermalExpansion");
        m.put("nuggetCopper", "Thaumcraft");
        m.put("nuggetIron", "Thaumcraft");
        m.put("nuggetSilver", "Thaumcraft");
        m.put("nuggetTin", "Thaumcraft");
        m.put("oreCopper", "IC2");
        m.put("oreSilver", "ThermalExpansion");
        m.put("oreTin", "IC2");

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
                log("No mod '"+preferredModID+"' found for ore '"+oreName+"'! Skipping");
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

        List<String> oreNames = Arrays.asList(OreDictionary.getOreNames());
        Collections.sort(oreNames);

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


    /**
     * Get an equivalent but preferred item
     * @param output The existing ore dictionary item
     * @return A new ore dictionary item, for the name ore but preferred by the user
     */
    public static ItemStack getPreferredOre(ItemStack output) {
        int oreID = OreDictionary.getOreID(output);
        if (oreID == -1) {
            // this isn't an ore
            return null;
        }

        String oreName = OreDictionary.getOreName(oreID);
        ItemStack newOutputType = oreName2PreferredItem.get(oreName);

        if (newOutputType == null) {
            // no preference, do not replace
            return null;
        }

        // replace with new stack of same size but new type
        ItemStack newOutput = new ItemStack(newOutputType.itemID, output.stackSize, newOutputType.getItemDamage());

        return newOutput;
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

            log("Modifying recipe "+iRecipe+" replacing "+output.itemID+":"+output.getItemDamage()+" -> "+newOutput.itemID+":"+newOutput.getItemDamage());
            setCraftingRecipeOutput(iRecipe, newOutput);
        }

    }

    public static void setCraftingRecipeOutput(IRecipe iRecipe, ItemStack output) {
        if (iRecipe instanceof ShapedRecipes) {
            ReflectionHelper.setPrivateValue(ShapedRecipes.class,(ShapedRecipes)iRecipe, output, "field_77575_e"); // recipeOutput
        } else if (iRecipe instanceof ShapelessRecipes) {
            ReflectionHelper.setPrivateValue(ShapelessRecipes.class,(ShapelessRecipes)iRecipe, output, "field_77580_a"); // recipeOutput
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

    public static void replaceDungeonLoot() {
        try {
            HashMap<String, ChestGenHooks> chestInfo = ReflectionHelper.getPrivateValue(ChestGenHooks.class, null, "chestInfo");
            for (Map.Entry<String, ChestGenHooks> entry : chestInfo.entrySet()) {
                ChestGenHooks chestGenHooks = entry.getValue();

                ArrayList<WeightedRandomChestContent> contents = ReflectionHelper.getPrivateValue(ChestGenHooks.class, chestGenHooks, "contents");
                for (WeightedRandomChestContent weightedRandomChestContent : contents) {
                    ItemStack output = weightedRandomChestContent.theItemId;
                    ItemStack newOutput = getPreferredOre(output);
                    if (newOutput == null) {
                        continue;
                    }

                    log("Modifying dungeon loot in "+entry.getKey()+", replacing "+output.itemID+":"+output.getItemDamage()+" -> "+newOutput.itemID+":"+newOutput.getItemDamage());
                    weightedRandomChestContent.theItemId = newOutput;
                }
            }
        } catch (Throwable t) {
            System.out.println("Failed to replace dungeon loot: " + t);
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

    public static void replaceIC2ScrapboxDrops() {
        // Replace scrapbox drops in the item itself -- cannot use the API
        // Ic2Recipes.getScrapboxDrops() call since it returns a copy :(

        try {
            List dropList = ItemScrapbox.dropList;

            // 'Drop' inner class
            Class dropClass = ItemScrapbox.class.getDeclaredClasses()[0];

            for (int i = 0; i < dropList.size(); i++) {
                Object drop = dropList.get(i);

                ItemStack output = ReflectionHelper.getPrivateValue((Class<? super Object>)dropClass, drop, 0);

                ItemStack newOutput = getPreferredOre(output);
                if (newOutput == null) {
                    continue;
                }

                log("Modifying IC2 scrapbox drop, replacing "+output.itemID+":"+output.getItemDamage()+" -> "+newOutput.itemID+":"+newOutput.getItemDamage());
                ReflectionHelper.setPrivateValue(dropClass, drop, newOutput, 0);
            }
        } catch (Throwable t) {
            System.out.println("Failed to replace IC2 scrapbox drops: "+t);
        }
    }
}

