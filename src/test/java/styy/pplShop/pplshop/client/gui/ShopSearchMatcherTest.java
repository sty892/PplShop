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
        assertTrue(ShopSearchMatcher.matchesRawTextQuery("dubovoe derevo akciya", "derevo"));
        assertFalse(ShopSearchMatcher.matchesRawTextQuery("oak log", "derevo"));
    }

    @Test
    void rawTextToggleNeedsNonBlankQuery() {
        assertTrue(ShopSearchMatcher.hasRawTextQuery("derevo"));
        assertFalse(ShopSearchMatcher.hasRawTextQuery(""));
        assertFalse(ShopSearchMatcher.hasRawTextQuery("   "));
    }
}
