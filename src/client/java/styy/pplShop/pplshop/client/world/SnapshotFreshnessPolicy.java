package styy.pplShop.pplshop.client.world;

public final class SnapshotFreshnessPolicy {
    private static final long FRESH_THRESHOLD_MILLIS = 24L * 60L * 60L * 1000L;
    private static final long STALE_THRESHOLD_MILLIS = 5L * 24L * 60L * 60L * 1000L;

    public ShopDataState classify(long lastRefreshTimeMillis, boolean refreshInProgress, boolean worldDirty) {
        if (refreshInProgress) {
            return ShopDataState.REFRESHING;
        }
        if (lastRefreshTimeMillis <= 0L) {
            return ShopDataState.NEVER_REFRESHED;
        }

        long ageMillis = this.snapshotAge(lastRefreshTimeMillis);
        if (ageMillis >= STALE_THRESHOLD_MILLIS) {
            return ShopDataState.STALE;
        }
        if (worldDirty || ageMillis >= FRESH_THRESHOLD_MILLIS) {
            return ShopDataState.AGING;
        }
        return ShopDataState.FRESH;
    }

    public long snapshotAge(long lastRefreshTimeMillis) {
        return lastRefreshTimeMillis <= 0L
                ? Long.MAX_VALUE
                : Math.max(0L, System.currentTimeMillis() - lastRefreshTimeMillis);
    }

    public long freshThresholdMillis() {
        return FRESH_THRESHOLD_MILLIS;
    }

    public long staleThresholdMillis() {
        return STALE_THRESHOLD_MILLIS;
    }
}
