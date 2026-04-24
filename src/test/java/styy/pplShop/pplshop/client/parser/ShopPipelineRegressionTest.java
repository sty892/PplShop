package styy.pplShop.pplshop.client.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import styy.pplShop.pplshop.client.config.CurrencyAliasConfig;
import styy.pplShop.pplshop.client.config.ItemAliasConfig;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.config.AliasTargetMappings;
import styy.pplShop.pplshop.client.model.ShopSignClassificationType;
import styy.pplShop.pplshop.client.model.ShopSignDiagnosticReason;
import styy.pplShop.pplshop.client.model.ShopSignFingerprint;
import styy.pplShop.pplshop.client.model.SignContainerRelation;
import styy.pplShop.pplshop.client.model.SignSide;
import styy.pplShop.pplshop.client.model.SignTextSnapshot;
import styy.pplShop.pplshop.client.world.NegativeShopCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPipelineRegressionTest {
    private static ParserRulesConfig rules;
    private static PriceParser priceParser;
    private static ItemAliasResolver itemAliasResolver;
    private static ShopSignClassifier classifier;

    @BeforeAll
    static void setUp() throws IOException {
        rules = ParserRulesConfig.defaults();
        CurrencyAliasConfig currencyAliases = CurrencyAliasConfig.defaults();
        priceParser = new PriceParser(currencyAliases, rules);
        ItemAliasConfig itemAliasConfig = loadItemAliasConfig(rules);
        itemAliasResolver = new ItemAliasResolver(itemAliasConfig, currencyAliases, rules);
        classifier = new ShopSignClassifier(rules);
    }

    @Test
    void falsePositiveInfoBarrelIsNotShop() {
        List<String> lines = List.of("\u0411\u043e\u0447\u043a\u0430 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u0438", "", "", "Owner_01");
        ShopSignClassifier.Classification classification = classifier.classify(lines, priceParser.extract(lines), barrelRelation());
        assertEquals(ShopSignClassificationType.NOT_SHOP, classification.type());
        assertEquals(ShopSignDiagnosticReason.NOT_SHOP_NO_PRICE, classification.reason());
    }

    @Test
    void priceLineNeverBecomesItemCandidate() {
        Identifier resolved = resolveItemId("", "1 \u0430\u043b\u043c\u0430\u0437 - 3 \u0430\u043b\u043c", "", "Seller_01");
        assertNull(resolved);
    }

    @Test
    void ownerLineDetectionWorks() {
        assertEquals(3, classifier.findOwnerLineIndex(List.of("\u041f\u0443\u0440\u043f\u0443\u0440\u043d\u044b\u0439 \u0431\u0435\u0442\u043e\u043d", "1 \u0430\u043b\u043c", "", "Seller_01")));
    }

    @Test
    void usernameLikeTopLineBecomesOwnerWhenItemLineExists() {
        List<String> lines = List.of("_ChIzHIk_", "\u0422\u043e\u0442\u0435\u043c\u044b", "1 \u0448\u0431 - 1 \u0430\u0431", "");
        PriceParser.Extraction priceExtraction = priceParser.extract(lines);
        ShopSignClassifier.Classification classification = classifier.classify(lines, priceExtraction, barrelRelation());
        assertEquals(0, classification.ownerLineIndex());
        assertEquals(List.of("\u0422\u043e\u0442\u0435\u043c\u044b"), classification.itemLines());
    }

    @Test
    void usernameLikeMiddleLineBecomesOwnerWhenDecorationIsLast() {
        List<String> lines = List.of("\u2605\u0428\u0430\u0431\u043b\u043e\u043d \u0448\u043f\u0438\u043b\u044c\u2605", "1\u0448\u0442-1 \u0430\u0431", "Mael1a", "\u2605\u2605\u2605\u2605\u2605\u2605\u2605\u2605");
        PriceParser.Extraction priceExtraction = priceParser.extract(lines);
        ShopSignClassifier.Classification classification = classifier.classify(lines, priceExtraction, barrelRelation());
        assertEquals(2, classification.ownerLineIndex());
        assertEquals(List.of("\u2605\u0428\u0430\u0431\u043b\u043e\u043d \u0448\u043f\u0438\u043b\u044c\u2605"), classification.itemLines());
    }

    @Test
    void decoratedNicknameBecomesOwnerAfterDecorationStrip() {
        List<String> lines = List.of("\u0424\u0418\u041b\u041e\u0421\u041e\u0412\u0421\u041a\u0418\u0419", "\u041a\u0410\u041c\u0415\u041d\u042c\u2605\u2605\u2605\u2605\u2605\u2605", "1 \u0448\u0442 - 1 \u0410\u0411", "\u2620\u2620\u2620DenazYT\u2620\u2620\u2620");
        PriceParser.Extraction priceExtraction = priceParser.extract(lines);
        ShopSignClassifier.Classification classification = classifier.classify(lines, priceExtraction, barrelRelation());
        assertEquals(3, classification.ownerLineIndex());
        assertEquals(List.of("\u0424\u0418\u041b\u041e\u0421\u041e\u0412\u0421\u041a\u0418\u0419", "\u041a\u0410\u041c\u0415\u041d\u042c\u2605\u2605\u2605\u2605\u2605\u2605"), classification.itemLines());
    }

    @Test
    void singleWordRussianItemIsNotTreatedAsOwnerLine() {
        assertEquals(-1, classifier.findOwnerLineIndex(List.of("\u041e\u0411\u0421\u0418\u0414\u0418\u0410\u041d")));
    }

    @Test
    void priceLineDetectionWorks() {
        assertEquals(1, priceParser.extract(List.of("\u041f\u0443\u0440\u043f\u0443\u0440\u043d\u044b\u0439 \u0431\u0435\u0442\u043e\u043d", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01")).lineIndex());
    }

    @Test
    void goldenApplesResolve() {
        assertEquals(Identifier.of("minecraft:golden_apple"), resolveItemId("\u0417\u043e\u043b\u043e\u0442\u044b\u0435 \u044f\u0431\u043b\u043e\u043a\u0438", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void decorativeTailIsIgnoredInItemCandidate() {
        assertEquals(
                Identifier.of("minecraft:golden_apple"),
                resolveItemId("\u0417\u043e\u043b\u043e\u0442\u044b\u0435 \u044f\u0431\u043b\u043e\u043a\u0438 \u0425\u0425\u0425\u0425\u0425", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01")
        );
    }

    @Test
    void obsidianResolves() {
        assertEquals(Identifier.of("minecraft:obsidian"), resolveItemId("\u041e\u0411\u0421\u0418\u0414\u0418\u0410\u041d", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void tntResolves() {
        assertEquals(Identifier.of("minecraft:tnt"), resolveItemId("\u0414\u0438\u043d\u0430\u043c\u0438\u0442 (\u0422\u041d\u0422)", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void flintResolves() {
        assertEquals(Identifier.of("minecraft:flint"), resolveItemId("\u041a\u0420\u0415\u041c\u0415\u041d\u042c", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void enderChestsResolve() {
        assertEquals(Identifier.of("minecraft:ender_chest"), resolveItemId("\u042d\u043d\u0434\u0435\u0440 \u0441\u0443\u043d\u0434\u0443\u043a\u0438", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void rootedDirtResolvesWithoutTokenSorting() {
        assertEquals(Identifier.of("minecraft:rooted_dirt"), resolveItemId("\u041a\u043e\u0440\u043d\u0438\u0441\u0442\u0430\u044f \u0437\u0435\u043c\u043b\u044f", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void decorativeFireworkAliasIsStrippedBeforeLookup() {
        assertEquals(Identifier.of("minecraft:firework_rocket"), resolveItemId("\ud83d\udd25\u0424\u0435\u0439\u0435\u0440\u0432\u0435\u0440\u043a\u0438\ud83d\udd25", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void ironBlockTypoFallsBackToFuzzyAlias() {
        assertEquals(Identifier.of("minecraft:iron_block"), resolveItemId("\u0416\u0435\u043b\u0435\u0437\u043d\u044b\u0435\u043a \u0431\u043b\u043e\u043a\u0438", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void shortlistSuggestionCanResolveWhenExactAndFuzzyFail() {
        assertEquals(Identifier.of("minecraft:deepslate_redstone_ore"), resolveItemId("\u0420\u0435\u0434\u0441\u0442\u043e\u0443\u043d\u0441\u043a\u0438\u0439 \u0441\u043b\u0430\u043d\u0435\u0446", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void confirmationNoiseLineIsExcludedFromItemCandidates() {
        assertEquals(
                Identifier.of("minecraft:totem_of_undying"),
                resolveItemId("\u2764\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u043e\u2764", "\u0422\u043e\u0442\u0435\u043c\u044b", "1 \u0448\u0431 - 1 \u0430\u0431", "_ChIzHIk_")
        );
    }

    @Test
    void promoNoiseLineIsExcludedSoItemLineResolves() {
        assertEquals(
                Identifier.of("minecraft:golden_apple"),
                resolveItemId("\u263a\u0414\u0415\u0428\u0401\u0412\u042b\u0415\u2600 \u041e\u041f\u0422!!", "\u0417\u043e\u043b\u043e\u0442\u044b\u0435 \u044f\u0431\u043b\u043e\u043a\u0438", "1 \u0448\u0431 - 1 \u0430\u0431", "MRBELOR")
        );
    }

    @Test
    void repeatedCyrillicSeparatorLineIsIgnoredAndMixedCaseOwnerDetected() {
        List<String> lines = List.of("\u0417\u043e\u043b\u043e\u0442\u044b\u0435 \u044f\u0431\u043b\u043e\u043a\u0438", "1 \u0441\u043b\u043e\u0442 - 1 \u0430\u0431", "\u0425\u0425\u0425\u0425\u0425\u0425\u0425\u0425\u0425\u0425\u0425\u0425\u0425", "FeVas");
        PriceParser.Extraction priceExtraction = priceParser.extract(lines);
        ShopSignClassifier.Classification classification = classifier.classify(lines, priceExtraction, barrelRelation());
        assertEquals(3, classification.ownerLineIndex());
        assertEquals(List.of("\u0417\u043e\u043b\u043e\u0442\u044b\u0435 \u044f\u0431\u043b\u043e\u043a\u0438"), classification.itemLines());
    }

    @Test
    void customDecoratedItemRemainsUnknown() {
        assertNull(resolveItemId("AUSTRIAN ==SCHNITZEL==", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void purpleConcreteResolves() {
        assertEquals(Identifier.of("minecraft:purple_concrete"), resolveItemId("\u041f\u0443\u0440\u043f\u0443\u0440\u043d\u044b\u0439 \u0431\u0435\u0442\u043e\u043d", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void azaleaResolvesSafely() {
        assertEquals(Identifier.of("minecraft:azalea"), resolveItemId("\u0410\u0437\u0430\u043b\u0438\u044f", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void axolotlResolvesSafely() {
        assertEquals(Identifier.of("minecraft:axolotl_spawn_egg"), resolveItemId("\u0410\u043a\u0441\u043e\u043b\u043e\u0442\u043b\u0438", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void blueAxolotlResolvesSafely() {
        assertEquals(Identifier.of("minecraft:blue_axolotl_bucket"), resolveItemId("\u0410\u043a\u0441\u043e\u043b\u043e\u0442\u043b\u044c \u0441\u0438\u043d\u0438\u0439", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void mixedLineDoesNotResolveToRandomItem() {
        assertNull(resolveItemId("\u0410\u043a\u0441\u043e\u043b\u043e\u0442\u043b\u0438 \u0438 \u0438\u0433\u043b\u043e\u0431\u0440\u044e\u0445\u0438", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
        assertNull(resolveItemId("\u0410\u0440\u0431\u0443\u0437 \u0438 \u0442\u044b\u043a\u0432\u0430", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
        assertNull(resolveItemId("\u041a\u043d\u0438\u0433\u0430 \u0438 \u043f\u0435\u0440\u043e", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void tradePhraseDoesNotResolveToRandomItem() {
        assertNull(resolveItemId("\u043a\u0443\u043f\u043b\u044e \u044d\u043b\u0438\u0442\u0440\u044b", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void blacklistedFirstLineMarksSignAsNotShop() {
        List<String> lines = List.of("\u041a\u0443\u043f\u043b\u044e \u044d\u043b\u0438\u0442\u0440\u044b", "\u0417\u0430 10 \u0430\u043b\u043c", "", "Seller_01");
        ShopSignClassifier.Classification classification = classifier.classify(lines, priceParser.extract(lines), barrelRelation());
        assertEquals(ShopSignClassificationType.NOT_SHOP, classification.type());
        assertEquals(ShopSignDiagnosticReason.BLACKLISTED_KEYWORD, classification.reason());
    }

    @Test
    void vagueCategoryDoesNotResolveToRepresentative() {
        assertNull(resolveItemId("\u043c\u0435\u0434\u043d\u044b\u0435 \u0431\u043b\u043e\u043a\u0438", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void enchantmentFallbackDoesNotResolveAsShopItem() {
        assertNull(resolveItemId("\u043f\u0440\u043e\u0447\u043a\u0430 3", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"));
    }

    @Test
    void negativeCacheStoresReason() {
        NegativeShopCache cache = new NegativeShopCache();
        ShopSignFingerprint fingerprint = ShopSignFingerprint.create(
                Identifier.of("minecraft", "overworld"),
                new BlockPos(0, 64, 0),
                new SignTextSnapshot(List.of("\u0411\u043e\u0447\u043a\u0430 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u0438", "", "", "Owner_01"), false, SignSide.FRONT),
                barrelRelation(),
                rules
        );
        cache.put("test", fingerprint, ShopSignDiagnosticReason.NOT_SHOP_NO_PRICE, "\u0411\u043e\u0447\u043a\u0430 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u0438");
        assertNotNull(cache.get("test"));
        assertEquals(ShopSignDiagnosticReason.NOT_SHOP_NO_PRICE, cache.get("test").reason());
    }

    @Test
    void unchangedFingerprintCacheHitIsStable() {
        ShopSignFingerprint left = ShopSignFingerprint.create(
                Identifier.of("minecraft", "overworld"),
                new BlockPos(0, 64, 0),
                new SignTextSnapshot(List.of("\u041f\u0443\u0440\u043f\u0443\u0440\u043d\u044b\u0439 \u0431\u0435\u0442\u043e\u043d", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"), false, SignSide.FRONT),
                barrelRelation(),
                rules
        );
        ShopSignFingerprint right = ShopSignFingerprint.create(
                Identifier.of("minecraft", "overworld"),
                new BlockPos(0, 64, 0),
                new SignTextSnapshot(List.of("\u041f\u0443\u0440\u043f\u0443\u0440\u043d\u044b\u0439 \u0431\u0435\u0442\u043e\u043d", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"), false, SignSide.FRONT),
                barrelRelation(),
                rules
        );
        assertEquals(left, right);
    }

    @Test
    void barrelRelationDetectionInputIsAcceptedByClassifier() {
        ShopSignClassifier.Classification classification = classifier.classify(
                List.of("\u041e\u0411\u0421\u0418\u0414\u0418\u0410\u041d", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01"),
                priceParser.extract(List.of("\u041e\u0411\u0421\u0418\u0414\u0418\u0410\u041d", "1 \u0430\u043b\u043c \u0437\u0430 1 \u0448\u0442", "", "Seller_01")),
                barrelRelation()
        );
        assertTrue(classification.type() == ShopSignClassificationType.SHOP);
    }

    @Test
    void multiDiscListingWithoutStandardPriceBecomesConfirmedUnresolvable() {
        List<String> lines = List.of(
                "\u0410\u0430\u0440\u043e\u043d \u0428\u0435\u0440\u043e\u0444 1\u0430\u043b",
                "\u041b\u0435\u043d\u0430 \u0420\u0435\u0439\u043d 1\u0430\u043b",
                "\u0421418-13 1\u0430\u043b",
                "MIN4CHIK"
        );
        int ownerLineIndex = classifier.findOwnerLineIndex(lines, -1);
        List<String> itemLines = classifier.extractItemLines(lines, ownerLineIndex, -1);
        assertEquals("confirmed-unresolvable:multi-disc-listing", ShopSignParser.confirmedUnresolvableNoPriceReason(itemLines));
    }

    @Test
    void enchantPriceListWithoutStandardPriceBecomesConfirmedUnresolvable() {
        List<String> lines = List.of(
                "\u0421\u0438\u043b\u0430 - 4\u0430\u043b\u043c",
                "\u041f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c - 1\u0430\u043b",
                "",
                "seller_name"
        );
        int ownerLineIndex = classifier.findOwnerLineIndex(lines, -1);
        List<String> itemLines = classifier.extractItemLines(lines, ownerLineIndex, -1);
        assertEquals("confirmed-unresolvable:enchant-price-list", ShopSignParser.confirmedUnresolvableNoPriceReason(itemLines));
    }

    @Test
    void enchantPriceListWithParenthesesBecomesConfirmedUnresolvable() {
        List<String> lines = List.of(
                "\u0417\u0430\u0449\u0438\u0442\u0430 (2\u0430\u043b\u043c)",
                "\u041f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c(1\u0430\u043b\u043c)",
                "",
                "spraytin"
        );
        int ownerLineIndex = classifier.findOwnerLineIndex(lines, -1);
        List<String> itemLines = classifier.extractItemLines(lines, ownerLineIndex, -1);
        assertEquals("confirmed-unresolvable:enchant-price-list", ShopSignParser.confirmedUnresolvableStructuredReason(itemLines));
    }

    @Test
    void enchantPriceListWithParsedPriceStillTriggersEarlyDetection() {
        List<String> lines = List.of(
                "\u0421\u0438\u043b\u0430 - 4\u0430\u043b\u043c",
                "\u041f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c - 1\u0430\u043b",
                "",
                "Naiwah"
        );
        int ownerLineIndex = classifier.findOwnerLineIndex(lines, priceParser.extract(lines).lineIndex());
        List<String> itemLines = classifier.extractItemLines(lines, ownerLineIndex, -1);
        assertEquals("confirmed-unresolvable:enchant-price-list", ShopSignParser.confirmedUnresolvableEarlyReason(lines, itemLines));
    }

    @Test
    void serviceSignsBecomeConfirmedUnresolvableEarly() {
        List<String> lines = List.of(
                "\u0410\u0440\u0435\u043d\u0434\u0430 \u0433\u0443\u0431\u043e\u043a",
                "1 \u0433\u0443\u0431\u043a\u0430 - 1 \u0430\u043b\u043c",
                "\u0432 \u0447\u0430\u0441",
                "Avstroduck"
        );
        int ownerLineIndex = classifier.findOwnerLineIndex(lines, priceParser.extract(lines).lineIndex());
        List<String> itemLines = classifier.extractItemLines(lines, ownerLineIndex, -1);
        assertEquals("confirmed-unresolvable:service-sign", ShopSignParser.confirmedUnresolvableEarlyReason(lines, itemLines));
    }

    private static Identifier resolveItemId(String line1, String line2, String line3, String line4) {
        return itemAliasResolver.resolve(List.of(line1, line2, line3, line4), priceParser.extract(List.of(line1, line2, line3, line4))).itemId();
    }

    private static SignContainerRelation barrelRelation() {
        return new SignContainerRelation(true, Identifier.of("minecraft", "barrel"), new BlockPos(0, 63, 0), "nearby");
    }

    private static ItemAliasConfig loadItemAliasConfig(ParserRulesConfig rules) throws IOException {
        ItemAliasConfig config = new ItemAliasConfig();
        mergeAliasFile(config, Path.of("src/client/resources/assets/pplshop/default-config/item_aliases.json"));
        mergeAliasFile(config, Path.of("src/client/resources/assets/pplshop/default-config/item_aliases_user.json"));
        config.rebuildNormalizedAliasIndex(rules);
        return config;
    }

    private static void mergeAliasFile(ItemAliasConfig config, Path path) throws IOException {
        Map<String, List<String>> target = config.items;
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            JsonElement value = entry.getValue();
            JsonArray aliasArray = value.isJsonObject()
                    ? value.getAsJsonObject().getAsJsonArray("aliases")
                    : value.getAsJsonArray();
            List<String> aliases = new ArrayList<>(aliasArray.size());
            for (JsonElement alias : aliasArray) {
                aliases.add(alias.getAsString());
            }

            String key = entry.getKey().startsWith("minecraft:enchanted_book_")
                    ? "minecraft:enchanted_book"
                    : entry.getKey();
            if (value.isJsonObject()) {
                JsonObject object = value.getAsJsonObject();
                String runtimeItemId = object.has("runtime_item_id") ? object.get("runtime_item_id").getAsString() : null;
                String subtype = object.has("subtype") ? object.get("subtype").getAsString() : "";
                String displayName = object.has("display_name") ? object.get("display_name").getAsString() : "";
                config.targetMetadata.put(key, AliasTargetMappings.resolve(key, runtimeItemId, subtype, displayName));
            }
            LinkedHashSet<String> merged = new LinkedHashSet<>(target.getOrDefault(key, List.of()));
            merged.addAll(aliases);
            target.put(key, new ArrayList<>(merged));
        }
    }
}
