package styy.pplShop.pplshop.client.parser;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import styy.pplShop.pplshop.client.config.CurrencyAliasConfig;
import styy.pplShop.pplshop.client.config.ItemAliasConfig;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.model.ParsedItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemAliasRuntimeTargetTest {
    private static ItemAliasResolver resolver;

    @BeforeAll
    static void setUp() {
        ParserRulesConfig rules = ParserRulesConfig.defaults();
        ItemAliasConfig config = new ItemAliasConfig();
        config.items.put("minecraft:fire_resistance_potion", List.of("огнестойкость"));
        config.items.put("minecraft:concrete_powder", List.of("сухой бетон"));
        config.items.put("minecraft:trim_smithing_template", List.of("отделки"));
        config.rebuildNormalizedAliasIndex(rules);
        resolver = new ItemAliasResolver(config, CurrencyAliasConfig.defaults(), rules);
    }

    @Test
    void potionAliasUsesValidRuntimeItemIdAndSubtype() {
        ParsedItem parsedItem = resolver.resolveItemLines(List.of("огнестойкость"));

        assertEquals(Identifier.of("minecraft", "potion"), parsedItem.itemId());
        assertEquals("minecraft:fire_resistance_potion", parsedItem.resolvedBucketId());
        assertEquals("fire_resistance", parsedItem.resolvedSubtypeKey());
        assertTrue(parsedItem.displayNameOverride().isBlank());
    }

    @Test
    void concretePowderAliasUsesRepresentativeRuntimeItemId() {
        ParsedItem parsedItem = resolver.resolveItemLines(List.of("сухой бетон"));

        assertEquals(Identifier.of("minecraft", "white_concrete_powder"), parsedItem.itemId());
        assertEquals("generic_concrete_powder", parsedItem.resolvedSubtypeKey());
    }

    @Test
    void smithingTemplateAliasUsesConcreteRuntimeItemId() {
        ParsedItem parsedItem = resolver.resolveItemLines(List.of("отделки"));

        assertEquals(Identifier.of("minecraft", "sentry_armor_trim_smithing_template"), parsedItem.itemId());
        assertEquals("generic_trim_template", parsedItem.resolvedSubtypeKey());
    }
}
