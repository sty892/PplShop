package styy.pplShop.pplshop.client.model;

public enum ItemResolutionResultType {
    EXACT_ALIAS,
    NORMALIZED_EXACT,
    FUZZY_ALIAS,
    HIGH_CONFIDENCE_SHORTLIST,
    SAFE_FUZZY_SHORTLIST,
    SUGGESTED_FALLBACK,
    UNKNOWN
}
