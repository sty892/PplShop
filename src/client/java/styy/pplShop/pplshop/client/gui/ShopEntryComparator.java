package styy.pplShop.pplshop.client.gui;

import net.minecraft.util.Identifier;
import styy.pplShop.pplshop.client.model.ParseStatus;
import styy.pplShop.pplshop.client.model.ShopSignEntry;

import java.util.Comparator;

public final class ShopEntryComparator {
    private static final Identifier BARRIER_ITEM_ID = Identifier.of("minecraft", "barrier");
    private static final Comparator<ShopSignEntry> DEFAULT_TIE_BREAKER = Comparator
            .comparing(ShopSignEntry::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(entry -> safePrice(entry).normalizedDisplayText(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparingLong(entry -> entry.pos().asLong());

    private ShopEntryComparator() {
    }

    public static Comparator<ShopSignEntry> forMode(ShopSortMode mode) {
        return Comparator.comparingInt(ShopEntryComparator::unknownRank)
                .thenComparing(modeComparator(mode))
                .thenComparing(DEFAULT_TIE_BREAKER);
    }

    public static boolean isUnknownEntry(ShopSignEntry entry) {
        return entry == null
                || entry.resolvedItemId() == null
                || BARRIER_ITEM_ID.equals(entry.resolvedItemId())
                || entry.parsedItem().parseStatus() == ParseStatus.UNKNOWN;
    }

    private static Comparator<ShopSignEntry> modeComparator(ShopSortMode mode) {
        return switch (mode) {
            case PRICE_ASC -> Comparator
                    .comparingInt(ShopEntryComparator::priceMissingRank)
                    .thenComparingInt(entry -> safePrice(entry).amount() == null ? Integer.MAX_VALUE : safePrice(entry).amount());
            case PRICE_DESC -> Comparator
                    .comparingInt(ShopEntryComparator::priceMissingRank)
                    .thenComparing(ShopEntryComparator::priceAmountOrMin, Comparator.reverseOrder());
            case PRICE_PER_UNIT_ASC -> Comparator
                    .comparingInt(ShopEntryComparator::pricePerUnitMissingRank)
                    .thenComparing(ShopEntryComparator::pricePerUnitOrMax);
            case PRICE_PER_UNIT_DESC -> Comparator
                    .comparingInt(ShopEntryComparator::pricePerUnitMissingRank)
                    .thenComparing(ShopEntryComparator::pricePerUnitOrMin, Comparator.reverseOrder());
            case NAME -> Comparator.comparing(ShopSignEntry::displayName, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private static int unknownRank(ShopSignEntry entry) {
        return isUnknownEntry(entry) ? 1 : 0;
    }

    private static int priceMissingRank(ShopSignEntry entry) {
        return safePrice(entry).amount() == null ? 1 : 0;
    }

    private static Integer priceAmountOrMin(ShopSignEntry entry) {
        Integer amount = safePrice(entry).amount();
        return amount == null ? Integer.MIN_VALUE : amount;
    }

    private static int pricePerUnitMissingRank(ShopSignEntry entry) {
        return entry.pricePerUnit() == null ? 1 : 0;
    }

    private static Double pricePerUnitOrMax(ShopSignEntry entry) {
        Double pricePerUnit = entry.pricePerUnit();
        return pricePerUnit == null ? Double.MAX_VALUE : pricePerUnit;
    }

    private static Double pricePerUnitOrMin(ShopSignEntry entry) {
        Double pricePerUnit = entry.pricePerUnit();
        return pricePerUnit == null ? -Double.MAX_VALUE : pricePerUnit;
    }

    private static styy.pplShop.pplshop.client.model.ParsedPrice safePrice(ShopSignEntry entry) {
        return entry.parsedPrice();
    }
}
