package styy.pplShop.pplshop.client.gui;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import styy.pplShop.pplshop.client.model.ParseStatus;
import styy.pplShop.pplshop.client.model.ParsedItem;
import styy.pplShop.pplshop.client.model.ParsedPrice;
import styy.pplShop.pplshop.client.model.ShopSignEntry;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopEntryComparatorTest {
    @Test
    void pricePerUnitSortPrefersCheaperStackOverSingleItem() {
        ShopSignEntry singleItem = entry(0, "Single", 1, 1, "item", 1);
        ShopSignEntry stack = entry(1, "Stack", 1, 1, "stack", 64);

        List<ShopSignEntry> sorted = List.of(singleItem, stack).stream()
                .sorted(ShopEntryComparator.forMode(ShopSortMode.PRICE_PER_UNIT_ASC))
                .toList();

        assertEquals(List.of(stack, singleItem), sorted);
        assertEquals(1.0D, singleItem.pricePerUnit());
        assertEquals(1.0D / 64.0D, stack.pricePerUnit());
    }

    @Test
    void totalPriceSortStillUsesRawPriceAmount() {
        ShopSignEntry cheaperTotal = entry(0, "Cheaper total", 1, 1, "item", 1);
        ShopSignEntry pricierTotal = entry(1, "Pricier total", 2, 1, "stack", 64);

        List<ShopSignEntry> sorted = List.of(pricierTotal, cheaperTotal).stream()
                .sorted(ShopEntryComparator.forMode(ShopSortMode.PRICE_ASC))
                .toList();

        assertEquals(List.of(cheaperTotal, pricierTotal), sorted);
    }

    private static ShopSignEntry entry(int x, String name, int amount, int quantityAmount, String quantityUnitKey, int quantityItemCount) {
        try {
            ShopSignEntry entry = (ShopSignEntry) unsafe().allocateInstance(ShopSignEntry.class);
            ParsedPrice parsedPrice = new ParsedPrice(
                    amount + " dia",
                    amount,
                    "diamond",
                    null,
                    100,
                    ParseStatus.EXACT,
                    amount + " dia",
                    "dia",
                    quantityAmount,
                    quantityUnitKey,
                    quantityItemCount
            );
            setField(entry, "pos", new BlockPos(x, 64, 0));
            setField(entry, "parsedPrice", parsedPrice);
            setField(entry, "parsedItem", new ParsedItem(name, name.toLowerCase(), null, null, 0, ParseStatus.UNKNOWN, null));
            setField(entry, "resolvedItemId", null);
            setField(entry, "resolvedDisplayName", name);
            setField(entry, "pricePerUnit", quantityItemCount <= 0 ? (double) amount : amount / (double) quantityItemCount);
            return entry;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to create synthetic ShopSignEntry", exception);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = ShopSignEntry.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Unsafe unsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }
}
