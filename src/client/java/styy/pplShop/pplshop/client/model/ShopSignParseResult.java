package styy.pplShop.pplshop.client.model;

public record ShopSignParseResult(
        ShopSignClassificationType classification,
        ShopSignDiagnosticReason reason,
        ShopSignFingerprint fingerprint,
        ShopSignEntry entry
) {
    public boolean isShop() {
        return this.classification == ShopSignClassificationType.SHOP
                || this.classification == ShopSignClassificationType.SHOP_WITH_UNKNOWN_ITEM;
    }
}
