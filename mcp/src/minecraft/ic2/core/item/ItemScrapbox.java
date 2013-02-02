package ic2.core.item;

import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Vector;

/**
 * Stub for modifying IC2 scrapbox drop list
 */
public class ItemScrapbox {
    public static List<Drop> dropList = new Vector<Drop>();

    static class Drop {
        ItemStack itemStack;
    }
}
