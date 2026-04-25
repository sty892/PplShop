package styy.pplShop.pplshop.client.parser;

import net.minecraft.util.Identifier;
import styy.pplShop.pplshop.client.config.AliasTargetMetadata;
import styy.pplShop.pplshop.client.config.CurrencyAliasConfig;
import styy.pplShop.pplshop.client.config.ItemAliasConfig;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.model.ItemResolutionResultType;
import styy.pplShop.pplshop.client.model.ItemResolutionTrace;
import styy.pplShop.pplshop.client.model.ParseStatus;
import styy.pplShop.pplshop.client.model.ParsedItem;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemAliasResolver {
    private static final int HIGH_CONFIDENCE_SHORTLIST_SCORE = 226_000;
    private static final int SAFE_FUZZY_SCORE = 95_000;
    private static final int MAX_FUZZY_ALIAS_DISTANCE = 2;
    private static final int MIN_FUZZY_ALIAS_LENGTH = 6;
    private static final Set<String> MIXED_CONNECTORS = Set.of(
            "\u0438",
            "\u0438\u043b\u0438",
            "\u043b\u0438\u0431\u043e",
            "\u043f\u043b\u044e\u0441"
    );
    private static final Set<String> TRADE_OR_CATEGORY_TOKENS = Set.of(
            "\u043a\u0443\u043f\u043b\u044e",
            "\u043f\u0440\u043e\u0434\u0430\u043c",
            "\u043f\u043e\u043a\u0443\u043f\u0430\u044e",
            "\u043e\u0431\u043c\u0435\u043d",
            "\u0442\u043e\u0432\u0430\u0440",
            "\u0442\u043e\u0432\u0430\u0440\u044b",
            "\u0440\u0435\u0441\u0443\u0440\u0441",
            "\u0440\u0435\u0441\u0443\u0440\u0441\u044b",
            "\u0432\u0435\u0449\u0438",
            "\u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442",
            "\u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442\u044b",
            "\u0431\u043b\u043e\u043a",
            "\u0431\u043b\u043e\u043a\u0438"
    );
    private static final Set<String> NOISE_TOKENS = Set.of(
            "купи",
            "покупай",
            "дешевые",
            "дешевый",
            "дешевая",
            "дешево",
            "открыто",
            "открыт",
            "опт",
            "оптом",
            "жест",
            "жесть",
            "крутой",
            "крутая",
            "крутые",
            "полезный",
            "полезная",
            "полезные",
            "непожалеешь",
            "чувак",
            "йоу",
            "цена",
            "эй",
            "тут",
            "некторская",
            "некоторская"
    );
    private static final Set<String> CONFIRMED_UNRESOLVABLE_CONTAINS = Set.of(
            "\u043b\u0443\u0442 \u0438\u0437",
            "\u043b\u0443\u0442 \u0441",
            "\u043b\u0443\u0442 \u043e\u0442",
            "\u043b\u0443\u0442 \u043f\u043e\u0441\u043b\u0435",
            "\u043a\u043d\u0438\u0433\u0438 \u0438\u0437 \u0434\u0430\u043d\u0436\u0430",
            "\u043a\u043d\u0438\u0433\u0438 \u0438\u0437 \u043f\u043e\u0434\u0437\u0435\u043c\u0435\u043b\u044c\u044f",
            "\u043a\u043e\u043b\u0435\u043a\u0446\u0438\u044f \u0440\u0443\u0434",
            "\u043f\u0435\u0440\u0435\u043f\u043b\u0430\u0432\u043b\u0435\u043d\u043d\u0430\u044f \u0440\u0443\u0434\u0430",
            "\u043f\u0435\u0440\u0435\u043f\u043b\u0430\u0432\u043b\u0435\u043d\u043d\u044b\u0435 \u0440\u0443\u0434\u044b",
            "\u043f\u0435\u0440\u0435\u043f\u043b\u0430\u0432\u043b\u0435\u043d\u043d\u044b\u0435",
            "\u0446\u0435\u043d\u043d\u043e\u0441\u0442\u0438",
            "\u044d\u043b\u0438\u0442\u0440\u044b \u043f\u043e\u0434\u0430\u0440\u043a\u0438",
            "\u0430\u043d\u0442\u0438 \u0432\u0430\u043c\u043f\u0438\u0440\u0441\u043a\u0438\u0439 \u043d\u0430\u0431\u043e\u0440"
    );
    private static final Set<String> CONFIRMED_UNRESOLVABLE_EXACT = Set.of(
            "\u0433\u0440\u0430\u0444\u0444\u0438\u0442\u0438 \u0430\u0440\u0442",
            "\u0430\u0432\u0441\u0442\u0440\u0438\u0430\u043d \u0448\u043d\u0438\u0442\u0446\u0435\u043b\u044c",
            "schnitzel",
            "austrian",
            "\u0444\u0435\u043c\u0431\u043e\u0439\u0441\u043a\u0430",
            "\u0448\u043d\u0438\u0442\u0446\u0435\u043b\u044c",
            "\u043c\u0443\u0440\u0443\u0440\u044c\u0435 \u0432\u0435\u0434\u0440\u043e\u0443",
            "\u0438\u044e\u043b\u044c\u0441\u043a\u043e\u0433\u043e \u043d\u0435\u0431\u0430",
            "\u0434\u0435\u043d\u044c\u0433\u0438 \u043d\u0430 \u0432\u0435\u0442\u0435\u0440",
            "\u0436\u0435\u043b\u0435\u0437\u043d\u044b\u0435 \u0432\u0435\u0449\u0438",
            "\u0436\u0435\u043b\u0435\u0437\u043d\u044b\u0439 \u0445\u043b\u0430\u043c",
            "\u043a\u043e\u043d\u0441\u0442\u0440\u0443\u043a\u0442\u043e\u0440 \u043f\u0430\u043a\u0435\u0442",
            "\u043a\u0443\u0441\u043e\u0447\u0435\u043a \u0444 \u0438\u044e\u043b\u044c\u0441\u043a\u043e\u0433\u043e \u043d\u0435\u0431\u0430",
            "\u0444\u0438\u043b\u043e\u0441\u043e\u0432\u0441\u043a\u0438\u0439 \u043a\u0430\u043c\u0435\u043d\u044c",
            "\u043c\u0430\u043b\u0435\u043d\u044c\u043a\u0430\u044f \u0431\u0440\u043e\u0441\u044f\u043d\u043a\u0430",
            "\u0431\u0440\u043e\u0441\u044f\u043d\u043a\u0430",
            "pooshka",
            "\u0434\u043e\u0445\u043e\u0434\u044f\u0433\u0438",
            "\u0432\u044b\u043a\u0443\u043f\u043b\u0435\u043d\u043e",
            "\u0441\u043b\u043e\u0442",
            "\u043e\u0434\u0438\u043d \u0441\u043b\u043e\u0442\u0444\u0446\u0443\u0446",
            "\u0440\u0430\u0437\u043d\u043e\u0435",
            "\u0446\u0435\u043d\u043d\u043e\u0441\u0442\u0438",
            "\u0441\u043a\u0438\u043d",
            "\u0444\u0438\u0433\u0443\u0440\u0430",
            "\u0430\u0440\u0442 \u0441 \u0432\u0430\u043c\u0438"
    );
    private static final Set<String> CONFIRMED_UNRESOLVABLE_CUSTOM_TITLE_CONTAINS = Set.of(
            "\u0444\u0435\u043c\u0431\u043e\u0439",
            "\u0444\u0435\u043c\u0431\u043e\u044f",
            "\u044d\u0441\u0441\u0435\u043d\u0446\u0438\u044f",
            "\u043f\u043e\u0439\u043b\u043e",
            "\u0431\u043b\u0435\u0431\u0430",
            "\u0431\u043b\u0451\u0431\u0430"
    );
    private static final Set<String> CONFIRMED_UNRESOLVABLE_CUSTOM_TITLE_PREFIXES = Set.of(
            "\u0438\u043d\u0444\u0430",
            "\u0438\u043d\u0444\u043e"
    );
    private static final Map<AliasTargetMetadata, List<String>> SUPPLEMENTAL_TARGET_ALIASES = Map.ofEntries(
            Map.entry(
                    new AliasTargetMetadata("minecraft:swiftness_potion", Identifier.of("minecraft", "potion"), "swiftness", ""),
                    List.of("\u0437\u0435\u043b\u044c\u0435 \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u0438", "\u0441\u043f\u043e\u0440\u0442\u0438\u0432\u044b\u0435 \u0437\u0435\u043b\u044c\u044f", "\u0441\u043f\u043e\u0440\u0442\u0438\u0432\u043d\u044b\u0435 \u0437\u0435\u043b\u044c\u044f")
            ),
            Map.entry(
                    new AliasTargetMetadata("minecraft:invisibility_potion", Identifier.of("minecraft", "potion"), "invisibility", ""),
                    List.of("\u043d\u0435\u0432\u0438\u0434\u0438\u043c\u043e\u0441\u0442\u044c")
            ),
            Map.entry(
                    new AliasTargetMetadata("minecraft:healing_potion", Identifier.of("minecraft", "potion"), "healing", ""),
                    List.of("\u0438\u0441\u0446\u0435\u043b\u0435\u043d\u0438\u0435", "\u0438\u0441\u0446\u0435\u043b\u0435\u043d\u0438\u0435 ii")
            )
    );

    private final ParserRulesConfig rules;
    private final ItemCandidateExtractor candidateExtractor;
    private final Map<String, AliasTarget> exactLookup = new LinkedHashMap<>();
    private final Map<String, AliasTarget> plainLookup = new LinkedHashMap<>();
    private final Map<String, Set<AliasTarget>> invertedIndex = new LinkedHashMap<>();
    private final List<AliasTarget> aliasTargets = new ArrayList<>();
    private final EnchantmentSignResolver enchantmentSignResolver;
    private final CategoryRepresentativeResolver categoryRepresentativeResolver = new CategoryRepresentativeResolver();
    private final ShopSignClassifier lineClassifier;

    public ItemAliasResolver(ItemAliasConfig config, CurrencyAliasConfig currencyAliasConfig, ParserRulesConfig rules) {
        this.rules = rules;
        this.candidateExtractor = new ItemCandidateExtractor(rules);
        this.enchantmentSignResolver = new EnchantmentSignResolver(rules);
        this.lineClassifier = new ShopSignClassifier(rules);

        for (Map.Entry<String, List<String>> entry : config.items.entrySet()) {
            AliasTargetMetadata metadata = config.targetMetadata(entry.getKey());
            if (metadata.runtimeItemId() == null || entry.getValue() == null) {
                continue;
            }
            for (String alias : entry.getValue()) {
                this.registerAlias(metadata, alias);
            }
        }
        for (Map.Entry<AliasTargetMetadata, List<String>> entry : SUPPLEMENTAL_TARGET_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                this.registerAlias(entry.getKey(), alias);
            }
        }
    }

    public ParsedItem resolve(List<String> lines) {
        return this.resolve(lines, null);
    }

    public ParsedItem resolve(List<String> lines, PriceParser.Extraction priceExtraction) {
        int priceLineIndex = priceExtraction == null ? -1 : priceExtraction.lineIndex();
        int ownerLineIndex = this.lineClassifier.findOwnerLineIndex(lines, priceLineIndex);
        ItemCandidateExtractor.Extraction extraction = this.candidateExtractor.extract(lines, ownerLineIndex, priceLineIndex);
        return this.resolveCandidates(extraction.candidates(), extraction.itemLines(), extraction.rejectedCandidates());
    }

    public ParsedItem resolveItemLines(List<String> itemLines) {
        ItemCandidateExtractor.Extraction extraction = this.candidateExtractor.extract(itemLines, -1, -1);
        return this.resolveCandidates(extraction.candidates(), extraction.itemLines(), extraction.rejectedCandidates());
    }

    public List<String> aliasesFor(Identifier itemId) {
        return this.aliasTargets.stream()
                .filter(target -> target.runtimeItemId().equals(itemId))
                .map(AliasTarget::rawAlias)
                .distinct()
                .toList();
    }

    private ParsedItem resolveCandidates(List<ItemCandidateExtractor.ItemCandidate> candidates, List<String> itemLines, List<String> rejectedCandidates) {
        ParsedItem specialUnknown = this.resolveSpecialUnknown(candidates, itemLines, rejectedCandidates);
        if (specialUnknown != null) {
            return specialUnknown;
        }

        List<String> considered = new ArrayList<>();
        List<String> rejected = new ArrayList<>(rejectedCandidates);
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        CandidateMatch best = null;
        CandidateMatch suggestedFallback = null;
        for (ItemCandidateExtractor.ItemCandidate candidate : candidates) {
            considered.add(candidate.describe());
            CandidateMatch resolved = this.resolveCandidate(candidate);
            if (resolved == null) {
                resolved = this.resolveNoiseStrippedCandidate(candidate);
            }
            if (resolved == null) {
                List<String> candidateSuggestions = this.resolveSuggestions(candidate, itemLines);
                suggestions.addAll(candidateSuggestions);
                if (suggestedFallback == null && !this.isUnsafeCompositeCandidate(candidate)) {
                    suggestedFallback = this.resolveSuggestedFallback(candidate, candidateSuggestions);
                }
                rejected.add(candidate.describe() + " -> no safe alias matched");
                continue;
            }
            considered.add(candidate.describe() + " -> " + resolved.describe());
            if (best == null || resolved.isBetterThan(best)) {
                best = resolved;
            }
        }

        if (best != null) {
            return best.toParsedItem(considered, rejected, List.copyOf(suggestions), "");
        }

        if (suggestedFallback != null) {
            considered.add("suggested-fallback -> " + suggestedFallback.describe());
            return suggestedFallback.toParsedItem(considered, rejected, List.copyOf(suggestions), "used first trace suggestion");
        }

        String fallback = itemLines.isEmpty() ? "" : String.join(" ", itemLines);
        return new ParsedItem(
                fallback,
                NormalizationUtils.normalizeForLookup(fallback, this.rules),
                null,
                null,
                0,
                ParseStatus.UNKNOWN,
                new ItemResolutionTrace(
                        fallback,
                        "item-lines",
                        "",
                        "",
                        "",
                        "no safe item alias matched",
                        List.copyOf(suggestions),
                        considered,
                        rejected
                ),
                ItemResolutionResultType.UNKNOWN,
                false,
                "",
                "",
                ""
        );
    }

    private ParsedItem resolveSpecialUnknown(
            List<ItemCandidateExtractor.ItemCandidate> candidates,
            List<String> itemLines,
            List<String> rejectedCandidates
    ) {
        if ("enchant-price-list".equals(ShopSignParser.confirmedUnresolvableStructuredReason(itemLines))) {
            ItemCandidateExtractor.ItemCandidate candidate = candidates.isEmpty() ? null : candidates.getFirst();
            if (candidate != null) {
                AliasTarget target = this.aliasTargetForSuggestion(Identifier.of("minecraft:enchanted_book"), "enchant-price-list");
                return new CandidateMatch(
                        candidate, target, 200_000, ParseStatus.PARTIAL, "enchant-price-list", ItemResolutionResultType.SUGGESTED_FALLBACK, true
                ).toParsedItem(List.of("enchant-price-list detected"), rejectedCandidates, List.of(), "enchant-price-list");
            }
        }

        for (ItemCandidateExtractor.ItemCandidate candidate : candidates) {
            ParsedItem commaSeparated = this.resolveCommaSeparatedCandidate(candidate, itemLines, rejectedCandidates);
            if (commaSeparated != null) {
                return commaSeparated;
            }
        }

        for (ItemCandidateExtractor.ItemCandidate candidate : candidates) {
            ParsedItem multiItem = this.resolveMultiItemConjunction(candidate, itemLines, rejectedCandidates);
            if (multiItem != null) {
                return multiItem;
            }
        }

        for (ItemCandidateExtractor.ItemCandidate candidate : candidates) {
            String fallbackReason = this.confirmedUnresolvableReason(candidate);
            if (!fallbackReason.isBlank()) {
                return this.unknownFromCandidate(candidate, itemLines, rejectedCandidates, fallbackReason);
            }
        }
        return null;
    }

    private ParsedItem resolveCommaSeparatedCandidate(
            ItemCandidateExtractor.ItemCandidate candidate,
            List<String> itemLines,
            List<String> rejectedCandidates
    ) {
        if (candidate == null || !candidate.rawText().contains(",")) {
            return null;
        }

        String[] halves = NormalizationUtils.normalizeVisibleText(candidate.rawText()).split("\\s*,\\s*");
        if (halves.length != 2) {
            return null;
        }

        CandidateMatch left = this.resolveStandaloneCandidate(halves[0].trim(), "comma-left-half");
        CandidateMatch right = this.resolveStandaloneCandidate(halves[1].trim(), "comma-right-half");

        if (left != null && right != null && !left.target().runtimeItemId().equals(right.target().runtimeItemId())) {
            CandidateMatch selected = this.selectPrimaryMultiItemMatch(candidate, left, right);
            List<String> considered = List.of(candidate.describe() + " -> multi-item-primary-comma selected " + selected.describe() + " from (" + left.describe() + " | " + right.describe() + ")");
            return selected.toParsedItem(considered, rejectedCandidates, List.of(), "multi-item-primary-comma");
        }
        if (left != null && right == null) {
            return this.partialCommaResolution(candidate, rejectedCandidates, left, halves[1].trim());
        }
        if (right != null && left == null) {
            return this.partialCommaResolution(candidate, rejectedCandidates, right, halves[0].trim());
        }
        if (left != null) {
            return this.partialCommaResolution(candidate, rejectedCandidates, left, "");
        }
        return null;
    }

    private ParsedItem resolveMultiItemConjunction(
            ItemCandidateExtractor.ItemCandidate candidate,
            List<String> itemLines,
            List<String> rejectedCandidates
    ) {
        if (candidate == null || !"joined-top-item-lines".equals(candidate.source()) || !candidate.normalizedPlain().contains(" \u0438 ")) {
            return null;
        }
        if (this.isWritableBookPhrase(candidate)) {
            return null;
        }

        String[] halves = candidate.normalizedPlain().split("\\s+\\u0438\\s+", 2);
        if (halves.length != 2) {
            return null;
        }

        CandidateMatch left = this.resolveStandaloneCandidate(halves[0].trim(), "joined-left-half");
        CandidateMatch right = this.resolveStandaloneCandidate(halves[1].trim(), "joined-right-half");
        if (left == null || right == null || left.target().runtimeItemId().equals(right.target().runtimeItemId())) {
            return null;
        }

        CandidateMatch selected = this.selectPrimaryMultiItemMatch(candidate, left, right);
        List<String> considered = List.of(candidate.describe() + " -> multi-item-primary-conjunction selected " + selected.describe() + " from (" + left.describe() + " | " + right.describe() + ")");
        return selected.toParsedItem(considered, rejectedCandidates, List.of(), "multi-item-primary-conjunction");
    }

    private boolean isWritableBookPhrase(ItemCandidateExtractor.ItemCandidate candidate) {
        if (candidate == null) {
            return false;
        }
        return "книга и перо".equals(candidate.normalizedPlain()) || "книга и перо".equals(candidate.normalizedLookup());
    }

    private CandidateMatch selectPrimaryMultiItemMatch(ItemCandidateExtractor.ItemCandidate candidate, CandidateMatch left, CandidateMatch right) {
        int leftPriority = this.primaryMultiItemPriority(candidate, left, true);
        int rightPriority = this.primaryMultiItemPriority(candidate, right, false);
        if (rightPriority > leftPriority) {
            return right;
        }
        return left;
    }

    private int primaryMultiItemPriority(ItemCandidateExtractor.ItemCandidate candidate, CandidateMatch match, boolean leftSide) {
        if (candidate == null || match == null || match.target() == null || match.target().runtimeItemId() == null) {
            return leftSide ? 1 : 0;
        }

        String normalized = candidate.normalizedPlain();
        String itemId = match.target().runtimeItemId().toString();
        int priority = leftSide ? 10 : 0;
        if (normalized.contains("земл") && "minecraft:dirt".equals(itemId)) {
            priority += 1_000;
        }
        if (normalized.contains("туф") && "minecraft:tuff".equals(itemId)) {
            priority += 950;
        }
        if (normalized.contains("тыкв") && "minecraft:pumpkin".equals(itemId)) {
            priority += 900;
        }
        if (normalized.contains("грав") && "minecraft:gravel".equals(itemId)) {
            priority += 850;
        }
        if (normalized.contains("аксолот") && "minecraft:axolotl_bucket".equals(itemId)) {
            priority += 800;
        }
        if (normalized.contains("мангр") && itemId.contains("mangrove")) {
            priority += 750;
        }
        if (normalized.contains("дуб") && "minecraft:oak_log".equals(itemId)) {
            priority += 700;
        }
        if (normalized.contains("вишн") && itemId.contains("cherry")) {
            priority += 650;
        }
        return priority;
    }

    private CandidateMatch resolveStandaloneCandidate(String rawText, String source) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        String compact = NormalizationUtils.compactDisplay(rawText);
        String normalizedPlain = NormalizationUtils.normalizeWithoutSorting(compact, this.rules);
        String normalizedLookup = NormalizationUtils.normalizeForLookup(compact, this.rules);
        if (normalizedPlain.isBlank() || normalizedLookup.isBlank()) {
            return null;
        }

        ItemCandidateExtractor.ItemCandidate half = new ItemCandidateExtractor.ItemCandidate(
                compact,
                normalizedPlain,
                normalizedLookup,
                List.of(),
                source,
                0,
                NormalizationUtils.tokenize(normalizedPlain).size(),
                normalizedPlain.replace(" ", "").length()
        );
        return this.resolveCandidate(half);
    }

    private ParsedItem partialCommaResolution(
            ItemCandidateExtractor.ItemCandidate originalCandidate,
            List<String> rejectedCandidates,
            CandidateMatch resolvedMatch,
            String unresolvedHalf
    ) {
        List<String> considered = new ArrayList<>();
        considered.add(originalCandidate.describe() + " -> partial-comma-fallback using " + resolvedMatch.describe());

        List<String> rejected = new ArrayList<>(rejectedCandidates);
        if (unresolvedHalf != null && !unresolvedHalf.isBlank()) {
            rejected.add("comma-side '" + unresolvedHalf + "' -> no safe alias matched");
        }
        return resolvedMatch.toParsedItem(considered, rejected, List.of(), "");
    }

    private String confirmedUnresolvableReason(ItemCandidateExtractor.ItemCandidate candidate) {
        if (candidate == null) {
            return "";
        }

        String normalized = candidate.normalizedPlain();
        for (String marker : CONFIRMED_UNRESOLVABLE_CONTAINS) {
            if (normalized.contains(marker)) {
                return "confirmed-unresolvable:loot-bundle";
            }
        }
        if (normalized.equals("\u0433\u0440\u0430\u0444\u0444\u0438\u0442\u0438 \u0430\u0440\u0442") || normalized.startsWith("\u0433\u0440\u0430\u0444\u0444\u0438\u0442\u0438")) {
            return "confirmed-unresolvable:graffiti";
        }
        if (CONFIRMED_UNRESOLVABLE_EXACT.contains(normalized)) {
            return "confirmed-unresolvable:custom-title";
        }
        for (String marker : CONFIRMED_UNRESOLVABLE_CUSTOM_TITLE_CONTAINS) {
            if (normalized.contains(marker)) {
                return "confirmed-unresolvable:custom-title";
            }
        }
        for (String prefix : CONFIRMED_UNRESOLVABLE_CUSTOM_TITLE_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return "confirmed-unresolvable:custom-title";
            }
        }
        if (normalized.startsWith("\u0444\u0444 ")) {
            return "confirmed-unresolvable:custom-title";
        }
        for (String marker : this.rules.combinedBlacklistedSignContains()) {
            String normalizedMarker = NormalizationUtils.normalizeWithoutSorting(marker, this.rules);
            if (!normalizedMarker.isBlank() && normalized.contains(normalizedMarker)) {
                return "confirmed-unresolvable:blacklisted-sign";
            }
        }
        return "";
    }

    private ParsedItem unknownFromCandidate(
            ItemCandidateExtractor.ItemCandidate candidate,
            List<String> itemLines,
            List<String> rejectedCandidates,
            String fallbackReason
    ) {
        return this.unknownFromCandidate(candidate, itemLines, rejectedCandidates, fallbackReason, List.of(candidate.describe()));
    }

    private ParsedItem unknownFromCandidate(
            ItemCandidateExtractor.ItemCandidate candidate,
            List<String> itemLines,
            List<String> rejectedCandidates,
            String fallbackReason,
            List<String> consideredCandidates
    ) {
        String fallback = itemLines.isEmpty() ? candidate.rawText() : String.join(" ", itemLines);
        List<String> rejected = new ArrayList<>(rejectedCandidates);
        rejected.add(candidate.describe() + " -> " + fallbackReason);
        return new ParsedItem(
                fallback,
                NormalizationUtils.normalizeForLookup(fallback, this.rules),
                null,
                null,
                0,
                ParseStatus.UNKNOWN,
                new ItemResolutionTrace(
                        candidate.rawText(),
                        candidate.source(),
                        "",
                        "",
                        "",
                        fallbackReason,
                        List.of(),
                        consideredCandidates,
                        rejected
                ),
                ItemResolutionResultType.UNKNOWN,
                false,
                "",
                "",
                ""
        );
    }

    private CandidateMatch resolveCandidate(ItemCandidateExtractor.ItemCandidate candidate) {
        AliasTarget exactPlain = this.plainLookup.get(candidate.normalizedPlain());
        if (exactPlain != null) {
            return new CandidateMatch(candidate, exactPlain, 400_000, ParseStatus.EXACT, "exact-alias", ItemResolutionResultType.EXACT_ALIAS, true);
        }

        AliasTarget exactLookup = this.exactLookup.get(candidate.normalizedLookup());
        if (exactLookup != null) {
            return new CandidateMatch(candidate, exactLookup, 380_000, ParseStatus.EXACT, "normalized-exact-alias", ItemResolutionResultType.NORMALIZED_EXACT, true);
        }

        CandidateMatch fuzzyAlias = this.resolveDirectFuzzyAlias(candidate);
        if (fuzzyAlias != null) {
            return fuzzyAlias;
        }

        if (this.isUnsafeCompositeCandidate(candidate)) {
            return null;
        }

        CandidateMatch shortlist = this.resolveFromShortlist(candidate, false);
        if (shortlist != null && shortlist.score() >= HIGH_CONFIDENCE_SHORTLIST_SCORE) {
            return shortlist;
        }

        CandidateMatch fuzzyShortlist = this.resolveFromShortlist(candidate, true);
        if (fuzzyShortlist != null && fuzzyShortlist.score() >= SAFE_FUZZY_SCORE && this.isSafeFuzzyCandidate(candidate, fuzzyShortlist)) {
            return fuzzyShortlist;
        }

        return null;
    }

    private CandidateMatch resolveNoiseStrippedCandidate(ItemCandidateExtractor.ItemCandidate candidate) {
        if (candidate == null || candidate.normalizedPlain().isBlank()) {
            return null;
        }

        List<String> originalTokens = NormalizationUtils.tokenize(candidate.normalizedPlain());
        List<String> strippedTokens = originalTokens.stream()
                .filter(token -> !NOISE_TOKENS.contains(token))
                .toList();
        if (strippedTokens.isEmpty() || strippedTokens.size() == originalTokens.size()) {
            return null;
        }
        for (String token : strippedTokens) {
            if (MIXED_CONNECTORS.contains(token)) {
                return null;
            }
        }

        String strippedPlain = String.join(" ", strippedTokens);
        String strippedLookup = NormalizationUtils.normalizeForLookup(strippedPlain, this.rules);
        if (strippedLookup.isBlank()) {
            return null;
        }
        ItemCandidateExtractor.ItemCandidate strippedCandidate = new ItemCandidateExtractor.ItemCandidate(
                strippedPlain,
                strippedPlain,
                strippedLookup,
                candidate.lineIndexes(),
                candidate.source() + "-noise-stripped",
                Math.max(0, candidate.sourcePriority() - 10),
                strippedTokens.size(),
                strippedPlain.replace(" ", "").length()
        );
        CandidateMatch match = this.resolveCandidate(strippedCandidate);
        if (match == null || !match.safe()) {
            return null;
        }
        return new CandidateMatch(
                strippedCandidate,
                match.target(),
                Math.max(match.score() - 1_500, SAFE_FUZZY_SCORE),
                ParseStatus.PARTIAL,
                "noise-stripped+" + match.resolver(),
                match.resultType(),
                true
        );
    }

    private CandidateMatch resolveDirectFuzzyAlias(ItemCandidateExtractor.ItemCandidate candidate) {
        if (candidate == null || candidate.charLength() < MIN_FUZZY_ALIAS_LENGTH) {
            return null;
        }

        return this.aliasTargets.stream()
                .filter(target -> Math.abs(target.charLength() - candidate.charLength()) <= MAX_FUZZY_ALIAS_DISTANCE)
                .map(target -> this.rankDirectFuzzyAlias(candidate, target))
                .filter(match -> match != null)
                .sorted(Comparator.reverseOrder())
                .findFirst()
                .orElse(null);
    }

    private CandidateMatch resolveFromShortlist(ItemCandidateExtractor.ItemCandidate candidate, boolean allowFuzzy) {
        Set<AliasTarget> shortlist = this.shortlistFor(candidate);
        if (shortlist.isEmpty()) {
            return null;
        }

        return shortlist.stream()
                .map(target -> this.rankCandidate(candidate, target, allowFuzzy))
                .filter(match -> match != null)
                .sorted(Comparator.reverseOrder())
                .findFirst()
                .orElse(null);
    }

    private List<String> resolveSuggestions(ItemCandidateExtractor.ItemCandidate candidate, List<String> itemLines) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        Set<AliasTarget> shortlist = this.shortlistFor(candidate);
        suggestions.addAll(shortlist.stream()
                .map(target -> this.rankSuggestion(candidate, target))
                .filter(match -> match != null)
                .sorted(Comparator.reverseOrder())
                .limit(5)
                .map(match -> match.target().runtimeItemId() + " via '" + match.target().rawAlias() + "'")
                .toList());
        suggestions.addAll(shortlist.stream()
                .map(target -> this.rankCandidate(candidate, target, true))
                .filter(match -> match != null)
                .sorted(Comparator.reverseOrder())
                .limit(5)
                .map(match -> match.target().runtimeItemId() + " via '" + match.target().rawAlias() + "'")
                .toList());

        EnchantmentSignResolver.Match enchantmentMatch = this.enchantmentSignResolver.resolve(candidate.normalizedPlain(), candidate.normalizedLookup());
        if (enchantmentMatch != null) {
            suggestions.add(enchantmentMatch.itemId() + " via enchantment-pattern");
        }

        List<String> context = itemLines.stream()
                .map(line -> NormalizationUtils.normalizeWithoutSorting(line, this.rules))
                .toList();
        CategoryRepresentativeResolver.Match categoryMatch = this.categoryRepresentativeResolver.resolve(candidate.normalizedPlain(), candidate.normalizedLookup(), context);
        if (categoryMatch != null) {
            suggestions.add(categoryMatch.itemId() + " via category-suggestion");
        }
        return List.copyOf(suggestions);
    }

    private CandidateMatch resolveSuggestedFallback(ItemCandidateExtractor.ItemCandidate candidate, List<String> suggestions) {
        for (String suggestion : suggestions) {
            CandidateMatch match = this.suggestedFallbackMatch(candidate, suggestion);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private Set<AliasTarget> shortlistFor(ItemCandidateExtractor.ItemCandidate candidate) {
        LinkedHashSet<AliasTarget> shortlist = new LinkedHashSet<>();
        for (String token : NormalizationUtils.tokenize(candidate.normalizedPlain())) {
            shortlist.addAll(this.invertedIndex.getOrDefault(token, Set.of()));
        }
        return shortlist;
    }

    private boolean isUnsafeCompositeCandidate(ItemCandidateExtractor.ItemCandidate candidate) {
        List<String> tokens = NormalizationUtils.tokenize(candidate.normalizedPlain());
        if (tokens.isEmpty()) {
            return true;
        }

        int connectorCount = 0;
        int categoryCount = 0;
        for (String token : tokens) {
            if (MIXED_CONNECTORS.contains(token)) {
                connectorCount++;
            }
            if (TRADE_OR_CATEGORY_TOKENS.contains(token)) {
                categoryCount++;
            }
        }
        return connectorCount > 0 || categoryCount > 0;
    }

    private boolean isSafeFuzzyCandidate(ItemCandidateExtractor.ItemCandidate candidate, CandidateMatch match) {
        MatchStats stats = compareTokens(candidate.normalizedPlain(), match.target().normalizedPlain(), true);
        return stats.fuzzyMatches() <= 1 && stats.unmatchedCandidateTokens() == 0 && stats.prefixMatches() <= 1;
    }

    private CandidateMatch rankCandidate(ItemCandidateExtractor.ItemCandidate candidate, AliasTarget target, boolean allowFuzzy) {
        MatchStats stats = compareTokens(candidate.normalizedPlain(), target.normalizedPlain(), allowFuzzy);
        if (!stats.accepts(allowFuzzy)) {
            return null;
        }

        int base = allowFuzzy ? 90_000 : 220_000;
        int score = base
                + stats.exactMatches() * 4_000
                + stats.prefixMatches() * 2_000
                + stats.fuzzyMatches() * 600
                - stats.unmatchedCandidateTokens() * 5_000
                + target.tokenCount() * 150
                + target.charLength();
        ParseStatus status = allowFuzzy ? ParseStatus.PARTIAL : ParseStatus.EXACT;
        ItemResolutionResultType resultType = allowFuzzy ? ItemResolutionResultType.SAFE_FUZZY_SHORTLIST : ItemResolutionResultType.HIGH_CONFIDENCE_SHORTLIST;
        return new CandidateMatch(candidate, target, score, status, allowFuzzy ? "shortlist-fuzzy" : "shortlist-token", resultType, true);
    }

    private CandidateMatch rankDirectFuzzyAlias(ItemCandidateExtractor.ItemCandidate candidate, AliasTarget target) {
        String compactCandidate = candidate.normalizedPlain().replace(" ", "");
        String compactAlias = target.normalizedPlain().replace(" ", "");
        if (compactCandidate.isBlank() || compactAlias.isBlank() || compactCandidate.charAt(0) != compactAlias.charAt(0)) {
            return null;
        }
        if (this.hasMismatchedCategoryTokens(candidate.normalizedPlain(), target.normalizedPlain())) {
            return null;
        }
        if (this.isUnsafeCompositeCandidate(candidate) && candidate.tokenCount() != target.tokenCount()) {
            return null;
        }

        int distance = boundedLevenshtein(candidate.normalizedPlain(), target.normalizedPlain(), MAX_FUZZY_ALIAS_DISTANCE);
        if (distance < 0) {
            return null;
        }

        int score = 340_000
                - distance * 5_000
                - Math.abs(target.charLength() - candidate.charLength()) * 100;
        return new CandidateMatch(
                candidate,
                target,
                score,
                ParseStatus.PARTIAL,
                "fuzzy-alias(distance=" + distance + ")",
                ItemResolutionResultType.FUZZY_ALIAS,
                true
        );
    }

    private boolean hasMismatchedCategoryTokens(String candidate, String alias) {
        Set<String> candidateCategories = this.categoryTokens(candidate);
        if (candidateCategories.isEmpty()) {
            return false;
        }
        return !candidateCategories.equals(this.categoryTokens(alias));
    }

    private Set<String> categoryTokens(String value) {
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        for (String token : NormalizationUtils.tokenize(value)) {
            if (TRADE_OR_CATEGORY_TOKENS.contains(token)) {
                categories.add(token);
            }
        }
        return categories;
    }

    private SuggestionMatch rankSuggestion(ItemCandidateExtractor.ItemCandidate candidate, AliasTarget target) {
        List<String> candidateTokens = NormalizationUtils.tokenize(candidate.normalizedPlain());
        List<String> aliasTokens = NormalizationUtils.tokenize(target.normalizedPlain());
        if (candidateTokens.isEmpty() || aliasTokens.isEmpty()) {
            return null;
        }

        int exactMatches = 0;
        int prefixMatches = 0;
        for (String candidateToken : candidateTokens) {
            int bestScore = aliasTokens.stream()
                    .mapToInt(aliasToken -> tokenMatchScore(candidateToken, aliasToken, false))
                    .max()
                    .orElse(-1);
            if (bestScore >= 100) {
                exactMatches++;
            } else if (bestScore >= 70) {
                prefixMatches++;
            }
        }

        int strongMatches = exactMatches + prefixMatches;
        if (strongMatches == 0 || strongMatches < Math.min(2, candidateTokens.size())) {
            return null;
        }

        int score = strongMatches * 4_000
                - Math.max(0, aliasTokens.size() - strongMatches) * 250
                + target.charLength();
        return new SuggestionMatch(target, score);
    }

    private CandidateMatch suggestedFallbackMatch(ItemCandidateExtractor.ItemCandidate candidate, String suggestion) {
        if (suggestion == null || suggestion.isBlank()) {
            return null;
        }

        int viaIndex = suggestion.indexOf(" via ");
        String itemIdText = viaIndex >= 0 ? suggestion.substring(0, viaIndex).trim() : suggestion.trim();
        Identifier runtimeItemId = Identifier.tryParse(itemIdText);
        if (runtimeItemId == null || Identifier.of("minecraft", "enchanted_book").equals(runtimeItemId)) {
            return null;
        }

        String rawAlias = "";
        if (viaIndex >= 0) {
            String detail = suggestion.substring(viaIndex + 5).trim();
            if (detail.startsWith("'") && detail.endsWith("'") && detail.length() >= 2) {
                rawAlias = detail.substring(1, detail.length() - 1);
            }
        }
        if (rawAlias.isBlank() || !this.hasStrongSuggestionOverlap(candidate, rawAlias)) {
            return null;
        }

        AliasTarget target = this.aliasTargetForSuggestion(runtimeItemId, rawAlias);
        return new CandidateMatch(
                candidate,
                target,
                40_000,
                ParseStatus.PARTIAL,
                "suggested-fallback",
                ItemResolutionResultType.SUGGESTED_FALLBACK,
                true
        );
    }

    private boolean hasStrongSuggestionOverlap(ItemCandidateExtractor.ItemCandidate candidate, String rawAlias) {
        if (candidate == null || rawAlias == null || rawAlias.isBlank()) {
            return false;
        }

        List<String> candidateTokens = NormalizationUtils.tokenize(candidate.normalizedPlain());
        List<String> aliasTokens = NormalizationUtils.tokenize(NormalizationUtils.normalizeWithoutSorting(rawAlias, this.rules));
        int strongMatches = 0;
        for (String candidateToken : candidateTokens) {
            int bestScore = aliasTokens.stream()
                    .mapToInt(aliasToken -> tokenMatchScore(candidateToken, aliasToken, false))
                    .max()
                    .orElse(-1);
            if (bestScore >= 70) {
                strongMatches++;
            }
        }
        return strongMatches >= Math.min(2, candidateTokens.size());
    }

    private void registerAlias(AliasTargetMetadata metadata, String alias) {
        String normalizedPlain = NormalizationUtils.normalizeWithoutSorting(alias, this.rules);
        String normalizedLookup = NormalizationUtils.normalizeForLookup(alias, this.rules);
        if (normalizedPlain.isBlank() || normalizedLookup.isBlank()) {
            return;
        }

        AliasTarget target = new AliasTarget(
                metadata.bucketId(),
                metadata.runtimeItemId(),
                metadata.subtypeKey(),
                metadata.displayNameOverride(),
                alias,
                normalizedPlain,
                normalizedLookup,
                NormalizationUtils.tokenize(normalizedPlain).size(),
                normalizedPlain.replace(" ", "").length()
        );
        this.aliasTargets.add(target);
        this.exactLookup.putIfAbsent(normalizedLookup, target);
        this.plainLookup.putIfAbsent(normalizedPlain, target);
        for (String token : NormalizationUtils.tokenize(normalizedPlain)) {
            this.invertedIndex.computeIfAbsent(token, ignored -> new LinkedHashSet<>()).add(target);
        }
    }

    private static MatchStats compareTokens(String candidate, String alias, boolean allowFuzzy) {
        List<String> candidateTokens = NormalizationUtils.tokenize(candidate);
        List<String> aliasTokens = NormalizationUtils.tokenize(alias);
        if (candidateTokens.isEmpty() || aliasTokens.isEmpty()) {
            return MatchStats.empty();
        }

        boolean[] matchedCandidate = new boolean[candidateTokens.size()];
        int exactMatches = 0;
        int prefixMatches = 0;
        int fuzzyMatches = 0;
        for (String aliasToken : aliasTokens) {
            int bestIndex = -1;
            int bestScore = -1;
            for (int index = 0; index < candidateTokens.size(); index++) {
                int score = tokenMatchScore(candidateTokens.get(index), aliasToken, allowFuzzy);
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = index;
                }
            }
            if (bestScore <= 0 || bestIndex < 0) {
                return MatchStats.empty();
            }
            matchedCandidate[bestIndex] = true;
            if (bestScore >= 100) {
                exactMatches++;
            } else if (bestScore >= 70) {
                prefixMatches++;
            } else {
                fuzzyMatches++;
            }
        }

        int unmatchedCandidateTokens = 0;
        for (int index = 0; index < matchedCandidate.length; index++) {
            if (!matchedCandidate[index] && candidateTokens.get(index).length() >= 3) {
                unmatchedCandidateTokens++;
            }
        }
        return new MatchStats(exactMatches, prefixMatches, fuzzyMatches, unmatchedCandidateTokens);
    }

    private static int tokenMatchScore(String candidateToken, String aliasToken, boolean allowFuzzy) {
        if (candidateToken.equals(aliasToken)) {
            return 100;
        }
        if (candidateToken.startsWith(aliasToken) || aliasToken.startsWith(candidateToken)) {
            return Math.min(candidateToken.length(), aliasToken.length()) >= 4 ? 70 : -1;
        }
        if (allowFuzzy && candidateToken.length() >= 5 && aliasToken.length() >= 5 && levenshtein(candidateToken, aliasToken) == 1) {
            return 35;
        }
        return -1;
    }

    private static int levenshtein(String left, String right) {
        int[][] distance = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            distance[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                distance[i][j] = Math.min(
                        Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + cost
                );
            }
        }
        return distance[left.length()][right.length()];
    }

    private static int boundedLevenshtein(String left, String right, int maxDistance) {
        if (left == null || right == null) {
            return -1;
        }
        if (Math.abs(left.length() - right.length()) > maxDistance) {
            return -1;
        }

        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            int rowMin = current[0];
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(previous[j] + 1, current[j - 1] + 1),
                        previous[j - 1] + cost
                );
                rowMin = Math.min(rowMin, current[j]);
            }
            if (rowMin > maxDistance) {
                return -1;
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[right.length()] <= maxDistance ? previous[right.length()] : -1;
    }

    private AliasTarget aliasTargetForSuggestion(Identifier runtimeItemId, String rawAlias) {
        for (AliasTarget target : this.aliasTargets) {
            if (!target.runtimeItemId().equals(runtimeItemId)) {
                continue;
            }
            if (!rawAlias.isBlank() && !target.rawAlias().equals(rawAlias)) {
                continue;
            }
            return target;
        }

        AliasTargetMetadata metadata = AliasTargetMetadata.direct(runtimeItemId.toString(), runtimeItemId);
        String normalizedAlias = rawAlias.isBlank() ? runtimeItemId.toString() : NormalizationUtils.normalizeWithoutSorting(rawAlias, this.rules);
        return new AliasTarget(
                metadata.bucketId(),
                runtimeItemId,
                metadata.subtypeKey(),
                metadata.displayNameOverride(),
                rawAlias,
                normalizedAlias,
                NormalizationUtils.normalizeForLookup(normalizedAlias, this.rules),
                NormalizationUtils.tokenize(normalizedAlias).size(),
                normalizedAlias.replace(" ", "").length()
        );
    }

    private record AliasTarget(
            String bucketId,
            Identifier runtimeItemId,
            String subtypeKey,
            String displayNameOverride,
            String rawAlias,
            String normalizedPlain,
            String normalizedLookup,
            int tokenCount,
            int charLength
    ) {
    }

    private record CandidateMatch(
            ItemCandidateExtractor.ItemCandidate candidate,
            AliasTarget target,
            int score,
            ParseStatus status,
            String resolver,
            ItemResolutionResultType resultType,
            boolean safe
    ) implements Comparable<CandidateMatch> {
        private boolean isBetterThan(CandidateMatch other) {
            return this.compareTo(other) > 0;
        }

        private String describe() {
            return this.resolver + " -> item=" + this.target.runtimeItemId() + " alias='" + this.target.rawAlias() + "' score=" + this.score;
        }

        private ParsedItem toParsedItem(List<String> considered, List<String> rejected, List<String> suggestions, String fallbackReason) {
            return new ParsedItem(
                    this.candidate.normalizedPlain(),
                    this.candidate.normalizedLookup(),
                    this.target.runtimeItemId(),
                    this.target.rawAlias(),
                    this.score,
                    this.status,
                    new ItemResolutionTrace(
                            this.candidate.rawText(),
                            this.candidate.source(),
                            this.resolver,
                        this.target.runtimeItemId().toString(),
                        this.target.rawAlias(),
                        fallbackReason,
                        suggestions,
                        considered,
                        rejected
                    ),
                    this.resultType,
                    this.safe,
                    this.target.bucketId(),
                    this.target.subtypeKey(),
                    this.target.displayNameOverride()
            );
        }

        @Override
        public int compareTo(CandidateMatch other) {
            if (this.resultType != other.resultType) {
                return Integer.compare(priority(this.resultType), priority(other.resultType));
            }
            if (this.score != other.score) {
                return Integer.compare(this.score, other.score);
            }
            if (this.target.tokenCount() != other.target.tokenCount()) {
                return Integer.compare(this.target.tokenCount(), other.target.tokenCount());
            }
            return Integer.compare(this.target.charLength(), other.target.charLength());
        }

        private static int priority(ItemResolutionResultType resultType) {
            return switch (resultType) {
                case EXACT_ALIAS -> 6;
                case NORMALIZED_EXACT -> 5;
                case FUZZY_ALIAS -> 4;
                case HIGH_CONFIDENCE_SHORTLIST -> 3;
                case SAFE_FUZZY_SHORTLIST -> 2;
                case SUGGESTED_FALLBACK -> 1;
                case UNKNOWN -> 0;
            };
        }
    }

    private record MatchStats(int exactMatches, int prefixMatches, int fuzzyMatches, int unmatchedCandidateTokens) {
        private static MatchStats empty() {
            return new MatchStats(0, 0, 0, Integer.MAX_VALUE);
        }

        private boolean accepts(boolean allowFuzzy) {
            if (this.exactMatches <= 0 && this.prefixMatches <= 0 && (!allowFuzzy || this.fuzzyMatches <= 0)) {
                return false;
            }
            return allowFuzzy ? this.unmatchedCandidateTokens == 0 : this.unmatchedCandidateTokens == 0;
        }
    }

    private record SuggestionMatch(AliasTarget target, int score) implements Comparable<SuggestionMatch> {
        @Override
        public int compareTo(SuggestionMatch other) {
            return Integer.compare(this.score, other.score);
        }
    }
}
