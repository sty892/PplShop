package styy.pplShop.pplshop.client.parser;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import styy.pplShop.pplshop.client.config.CurrencyAliasConfig;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.model.ParseStatus;
import styy.pplShop.pplshop.client.model.ParsedPrice;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PriceParser {
    private static final String QUANTITY_UNIT_ITEM = "item";
    private static final String QUANTITY_UNIT_STACK = "stack";
    private static final String QUANTITY_UNIT_SLOT = "slot";
    private static final String QUANTITY_UNIT_SHULKER = "shulker";
    private static final Logger LOGGER = LoggerFactory.getLogger("PPLShop");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final List<String> IGNORE_MARKERS = List.of(
            "\u0441\u043f\u0430\u0441\u0438\u0431\u043e",
            "\u0437\u0430\u0440\u0430\u043d\u0435\u0435 \u0441\u043f\u0430\u0441\u0438\u0431\u043e",
            "\u043d\u0430\u043f\u0438\u0441\u0430\u043d\u043e",
            "\u0446\u0435\u043d\u0430 \u0432\u043d\u0443\u0442\u0440\u0438",
            "\u0441\u043a\u043e\u0440\u043e \u043f\u043e\u043f\u043e\u043b\u043d\u044e"
    );
    private static final List<String> EXTRA_COUNT_WORDS = List.of(
            "\u0448\u0442",
            "\u0448\u0442\u0443\u043a\u0430",
            "\u0448\u0442\u0443\u043a\u0438",
            "\u0448\u0442\u0443\u043a",
            "\u0441\u0442",
            "\u0441\u0442\u0430\u043a",
            "\u0441\u0442\u0430\u043a\u0430\u043d",
            "\u0441\u0442\u0430\u043a\u0430",
            "\u0441\u0442\u0430\u043a\u043e\u0432",
            "\u0441\u043b\u043e\u0442",
            "\u0441\u043b\u043e\u0442\u0430",
            "\u0441\u043b\u043e\u0442\u043e\u0432",
            "\u0448\u0431",
            "\u0448\u0430\u043b\u043a\u0435\u0440",
            "\u0448\u0430\u043b\u043a\u0435\u0440 \u0431\u043e\u043a\u0441",
            "\u0441\u043b",
            "\u0431\u043b",
            "\u0431\u043b\u043e\u043a",
            "\u0431\u043b\u043e\u043a\u0430",
            "\u0431\u043b\u043e\u043a\u043e\u0432",
            "\u043b\u043e\u0442",
            "\u043b\u043e\u0442\u0430",
            "\u0441\u0435\u0442",
            "\u0441\u0435\u0442\u0430",
            "\u0441\u0435\u0442\u043e\u0432",
            "\u043a\u043d\u0438\u0433\u0430",
            "\u043a\u043d\u0438\u0433\u0438",
            "\u0432\u0435\u0434\u0440\u043e",
            "\u0432\u0435\u0434\u0440\u0430",
            "\u0432\u0451\u0434\u0440\u0430",
            "slot",
            "stack",
            "stacks",
            "block",
            "blocks",
            "pc",
            "pcs",
            "piece",
            "pieces"
    );
    private static final Set<String> DEFAULT_JOINERS = Set.of("\u0437\u0430", "for");
    private static final Map<String, List<String>> SUPPLEMENTAL_CURRENCY_ALIASES = Map.of(
            "diamond", List.of(
                    "\u0430\u043b\u043c",
                    "\u0430\u043b",
                    "\u0430\u043b\u043c\u0430\u0437",
                    "\u0430\u043b\u043c\u0430\u0437\u0430",
                    "\u0430\u043b\u043c\u0430\u0437\u043e\u0432",
                    "\u0430\u043b\u043c\u0430\u0437\u044b",
                    "a\u043b\u043c",
                    "a\u043b",
                    "diamond",
                    "diamonds"
            ),
            "diamond_block", List.of(
                    "\u0430\u0431",
                    "a\u0431",
                    "ab",
                    "\u0430\u043b\u043c \u0431\u043b\u043e\u043a",
                    "\u0430\u043b\u043c\u0430\u0437\u043d\u044b\u0439 \u0431\u043b\u043e\u043a",
                    "\u0430\u043b\u043c\u0430\u0437\u043d\u044b\u0435 \u0431\u043b\u043e\u043a\u0438",
                    "diamond block",
                    "diamond blocks"
            )
    );

    private final ParserRulesConfig rules;
    private final Map<String, CurrencyTarget> exactAliases = new LinkedHashMap<>();
    private final Map<String, CurrencyTarget> aliasesByKey = new LinkedHashMap<>();
    private final Map<String, String> countWordAliases = new LinkedHashMap<>();
    private final Map<String, String> countWordUnitKeys = new LinkedHashMap<>();
    private final Set<String> joinerWords = new LinkedHashSet<>();
    private final int maxAliasTokenCount;
    private final int maxCountWordTokenCount;

    public PriceParser(CurrencyAliasConfig config, ParserRulesConfig rules) {
        this.rules = rules;

        int aliasTokenCount = 1;
        for (CurrencyAliasConfig.CurrencyDefinition definition : config.currencies) {
            aliasTokenCount = Math.max(aliasTokenCount, this.registerCurrencyDefinition(definition));
        }
        for (Map.Entry<String, List<String>> entry : SUPPLEMENTAL_CURRENCY_ALIASES.entrySet()) {
            CurrencyTarget existing = this.aliasesByKey.get(entry.getKey());
            Identifier itemId = existing == null ? null : existing.itemId();
            for (String alias : entry.getValue()) {
                aliasTokenCount = Math.max(aliasTokenCount, this.registerCurrencyAlias(entry.getKey(), itemId, alias));
            }
        }
        this.maxAliasTokenCount = aliasTokenCount;

        LinkedHashSet<String> countWords = new LinkedHashSet<>();
        countWords.addAll(EXTRA_COUNT_WORDS);
        countWords.addAll(rules.quantity_suffixes);

        int countWordTokenCount = 1;
        for (String countWord : countWords) {
            String normalized = NormalizationUtils.normalizeWithoutSorting(countWord, rules);
            if (normalized.isBlank()) {
                continue;
            }
            this.countWordAliases.putIfAbsent(normalized, normalized);
            this.countWordUnitKeys.putIfAbsent(normalized, canonicalQuantityUnit(normalized));
            countWordTokenCount = Math.max(countWordTokenCount, NormalizationUtils.tokenize(normalized).size());
        }
        this.maxCountWordTokenCount = countWordTokenCount;

        this.joinerWords.addAll(DEFAULT_JOINERS);
        for (String joiner : rules.price_joiner_words) {
            String normalizedJoiner = NormalizationUtils.normalizeWithoutSorting(joiner, rules);
            if (!normalizedJoiner.isBlank()) {
                this.joinerWords.add(normalizedJoiner);
            }
        }
    }

    public ParsedPrice parse(List<String> lines) {
        return this.extract(lines).parsedPrice();
    }

    public Extraction extract(List<String> lines) {
        List<CandidateLine> candidateLines = this.buildCandidateLines(lines);
        List<String> normalizedCandidates = new ArrayList<>(candidateLines.size());
        List<String> rejectionReasons = new ArrayList<>(candidateLines.size());

        PriceMatch best = null;
        for (CandidateLine candidate : candidateLines) {
            ParseAttempt attempt = this.parseSingle(candidate.text(), candidate.index());
            normalizedCandidates.add("line" + (candidate.index() + 1) + "=" + attempt.normalizedCandidate());
            if (attempt.match() == null) {
                rejectionReasons.add("line" + (candidate.index() + 1) + "=" + attempt.rejectionReason());
                continue;
            }

            if (best == null || attempt.match().confidence() > best.confidence()) {
                best = attempt.match();
            }
            if (attempt.match().confidence() >= 120) {
                break;
            }
        }

        ParsedPrice parsedPrice = best == null ? this.unknownPrice() : best.parsedPrice();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "PPLShop price debug: rawLines={} normalizedCandidatePriceLines={} matchedPricePattern={} parsedPriceText={} rejectionReason={}",
                    this.sanitizeLines(lines),
                    normalizedCandidates,
                    best == null ? "-" : best.patternName(),
                    parsedPrice.normalizedDisplayText(),
                    best == null ? String.join(" | ", rejectionReasons) : "-"
            );
        }

        return new Extraction(
                parsedPrice,
                this.buildCleanedLines(lines, best),
                best == null ? null : best.fragment(),
                best == null ? -1 : best.lineIndex()
        );
    }

    public List<String> aliasesFor(String currencyKey) {
        return this.exactAliases.values().stream()
                .filter(target -> target.key().equals(currencyKey))
                .map(CurrencyTarget::rawAlias)
                .distinct()
                .toList();
    }

    private List<CandidateLine> buildCandidateLines(List<String> lines) {
        List<CandidateLine> candidates = new ArrayList<>();
        for (int index : this.rules.preferred_price_lines) {
            if (index >= 0 && index < lines.size() && !this.isIgnorablePriceLine(lines.get(index), index)) {
                candidates.add(new CandidateLine(index, lines.get(index)));
            }
        }

        if (this.rules.scan_all_lines_for_price) {
            for (int i = 0; i < lines.size(); i++) {
                CandidateLine candidate = new CandidateLine(i, lines.get(i));
                if (!candidates.contains(candidate) && !this.isIgnorablePriceLine(candidate.text(), i)) {
                    candidates.add(candidate);
                }
            }
        }
        return candidates;
    }

    private List<String> buildCleanedLines(List<String> lines, PriceMatch matchedPrice) {
        List<String> cleaned = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            String prepared = this.preparePriceText(this.stripPricePrefixes(lines.get(index)));
            if (matchedPrice != null && matchedPrice.lineIndex() == index) {
                prepared = this.removeCharRange(prepared, matchedPrice.start(), matchedPrice.end());
            }

            prepared = this.stripAllPriceFragments(prepared);
            prepared = NormalizationUtils.stripDecorativeCharacters(prepared)
                    .replaceAll("\\s*(?:[-=/]|->|=>|\\u279c)+\\s*", " ");
            cleaned.add(NormalizationUtils.compactDisplay(prepared));
        }
        return cleaned;
    }

    private ParseAttempt parseSingle(String rawLine, int lineIndex) {
        String prepared = this.preparePriceText(this.stripPricePrefixes(rawLine));
        String normalizedCandidate = this.normalizeCandidateLine(prepared);
        if (normalizedCandidate.isBlank()) {
            return new ParseAttempt("", null, "blank after normalization");
        }

        PriceMatch match = this.findPriceMatch(prepared, lineIndex);
        if (match == null) {
            return new ParseAttempt(normalizedCandidate, null, "no supported price pattern matched");
        }
        return new ParseAttempt(normalizedCandidate, match, null);
    }

    private PriceMatch findPriceMatch(String preparedLine, int lineIndex) {
        List<TokenSpan> tokens = this.tokenizeWithPositions(preparedLine);
        if (tokens.isEmpty()) {
            return null;
        }

        PriceMatch countThenPrice = this.matchCountThenPrice(preparedLine, tokens, lineIndex);
        if (countThenPrice != null) {
            return countThenPrice;
        }

        PriceMatch priceThenCount = this.matchPriceThenCount(preparedLine, tokens, lineIndex);
        if (priceThenCount != null) {
            return priceThenCount;
        }

        return this.matchCurrencyOnly(preparedLine, tokens, lineIndex);
    }

    private PriceMatch matchCountThenPrice(String preparedLine, List<TokenSpan> tokens, int lineIndex) {
        for (int index = 0; index < tokens.size(); index++) {
            QuantityMatch quantity = this.matchQuantity(tokens, index);
            if (quantity == null || quantity.endIndexExclusive() >= tokens.size()) {
                continue;
            }

            Integer amount = this.parseInteger(tokens.get(quantity.endIndexExclusive()).normalized());
            if (amount == null) {
                continue;
            }

            CurrencyMatch currency = this.matchCurrency(tokens, quantity.endIndexExclusive() + 1);
            if (currency == null) {
                continue;
            }

            String displayText = this.formatPriceText(amount, currency.displayText(), quantity.amount(), quantity.displayText());
            return this.buildPriceMatch(
                    preparedLine,
                    tokens,
                    lineIndex,
                    quantity.startIndex(),
                    currency.endIndexExclusive(),
                    "count_then_price",
                    120,
                    amount,
                    currency,
                    quantity,
                    displayText
            );
        }
        return null;
    }

    private PriceMatch matchPriceThenCount(String preparedLine, List<TokenSpan> tokens, int lineIndex) {
        for (int index = 0; index < tokens.size(); index++) {
            Integer amount = this.parseInteger(tokens.get(index).normalized());
            if (amount == null) {
                continue;
            }

            CurrencyMatch currency = this.matchCurrency(tokens, index + 1);
            if (currency == null) {
                continue;
            }

            int cursor = currency.endIndexExclusive();
            boolean explicitJoiner = false;
            if (cursor < tokens.size() && this.joinerWords.contains(tokens.get(cursor).normalized())) {
                explicitJoiner = true;
                cursor++;
            }

            QuantityMatch quantity = this.matchQuantity(tokens, cursor);
            if (quantity == null) {
                continue;
            }

            String displayText = this.formatPriceText(amount, currency.displayText(), quantity.amount(), quantity.displayText());
            return this.buildPriceMatch(
                    preparedLine,
                    tokens,
                    lineIndex,
                    index,
                    quantity.endIndexExclusive(),
                    explicitJoiner ? "price_then_count_joiner" : "price_then_count",
                    explicitJoiner ? 115 : 105,
                    amount,
                    currency,
                    quantity,
                    displayText
            );
        }
        return null;
    }

    private PriceMatch matchCurrencyOnly(String preparedLine, List<TokenSpan> tokens, int lineIndex) {
        for (int index = tokens.size() - 1; index >= 0; index--) {
            Integer amount = this.parseInteger(tokens.get(index).normalized());
            if (amount == null) {
                continue;
            }

            CurrencyMatch currency = this.matchCurrency(tokens, index + 1);
            if (currency == null) {
                continue;
            }

            return this.buildPriceMatch(
                    preparedLine,
                    tokens,
                    lineIndex,
                    index,
                    currency.endIndexExclusive(),
                    "currency_only",
                    70,
                    amount,
                    currency,
                    null,
                    amount + " " + currency.displayText()
            );
        }
        return null;
    }

    private PriceMatch buildPriceMatch(
            String preparedLine,
            List<TokenSpan> tokens,
            int lineIndex,
            int startTokenIndex,
            int endTokenIndexExclusive,
            String patternName,
            int confidence,
            int amount,
            CurrencyMatch currency,
            QuantityMatch quantity,
            String displayText
    ) {
        int start = tokens.get(startTokenIndex).start();
        int end = tokens.get(endTokenIndexExclusive - 1).end();
        String fragment = preparedLine.substring(start, end).trim();

        ParsedPrice parsedPrice = new ParsedPrice(
                fragment,
                amount,
                currency.target().key(),
                currency.target().itemId(),
                confidence,
                ParseStatus.EXACT,
                displayText,
                currency.target().rawAlias(),
                quantity == null ? null : quantity.amount(),
                quantity == null ? null : quantity.unitKey(),
                quantity == null ? null : quantity.itemCount()
        );
        return new PriceMatch(lineIndex, start, end, fragment, parsedPrice, confidence, patternName);
    }

    private QuantityMatch matchQuantity(List<TokenSpan> tokens, int startIndex) {
        if (startIndex < 0 || startIndex >= tokens.size()) {
            return null;
        }

        Integer amount = this.parseInteger(tokens.get(startIndex).normalized());
        int cursor = startIndex;
        if (amount != null) {
            cursor++;
        } else {
            amount = 1;
        }

        CountWordMatch countWord = this.matchCountWord(tokens, cursor);
        if (countWord != null) {
            return new QuantityMatch(
                    startIndex,
                    countWord.endIndexExclusive(),
                    amount,
                    countWord.displayText(),
                    countWord.unitKey(),
                    quantityItemCount(amount, countWord.unitKey())
            );
        }

        GenericUnitMatch genericUnit = this.matchGenericUnit(tokens, cursor);
        if (genericUnit != null) {
            return new QuantityMatch(
                    startIndex,
                    genericUnit.endIndexExclusive(),
                    amount,
                    genericUnit.displayText(),
                    genericUnit.unitKey(),
                    quantityItemCount(amount, genericUnit.unitKey())
            );
        }
        return null;
    }

    private CurrencyMatch matchCurrency(List<TokenSpan> tokens, int startIndex) {
        if (startIndex < 0 || startIndex >= tokens.size()) {
            return null;
        }

        for (int end = Math.min(tokens.size(), startIndex + this.maxAliasTokenCount); end > startIndex; end--) {
            String alias = this.joinNormalized(tokens, startIndex, end);
            CurrencyTarget target = this.exactAliases.get(alias);
            if (target != null) {
                return new CurrencyMatch(startIndex, end, target, target.normalizedAlias());
            }
        }
        return null;
    }

    private CountWordMatch matchCountWord(List<TokenSpan> tokens, int startIndex) {
        if (startIndex < 0 || startIndex >= tokens.size()) {
            return null;
        }

        for (int end = Math.min(tokens.size(), startIndex + this.maxCountWordTokenCount); end > startIndex; end--) {
            String alias = this.joinNormalized(tokens, startIndex, end);
            String displayText = this.countWordAliases.get(alias);
            if (displayText != null) {
                return new CountWordMatch(startIndex, end, displayText, this.countWordUnitKeys.getOrDefault(alias, canonicalQuantityUnit(alias)));
            }
        }
        return null;
    }

    private GenericUnitMatch matchGenericUnit(List<TokenSpan> tokens, int startIndex) {
        if (startIndex < 0 || startIndex >= tokens.size()) {
            return null;
        }

        int end = startIndex;
        while (end < tokens.size() && end < startIndex + 2) {
            String token = tokens.get(end).normalized();
            if (this.parseInteger(token) != null || this.joinerWords.contains(token) || this.matchCurrency(tokens, end) != null) {
                break;
            }
            end++;
        }

        if (end == startIndex) {
            return null;
        }
        String normalizedUnit = this.joinNormalized(tokens, startIndex, end);
        return new GenericUnitMatch(startIndex, end, normalizedUnit, canonicalQuantityUnit(normalizedUnit));
    }

    private List<TokenSpan> tokenizeWithPositions(String input) {
        List<TokenSpan> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);
        while (matcher.find()) {
            String rawToken = this.normalizeMixedScriptToken(matcher.group());
            String normalized = NormalizationUtils.normalizeWithoutSorting(rawToken, this.rules);
            if (!normalized.isBlank()) {
                tokens.add(new TokenSpan(matcher.start(), matcher.end(), normalized));
            }
        }
        return tokens;
    }

    private int registerCurrencyDefinition(CurrencyAliasConfig.CurrencyDefinition definition) {
        Identifier itemId = Identifier.tryParse(definition.item_id());
        if (definition.aliases() == null) {
            return 1;
        }

        int maxTokenCount = 1;
        for (String alias : definition.aliases()) {
            maxTokenCount = Math.max(maxTokenCount, this.registerCurrencyAlias(definition.key(), itemId, alias));
        }
        return maxTokenCount;
    }

    private int registerCurrencyAlias(String key, Identifier itemId, String alias) {
        String normalized = NormalizationUtils.normalizeWithoutSorting(this.normalizeMixedScriptToken(alias), this.rules);
        if (normalized.isBlank()) {
            return 1;
        }

        CurrencyTarget target = new CurrencyTarget(key, itemId, alias, normalized);
        this.exactAliases.putIfAbsent(normalized, target);
        this.aliasesByKey.putIfAbsent(key, target);
        return Math.max(1, NormalizationUtils.tokenize(normalized).size());
    }

    private String joinNormalized(List<TokenSpan> tokens, int startInclusive, int endExclusive) {
        StringBuilder builder = new StringBuilder();
        for (int i = startInclusive; i < endExclusive; i++) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(tokens.get(i).normalized());
        }
        return builder.toString();
    }

    private String stripPricePrefixes(String rawLine) {
        String raw = rawLine == null ? "" : rawLine.trim();
        String normalized = NormalizationUtils.normalizeWithoutSorting(raw, this.rules);
        for (String prefix : this.rules.price_prefixes) {
            String normalizedPrefix = NormalizationUtils.normalizeWithoutSorting(prefix, this.rules);
            if (normalized.startsWith(normalizedPrefix + " ")) {
                int separatorIndex = raw.indexOf(':');
                if (separatorIndex >= 0 && separatorIndex + 1 < raw.length()) {
                    return raw.substring(separatorIndex + 1).trim();
                }
                return raw.substring(Math.min(raw.length(), prefix.length())).trim();
            }
        }
        return raw;
    }

    private String preparePriceText(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        return NormalizationUtils.compactDisplay(input)
                .replace('\u2014', '-')
                .replace('\u2013', '-')
                .replace('\u2212', '-')
                .replace('\u2192', ' ')
                .replace('\u279c', ' ')
                .replace('\u27a1', ' ')
                .replace('+', ' ')
                .replaceAll("(?<=\\d)(?=\\p{L})", " ")
                .replaceAll("(?<=\\p{L})(?=\\d)", " ");
    }

    private String stripAllPriceFragments(String preparedLine) {
        if (preparedLine.isBlank()) {
            return preparedLine;
        }

        String working = preparedLine;
        for (int attempt = 0; attempt < 4; attempt++) {
            PriceMatch match = this.findPriceMatch(working, -1);
            if (match == null) {
                break;
            }
            working = this.removeCharRange(working, match.start(), match.end());
        }
        return NormalizationUtils.compactDisplay(working);
    }

    private boolean isIgnorablePriceLine(String rawLine, int lineIndex) {
        if (rawLine == null || rawLine.isBlank()) {
            return true;
        }

        String normalized = this.normalizeCandidateLine(this.preparePriceText(rawLine));
        if (normalized.isBlank()) {
            return true;
        }
        if (lineIndex == 3 && this.looksLikePlayerName(normalized)) {
            return true;
        }
        if (!NormalizationUtils.containsDigits(normalized) && !this.containsCurrencyAlias(normalized)) {
            return true;
        }

        for (String marker : IGNORE_MARKERS) {
            String normalizedMarker = NormalizationUtils.normalizeWithoutSorting(marker, this.rules);
            if (normalized.contains(normalizedMarker)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCurrencyAlias(String normalized) {
        return this.exactAliases.keySet().stream().anyMatch(normalized::contains);
    }

    private String normalizeCandidateLine(String preparedLine) {
        return NormalizationUtils.normalizeWithoutSorting(this.normalizeMixedScriptToken(preparedLine), this.rules);
    }

    private String formatPriceText(int amount, String currencyText, int itemCount, String itemUnitText) {
        if (itemUnitText == null || itemUnitText.isBlank()) {
            return amount + " " + currencyText;
        }
        return amount + " " + currencyText + " \u0437\u0430 " + itemCount + " " + itemUnitText;
    }

    private static String canonicalQuantityUnit(String normalizedUnit) {
        if (normalizedUnit == null || normalizedUnit.isBlank()) {
            return "";
        }
        return switch (normalizedUnit) {
            case "\u0448\u0442", "\u0448\u0442\u0443\u043a\u0430", "\u0448\u0442\u0443\u043a\u0438", "\u0448\u0442\u0443\u043a",
                 "pc", "pcs", "piece", "pieces" -> QUANTITY_UNIT_ITEM;
            case "\u0441\u0442", "\u0441\u0442\u0430\u043a", "\u0441\u0442\u0430\u043a\u0430", "\u0441\u0442\u0430\u043a\u043e\u0432",
                 "\u0441\u0442\u0430\u043a\u0430\u043d", "stack", "stacks" -> QUANTITY_UNIT_STACK;
            case "\u0441\u043b\u043e\u0442", "\u0441\u043b\u043e\u0442\u0430", "\u0441\u043b\u043e\u0442\u043e\u0432", "slot" -> QUANTITY_UNIT_SLOT;
            case "\u0448\u0431", "\u0448\u0430\u043b\u043a\u0435\u0440", "\u0448\u0430\u043b\u043a\u0435\u0440 \u0431\u043e\u043a\u0441" -> QUANTITY_UNIT_SHULKER;
            default -> "";
        };
    }

    private static Integer quantityItemCount(int amount, String unitKey) {
        if (amount <= 0 || unitKey == null || unitKey.isBlank()) {
            return null;
        }
        return switch (unitKey) {
            case QUANTITY_UNIT_ITEM -> amount;
            case QUANTITY_UNIT_STACK, QUANTITY_UNIT_SLOT -> amount * 64;
            case QUANTITY_UNIT_SHULKER -> amount * 1728;
            default -> null;
        };
    }

    private String normalizeMixedScriptToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        boolean hasCyrillic = raw.codePoints().anyMatch(codePoint -> Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.CYRILLIC);
        if (!hasCyrillic) {
            return raw;
        }
        return raw.replace('a', '\u0430').replace('A', '\u0410');
    }

    private String removeCharRange(String input, int startInclusive, int endExclusive) {
        if (input == null || input.isBlank()) {
            return "";
        }
        if (startInclusive < 0 || endExclusive <= startInclusive || startInclusive >= input.length()) {
            return input;
        }

        int safeEnd = Math.min(endExclusive, input.length());
        String combined = input.substring(0, startInclusive) + " " + input.substring(safeEnd);
        return NormalizationUtils.compactDisplay(combined);
    }

    private boolean looksLikePlayerName(String normalized) {
        return normalized.matches("^@?[a-z\\u0430-\\u044f0-9_]{3,20}$");
    }

    private String sanitizeLines(List<String> lines) {
        List<String> sanitized = new ArrayList<>(lines.size());
        for (String line : lines) {
            sanitized.add(sanitize(line));
        }
        return sanitized.toString();
    }

    private static String sanitize(String raw) {
        return raw == null ? "" : raw.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private Integer parseInteger(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private ParsedPrice unknownPrice() {
        return new ParsedPrice("", null, null, null, 0, ParseStatus.UNKNOWN, null, null, null, null, null);
    }

    public record Extraction(ParsedPrice parsedPrice, List<String> cleanedLines, String matchedFragment, int lineIndex) {
    }

    private record CurrencyTarget(String key, Identifier itemId, String rawAlias, String normalizedAlias) {
    }

    private record CandidateLine(int index, String text) {
    }

    private record PriceMatch(
            int lineIndex,
            int start,
            int end,
            String fragment,
            ParsedPrice parsedPrice,
            int confidence,
            String patternName
    ) {
    }

    private record ParseAttempt(String normalizedCandidate, PriceMatch match, String rejectionReason) {
    }

    private record QuantityMatch(int startIndex, int endIndexExclusive, int amount, String displayText, String unitKey, Integer itemCount) {
    }

    private record CurrencyMatch(int startIndex, int endIndexExclusive, CurrencyTarget target, String displayText) {
    }

    private record CountWordMatch(int startIndex, int endIndexExclusive, String displayText, String unitKey) {
    }

    private record GenericUnitMatch(int startIndex, int endIndexExclusive, String displayText, String unitKey) {
    }

    private record TokenSpan(int start, int end, String normalized) {
    }
}
