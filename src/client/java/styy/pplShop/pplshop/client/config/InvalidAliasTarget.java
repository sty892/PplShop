package styy.pplShop.pplshop.client.config;

public record InvalidAliasTarget(
        String sourceName,
        String bucketId,
        String runtimeItemId,
        String reason
) {
    public InvalidAliasTarget {
        sourceName = sourceName == null ? "" : sourceName;
        bucketId = bucketId == null ? "" : bucketId;
        runtimeItemId = runtimeItemId == null ? "" : runtimeItemId;
        reason = reason == null ? "" : reason;
    }
}
