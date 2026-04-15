package styy.pplShop.pplshop.client.config;

import net.minecraft.util.Identifier;

import java.util.Map;

public final class AliasTargetMappings {
    private static final Map<String, AliasTargetMetadata> LEGACY_BUCKETS = Map.ofEntries(
            Map.entry("minecraft:fire_resistance_potion", new AliasTargetMetadata("minecraft:fire_resistance_potion", Identifier.of("minecraft", "potion"), "fire_resistance", "")),
            Map.entry("minecraft:swiftness_potion", new AliasTargetMetadata("minecraft:swiftness_potion", Identifier.of("minecraft", "potion"), "swiftness", "")),
            Map.entry("minecraft:night_vision_potion", new AliasTargetMetadata("minecraft:night_vision_potion", Identifier.of("minecraft", "potion"), "night_vision", "")),
            Map.entry("minecraft:invisibility_potion", new AliasTargetMetadata("minecraft:invisibility_potion", Identifier.of("minecraft", "potion"), "invisibility", "")),
            Map.entry("minecraft:regeneration_potion", new AliasTargetMetadata("minecraft:regeneration_potion", Identifier.of("minecraft", "potion"), "regeneration", "")),
            Map.entry("minecraft:concrete_powder", new AliasTargetMetadata("minecraft:concrete_powder", Identifier.of("minecraft", "white_concrete_powder"), "generic_concrete_powder", "")),
            Map.entry("minecraft:concrete", new AliasTargetMetadata("minecraft:concrete", Identifier.of("minecraft", "white_concrete"), "generic_concrete", "")),
            Map.entry("minecraft:coral_block", new AliasTargetMetadata("minecraft:coral_block", Identifier.of("minecraft", "tube_coral_block"), "generic_coral_block", "")),
            Map.entry("minecraft:trim_smithing_template", new AliasTargetMetadata("minecraft:trim_smithing_template", Identifier.of("minecraft", "sentry_armor_trim_smithing_template"), "generic_trim_template", ""))
    );

    private AliasTargetMappings() {
    }

    public static AliasTargetMetadata resolve(String rawBucketId) {
        return resolve(rawBucketId, null, null, null);
    }

    public static AliasTargetMetadata resolve(String rawBucketId, String explicitRuntimeItemId, String subtypeKey, String displayNameOverride) {
        if (rawBucketId == null || rawBucketId.isBlank()) {
            return new AliasTargetMetadata("", null, subtypeKey, displayNameOverride);
        }

        if (rawBucketId.startsWith("minecraft:enchanted_book_")) {
            return new AliasTargetMetadata("minecraft:enchanted_book", Identifier.of("minecraft", "enchanted_book"), subtypeKey, displayNameOverride);
        }

        Identifier explicitRuntimeId = explicitRuntimeItemId == null || explicitRuntimeItemId.isBlank()
                ? null
                : Identifier.tryParse(explicitRuntimeItemId);
        if (explicitRuntimeId != null) {
            return new AliasTargetMetadata(rawBucketId, explicitRuntimeId, subtypeKey, displayNameOverride);
        }

        AliasTargetMetadata legacy = LEGACY_BUCKETS.get(rawBucketId);
        if (legacy != null) {
            return legacy;
        }

        Identifier directId = Identifier.tryParse(rawBucketId);
        return new AliasTargetMetadata(rawBucketId, directId, subtypeKey, displayNameOverride);
    }
}
