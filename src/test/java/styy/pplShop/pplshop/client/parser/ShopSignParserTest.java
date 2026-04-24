package styy.pplShop.pplshop.client.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import styy.pplShop.pplshop.client.config.CurrencyAliasConfig;
import styy.pplShop.pplshop.client.config.ItemAliasConfig;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.config.AliasTargetMappings;
import styy.pplShop.pplshop.client.model.ItemResolutionResultType;
import styy.pplShop.pplshop.client.model.ParsedItem;

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

class ShopSignParserTest {
    private static ItemAliasResolver itemAliasResolver;
    private static PriceParser priceParser;

    @BeforeAll
    static void setUp() throws IOException {
        ParserRulesConfig rules = ParserRulesConfig.defaults();
        CurrencyAliasConfig currencyAliases = CurrencyAliasConfig.defaults();
        itemAliasResolver = new ItemAliasResolver(loadItemAliasConfig(rules), currencyAliases, rules);
        priceParser = new PriceParser(currencyAliases, rules);
    }

    @Test
    void exactItemStillResolves() {
        ParsedItem parsedItem = parseItem("\u0410\u0437\u0430\u043b\u0438\u044f", "1 \u0430\u043b\u043c\u0430\u0437 - 3 \u0430\u043b", "", "Seller");
        assertEquals(Identifier.of("minecraft:azalea"), parsedItem.itemId());
    }

    @Test
    void enchantmentOnlyBecomesEnchantedBook() {
        ParsedItem parsedItem = parseItem("\u0414\u043e\u0431\u044b\u0447\u0430 III", "3 \u0430\u043b\u043c \u0437\u0430 \u0448\u0442\u0443\u043a\u0443", "", "REALCHIKLAPA");
        assertEquals(Identifier.of("minecraft:enchanted_book"), parsedItem.itemId());
        assertNotNull(parsedItem.resolutionTrace());
    }

    @Test
    void netheriteSetGetsRepresentativeItem() {
        ParsedItem parsedItem = parseItem("\u0424\u0423\u041b\u041b \u041d\u0415\u0417\u0415\u0420", "\u0421\u0415\u0422", "1\u043d\u0437-35\u0430\u043b\u043c", "__weker__");
        assertEquals(Identifier.of("minecraft:netherite_chestplate"), parsedItem.itemId());
        assertNotNull(parsedItem.resolutionTrace());
    }

    @Test
    void noisyNetheriteAxeStillResolves() {
        ParsedItem parsedItem = parseItem("\u0424\u0443\u043b\u043b \u0417\u0430\u0447\u0430\u0440 / \u041d\u0435\u0437\u0435\u0440\u0438\u0442 \u0422\u043e\u043f\u043e\u0440", "\u041f\u043e\u0447\u0438\u043d\u043a\u0430 \u0443\u0434\u0430\u0447\u0430 \u0448\u0435\u043b\u043a", "1 \u0448\u0442\u0443\u043a\u0430-10\u0430\u043b\u043c", "ZigOval");
        assertEquals(Identifier.of("minecraft:netherite_axe"), parsedItem.itemId());
    }

