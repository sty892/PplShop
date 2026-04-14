package styy.pplShop.pplshop.client.gui;

public enum ShopSortMode {
    NAME("screen.pplshop.sort.name"),
    PRICE_ASC("screen.pplshop.sort.price_asc"),
    PRICE_DESC("screen.pplshop.sort.price_desc"),
    PRICE_PER_UNIT_ASC("screen.pplshop.sort.price_per_unit_asc"),
    PRICE_PER_UNIT_DESC("screen.pplshop.sort.price_per_unit_desc");

    private final String translationKey;

    ShopSortMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public ShopSortMode next(ShopPriceSortBasis priceSortBasis) {
        ShopSortMode ascending = priceSortBasis == ShopPriceSortBasis.PER_UNIT ? PRICE_PER_UNIT_ASC : PRICE_ASC;
        ShopSortMode descending = priceSortBasis == ShopPriceSortBasis.PER_UNIT ? PRICE_PER_UNIT_DESC : PRICE_DESC;
        return switch (this) {
            case NAME -> ascending;
            case PRICE_ASC, PRICE_PER_UNIT_ASC -> descending;
            case PRICE_DESC, PRICE_PER_UNIT_DESC -> NAME;
        };
    }

    public ShopSortMode withPriceSortBasis(ShopPriceSortBasis priceSortBasis) {
        return switch (this) {
            case PRICE_ASC, PRICE_PER_UNIT_ASC -> priceSortBasis == ShopPriceSortBasis.PER_UNIT ? PRICE_PER_UNIT_ASC : PRICE_ASC;
            case PRICE_DESC, PRICE_PER_UNIT_DESC -> priceSortBasis == ShopPriceSortBasis.PER_UNIT ? PRICE_PER_UNIT_DESC : PRICE_DESC;
            case NAME -> NAME;
        };
    }
}
