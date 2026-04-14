package styy.pplShop.pplshop.client.gui;

import net.minecraft.util.Identifier;
import styy.pplShop.pplshop.client.model.ShopSignEntry;

import java.util.EnumSet;
import java.util.Set;

public final class ItemThemeResolver {
    public boolean matches(ShopSignEntry entry, ShopTheme theme) {
        if (theme == null || theme == ShopTheme.ALL) {
            return true;
        }
        if (entry == null || entry.resolvedItemId() == null) {
            return theme == ShopTheme.UNKNOWN;
        }
        return this.resolveThemes(entry).contains(theme);
    }

    public Set<ShopTheme> resolveThemes(ShopSignEntry entry) {
        if (entry == null || entry.resolvedItemId() == null) {
            return EnumSet.of(ShopTheme.UNKNOWN);
        }
        return this.resolveThemes(entry.resolvedItemId(), entry.parsedItem().resolvedBucketId(), entry.parsedItem().resolvedSubtypeKey());
    }

    Set<ShopTheme> resolveThemes(Identifier itemId, String bucketId, String subtypeKey) {
        if (itemId == null) {
            return EnumSet.of(ShopTheme.UNKNOWN);
        }

        EnumSet<ShopTheme> themes = EnumSet.noneOf(ShopTheme.class);
        String path = itemId.getPath();
        bucketId = bucketId == null ? "" : bucketId;
        subtypeKey = subtypeKey == null ? "" : subtypeKey;

        if (isPotion(path, bucketId)) {
            themes.add(ShopTheme.POTIONS);
            themes.add(ShopTheme.VALUABLES);
        }
        if (isMobItem(path)) {
            themes.add(ShopTheme.MOBS);
        }
        if (isFoodItem(path)) {
            themes.add(ShopTheme.FOOD);
            if (isFarmItem(path)) {
                themes.add(ShopTheme.FARM);
            }
        }
        if (isPlantItem(path)) {
            themes.add(ShopTheme.PLANTS);
        }
        if (isFarmItem(path)) {
            themes.add(ShopTheme.FARM);
        }
        if (isRedstoneItem(path)) {
            themes.add(ShopTheme.REDSTONE);
            themes.add(ShopTheme.CRAFTING);
        }
        if (isToolItem(path)) {
            themes.add(ShopTheme.TOOLS);
        }
        if (isWeaponItem(path)) {
            themes.add(ShopTheme.WEAPONS);
        }
        if (isArmorItem(path)) {
            themes.add(ShopTheme.ARMOR);
        }
        if (isResourceItem(path, bucketId)) {
            themes.add(ShopTheme.RESOURCES);
            themes.add(ShopTheme.CRAFTING);
        }
        if (isValuableItem(path, bucketId, subtypeKey)) {
            themes.add(ShopTheme.VALUABLES);
        }
        if (isNaturalBlock(path)) {
            themes.add(ShopTheme.NATURAL);
            themes.add(ShopTheme.BUILDING);
        }
        if (isDecorativeBlock(path, bucketId)) {
            themes.add(ShopTheme.DECORATIVE);
            themes.add(ShopTheme.BUILDING);
        }
        if (isBuildingMaterial(path, bucketId)) {
            themes.add(ShopTheme.BUILDING);
        }

        if (themes.isEmpty()) {
            themes.add(ShopTheme.MISC);
        }
        return themes;
    }

    private static boolean isPotion(String path, String bucketId) {
        return "potion".equals(path)
                || "splash_potion".equals(path)
                || "lingering_potion".equals(path)
                || bucketId.contains("potion");
    }

    private static boolean isMobItem(String path) {
        return path.endsWith("_spawn_egg")
                || "axolotl_bucket".equals(path)
                || "tadpole_bucket".equals(path)
                || "cod_bucket".equals(path)
                || "salmon_bucket".equals(path)
                || "pufferfish_bucket".equals(path)
                || "tropical_fish_bucket".equals(path);
    }

    private static boolean isFoodItem(String path) {
        return path.contains("apple")
                || path.contains("bread")
                || path.contains("carrot")
                || path.contains("potato")
                || path.contains("beef")
                || path.contains("pork")
                || path.contains("chicken")
                || path.contains("mutton")
                || path.contains("cod")
                || path.contains("salmon")
                || path.contains("stew")
                || path.contains("soup")
                || path.contains("cookie")
                || path.contains("cake")
                || path.contains("pie")
                || path.contains("melon_slice")
                || path.contains("dried_kelp")
                || path.contains("sweet_berries")
                || path.contains("glow_berries")
                || path.contains("golden_");
    }

    private static boolean isPlantItem(String path) {
        return path.contains("sapling")
                || path.contains("flower")
                || path.contains("rose")
                || path.contains("bush")
                || path.contains("moss")
                || path.contains("vine")
                || path.contains("leaves")
                || path.contains("azalea")
                || path.contains("grass")
                || path.contains("fern")
                || path.contains("seed")
                || path.contains("crop")
                || path.contains("roots")
                || path.contains("wheat")
                || path.contains("carrot")
                || path.contains("potato")
                || path.contains("beetroot")
                || path.contains("cactus")
                || path.contains("sugar_cane")
                || path.contains("bamboo");
    }

