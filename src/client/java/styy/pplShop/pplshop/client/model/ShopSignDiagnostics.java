package styy.pplShop.pplshop.client.model;

import java.util.List;

public record ShopSignDiagnostics(
        ShopSignClassificationType classification,
        ShopSignDiagnosticReason primaryReason,
        ShopSignDiagnosticReason cacheReason,
        int ownerLineIndex,
        int priceLineIndex,
        List<String> itemLines,
        String linkedContainerType,
        String linkedContainerRelation,
        String fingerprintSummary
) {
    public ShopSignDiagnostics {
        classification = classification == null ? ShopSignClassificationType.NOT_SHOP : classification;
        primaryReason = primaryReason == null ? ShopSignDiagnosticReason.NONE : primaryReason;
        cacheReason = cacheReason == null ? ShopSignDiagnosticReason.NONE : cacheReason;
        itemLines = itemLines == null ? List.of() : List.copyOf(itemLines);
        linkedContainerType = linkedContainerType == null ? "" : linkedContainerType;
        linkedContainerRelation = linkedContainerRelation == null ? "" : linkedContainerRelation;
        fingerprintSummary = fingerprintSummary == null ? "" : fingerprintSummary;
    }

    public static ShopSignDiagnostics empty() {
        return new ShopSignDiagnostics(
                ShopSignClassificationType.NOT_SHOP,
                ShopSignDiagnosticReason.NONE,
                ShopSignDiagnosticReason.NONE,
                -1,
                -1,
                List.of(),
                "",
                "",
                ""
        );
    }
}
