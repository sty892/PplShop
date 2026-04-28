package styy.pplShop.pplshop.client;

import styy.pplShop.pplshop.client.config.RefreshUxConfig;

public final class AdaptiveRefreshBudgetController {
    private static final int MIN_SAMPLE_FPS = 10;
    private static final int MIN_USABLE_FPS = 24;
    private static final int ADJUSTMENT_INTERVAL_TICKS = 8;

    private boolean active;
    private int baselineFps;
    private int currentBudget;
    private int ticksUntilAdjustment;

    public void start(int sampledFps, RefreshUxConfig config) {
        int floorBudget = floorBudget(config);
        this.active = true;
        this.baselineFps = Math.max(MIN_USABLE_FPS, sampledFps);
        this.currentBudget = floorBudget;
        this.ticksUntilAdjustment = ADJUSTMENT_INTERVAL_TICKS;
    }

    public void stop() {
        this.active = false;
        this.baselineFps = 0;
        this.currentBudget = 0;
        this.ticksUntilAdjustment = 0;
    }

    public int budgetForTick(int sampledFps, RefreshUxConfig config) {
        int floorBudget = floorBudget(config);
        int ceilingBudget = ceilingBudget(config, floorBudget);
        if (!config.adaptiveRefreshBudget) {
            return floorBudget;
        }
        if (!this.active) {
            this.start(sampledFps, config);
            return this.currentBudget;
        }

        this.currentBudget = clamp(this.currentBudget, floorBudget, ceilingBudget);
        if (--this.ticksUntilAdjustment > 0) {
            return this.currentBudget;
        }
        this.ticksUntilAdjustment = ADJUSTMENT_INTERVAL_TICKS;

        int fps = Math.max(MIN_SAMPLE_FPS, sampledFps);
        if (fps * 2 < this.baselineFps || fps < MIN_USABLE_FPS) {
            this.currentBudget = Math.max(floorBudget, (this.currentBudget * 3) / 4);
            return this.currentBudget;
        }

        int growth = Math.max(8, this.currentBudget / 3);
        this.currentBudget = Math.min(ceilingBudget, this.currentBudget + growth);
        return this.currentBudget;
    }

    static int floorBudget(RefreshUxConfig config) {
        return clamp(config.refreshBudgetPerTick, 1, 1_200);
    }

    static int ceilingBudget(RefreshUxConfig config, int floorBudget) {
        return clamp(config.maxRefreshBudgetPerTick, floorBudget, 1_200);
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