    private static boolean isFarmItem(String path) {
        return path.contains("wheat")
                || path.contains("carrot")
                || path.contains("potato")
                || path.contains("beetroot")
                || path.contains("pumpkin")
                || path.contains("melon")
                || path.contains("egg")
                || path.contains("honey")
                || path.contains("bucket_of_milk")
                || path.contains("milk_bucket")
                || path.contains("nether_wart")
                || path.contains("sugar_cane");
    }

    private static boolean isRedstoneItem(String path) {
        return path.contains("redstone")
                || path.contains("comparator")
                || path.contains("repeater")
                || path.contains("observer")
                || path.contains("piston")
                || path.contains("lever")
                || path.contains("button")
                || path.contains("pressure_plate")
                || path.contains("hopper")
                || path.contains("dispenser")
                || path.contains("dropper")
                || path.contains("daylight_detector")
                || path.contains("target")
                || path.contains("tripwire")
                || path.contains("note_block")
                || path.contains("sculk_sensor");
    }

    private static boolean isToolItem(String path) {
        return path.endsWith("_pickaxe")
                || path.endsWith("_axe")
                || path.endsWith("_shovel")
                || path.endsWith("_hoe")
                || path.endsWith("_shears")
                || path.endsWith("_fishing_rod")
                || path.endsWith("_flint_and_steel")
                || path.endsWith("_brush")
                || path.endsWith("_spyglass")
                || path.endsWith("_compass")
                || path.endsWith("_clock");
    }

    private static boolean isWeaponItem(String path) {
        return path.endsWith("_sword")
                || path.endsWith("_bow")
                || path.endsWith("_crossbow")
                || path.endsWith("_trident")
                || path.endsWith("_mace")
                || path.contains("arrow");
    }

    private static boolean isArmorItem(String path) {
        return path.endsWith("_helmet")
                || path.endsWith("_chestplate")
                || path.endsWith("_leggings")
                || path.endsWith("_boots")
                || path.contains("elytra")
                || path.contains("shield")
                || path.contains("trim_smithing_template");
    }

    private static boolean isResourceItem(String path, String bucketId) {
        return path.contains("ingot")
                || path.contains("nugget")
                || path.contains("gem")
                || path.contains("diamond")
                || path.contains("emerald")
                || path.contains("lapis")
                || path.contains("quartz")
                || path.contains("amethyst")
                || path.contains("shard")
                || path.contains("scrap")
                || path.contains("dust")
                || path.contains("string")
                || path.contains("leather")
                || path.contains("slime")
                || path.contains("blaze_rod")
                || path.contains("ender_pearl")
                || path.contains("ghast_tear")
                || path.contains("gunpowder")
                || path.contains("flint")
                || path.contains("coal")
                || path.contains("charcoal")
                || path.contains("raw_")
                || bucketId.contains("trim_smithing_template");
    }

    private static boolean isValuableItem(String path, String bucketId, String subtypeKey) {
        return path.contains("diamond")
                || path.contains("netherite")
                || path.contains("ancient_debris")
                || path.contains("elytra")
                || path.contains("totem")
                || path.contains("enchanted_golden_apple")
                || path.contains("shulker")
                || path.contains("ender_chest")
                || path.contains("beacon")
                || path.contains("dragon")
                || bucketId.contains("trim_smithing_template")
                || subtypeKey.startsWith("generic_trim")
                || isPotion(path, bucketId);
    }

    private static boolean isNaturalBlock(String path) {
        return path.contains("dirt")
                || path.contains("grass_block")
                || path.contains("sand")
                || path.contains("gravel")
                || path.contains("clay")
                || path.contains("mud")
                || path.contains("snow")
                || path.contains("ice")
                || path.contains("obsidian")
                || path.contains("netherrack")
                || path.contains("soul_sand")
                || path.contains("soul_soil")
                || path.contains("basalt")
                || path.contains("deepslate")
                || path.contains("stone")
                || path.contains("cobblestone")
                || path.contains("ore")
                || path.contains("magma")
                || path.contains("dripstone")
                || path.contains("calcite")
                || path.contains("tuff");
    }

    private static boolean isDecorativeBlock(String path, String bucketId) {
        return path.contains("glass")
                || path.contains("terracotta")
                || path.contains("wool")
                || path.contains("carpet")
                || path.contains("banner")
                || path.contains("glazed")
                || path.contains("concrete")
                || bucketId.contains("concrete_powder")
                || path.contains("coral")
                || path.contains("lantern")
                || path.contains("torch")
                || path.contains("sea_lantern")
                || path.contains("shroomlight")
                || path.contains("froglight")
                || path.contains("candle")
                || path.contains("painting")
                || path.contains("item_frame");
    }

    private static boolean isBuildingMaterial(String path, String bucketId) {
        return path.contains("planks")
                || path.contains("log")
                || path.contains("wood")
                || path.contains("stripped_")
                || path.contains("slab")
                || path.contains("stairs")
                || path.contains("wall")
                || path.contains("brick")
                || path.contains("tile")
                || path.contains("pillar")
                || path.contains("concrete")
                || bucketId.contains("concrete_powder")
                || path.contains("sandstone")
                || path.contains("quartz_block")
                || path.contains("prismarine")
                || path.contains("purpur")
                || path.contains("copper")
                || path.contains("blackstone");
    }
}
