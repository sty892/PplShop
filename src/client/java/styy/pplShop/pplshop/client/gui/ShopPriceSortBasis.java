package styy.pplShop.pplshop.client.gui;

public enum ShopPriceSortBasis {
    TOTAL("config.pplshop.preferred_price_basis.total"),
    PER_UNIT("config.pplshop.preferred_price_basis.per_unit");

    private final String translationKey;

    ShopPriceSortBasis(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public ShopPriceSortBasis next() {
        return this == TOTAL ? PER_UNIT : TOTAL;
    }
}
