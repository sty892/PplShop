package styy.pplShop.pplshop.client.config;

public enum AutoRefreshMode {
    NEVER("config.pplshop.auto_refresh.never"),
    DAILY("config.pplshop.auto_refresh.daily"),
    WEEKLY("config.pplshop.auto_refresh.weekly");

    private final String translationKey;

    AutoRefreshMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public long staleThresholdMillis() {
        return switch (this) {
            case NEVER -> 7L * 24L * 60L * 60L * 1000L;
            case DAILY -> 24L * 60L * 60L * 1000L;
            case WEEKLY -> 7L * 24L * 60L * 60L * 1000L;
        };
    }

    public boolean shouldRefreshOnGuiOpen(long lastRefreshAtMillis, long nowMillis) {
        if (this == NEVER) {
            return false;
        }
        return this.shouldRefreshByAge(lastRefreshAtMillis, nowMillis);
    }

    public boolean shouldRefreshByAge(long lastRefreshAtMillis, long nowMillis) {
        if (lastRefreshAtMillis <= 0L) {
            return this != NEVER;
        }
        return nowMillis - lastRefreshAtMillis >= this.staleThresholdMillis();
    }
}