    @Test
    void shortDirtyBootsAliasStillWorks() {
        ParsedItem parsedItem = parseItem("\u0430\u043b\u043c \u0431\u043e\u0442\u0438\u043d\u043a\u0438", "2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:diamond_boots"), parsedItem.itemId());
    }

    @Test
    void toolsCategoryPhraseStaysUnknown() {
        ParsedItem parsedItem = parseItem("\u0430\u043b\u043c\u0430\u0437\u043d\u044b\u0435 \u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442\u044b", "1 \u0448\u0442\u0443\u043a\u0430 - 5 \u0430\u043b\u043c", "", "seller_name");
        assertNull(parsedItem.itemId());
    }

    @Test
    void dirtyDecoratedEnchantAliasStillWorks() {
        ParsedItem parsedItem = parseItem("\u2764 \u041f\u043e\u0434\u0432\u043e\u0434\u043d\u043e\u0435 \u0434\u044b\u0445 \u2764", "1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:enchanted_book"), parsedItem.itemId());
    }

    @Test
    void priceTailNoLongerTurnsIntoPotionAlias() {
        ParsedItem parsedItem = parseItem("\u0411\u0430\u0433\u0440\u043e\u0432\u044b\u0435 \u043a\u043e\u0440\u043d\u0438", "2 \u0441\u0442 - 1 \u0430\u043b\u043c", "", "Lankhmaro");
        assertEquals(Identifier.of("minecraft:crimson_roots"), parsedItem.itemId());
        assertEquals("\u0431\u0430\u0433\u0440\u043e\u0432\u044b\u0435 \u043a\u043e\u0440\u043d\u0438", parsedItem.rawText());
    }

    @Test
    void splitMangroveLogStillResolves() {
        ParsedItem parsedItem = parseItem("\u041c\u0430\u043d\u0433\u0440\u043e\u0432\u043e\u0435", "\u0431\u0440\u0435\u0432\u043d\u043e", "1\u0441\u0442 = 3 \u0430\u043b\u043c", "Fanya__");
        assertEquals(Identifier.of("minecraft:mangrove_log"), parsedItem.itemId());
    }

    @Test
    void experienceBottleAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0411\u0443\u0442\u044b\u043b\u044c\u043a\u0438 \u043e\u043f\u044b\u0442\u0430", "1 \u0441\u043b\u043e\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:experience_bottle"), parsedItem.itemId());
    }

    @Test
    void experienceBottleTypoAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0411\u0443\u0442\u044b\u043b\u043e\u0447\u043a\u0438\u0435 \u043e\u043f\u044b\u0442\u0430", "1 \u0441\u043b\u043e\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:experience_bottle"), parsedItem.itemId());
    }

    @Test
    void honeyBottleAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041c\u0435\u0434\u043e\u0432\u0430\u044f \u0431\u0443\u0442\u044b\u043b\u043a\u0430", "1 \u0448\u0442\u0443\u043a\u0430 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:honey_bottle"), parsedItem.itemId());
    }

    @Test
    void oakSignsResolveFromPluralAlias() {
        ParsedItem parsedItem = parseItem("\u0414\u0443\u0431\u043e\u0432\u044b\u0435 \u0442\u0430\u0431\u043b\u0438\u0447\u043a\u0438", "1 \u0441\u0442\u0430\u043a - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:oak_sign"), parsedItem.itemId());
    }

    @Test
    void endRodsDoNotFallbackToPotion() {
        ParsedItem parsedItem = parseItem("\u0421\u0442\u0435\u0440\u0436\u043d\u0438 \u044d\u043d\u0434\u0430", "1 \u0441\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:end_rod"), parsedItem.itemId());
    }

    @Test
    void typoCanResolveThroughDirectFuzzyAlias() {
        ParsedItem parsedItem = parseItem("\u0416\u0435\u043b\u0435\u0437\u043d\u044b\u0435\u043a \u0431\u043b\u043e\u043a\u0438", "1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:iron_block"), parsedItem.itemId());
        assertEquals(ItemResolutionResultType.FUZZY_ALIAS, parsedItem.resultType());
    }

    @Test
    void spireTemplateAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0428\u0430\u0431\u043b\u043e\u043d \u0448\u043f\u0438\u043b\u044c", "1 \u0448\u0442 - 3 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:spire_armor_trim_smithing_template"), parsedItem.itemId());
    }

    @Test
    void enchantedGoldenAppleAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0417\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u043d\u044b\u0435 \u044f\u0431\u043b\u043e\u043a\u0438", "1 \u0448\u0442 - 4 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:enchanted_golden_apple"), parsedItem.itemId());
    }

    @Test
    void eggAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041a\u0443\u0440\u0438\u043d\u043e\u0435 \u044f\u0439\u0446\u043e", "1 \u0441\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:egg"), parsedItem.itemId());
    }

    @Test
    void myEggsAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041c\u043e\u0438 \u044f\u0439\u0446\u0430", "1 \u0441\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:egg"), parsedItem.itemId());
    }

    @Test
    void basaltAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0411\u0430\u0437\u0430\u043b\u044c\u0442", "1 \u0441\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:basalt"), parsedItem.itemId());
    }

    @Test
    void englishTntAliasResolves() {
        ParsedItem parsedItem = parseItem("TNT", "1 \u0441\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:tnt"), parsedItem.itemId());
    }

    @Test
    void hopperCombinedAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0412\u043e\u0440\u043e\u043d\u043a\u0430 \u0432\u043e\u0440\u043e\u043d\u043a\u0438", "1 \u0448\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:hopper"), parsedItem.itemId());
    }

    @Test
    void speedPotionAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0421\u043f\u043e\u0440\u0442\u0438\u0432\u043d\u044b\u0435 \u0437\u0435\u043b\u044c\u044f", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:potion"), parsedItem.itemId());
        assertEquals("minecraft:swiftness_potion", parsedItem.resolvedBucketId());
    }

    @Test
    void invisibilityAliasResolvesToPotionSubtype() {
        ParsedItem parsedItem = parseItem("\u041d\u0435\u0432\u0438\u0434\u0438\u043c\u043e\u0441\u0442\u044c", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:potion"), parsedItem.itemId());
        assertEquals("minecraft:invisibility_potion", parsedItem.resolvedBucketId());
    }

    @Test
    void healingAliasResolvesToPotionSubtype() {
        ParsedItem parsedItem = parseItem("\u0418\u0441\u0446\u0435\u043b\u0435\u043d\u0438\u0435 II", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:potion"), parsedItem.itemId());
        assertEquals("minecraft:healing_potion", parsedItem.resolvedBucketId());
    }

    @Test
    void genericPotionsAliasResolvesToPotion() {
        ParsedItem parsedItem = parseItem("\u0417\u0435\u043b\u044c\u044f", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:potion"), parsedItem.itemId());
    }

    @Test
    void bedsAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041a\u0440\u043e\u0432\u0430\u0442\u0438", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:red_bed"), parsedItem.itemId());
    }

    @Test
    void deadBushSingularAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041c\u0451\u0440\u0442\u0432\u044b\u0439 \u043a\u0443\u0441\u0442", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:dead_bush"), parsedItem.itemId());
    }

    @Test
    void blazeRodAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041e\u0433\u043d\u0435\u043d\u043d\u044b\u0435 \u0441\u0442\u0435\u0440\u0436\u043d", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:blaze_rod"), parsedItem.itemId());
    }

    @Test
    void pinkPetalsAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0420\u043e\u0437 \u043b\u0435\u043f\u0435\u0441\u0442\u043a\u0438", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:pink_petals"), parsedItem.itemId());
    }

    @Test
    void calciteAliasStillResolves() {
        ParsedItem parsedItem = parseItem("\u041a\u0430\u043b\u044c\u0446\u0438\u0442", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:calcite"), parsedItem.itemId());
    }

    @Test
    void deepslateAliasStillResolves() {
        ParsedItem parsedItem = parseItem("\u0421\u043b\u0430\u043d\u0435\u0446", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:deepslate"), parsedItem.itemId());
    }

    @Test
    void enchantedBookPluralAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041a\u043d\u0438\u0433\u0438 \u0437\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u0438\u044f", "1 \u0448\u0442 - 2 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:enchanted_book"), parsedItem.itemId());
    }

    @Test
    void precipiceDiscAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041f\u043b\u0430\u0441\u0442\u0438\u043d\u043a\u0430 precipice", "1 \u0448\u0442 - 5 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:music_disc_precipice"), parsedItem.itemId());
    }

    @Test
    void multiItemConjunctionUsesFallbackReason() {
        ParsedItem parsedItem = parseItem("\u041e\u0431\u0441\u0438\u0434\u0438\u0430\u043d \u0438", "\u041a\u0440\u0435\u043c\u0435\u043d\u044c", "1 \u0430\u043b\u043c", "seller_name");
        assertEquals(Identifier.of("minecraft:bundle"), parsedItem.itemId());
        assertEquals(ItemResolutionResultType.MIXED_ITEM, parsedItem.resultType());
        assertEquals("multi-item-sign-conjunction", parsedItem.resolutionTrace().fallbackReason());
    }

    @Test
    void multiItemCommaUsesFallbackReason() {
        ParsedItem parsedItem = parseItem("\u041a\u0430\u043b\u044c\u0446\u0438\u0442, \u0441\u043b\u0430\u043d\u0435\u0446", "1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:bundle"), parsedItem.itemId());
        assertEquals(ItemResolutionResultType.MIXED_ITEM, parsedItem.resultType());
        assertEquals("multi-item-sign-comma", parsedItem.resolutionTrace().fallbackReason());
    }

    @Test
    void commaPartialFallbackResolvesMatchingSide() {
        ParsedItem parsedItem = parseItem("\u041a\u0430\u043b\u044c\u0446\u0438\u0442, \u043d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u043e", "1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:calcite"), parsedItem.itemId());
    }

    @Test
    void lootBundlePhraseBecomesConfirmedUnresolvable() {
        ParsedItem parsedItem = parseItem("\u041b\u0443\u0442 \u0438\u0437 \u0424\u0435\u043c\u0431\u043e\u0439\u0441\u043a\u0430", "1 \u0430\u043b\u043c", "", "seller_name");
        assertNull(parsedItem.itemId());
        assertEquals("confirmed-unresolvable:loot-bundle", parsedItem.resolutionTrace().fallbackReason());
    }

    @Test
    void booksFromDungeonPhraseBecomesConfirmedUnresolvable() {
        ParsedItem parsedItem = parseItem("\u041a\u043d\u0438\u0433\u0438 \u0438\u0437 \u0434\u0430\u043d\u0436\u0430", "1 \u0430\u043b\u043c", "", "seller_name");
        assertNull(parsedItem.itemId());
        assertEquals("confirmed-unresolvable:loot-bundle", parsedItem.resolutionTrace().fallbackReason());
    }

    @Test
    void shortlistSuggestionBecomesSuggestedFallback() {
        ParsedItem parsedItem = parseItem("\u0420\u0435\u0434\u0441\u0442\u043e\u0443\u043d\u0441\u043a\u0438\u0439 \u0441\u043b\u0430\u043d\u0435\u0446", "1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:deepslate_redstone_ore"), parsedItem.itemId());
        assertEquals(ItemResolutionResultType.SUGGESTED_FALLBACK, parsedItem.resultType());
    }

    @Test
    void shorthandUnbreakingBookStaysUnknownWithoutSafeAlias() {
        ParsedItem parsedItem = parseItem("\u041f\u0440\u043e\u0447\u043a\u0430 3", "1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:enchanted_book"), parsedItem.itemId());
    }

    @Test
    void textOnlySignStaysUnknown() {
        ParsedItem parsedItem = parseItem("\u041f\u0440\u0430\u0432\u0438\u043b\u0430 Pepeland 10", "1 \u0432\u0435\u0449\u044c - 3 \u0430\u043b", "", "tribys_");
        assertNull(parsedItem.itemId());
        assertNotNull(parsedItem.resolutionTrace());
    }

    @Test
    void breadAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0425\u043b\u0435\u0431", "1 \u0448\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:bread"), parsedItem.itemId());
    }

    @Test
    void mushroomStewVariantResolves() {
        ParsedItem parsedItem = parseItem("\u0413\u0440\u0438\u0431\u043e\u0432\u043e\u0439 \u0441\u0443\u043f", "1 \u0448\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:mushroom_stew"), parsedItem.itemId());
    }

    @Test
    void copperIngotAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041c\u0435\u0434\u044c", "1 \u0448\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:copper_ingot"), parsedItem.itemId());
    }

    @Test
    void copperBlockAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041c\u0435\u0434\u043d\u044b\u0439 \u0431\u043b\u043e\u043a", "1 \u0448\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:copper_block"), parsedItem.itemId());
    }

    @Test
    void ghastTearAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0421\u0443\u0445\u043e\u0439 \u0433\u0430\u0441\u0442", "1 \u0448\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:ghast_tear"), parsedItem.itemId());
    }

    @Test
    void wetSpongeAliasResolves() {
        ParsedItem parsedItem = parseItem("\u041c\u043e\u043a\u0440\u0430\u044f \u0433\u0443\u0431\u043a\u0430", "1 \u0448\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:wet_sponge"), parsedItem.itemId());
    }

    @Test
    void shortenedEnchantedBookAliasResolves() {
        ParsedItem parsedItem = parseItem("\u0417\u0430\u0447\u0430\u0440 \u043a\u043d\u0438\u0433\u0430", "1 \u0448\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:enchanted_book"), parsedItem.itemId());
    }

    @Test
    void quantityNoiseFallbackResolvesCopper() {
        ParsedItem parsedItem = parseItem("\u041c\u0435\u0434\u044c 2 \u0441\u0442\u0430\u043a\u043001", "1 \u0448\u0442 - 1 \u0430\u043b\u043c", "", "seller_name");
        assertEquals(Identifier.of("minecraft:copper_ingot"), parsedItem.itemId());
    }

    @Test
    void smeltedOreBundleBecomesConfirmedUnresolvable() {
        ParsedItem parsedItem = parseItem("\u041f\u0435\u0440\u0435\u043f\u043b\u0430\u0432\u043b\u0435\u043d\u043d\u044b\u0435 \u0440\u0443\u0434\u044b", "1 \u0430\u043b\u043c", "", "seller_name");
        assertNull(parsedItem.itemId());
        assertEquals("confirmed-unresolvable:loot-bundle", parsedItem.resolutionTrace().fallbackReason());
    }

    @Test
    void customPotionTitleBecomesConfirmedUnresolvable() {
        ParsedItem parsedItem = parseItem("\u041f\u043e\u0439\u043b\u043e \u0432\u0435\u0434\u044c\u043c\u044b", "1 \u0430\u043b\u043c", "", "seller_name");
        assertNull(parsedItem.itemId());
        assertEquals("confirmed-unresolvable:custom-title", parsedItem.resolutionTrace().fallbackReason());
    }

    private static ParsedItem parseItem(String line1, String line2, String line3, String line4) {
        List<String> lines = List.of(line1, line2, line3, line4);
        PriceParser.Extraction priceExtraction = priceParser.extract(lines);
        return itemAliasResolver.resolve(lines, priceExtraction);
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
