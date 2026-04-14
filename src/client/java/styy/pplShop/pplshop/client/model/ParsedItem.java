package styy.pplShop.pplshop.client.model;

import net.minecraft.util.Identifier;

public record ParsedItem(
        String rawText,
        String normalizedText,
        Identifier itemId,
        String matchedAlias,
        int parseConfidence,
        ParseStatus parseStatus,
        ItemResolutionTrace resolutionTrace,
        ItemResolutionResultType resultType,
        boolean safeResult,
        String resolvedBucketId,
        String resolvedSubtypeKey,
        String displayNameOverride
) {
    public ParsedItem {
        resolutionTrace = resolutionTrace == null ? ItemResolutionTrace.empty() : resolutionTrace;
        resultType = resultType == null ? ItemResolutionResultType.UNKNOWN : resultType;
        resolvedBucketId = resolvedBucketId == null ? "" : resolvedBucketId;
        resolvedSubtypeKey = resolvedSubtypeKey == null ? "" : resolvedSubtypeKey;
        displayNameOverride = displayNameOverride == null ? "" : displayNameOverride;
    }

    public ParsedItem(
            String rawText,
            String normalizedText,
            Identifier itemId,
            String matchedAlias,
            int parseConfidence,
            ParseStatus parseStatus,
            ItemResolutionTrace resolutionTrace
    ) {
        this(rawText, normalizedText, itemId, matchedAlias, parseConfidence, parseStatus, resolutionTrace, ItemResolutionResultType.UNKNOWN, itemId != null, "", "", "");
    }

    public boolean isResolved() {
        return this.itemId != null && this.safeResult;
    }
}
