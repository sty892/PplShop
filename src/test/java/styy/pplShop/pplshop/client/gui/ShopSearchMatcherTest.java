package styy.pplShop.pplshop.client.gui;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopSearchMatcherTest {
    @Test
    void smartQueryMatchesAliasesAndThemes() {
        assertTrue(ShopSearchMatcher.matchesSmartQuery("oak log\ndubovoe derevo", Set.of(), "dubovoe derevo", Set.of()));
        assertTrue(ShopSearchMatcher.matchesSmartQuery("plain blob", Set.of(ShopTheme.REDSTONE), "red", Set.of(ShopTheme.REDSTONE)));
    }

    @Test
    void rawTextQueryOnlyMatchesRealSignText() {
        assertTrue(ShopSearchMatcher.matchesRawTextQuery("дубовое дерево акция", "дерево"));
        assertFalse(ShopSearchMatcher.matchesRawTextQuery("oak log", "дерево"));
    }

    @Test
    void rawTextToggleAppearsOnlyWhenItAddsSomething() {
        assertTrue(ShopSearchMatcher.shouldShowRawTextToggle("дерево", false, 3, 7));
        assertFalse(ShopSearchMatcher.shouldShowRawTextToggle("дерево", false, 7, 7));
        assertTrue(ShopSearchMatcher.shouldShowRawTextToggle("дерево", true, 7, 7));
        assertFalse(ShopSearchMatcher.shouldShowRawTextToggle("", false, 0, 5));
    }
}
