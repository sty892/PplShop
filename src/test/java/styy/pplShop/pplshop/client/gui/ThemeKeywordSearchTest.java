package styy.pplShop.pplshop.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeKeywordSearchTest {
    private final ThemeKeywordSearch search = new ThemeKeywordSearch();

    @Test
    void exactKeywordMatchesMappedTheme() {
        assertEquals(java.util.Set.of(ShopTheme.REDSTONE), this.search.resolve("редстоун"));
    }

    @Test
    void prefixKeywordMatchWorksAfterFourCharacters() {
        assertTrue(this.search.resolve("редс").contains(ShopTheme.REDSTONE));
    }

    @Test
    void combatKeywordCanReturnMultipleThemes() {
        assertTrue(this.search.resolve("бой").contains(ShopTheme.WEAPONS));
        assertTrue(this.search.resolve("бой").contains(ShopTheme.ARMOR));
    }
}
