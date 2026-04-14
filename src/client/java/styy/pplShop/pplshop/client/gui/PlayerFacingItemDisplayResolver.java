package styy.pplShop.pplshop.client.gui;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import styy.pplShop.pplshop.client.model.ShopSignEntry;

public final class PlayerFacingItemDisplayResolver {
    public DisplayInfo resolve(ShopSignEntry entry) {
        if (entry == null) {
            return this.unresolvedDisplay("missing-entry");
        }

        Identifier resolvedItemId = entry.resolvedItemId();
        if (resolvedItemId == null) {
            return this.unresolvedDisplay(determineFallbackReason(entry));
        }

        ItemStack widgetStack = entry.resolvedItemStack();
        if (widgetStack.isEmpty()) {
            return this.unresolvedDisplay("resolved-item-stack-missing");
        }

        String playerFacingTitle = this.resolvePlayerFacingTitle(entry, widgetStack);
        String internalTitle = resolveInternalTitle(entry, widgetStack);
        Identifier widgetStackItemId = Registries.ITEM.getId(widgetStack.getItem());
        return new DisplayInfo(
                internalTitle,
                playerFacingTitle,
                resolvedItemId,
                widgetStackItemId,
                widgetStack,
                false,
                "none"
        );
    }

    private DisplayInfo unresolvedDisplay(String reason) {
        ItemStack fallbackStack = new ItemStack(Items.BARRIER);
        Identifier fallbackItemId = Registries.ITEM.getId(fallbackStack.getItem());
        return new DisplayInfo(
                title("item.pplshop.unknown"),
                title("item.pplshop.unknown"),
                fallbackItemId,
                fallbackItemId,
                fallbackStack,
                true,
                reason
        );
    }

    private String resolvePlayerFacingTitle(ShopSignEntry entry, ItemStack widgetStack) {
        String bucketId = entry.parsedItem().resolvedBucketId();
        String subtypeKey = entry.parsedItem().resolvedSubtypeKey();
        Identifier resolvedItemId = entry.resolvedItemId();
        if ("generic_concrete_powder".equals(subtypeKey) || "minecraft:concrete_powder".equals(bucketId)) {
            return title("item.pplshop.category.concrete_powder");
        }
        if ("generic_trim_template".equals(subtypeKey) || "minecraft:trim_smithing_template".equals(bucketId)) {
            return title("item.pplshop.category.armor_trim_template");
        }
        if (resolvedItemId != null && isPotionItem(resolvedItemId) && !subtypeKey.isBlank()) {
            return this.resolvePotionTitle(resolvedItemId, subtypeKey);
        }
        if (!entry.parsedItem().displayNameOverride().isBlank()
                && !looksTechnicalEnglish(entry.parsedItem().displayNameOverride())) {
            return entry.parsedItem().displayNameOverride();
        }
        String stackName = widgetStack.getName().getString();
        if (stackName != null && !stackName.isBlank()) {
            return stackName;
        }
        return resolveInternalTitle(entry, widgetStack);
    }

    private String resolvePotionTitle(Identifier resolvedItemId, String subtypeKey) {
        String typeKey = switch (resolvedItemId.getPath()) {
            case "splash_potion" -> "item.pplshop.potion.type.splash";
            case "lingering_potion" -> "item.pplshop.potion.type.lingering";
            default -> "item.pplshop.potion.type.drinkable";
        };
        String effectKey = switch (subtypeKey) {
            case "fire_resistance" -> "item.pplshop.potion.effect.fire_resistance";
            case "swiftness" -> "item.pplshop.potion.effect.swiftness";
            case "night_vision" -> "item.pplshop.potion.effect.night_vision";
            case "invisibility" -> "item.pplshop.potion.effect.invisibility";
            case "regeneration" -> "item.pplshop.potion.effect.regeneration";
            default -> "item.pplshop.potion.effect.generic";
        };
        return Text.translatable(typeKey, Text.translatable(effectKey)).getString();
    }

    private static String resolveInternalTitle(ShopSignEntry entry, ItemStack widgetStack) {
        if (!entry.parsedItem().displayNameOverride().isBlank()) {
            return entry.parsedItem().displayNameOverride();
        }
        if (!entry.parsedItem().rawText().isBlank()) {
            return entry.parsedItem().rawText();
        }
        String stackName = widgetStack == null || widgetStack.isEmpty() ? "" : widgetStack.getName().getString();
        if (!stackName.isBlank()) {
            return stackName;
        }
        return title("item.pplshop.unknown");
    }

    private static boolean isPotionItem(Identifier itemId) {
        String path = itemId.getPath();
        return "potion".equals(path) || "splash_potion".equals(path) || "lingering_potion".equals(path);
    }

    private static boolean looksTechnicalEnglish(String value) {
        String lower = value.toLowerCase();
        return lower.contains("potion of")
                || lower.contains("armor trim")
                || lower.contains("concrete powder")
                || lower.contains("unknown item");
    }

    private static String determineFallbackReason(ShopSignEntry entry) {
        String traceFallback = entry.parsedItem().resolutionTrace().fallbackReason();
        if (traceFallback != null && !traceFallback.isBlank()) {
            return traceFallback;
        }
        if (entry.parsedItem().matchedAlias() == null || entry.parsedItem().matchedAlias().isBlank()) {
            return "no-alias-matched";
        }
        return "unresolved-item";
    }

    private static String title(String translationKey) {
        return Text.translatable(translationKey).getString();
    }

    public record DisplayInfo(
            String internalTitle,
            String playerFacingTitle,
            Identifier widgetItemId,
            Identifier widgetStackItemId,
            ItemStack widgetStack,
            boolean fallbackUsed,
            String fallbackReason
    ) {
        public DisplayInfo {
            internalTitle = internalTitle == null ? "" : internalTitle;
            playerFacingTitle = playerFacingTitle == null ? "" : playerFacingTitle;
            widgetStack = widgetStack == null ? ItemStack.EMPTY : widgetStack.copy();
            fallbackReason = fallbackReason == null ? "" : fallbackReason;
        }

        @Override
        public ItemStack widgetStack() {
            return this.widgetStack.copy();
        }
    }
}
