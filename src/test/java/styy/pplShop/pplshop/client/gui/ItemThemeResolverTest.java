package styy.pplShop.pplshop.client.gui;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemThemeResolverTest {
    private final ItemThemeResolver resolver = new ItemThemeResolver();

    @Test
    void potionEntryMapsToPotionTheme() {
        assertTrue(this.resolver.resolveThemes(Identifier.of("minecraft", "potion"), "minecraft:fire_resistance_potion", "fire_resistance").contains(ShopTheme.POTIONS));
        assertTrue(this.resolver.resolveThemes(Identifier.of("minecraft", "potion"), "minecraft:fire_resistance_potion", "fire_resistance").contains(ShopTheme.VALUABLES));
    }

    @Test
    void unknownEntryAppearsOnlyInUnknownOrAll() {
        assertTrue(this.resolver.resolveThemes(null, "", "").contains(ShopTheme.UNKNOWN));
        assertFalse(this.resolver.resolveThemes(null, "", "").contains(ShopTheme.BUILDING));
    }

    @Test
    void redstoneBlockMapsToRedstoneTheme() {
        assertTrue(this.resolver.resolveThemes(Identifier.of("minecraft", "comparator"), "minecraft:comparator", "").contains(ShopTheme.REDSTONE));
        assertTrue(this.resolver.resolveThemes(Identifier.of("minecraft", "comparator"), "minecraft:comparator", "").contains(ShopTheme.CRAFTING));
    }
}
