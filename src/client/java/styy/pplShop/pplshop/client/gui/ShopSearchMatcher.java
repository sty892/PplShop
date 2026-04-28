package styy.pplShop.pplshop.client.gui;

import java.util.Set;

final class ShopSearchMatcher {
    private ShopSearchMatcher() {
    }

    static boolean matchesSmartQuery(String searchBlob, Set<ShopTheme> themes, String normalizedQuery, Set<ShopTheme> matchedThemes) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return true;
        }
        return searchBlob.contains(normalizedQuery) || matchesThemeQuery(themes, matchedThemes);
    }

    static boolean matchesRawTextQuery(String rawTextBlob, String normalizedQuery) {
        return normalizedQuery != null
                && !normalizedQuery.isBlank()
                && rawTextBlob != null
                && rawTextBlob.contains(normalizedQuery);
    }

    static boolean shouldShowRawTextToggle(String normalizedQuery, boolean rawTextMode, int smartMatchCount, int rawTextMatchCount) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return false;
        }
        if (rawTextMode) {
            return rawTextMatchCount > 0;
        }
        return rawTextMatchCount > smartMatchCount;
    }

    private static boolean matchesThemeQuery(Set<ShopTheme> themes, Set<ShopTheme> matchedThemes) {
        if (matchedThemes == null || matchedThemes.isEmpty() || themes == null || themes.isEmpty()) {
            return false;
        }
        for (ShopTheme theme : matchedThemes) {
            if (themes.contains(theme)) {
                return true;
            }
        }
        return false;
    }
}
