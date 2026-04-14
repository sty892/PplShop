package styy.pplShop.pplshop.client.gui;

public enum ShopTheme {
    ALL("screen.pplshop.theme.all"),
    BUILDING("screen.pplshop.theme.building"),
    DECORATIVE("screen.pplshop.theme.decorative"),
    NATURAL("screen.pplshop.theme.natural"),
    RESOURCES("screen.pplshop.theme.resources"),
    REDSTONE("screen.pplshop.theme.redstone"),
    TOOLS("screen.pplshop.theme.tools"),
    WEAPONS("screen.pplshop.theme.weapons"),
    ARMOR("screen.pplshop.theme.armor"),
    FOOD("screen.pplshop.theme.food"),
    POTIONS("screen.pplshop.theme.potions"),
    MOBS("screen.pplshop.theme.mobs"),
    PLANTS("screen.pplshop.theme.plants"),
    FARM("screen.pplshop.theme.farm"),
    CRAFTING("screen.pplshop.theme.crafting"),
    VALUABLES("screen.pplshop.theme.valuables"),
    MISC("screen.pplshop.theme.misc"),
    UNKNOWN("screen.pplshop.theme.unknown");

    private final String translationKey;

    ShopTheme(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public ShopTheme next() {
        ShopTheme[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
