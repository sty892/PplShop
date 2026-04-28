package styy.pplShop.pplshop.client.config;

import styy.pplShop.pplshop.client.gui.ShopPriceSortBasis;

public final class RefreshUxConfig {
    public boolean showStaleCacheWarning = true;
    public AutoRefreshMode autoRefreshMode = AutoRefreshMode.WEEKLY;
    public boolean suppressManualRefreshWarning = false;
    public boolean autoRefreshIfSnapshotTooSmall = false;
    public int minimumExpectedEntries = 100;
    public boolean persistCacheBetweenSessions = true;
    public int refreshBudgetPerTick = 12;
    public boolean adaptiveRefreshBudget = true;
    public int maxRefreshBudgetPerTick = 600;
    public ShopPriceSortBasis preferredPriceBasis = ShopPriceSortBasis.PER_UNIT;

    public static RefreshUxConfig defaults() {
        RefreshUxConfig config = new RefreshUxConfig();
        config.sanitize();
        return config;
    }

    public void sanitize() {
        if (this.autoRefreshMode == null) {
            this.autoRefreshMode = AutoRefreshMode.WEEKLY;
        }
        if (this.preferredPriceBasis == null) {
            this.preferredPriceBasis = ShopPriceSortBasis.PER_UNIT;
        }
        this.minimumExpectedEntries = clamp(this.minimumExpectedEntries, 1, 10_000);
        this.refreshBudgetPerTick = clamp(this.refreshBudgetPerTick, 1, 1_200);
        this.maxRefreshBudgetPerTick = clamp(this.maxRefreshBudgetPerTick, this.refreshBudgetPerTick, 1_200);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
