package styy.pplShop.pplshop.client.gui;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ThemeKeywordSearch {
    private static final Map<String, Set<ShopTheme>> KEYWORDS = createKeywordMap();

    public Set<ShopTheme> resolve(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return Set.of();
        }

        LinkedHashSet<ShopTheme> matchedThemes = new LinkedHashSet<>();
        for (Map.Entry<String, Set<ShopTheme>> entry : KEYWORDS.entrySet()) {
            if (matches(normalizedQuery, entry.getKey())) {
                matchedThemes.addAll(entry.getValue());
            }
        }
        return matchedThemes.isEmpty() ? Set.of() : Set.copyOf(matchedThemes);
    }

    static Map<String, Set<ShopTheme>> keywords() {
        return KEYWORDS;
    }

    private static boolean matches(String query, String keyword) {
        return query.equals(keyword) || (query.length() >= 4 && keyword.startsWith(query));
    }

    private static Map<String, Set<ShopTheme>> createKeywordMap() {
        LinkedHashMap<String, Set<ShopTheme>> keywords = new LinkedHashMap<>();
        register(keywords, EnumSet.of(ShopTheme.REDSTONE),
                "редстоун",
                "поршень",
                "залипающий поршень",
                "компаратор",
                "повторитель",
                "рычаг",
                "кнопка",
                "наблюдатель",
                "воронка",
                "раздатчик",
                "дроппер");
        register(keywords, EnumSet.of(ShopTheme.BUILDING),
                "строение",
                "строительный",
                "строительство");
        register(keywords, EnumSet.of(ShopTheme.FOOD),
                "еда",
                "пища");
        register(keywords, EnumSet.of(ShopTheme.POTIONS),
                "зелья",
                "зелье",
                "алхимия",
                "варка");
        register(keywords, EnumSet.of(ShopTheme.WEAPONS, ShopTheme.ARMOR),
                "бой",
                "оружие",
                "сражение");
        register(keywords, EnumSet.of(ShopTheme.TOOLS),
                "инструменты",
                "инструмент");
        register(keywords, EnumSet.of(ShopTheme.ARMOR),
                "броня",
                "доспехи");
        register(keywords, EnumSet.of(ShopTheme.FARM),
                "фермерство",
                "ферма",
                "урожай");
        register(keywords, EnumSet.of(ShopTheme.NATURAL, ShopTheme.DECORATIVE, ShopTheme.MOBS),
                "море",
                "океан",
                "морской");
        register(keywords, EnumSet.of(ShopTheme.NATURAL, ShopTheme.RESOURCES, ShopTheme.VALUABLES),
                "нижний мир",
                "незер",
                "ад");
        register(keywords, EnumSet.of(ShopTheme.RESOURCES, ShopTheme.VALUABLES, ShopTheme.MOBS),
                "эндер",
                "энд",
                "край");
        register(keywords, EnumSet.of(ShopTheme.DECORATIVE),
                "декор",
                "декорации",
                "украшения");
        return Map.copyOf(keywords);
    }

    private static void register(Map<String, Set<ShopTheme>> keywords, Set<ShopTheme> themes, String... aliases) {
        Set<ShopTheme> themeSet = Set.copyOf(themes);
        for (String alias : aliases) {
            keywords.put(alias, themeSet);
        }
    }
}
