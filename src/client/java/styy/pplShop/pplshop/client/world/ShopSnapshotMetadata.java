package styy.pplShop.pplshop.client.world;

public record ShopSnapshotMetadata(
        int schemaVersion,
        String worldKey,
        String worldDisplayName,
        String sessionFingerprint,
        String dimensionId,
        long lastRefreshTimeMillis,
        int entryCount,
        int lastKnownChunkCount,
        int lastKnownSignCount
) {
    public ShopSnapshotMetadata {
        worldKey = safe(worldKey);
        worldDisplayName = safe(worldDisplayName);
        sessionFingerprint = safe(sessionFingerprint);
        dimensionId = safe(dimensionId);
    }

    public long ageMillis(long nowMillis) {
        if (this.lastRefreshTimeMillis <= 0L) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, nowMillis - this.lastRefreshTimeMillis);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
