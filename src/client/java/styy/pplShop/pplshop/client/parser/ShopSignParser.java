package styy.pplShop.pplshop.client.parser;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import styy.pplShop.pplshop.client.config.CurrencyAliasConfig;
import styy.pplShop.pplshop.client.config.ItemAliasConfig;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.model.ParseStatus;
import styy.pplShop.pplshop.client.model.ParsedItem;
import styy.pplShop.pplshop.client.model.ParsedPrice;
import styy.pplShop.pplshop.client.model.ItemResolutionResultType;
import styy.pplShop.pplshop.client.model.ShopSignClassificationType;
import styy.pplShop.pplshop.client.model.ShopSignDiagnosticReason;
import styy.pplShop.pplshop.client.model.ShopSignDiagnostics;
import styy.pplShop.pplshop.client.model.ShopSignEntry;
import styy.pplShop.pplshop.client.model.ShopSignFingerprint;
import styy.pplShop.pplshop.client.model.ShopSignParseResult;
import styy.pplShop.pplshop.client.model.SignContainerRelation;
import styy.pplShop.pplshop.client.model.SignTextSnapshot;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;
import styy.pplShop.pplshop.client.world.SignContainerRelationResolver;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public final class ShopSignParser {
    private static final Pattern TRAILING_INLINE_PRICE = Pattern.compile(".*\\s\\d+\\s*(?:\\u0430\\u043b\\u043c|\\u0430\\u043b|\\u0430\\u0431|\\u0430\\u043b\\u043c\\u0430\\u0437(?:\\u0430|\\u043e\\u0432|\\u044b)?)$");
    private static final Pattern ENCHANT_PRICE_LIST_LINE = Pattern.compile("^[\\p{L}\\p{N}_ ]+[\\p{L}\\p{N}_ -]*\\s-\\s\\d+\\s*(?:\\u0430\\u043b\\u043c|\\u0430\\u043b|\\u0430\\u0431|\\u0430\\u043b\\u043c\\u0430\\u0437(?:\\u0430|\\u043e\\u0432|\\u044b)?)$");
    private static final Pattern ENCHANT_PRICE_LIST_PAREN_LINE = Pattern.compile("^[\\p{L}\\p{N}_ ]+[\\p{L}\\p{N}_ -]*\\(\\d+\\s*(?:\\u0430\\u043b\\u043c|\\u0430\\u043b|\\u0430\\u0431|\\u0430\\u043b\\u043c\\u0430\\u0437(?:\\u0430|\\u043e\\u0432|\\u044b)?)\\)$");
    private static final Set<String> SERVICE_SIGN_PREFIXES = Set.of(
            "\u0430\u0440\u0435\u043d\u0434\u0430",
            "\u0443\u0441\u043b\u0443\u0433\u0430",
            "\u0443\u0441\u043b\u0443\u0433\u0438",
            "\u0437\u0430\u043a\u0430\u0437",
            "\u0440\u0430\u0431\u043e\u0442\u0430",
            "\u0440\u0435\u043c\u043e\u043d\u0442"
    );

    private final ParserRulesConfig rules;
    private final ItemAliasResolver itemAliasResolver;
    private final PriceParser priceParser;
    private final ShopSignClassifier classifier;
    private final SignContainerRelationResolver relationResolver;

    public ShopSignParser(ItemAliasConfig itemAliasConfig, CurrencyAliasConfig currencyAliasConfig, ParserRulesConfig parserRulesConfig) {
        this.rules = parserRulesConfig;
        this.itemAliasResolver = new ItemAliasResolver(itemAliasConfig, currencyAliasConfig, parserRulesConfig);
        this.priceParser = new PriceParser(currencyAliasConfig, parserRulesConfig);
        this.classifier = new ShopSignClassifier(parserRulesConfig);
        this.relationResolver = new SignContainerRelationResolver(parserRulesConfig);
    }

    public ShopSignParseResult parse(
            ClientWorld world,
            Identifier dimensionId,
            BlockPos pos,
            SignTextSnapshot snapshot,
            long scanTick,
            ShopSignDiagnosticReason cacheReason
    ) {
        SignContainerRelation relation = this.relationResolver.resolve(world, pos, snapshot, this.rules.barrel_search_radius);
        return this.parsePrepared(dimensionId, pos, snapshot, relation, scanTick, cacheReason);
    }

    public ShopSignParseResult parsePrepared(
            Identifier dimensionId,
            BlockPos pos,
            SignTextSnapshot snapshot,
            SignContainerRelation relation,
            long scanTick,
            ShopSignDiagnosticReason cacheReason
    ) {
        ShopSignFingerprint fingerprint = ShopSignFingerprint.create(dimensionId, pos, snapshot, relation, this.rules);
        PriceParser.Extraction priceExtraction = this.priceParser.extract(snapshot.lines());
        ShopSignParseResult earlyConfirmedUnresolvable = this.parseEarlyConfirmedUnresolvable(
                dimensionId,
                pos,
                snapshot,
                relation,
                scanTick,
                cacheReason,
                fingerprint,
                priceExtraction
        );
        if (earlyConfirmedUnresolvable != null) {
            return earlyConfirmedUnresolvable;
        }
        ShopSignClassifier.Classification classification = this.classifier.classify(snapshot.lines(), priceExtraction, relation);
        if (classification.type() == ShopSignClassificationType.NOT_SHOP) {
            ShopSignParseResult confirmedUnresolvable = this.parseConfirmedUnresolvableNoPrice(
                    dimensionId,
                    pos,
                    snapshot,
                    relation,
                    scanTick,
                    cacheReason,
                    fingerprint,
                    classification,
                    priceExtraction
            );
            if (confirmedUnresolvable != null) {
                return confirmedUnresolvable;
            }
            return new ShopSignParseResult(ShopSignClassificationType.NOT_SHOP, classification.reason(), fingerprint, null);
        }

        ParsedPrice parsedPrice = priceExtraction.parsedPrice();
        ParsedItem parsedItem = this.itemAliasResolver.resolveItemLines(classification.itemLines());
        ShopSignDiagnosticReason primaryReason = parsedItem.itemId() == null
                ? this.unknownItemReason(parsedItem)
                : classification.reason();

        ShopSignEntry entry = new ShopSignEntry(
                dimensionId,
                pos,
                snapshot,
                parsedItem.itemId() == null && classification.type() == ShopSignClassificationType.SHOP
                        ? new ParsedItem(
                        String.join(" ", classification.itemLines()),
                        NormalizationUtils.normalizeForLookup(String.join(" ", classification.itemLines()), this.rules),
                        null,
                        null,
                        parsedItem.parseConfidence(),
                        ParseStatus.UNKNOWN,
                        parsedItem.resolutionTrace(),
                        ItemResolutionResultType.UNKNOWN,
                        false,
                        "",
                        "",
                        ""
                )
                        : parsedItem,
                parsedPrice,
                this.buildRawCombinedText(snapshot),
                scanTick,
                new ShopSignDiagnostics(
                        parsedItem.itemId() == null ? ShopSignClassificationType.SHOP_WITH_UNKNOWN_ITEM : ShopSignClassificationType.SHOP,
                        primaryReason,
                        cacheReason,
                        classification.ownerLineIndex(),
                        classification.priceLineIndex(),
                        classification.itemLines(),
                        relation.containerBlockId() == null ? "" : relation.containerBlockId().toString(),
                        relation.relationKind(),
                        fingerprint.normalizedTextHash() + "|" + fingerprint.relationSignature()
                ),
                relation
        );
        return new ShopSignParseResult(
                parsedItem.itemId() == null ? ShopSignClassificationType.SHOP_WITH_UNKNOWN_ITEM : ShopSignClassificationType.SHOP,
                primaryReason,
                fingerprint,
                entry
        );
    }

    public ItemAliasResolver itemAliasResolver() {
        return this.itemAliasResolver;
    }

    public PriceParser priceParser() {
        return this.priceParser;
    }

    private ShopSignDiagnosticReason unknownItemReason(ParsedItem parsedItem) {
        if (parsedItem == null || parsedItem.itemId() != null) {
            return ShopSignDiagnosticReason.NONE;
        }
        return parsedItem.resolutionTrace().fallbackReason().startsWith("confirmed-unresolvable:")
                ? ShopSignDiagnosticReason.CONFIRMED_UNRESOLVABLE
                : ShopSignDiagnosticReason.UNKNOWN_ITEM_AFTER_CLASSIFICATION;
    }

    private ShopSignParseResult parseEarlyConfirmedUnresolvable(
            Identifier dimensionId,
            BlockPos pos,
            SignTextSnapshot snapshot,
            SignContainerRelation relation,
            long scanTick,
            ShopSignDiagnosticReason cacheReason,
            ShopSignFingerprint fingerprint,
            PriceParser.Extraction priceExtraction
    ) {
        if (relation == null || !relation.linked()) {
            return null;
        }

        int priceLineIndex = priceExtraction == null ? -1 : priceExtraction.lineIndex();
        int ownerLineIndex = this.classifier.findOwnerLineIndex(snapshot.lines(), priceLineIndex);
        List<String> itemLines = this.classifier.extractItemLines(snapshot.lines(), ownerLineIndex, -1);
        String fallbackReason = confirmedUnresolvableEarlyReason(snapshot.lines(), itemLines);
        if (fallbackReason.isBlank()) {
            return null;
        }

        return this.confirmedUnresolvableResult(
                dimensionId,
                pos,
                snapshot,
                relation,
                scanTick,
                cacheReason,
                fingerprint,
                ownerLineIndex,
                priceLineIndex,
                itemLines,
                fallbackReason,
                List.of("early confirmed-unresolvable detection")
        );
    }

    private ShopSignParseResult parseConfirmedUnresolvableNoPrice(
            Identifier dimensionId,
            BlockPos pos,
            SignTextSnapshot snapshot,
            SignContainerRelation relation,
            long scanTick,
            ShopSignDiagnosticReason cacheReason,
            ShopSignFingerprint fingerprint,
            ShopSignClassifier.Classification classification,
            PriceParser.Extraction priceExtraction
    ) {
        if (relation == null || !relation.linked()) {
            return null;
        }
        if (classification.reason() != ShopSignDiagnosticReason.NOT_SHOP_NO_PRICE) {
            return null;
        }
        if (priceExtraction != null && priceExtraction.parsedPrice() != null && priceExtraction.parsedPrice().hasAmount()) {
            return null;
        }

        int ownerLineIndex = classification.ownerLineIndex() >= 0
                ? classification.ownerLineIndex()
                : this.classifier.findOwnerLineIndex(snapshot.lines(), -1);
        java.util.List<String> itemLines = this.classifier.extractItemLines(snapshot.lines(), ownerLineIndex, -1);
        String fallbackReason = confirmedUnresolvableNoPriceReason(itemLines);
        if (fallbackReason.isBlank()) {
            return null;
        }

        return this.confirmedUnresolvableResult(
                dimensionId,
                pos,
                snapshot,
                relation,
                scanTick,
                cacheReason,
                fingerprint,
                ownerLineIndex,
                -1,
                itemLines,
                fallbackReason,
                List.of("no standard price line matched")
        );
    }

    static String confirmedUnresolvableNoPriceReason(List<String> itemLines) {
        String structured = confirmedUnresolvableStructuredReason(itemLines);
        if (structured.startsWith("confirmed-unresolvable:")) {
            return structured;
        }
        return "";
    }

    static String confirmedUnresolvableEarlyReason(List<String> lines, List<String> itemLines) {
        String serviceReason = serviceSignReason(lines == null || lines.isEmpty() ? "" : lines.getFirst());
        if (!serviceReason.isBlank()) {
            return serviceReason;
        }
        String structured = confirmedUnresolvableStructuredReason(itemLines);
        if (structured.startsWith("confirmed-unresolvable:")) {
            return structured;
        }
        return "";
    }

    static String serviceSignReason(String firstLine) {
        String normalized = NormalizationUtils.normalizeVisibleText(firstLine);
        for (String prefix : SERVICE_SIGN_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return "confirmed-unresolvable:service-sign";
            }
        }
        return "";
    }

    static String confirmedUnresolvableStructuredReason(List<String> itemLines) {
        if (itemLines == null || itemLines.size() < 2) {
            return "";
        }

        int enchantPriceLines = 0;
        int trailingPriceLines = 0;
        for (String line : itemLines) {
            String normalized = NormalizationUtils.normalizeVisibleText(line);
            if (ENCHANT_PRICE_LIST_LINE.matcher(normalized).matches()
                    || ENCHANT_PRICE_LIST_PAREN_LINE.matcher(normalized).matches()) {
                enchantPriceLines++;
            }
            if (TRAILING_INLINE_PRICE.matcher(normalized).matches()) {
                trailingPriceLines++;
            }
        }

        if (enchantPriceLines >= 2) {
            return "enchant-price-list";
        }
        if (trailingPriceLines >= 2) {
            return "confirmed-unresolvable:multi-disc-listing";
        }
        return "";
    }

    private ShopSignParseResult confirmedUnresolvableResult(
            Identifier dimensionId,
            BlockPos pos,
            SignTextSnapshot snapshot,
            SignContainerRelation relation,
            long scanTick,
            ShopSignDiagnosticReason cacheReason,
            ShopSignFingerprint fingerprint,
            int ownerLineIndex,
            int priceLineIndex,
            List<String> itemLines,
            String fallbackReason,
            List<String> rejectedCandidates
    ) {
        String rawItemText = itemLines.isEmpty() ? this.buildRawCombinedText(snapshot) : String.join(" ", itemLines);
        ParsedItem parsedItem = new ParsedItem(
                rawItemText,
                NormalizationUtils.normalizeForLookup(rawItemText, this.rules),
                null,
                null,
                0,
                ParseStatus.UNKNOWN,
                new styy.pplShop.pplshop.client.model.ItemResolutionTrace(
                        rawItemText,
                        "item-lines",
                        "",
                        "",
                        "",
                        fallbackReason,
                        List.of(),
                        itemLines,
                        rejectedCandidates
                ),
                ItemResolutionResultType.UNKNOWN,
                false,
                "",
                "",
                ""
        );

        ShopSignEntry entry = new ShopSignEntry(
                dimensionId,
                pos,
                snapshot,
                parsedItem,
                new ParsedPrice("", null, null, null, 0, ParseStatus.UNKNOWN, null, null, null, null, null),
                this.buildRawCombinedText(snapshot),
                scanTick,
                new ShopSignDiagnostics(
                        ShopSignClassificationType.SHOP_WITH_UNKNOWN_ITEM,
                        ShopSignDiagnosticReason.CONFIRMED_UNRESOLVABLE,
                        cacheReason,
                        ownerLineIndex,
                        priceLineIndex,
                        itemLines,
                        relation.containerBlockId() == null ? "" : relation.containerBlockId().toString(),
                        relation.relationKind(),
                        fingerprint.normalizedTextHash() + "|" + fingerprint.relationSignature()
                ),
                relation
        );
        return new ShopSignParseResult(
                ShopSignClassificationType.SHOP_WITH_UNKNOWN_ITEM,
                ShopSignDiagnosticReason.CONFIRMED_UNRESOLVABLE,
                fingerprint,
                entry
        );
    }

    private String buildRawCombinedText(SignTextSnapshot snapshot) {
        StringJoiner joiner = new StringJoiner(" | ");
        for (String line : snapshot.lines()) {
            if (line != null && !line.isBlank()) {
                joiner.add(line);
            }
        }
        return NormalizationUtils.compactDisplay(joiner.toString());
    }
}
