package styy.pplShop.pplshop.client.parser;

import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.model.ShopSignClassificationType;
import styy.pplShop.pplshop.client.model.ShopSignDiagnosticReason;
import styy.pplShop.pplshop.client.model.SignContainerRelation;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ShopSignClassifier {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("(?i)^@?[a-z0-9_]{3,16}$");
    private static final Pattern DECORATIVE_ONLY = Pattern.compile("^[\\p{Punct}\\s=+|/*\\\\-]+$");
    private static final Pattern COMMAND_PATTERN = Pattern.compile(".*(?:^|\\s)/[a-zР°-СЏ0-9_]+.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DATE_PATTERN = Pattern.compile(".*\\b\\d{1,2}[./-]\\d{1,2}(?:[./-]\\d{2,4})?\\b.*");
    private static final Set<String> NOISE_MARKERS = Set.of(
            "РїСЂР°РІРёР»",
            "pepeland",
            "СЂРµРєР»Р°Рј",
            "РѕР±СЉСЏРІР»",
            "СЃРїР°СЃРёР±Рѕ",
            "РёРЅС„РѕСЂРјР°С†",
            "РёРЅС„Рѕ",
            "РѕР±РјРµРЅ",
            "Р°СЂРµРЅРґ"
    );
    private static final Set<String> NON_ITEM_MARKERS = Set.of(
            "\u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u043e",
            "\u043f\u0440\u043e\u0432\u0435\u0440\u0435\u043d\u043e",
            "\u0430\u043a\u0446\u0438\u044f",
            "\u0440\u0430\u0441\u043f\u0440\u043e\u0434\u0430\u0436\u0430",
            "\u043e\u0441\u0442\u0430\u043b\u043e\u0441\u044c",
            "\u043d\u0435\u0442 \u0432 \u043d\u0430\u043b\u0438\u0447\u0438\u0438"
    );
    private static final Set<String> BLACKLISTED_FIRST_LINE_KEYWORDS = Set.of(
            "\u043a\u0443\u043f\u043b\u044e",
            "\u043f\u043e\u043a\u0443\u043f\u0430\u044e",
            "\u043f\u043e\u043a\u0443\u043f\u043a\u0430",
            "\u0438\u0449\u0443",
            "wanted",
            "wts",
            "wtb",
            "\u043c\u0435\u043d\u044f\u044e",
            "\u043e\u0431\u043c\u0435\u043d",
            "\u043e\u0431\u043c\u0435\u043d\u044f\u044e",
            "\u043f\u0440\u043e\u0434\u0430\u043c",
            "\u043e\u0431\u044a\u044f\u0432\u043b\u0435\u043d\u0438\u0435",
            "\u0438\u043d\u0444\u043e",
            "\u0438\u043d\u0444\u0430",
            "\u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u044f",
            "\u0437\u0430\u043a\u0440\u044b\u0442\u043e",
            "\u043d\u0435\u0442 \u0432 \u043d\u0430\u043b\u0438\u0447\u0438\u0438",
            "\u0440\u0430\u0441\u043f\u0440\u043e\u0434\u0430\u043d\u043e",
            "\u043f\u0443\u0441\u0442\u043e"
    );

    private final ParserRulesConfig rules;

    public ShopSignClassifier(ParserRulesConfig rules) {
        this.rules = rules;
    }

    public Classification classify(List<String> lines, PriceParser.Extraction priceExtraction, SignContainerRelation relation) {
        if (this.hasBlacklistedFirstLineKeyword(lines)) {
            return new Classification(ShopSignClassificationType.NOT_SHOP, ShopSignDiagnosticReason.BLACKLISTED_KEYWORD, this.findOwnerLineIndex(lines, -1), -1, List.of());
        }
        int priceLineIndex = priceExtraction == null ? -1 : priceExtraction.lineIndex();
        if (relation == null || !relation.linked()) {
            return new Classification(ShopSignClassificationType.NOT_SHOP, ShopSignDiagnosticReason.NOT_SHOP_NO_CONTAINER, -1, priceLineIndex, List.of());
        }
        if (priceExtraction == null || priceExtraction.parsedPrice() == null || !priceExtraction.parsedPrice().hasAmount()) {
            return new Classification(ShopSignClassificationType.NOT_SHOP, ShopSignDiagnosticReason.NOT_SHOP_NO_PRICE, this.findOwnerLineIndex(lines, -1), -1, List.of());
        }

        int ownerLineIndex = this.findOwnerLineIndex(lines, priceLineIndex);
        List<String> itemLines = this.extractItemLines(lines, ownerLineIndex, priceLineIndex);

        if (itemLines.isEmpty()) {
            return new Classification(ShopSignClassificationType.NOT_SHOP, ShopSignDiagnosticReason.NOT_SHOP_NO_ITEM_LINES, ownerLineIndex, priceLineIndex, List.of());
        }
        return new Classification(ShopSignClassificationType.SHOP, ShopSignDiagnosticReason.NONE, ownerLineIndex, priceLineIndex, itemLines);
    }

    private boolean hasBlacklistedFirstLineKeyword(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        String firstLine = NormalizationUtils.stripDecorativeCharacters(lines.getFirst());
        if (firstLine.isBlank()) {
            return false;
        }
        for (String keyword : BLACKLISTED_FIRST_LINE_KEYWORDS) {
            String normalizedKeyword = NormalizationUtils.normalizeWithoutSorting(keyword, this.rules);
            if (!normalizedKeyword.isBlank() && (firstLine.startsWith(normalizedKeyword) || firstLine.contains(normalizedKeyword))) {
                return true;
            }
        }
        return false;
    }

    public int findOwnerLineIndex(List<String> lines) {
        return this.findOwnerLineIndex(lines, -1);
    }

    public int findOwnerLineIndex(List<String> lines, int priceLineIndex) {
        List<LineCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (index == priceLineIndex || line == null || line.isBlank()) {
                continue;
            }
            String compact = NormalizationUtils.compactDisplay(line);
            String normalized = NormalizationUtils.normalizeWithoutSorting(compact, this.rules);
            if (normalized.isBlank()) {
                continue;
            }
            candidates.add(new LineCandidate(index, compact, normalized));
        }

        for (int index = candidates.size() - 1; index >= 0; index--) {
            LineCandidate candidate = candidates.get(index);
            if (this.isDecorativeOrNoise(candidate.normalized())) {
                continue;
            }
            if (this.isStrongUsernameLike(candidate.compact(), candidate.normalized())) {
                return candidate.index();
            }
            if (index == candidates.size() - 1 && this.isWeakUsernameLike(candidate.compact(), candidate.normalized())) {
                return candidate.index();
            }
        }

        for (int index = candidates.size() - 1; index >= 0; index--) {
            LineCandidate candidate = candidates.get(index);
            if (this.isDecorativeOrNoise(candidate.normalized())) {
                continue;
            }
            if (!this.isWeakUsernameLike(candidate.compact(), candidate.normalized())) {
                continue;
            }
            if (this.hasLikelyItemLine(candidates, candidate.index())) {
                return candidate.index();
            }
        }
        return -1;
    }

    public List<String> extractItemLines(List<String> lines, int ownerLineIndex, int priceLineIndex) {
        List<String> itemLines = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null || line.isBlank()) {
                continue;
            }
            if (index == ownerLineIndex || index == priceLineIndex) {
                continue;
            }

            String normalized = NormalizationUtils.normalizeWithoutSorting(line, this.rules);
            if (normalized.isBlank() || this.isDecorativeOrNoise(normalized)) {
                continue;
            }
            itemLines.add(NormalizationUtils.compactDisplay(line));
        }
        return itemLines;
    }

    private boolean isStrongUsernameLike(String compact, String normalized) {
        if (!this.isUsernameLike(compact)) {
            return false;
        }
        if (compact.startsWith("@") || compact.contains("_")) {
            return true;
        }
        if (compact.chars().anyMatch(Character::isDigit)) {
            return true;
        }
        return !compact.equals(compact.toLowerCase(Locale.ROOT));
    }

    private boolean isWeakUsernameLike(String compact, String normalized) {
        return !compact.contains(" ")
                && !this.isDecorativeOrNoise(normalized)
                && this.isUsernameLike(compact);
    }

    private boolean isUsernameLike(String compact) {
        String visible = NormalizationUtils.normalizeVisibleText(compact);
        String stripped = visible.replaceAll("[^a-z0-9_\\s]", "").trim();
        if (stripped.contains(" ")) {
            return false;
        }
        return USERNAME_PATTERN.matcher(stripped).matches();
    }

    private boolean hasLikelyItemLine(List<LineCandidate> candidates, int ownerCandidateIndex) {
        for (LineCandidate candidate : candidates) {
            if (candidate.index() == ownerCandidateIndex || this.isDecorativeOrNoise(candidate.normalized())) {
                continue;
            }
            if (!this.isWeakUsernameLike(candidate.compact(), candidate.normalized())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDecorativeOrNoise(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return true;
        }
        if (DECORATIVE_ONLY.matcher(normalized).matches() || this.isRepeatedSingleCharacter(normalized)) {
            return true;
        }
        if (COMMAND_PATTERN.matcher(normalized).matches() || DATE_PATTERN.matcher(normalized).matches()) {
            return true;
        }
        for (String marker : NOISE_MARKERS) {
            if (normalized.contains(marker)) {
                return true;
            }
        }
        for (String marker : NON_ITEM_MARKERS) {
            if (normalized.equals(marker) || normalized.contains(marker)) {
                return true;
            }
        }
        for (String ignored : this.rules.combinedBlacklistedSignContains()) {
            String normalizedIgnored = NormalizationUtils.normalizeWithoutSorting(ignored, this.rules);
            if (!normalizedIgnored.isBlank() && normalized.contains(normalizedIgnored)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRepeatedSingleCharacter(String normalized) {
        String compact = normalized.replace(" ", "");
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

    public record Classification(
            ShopSignClassificationType type,
            ShopSignDiagnosticReason reason,
            int ownerLineIndex,
            int priceLineIndex,
            List<String> itemLines
    ) {
        public Classification {
            itemLines = itemLines == null ? List.of() : List.copyOf(itemLines);
        }
    }

    private record LineCandidate(int index, String compact, String normalized) {
    }
}
