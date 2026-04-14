package styy.pplShop.pplshop.client.parser;

import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class ItemCandidateExtractor {
    private static final Pattern QUANTITY_STRIPPED_FIRST_TOKEN = Pattern.compile("^(\\w+)\\s+\\d+\\s*(?:стака?|стак|стек|шт|шб|ал|алм|алмаз)?(?:\\d+)?$");
    private static final Pattern DECORATIVE_ONLY = Pattern.compile("^[\\p{Punct}\\s=+|/*\\\\-]+$");
    private static final Pattern QUANTITY_STRIPPED_FIRST_TOKEN_SAFE = Pattern.compile(
            "^([\\p{L}\\p{N}_]+)\\s+\\d+\\s*(?:\\u0441\\u0442\\u0430\\u043a\\u0430?|\\u0441\\u0442\\u0430\\u043a|\\u0441\\u0442\\u0435\\u043a|\\u0448\\u0442|\\u0448\\u0431|\\u0430\\u043b|\\u0430\\u043b\\u043c|\\u0430\\u043b\\u043c\\u0430\\u0437)?(?:\\d+)?$"
    );
    private static final Pattern DECORATIVE_TOKEN = Pattern.compile("^[=+|/*\\\\_~#xXС…РҐ-]{3,}$");
    private static final Set<String> NOISE_MARKERS = Set.of(
            "Р С—РЎР‚Р В°Р Р†Р С‘Р В»",
            "\u043f\u0440\u0430\u0432\u0438\u043b",
            "pepeland",
            "РЎР‚Р ВµР С”Р В»Р В°Р С",
            "\u0440\u0435\u043a\u043b\u0430\u043c",
            "Р С•Р В±РЎР‰РЎРЏР Р†Р В»",
            "\u043e\u0431\u044a\u044f\u0432\u043b",
            "Р С‘Р Р…РЎвЂћР С•РЎР‚Р СР В°РЎвЂ ",
            "\u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446",
            "Р С‘Р Р…РЎвЂћР С•",
            "\u0438\u043d\u0444\u043e",
            "РЎРѓР С—Р В°РЎРѓР С‘Р В±Р С•",
            "\u0441\u043f\u0430\u0441\u0438\u0431\u043e",
            "Р С•Р В±Р СР ВµР Р…",
            "\u043e\u0431\u043c\u0435\u043d",
            "Р В°РЎР‚Р ВµР Р…Р Т‘",
            "\u0430\u0440\u0435\u043d\u0434"
    );
    private static final Set<String> SERVICE_TOKENS = Set.of(
            "\u0444\u0443\u043b\u043b",
            "\u0444\u0443\u043b",
            "\u0437\u0430\u0447\u0430\u0440",
            "full"
    );
    private static final Set<String> NON_ITEM_PHRASES = Set.of(
            "\u0431\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e",
            "\u0441\u043a\u043e\u043b\u044c\u043a\u043e \u0445\u043e\u0442\u0438\u0442\u0435",
            "\u0441\u043a\u043e\u043b\u044c\u043a\u043e \u043d\u0435 \u0436\u0430\u043b\u043a\u043e",
            "\u0441\u043a\u043e\u043b\u044c\u043a\u043e \u0443\u0433\u043e\u0434\u043d\u043e",
            "\u0431\u0435\u0440\u0438 \u0441\u043a\u043e\u043b\u044c\u043a\u043e \u0445\u043e\u0447\u0435\u0448\u044c",
            "\u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u043e",
            "\u043f\u0440\u043e\u0432\u0435\u0440\u0435\u043d\u043e",
            "\u0430\u043a\u0446\u0438\u044f",
            "\u0440\u0430\u0441\u043f\u0440\u043e\u0434\u0430\u0436\u0430",
            "\u043e\u0441\u0442\u0430\u043b\u043e\u0441\u044c",
            "\u043d\u0435\u0442 \u0432 \u043d\u0430\u043b\u0438\u0447\u0438\u0438"
    );
    private static final Set<String> PROMO_ONLY_TOKENS = Set.of(
            "\u0434\u0435\u0448\u0435\u0432\u044b\u0435",
            "\u0434\u0435\u0448\u0435\u0432\u043e",
            "\u043e\u043f\u0442",
            "\u043e\u043f\u0442\u043e\u043c",
            "\u0430\u043a\u0446\u0438\u044f",
            "\u0441\u043a\u0438\u0434\u043a\u0430",
            "\u0440\u0430\u0441\u043f\u0440\u043e\u0434\u0430\u0436\u0430",
            "\u0432\u044b\u0433\u043e\u0434\u043d\u043e",
            "\u043d\u0435\u0434\u043e\u0440\u043e\u0433\u043e",
            "\u0441\u0440\u043e\u0447\u043d\u043e",
            "\u043d\u043e\u0432\u0438\u043d\u043a\u0430"
    );

    private final ParserRulesConfig rules;

    ItemCandidateExtractor(ParserRulesConfig rules) {
        this.rules = rules;
    }

    Extraction extract(List<String> rawLines, int ownerLineIndex, int priceLineIndex) {
        List<PreparedLine> itemLines = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (int index = 0; index < rawLines.size(); index++) {
            String raw = rawLines.get(index);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            if (index == ownerLineIndex) {
                rejected.add("line" + (index + 1) + ": owner line skipped");
                continue;
            }
            if (index == priceLineIndex) {
                rejected.add("line" + (index + 1) + ": price line skipped");
                continue;
            }

            String prepared = this.prepareLine(raw);
            if (prepared.isBlank()) {
                rejected.add("line" + (index + 1) + ": decorative/noise line skipped");
                continue;
            }
            itemLines.add(new PreparedLine(index, prepared));
        }

        LinkedHashMap<String, ItemCandidate> candidates = new LinkedHashMap<>();
        if (!itemLines.isEmpty()) {
            String joinedTopLines = itemLines.stream()
                    .map(PreparedLine::text)
                    .reduce((left, right) -> left + " " + right)
                    .orElse("");
            this.addCandidate(candidates, joinedTopLines, itemLines.stream().map(PreparedLine::index).toList(), "joined-top-item-lines", 300);
            this.addCandidate(candidates, itemLines.getFirst().text(), List.of(itemLines.getFirst().index()), "first-item-line", 250);
            if (itemLines.size() >= 2) {
                this.addCandidate(
                        candidates,
                        itemLines.get(0).text() + " " + itemLines.get(1).text(),
                        List.of(itemLines.get(0).index(), itemLines.get(1).index()),
                        "first-second-item-lines",
                        220
                );
            }
        }

        return new Extraction(List.copyOf(candidates.values()), List.copyOf(rejected), itemLines.stream().map(PreparedLine::text).toList());
    }

    private void addCandidate(Map<String, ItemCandidate> candidates, String rawText, List<Integer> lineIndexes, String source, int priority) {
        if (rawText == null || rawText.isBlank()) {
            return;
        }
        String compact = NormalizationUtils.compactDisplay(rawText);
        String normalizedSource = NormalizationUtils.normalizeWithoutSorting(compact, this.rules);
        String normalizedPlain = this.stripServiceTokens(normalizedSource);
        String normalizedLookup = NormalizationUtils.normalizeForLookup(normalizedPlain, this.rules);
        if (normalizedPlain.isBlank() || normalizedLookup.isBlank()) {
            return;
        }
        this.putCandidate(candidates, new ItemCandidate(
                compact,
                normalizedPlain,
                normalizedLookup,
                List.copyOf(lineIndexes),
                source,
                priority,
                NormalizationUtils.tokenize(normalizedPlain).size(),
                normalizedPlain.replace(" ", "").length()
        ));

        String originalLookup = NormalizationUtils.normalizeForLookup(normalizedSource, this.rules);
        if (!normalizedSource.equals(normalizedPlain) && !originalLookup.isBlank()) {
            this.putCandidate(candidates, new ItemCandidate(
                    compact,
                    normalizedSource,
                    originalLookup,
                    List.copyOf(lineIndexes),
                    source + "-service-token-preserved",
                    Math.max(0, priority - 20),
                    NormalizationUtils.tokenize(normalizedSource).size(),
                    normalizedSource.replace(" ", "").length()
            ));
        }

        String quantityStripped = this.quantityStrippedFirstToken(originalLookup);
        if (!quantityStripped.isBlank()) {
            this.putCandidate(candidates, new ItemCandidate(
                    quantityStripped,
                    quantityStripped,
                    quantityStripped,
                    List.copyOf(lineIndexes),
                    "quantity-stripped-first-token",
                    Math.max(0, priority - 40),
                    1,
                    quantityStripped.length()
            ));
        }
    }

    private void putCandidate(Map<String, ItemCandidate> candidates, ItemCandidate next) {
        ItemCandidate previous = candidates.get(next.normalizedLookup());
        if (previous == null || next.isBetterSourceThan(previous)) {
            candidates.put(next.normalizedLookup(), next);
        }
    }

    private String quantityStrippedFirstToken(String normalizedLookup) {
        if (normalizedLookup == null || normalizedLookup.isBlank()) {
            return "";
        }
        java.util.regex.Matcher matcher = QUANTITY_STRIPPED_FIRST_TOKEN_SAFE.matcher(normalizedLookup);
        if (!matcher.matches()) {
            return "";
        }
        return matcher.group(1);
    }

    private String prepareLine(String input) {
        String compact = NormalizationUtils.compactDisplay(input);
        if (compact.isBlank()) {
            return "";
        }

        compact = compact
                .replace('/', ' ')
                .replace('(', ' ')
                .replace(')', ' ');
        compact = this.stripDecorativeEdges(compact);
        String normalized = NormalizationUtils.normalizeWithoutSorting(compact, this.rules);
        if (normalized.isBlank() || DECORATIVE_ONLY.matcher(normalized).matches() || this.isRepeatedSingleCharacter(normalized)) {
            return "";
        }
        if (this.isNonItemPhrase(normalized)) {
            return "";
        }
        if (this.isPromoNoiseOnly(normalized)) {
            return "";
        }
        if (this.stripServiceTokens(normalized).isBlank()) {
            return "";
        }
        for (String marker : NOISE_MARKERS) {
            if (normalized.contains(marker)) {
                return "";
            }
        }
        return compact;
    }

    private String stripDecorativeEdges(String input) {
        List<String> tokens = new ArrayList<>(List.of(input.trim().split("\\s+")));
        while (!tokens.isEmpty() && this.isDecorativeToken(tokens.getFirst())) {
            tokens.removeFirst();
        }
        while (!tokens.isEmpty() && this.isDecorativeToken(tokens.get(tokens.size() - 1))) {
            tokens.remove(tokens.size() - 1);
        }
        return String.join(" ", tokens).trim();
    }

    private boolean isDecorativeToken(String token) {
        if (token == null || token.isBlank()) {
            return true;
        }
        String trimmed = token.trim();
        if (DECORATIVE_TOKEN.matcher(trimmed).matches()) {
            return true;
        }
        String lettersOnly = trimmed.replaceAll("[^\\p{L}\\p{N}]", "");
        if (lettersOnly.length() < 3) {
            return false;
        }
        long distinctChars = lettersOnly.chars().map(Character::toLowerCase).distinct().count();
        return distinctChars <= 1;
    }

    record Extraction(List<ItemCandidate> candidates, List<String> rejectedCandidates, List<String> itemLines) {
    }

    private String stripServiceTokens(String normalizedPlain) {
        if (normalizedPlain == null || normalizedPlain.isBlank()) {
            return "";
        }
        return String.join(" ", NormalizationUtils.tokenize(normalizedPlain).stream()
                .filter(token -> !this.isServiceToken(token))
                .toList());
    }

    private boolean isNonItemPhrase(String normalized) {
        for (String phrase : NON_ITEM_PHRASES) {
            if (normalized.equals(phrase) || normalized.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPromoNoiseOnly(String normalized) {
        List<String> tokens = NormalizationUtils.tokenize(normalized);
        if (tokens.isEmpty()) {
            return true;
        }
        for (String token : tokens) {
            if (!PROMO_ONLY_TOKENS.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private boolean isRepeatedSingleCharacter(String normalized) {
        String compact = normalized == null ? "" : normalized.replace(" ", "");
        if (compact.length() < 3) {
            return false;
        }
        int first = compact.codePointAt(0);
        int index = Character.charCount(first);
        while (index < compact.length()) {
            int current = compact.codePointAt(index);
            if (Character.toLowerCase(current) != Character.toLowerCase(first)) {
                return false;
            }
            index += Character.charCount(current);
        }
        return true;
    }

    private boolean isServiceToken(String token) {
        if (SERVICE_TOKENS.contains(token)) {
            return true;
        }
        if (token.chars().allMatch(Character::isDigit)) {
            return true;
        }
        if (token.matches("\\d+[\\p{L}]") || token.matches("\\d{1,2}\\s*\\d{2}") || token.matches("\\d{1,2}:\\d{2}")) {
            return true;
        }
        return token.matches("i{1,4}|iv|vi{0,3}|lvl|lv");
    }

    record ItemCandidate(
            String rawText,
            String normalizedPlain,
            String normalizedLookup,
            List<Integer> lineIndexes,
            String source,
            int sourcePriority,
            int tokenCount,
            int charLength
    ) {
        private boolean isBetterSourceThan(ItemCandidate other) {
            if (this.sourcePriority != other.sourcePriority) {
                return this.sourcePriority > other.sourcePriority;
            }
            if (this.lineIndexes.size() != other.lineIndexes.size()) {
                return this.lineIndexes.size() > other.lineIndexes.size();
            }
            return this.charLength > other.charLength;
        }

        String describe() {
            return this.source + "('" + this.rawText + "', lookup='" + this.normalizedLookup + "', lines=" + this.lineIndexes + ")";
        }
    }

    private record PreparedLine(int index, String text) {
    }
}
