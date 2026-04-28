package styy.pplShop.pplshop.client;

import org.junit.jupiter.api.Test;
import styy.pplShop.pplshop.client.config.RefreshUxConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveRefreshBudgetControllerTest {
    @Test
    void disabledAdaptiveModeKeepsFloorBudget() {
        RefreshUxConfig config = new RefreshUxConfig();
        config.refreshBudgetPerTick = 12;
        config.maxRefreshBudgetPerTick = 300;
        config.adaptiveRefreshBudget = false;
        config.sanitize();

        AdaptiveRefreshBudgetController controller = new AdaptiveRefreshBudgetController();
        assertEquals(12, controller.budgetForTick(144, config));
        assertEquals(12, controller.budgetForTick(20, config));
    }

    @Test
    void stableFpsLetsBudgetGrowTowardConfiguredCeiling() {
        RefreshUxConfig config = new RefreshUxConfig();
        config.refreshBudgetPerTick = 12;
        config.maxRefreshBudgetPerTick = 200;
        config.adaptiveRefreshBudget = true;
        config.sanitize();

        AdaptiveRefreshBudgetController controller = new AdaptiveRefreshBudgetController();
        int budget = controller.budgetForTick(120, config);
        for (int tick = 0; tick < 24; tick++) {
            budget = controller.budgetForTick(118, config);
        }

        assertTrue(budget > 12);
        assertTrue(budget <= 200);
    }

    @Test
    void heavyFpsDropCutsCurrentBudgetBackDown() {
        RefreshUxConfig config = new RefreshUxConfig();
        config.refreshBudgetPerTick = 20;
        config.maxRefreshBudgetPerTick = 240;
        config.adaptiveRefreshBudget = true;
        config.sanitize();

        AdaptiveRefreshBudgetController controller = new AdaptiveRefreshBudgetController();
        int budget = controller.budgetForTick(120, config);
        for (int tick = 0; tick < 24; tick++) {
            budget = controller.budgetForTick(120, config);
        }
        int grownBudget = budget;

        for (int tick = 0; tick < 8; tick++) {
            budget = controller.budgetForTick(18, config);
        }

        assertTrue(grownBudget > config.refreshBudgetPerTick);
        assertTrue(budget < grownBudget);
        assertTrue(budget >= config.refreshBudgetPerTick);
    }
}
