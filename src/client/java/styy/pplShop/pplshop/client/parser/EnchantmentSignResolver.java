package styy.pplShop.pplshop.client.parser;

import net.minecraft.util.Identifier;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class EnchantmentSignResolver {
    private static final Identifier ENCHANTED_BOOK_ID = Identifier.of("minecraft", "enchanted_book");
    private static final Pattern TRAILING_LEVEL_PATTERN = Pattern.compile("\\s+(?:(?:lvl|лвл|ур|уровень)\\s*\\d+|\\d+|[ivxlcdm]+)\\s*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final List<EnchantmentAliasGroup> ENCHANTMENT_ALIASES = List.of(
            new EnchantmentAliasGroup("Починка", List.of("починка", "mending")),
            new EnchantmentAliasGroup("Прочность", List.of("прочность", "прочка", "прочка 3", "unbreaking")),
            new EnchantmentAliasGroup("Эффективность", List.of("эффективность", "эфективность", "efficiency")),
            new EnchantmentAliasGroup("Острота", List.of("острота", "sharpness")),
            new EnchantmentAliasGroup("Заговор огня", List.of("заговор огня", "fire aspect")),
            new EnchantmentAliasGroup("Разящий клинок", List.of("разящий клинок", "sweeping edge")),
            new EnchantmentAliasGroup("Добыча", List.of("добыча", "looting")),
            new EnchantmentAliasGroup("Защита", List.of("защита", "protection")),
            new EnchantmentAliasGroup("Защита от снарядов", List.of("защита от снарядов", "projectile protection")),
            new EnchantmentAliasGroup("Невесомость", List.of("невесомость", "плавное падение", "feather falling")),
            new EnchantmentAliasGroup("Подводное дыхание", List.of("подводное дыхание", "подводное дых", "подводник", "respiration")),
            new EnchantmentAliasGroup("Проворство", List.of("проворство", "swift sneak")),
            new EnchantmentAliasGroup("Удача", List.of("удача", "fortune")),
            new EnchantmentAliasGroup("Шёлковое касание", List.of("шелк", "шёлк", "шелковое касание", "шёлковое касание", "silk touch")),
            new EnchantmentAliasGroup("Скорость души", List.of("скорость души", "скорость душ", "soul speed")),
            new EnchantmentAliasGroup("Шипы", List.of("шипы", "thorns")),
            new EnchantmentAliasGroup("Пробитие", List.of("пробитие", "piercing")),
            new EnchantmentAliasGroup("Отдача", List.of("отдача", "knockback")),
            new EnchantmentAliasGroup("Плотность", List.of("плотность", "density")),
            new EnchantmentAliasGroup("Сила", List.of("сила", "power")),
            new EnchantmentAliasGroup("Тягун", List.of("тягун", "riptide")),
            new EnchantmentAliasGroup("Верность", List.of("верность", "loyalty")),
            new EnchantmentAliasGroup("Порыв ветра", List.of("порыв ветра", "wind burst"))
    );
    private static final List<String> SERVICE_LINES = List.of(
            "фулл зачар",
            "фул зачар",
            "фулл чары",
            "фул чары",
            "полный зачар",
            "полные чары",
            "full ench",
            "full enchant",
            "full enchanted"
    );

    private final ParserRulesConfig rules;
    private final Map<String, Match> enchantmentLookup = new LinkedHashMap<>();
    private final List<String> servicePhrases = new ArrayList<>();

    EnchantmentSignResolver(ParserRulesConfig rules) {
        this.rules = rules;

        for (EnchantmentAliasGroup group : ENCHANTMENT_ALIASES) {
            for (String alias : group.aliases()) {
                String normalized = this.canonicalLookup(alias);
                if (!normalized.isBlank()) {
                    this.enchantmentLookup.putIfAbsent(normalized, new Match(ENCHANTED_BOOK_ID, group.displayAlias()));
                }
            }
        }

        for (String serviceLine : SERVICE_LINES) {
            String normalized = this.canonicalLookup(serviceLine);
            if (!normalized.isBlank()) {
                this.servicePhrases.add(normalized);
            }
        }
    }

    Match resolve(String normalizedPlain, String normalizedLookup) {
        String canonical = this.canonicalLookup(!isBlank(normalizedPlain) ? normalizedPlain : normalizedLookup);
        if (canonical.isBlank() || this.isServiceLine(normalizedPlain, normalizedLookup)) {
            return null;
        }

        Match exact = this.enchantmentLookup.get(canonical);
        if (exact != null) {
            return exact;
        }

        for (Map.Entry<String, Match> entry : this.enchantmentLookup.entrySet()) {
            if (tokenSubsetMatch(canonical, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    boolean isServiceLine(String normalizedPlain, String normalizedLookup) {
        String canonical = this.canonicalLookup(!isBlank(normalizedPlain) ? normalizedPlain : normalizedLookup);
        if (canonical.isBlank()) {
            return false;
        }
        for (String phrase : this.servicePhrases) {
            if (canonical.equals(phrase)) {
                return true;
            }
        }
        return false;
    }

    private String canonicalLookup(String text) {
        if (isBlank(text)) {
            return "";
        }

        String canonical = NormalizationUtils.normalizeWithoutSorting(text, this.rules)
                .replace('/', ' ')
                .replace('|', ' ')
                .replace('0', 'о')
                .replace("шолковое", "шелковое")
                .replace("шёлковое", "шелковое")
                .replace("силк", "шелк")
                .replace("скорость душ", "скорость души")
                .replace("подводное дых", "подводное дыхание")
                .replace("подводник", "подводное дыхание")
                .replace("п0рыв", "порыв")
                .replace("pорыв", "порыв");

        List<String> tokens = new ArrayList<>();
        for (String token : NormalizationUtils.tokenize(canonical)) {
            String normalizedToken = token.toLowerCase(Locale.ROOT);
            if (!normalizedToken.isBlank()) {
                tokens.add(normalizedToken);
            }
        }

        canonical = String.join(" ", tokens).trim();
        canonical = TRAILING_LEVEL_PATTERN.matcher(canonical).replaceFirst("").trim();
        return NormalizationUtils.normalizeWithoutSorting(canonical, this.rules);
    }

    private static boolean tokenSubsetMatch(String candidate, String alias) {
        List<String> candidateTokens = NormalizationUtils.tokenize(candidate);
        List<String> aliasTokens = NormalizationUtils.tokenize(alias);
        if (candidateTokens.isEmpty() || aliasTokens.isEmpty()) {
            return false;
        }

        for (String aliasToken : aliasTokens) {
            boolean matched = false;
            for (String candidateToken : candidateTokens) {
                if (candidateToken.equals(aliasToken)
                        || candidateToken.startsWith(aliasToken)
                        || aliasToken.startsWith(candidateToken)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record Match(Identifier itemId, String matchedAlias) {
    }

    private record EnchantmentAliasGroup(String displayAlias, List<String> aliases) {
    }
}
