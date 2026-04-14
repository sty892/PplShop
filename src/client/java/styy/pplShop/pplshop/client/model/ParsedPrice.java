package styy.pplShop.pplshop.client.model;

import net.minecraft.util.Identifier;

public record ParsedPrice(
        String rawText,
        Integer amount,
        String currencyKey,
        Identifier currencyItemId,
        int parseConfidence,
        ParseStatus parseStatus,
        String normalizedDisplayText,
        String matchedAlias,
        Integer quantityAmount,
        String quantityUnitKey,
        Integer quantityItemCount
) {
    public boolean hasAmount() {
        return this.amount != null;
    }
}
