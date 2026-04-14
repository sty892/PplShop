package styy.pplShop.pplshop.client.world;

public record RefreshUiState(
        ShopDataState dataState,
        boolean snapshotLoadedFromDisk,
        boolean refreshInProgress,
        RefreshTriggerSource triggerSource,
        long lastRefreshTimeMillis,
        long snapshotAgeMillis,
        long staleThresholdMillis,
        int entryCount,
        int minimumExpectedEntries,
        String worldDisplayName,
        boolean showStaleWarning
) {
    public RefreshUiState {
        dataState = dataState == null ? ShopDataState.NEVER_REFRESHED : dataState;
        triggerSource = triggerSource == null ? RefreshTriggerSource.NONE : triggerSource;
        worldDisplayName = worldDisplayName == null ? "" : worldDisplayName;
    }

    public boolean isLowEntrySnapshot() {
        return this.entryCount < this.minimumExpectedEntries;
    }

    public boolean hasRefreshHistory() {
        return this.lastRefreshTimeMillis > 0L;
    }
}
