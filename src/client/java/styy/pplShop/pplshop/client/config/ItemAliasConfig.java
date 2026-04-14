package styy.pplShop.pplshop.client.config;

import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemAliasConfig {
    public Map<String, List<String>> items = new LinkedHashMap<>();
    public transient Map<String, String> normalizedAliasToItemId = new LinkedHashMap<>();
    public transient Map<String, AliasTargetMetadata> targetMetadata = new LinkedHashMap<>();
    public transient List<InvalidAliasTarget> invalidTargets = new ArrayList<>();

    public int itemCount() {
        return this.items.size();
    }

    public int aliasCount() {
        return this.items.values().stream()
                .filter(aliases -> aliases != null)
                .mapToInt(List::size)
                .sum();
    }

    public int normalizedAliasCount() {
        return this.normalizedAliasToItemId.size();
    }

    public void rebuildNormalizedAliasIndex(ParserRulesConfig rules) {
        LinkedHashMap<String, String> reverse = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : this.items.entrySet()) {
            this.targetMetadata.computeIfAbsent(entry.getKey(), AliasTargetMappings::resolve);
            if (entry.getValue() == null) {
                continue;
            }
            for (String alias : entry.getValue()) {
                String normalized = NormalizationUtils.normalizeForLookup(alias, rules);
                if (!normalized.isBlank()) {
                    reverse.putIfAbsent(normalized, entry.getKey());
                }
            }
        }
        this.normalizedAliasToItemId = reverse;
    }

    public ItemAliasConfig copy() {
        ItemAliasConfig copy = new ItemAliasConfig();
        for (Map.Entry<String, List<String>> entry : this.items.entrySet()) {
            copy.items.put(entry.getKey(), entry.getValue() == null ? List.of() : new ArrayList<>(entry.getValue()));
        }
        copy.normalizedAliasToItemId = new LinkedHashMap<>(this.normalizedAliasToItemId);
        copy.targetMetadata = new LinkedHashMap<>(this.targetMetadata);
        copy.invalidTargets = new ArrayList<>(this.invalidTargets);
        return copy;
    }

    public AliasTargetMetadata targetMetadata(String bucketId) {
        return this.targetMetadata.getOrDefault(bucketId, AliasTargetMappings.resolve(bucketId));
    }

    public static ItemAliasConfig defaults() {
        ItemAliasConfig config = new ItemAliasConfig();
        config.items.put("minecraft:amethyst_block", List.of("аметистовый блок", "аметистовые блоки", "amethyst block", "amethyst blocks"));
        config.items.put("minecraft:bone_block", List.of("костный блок", "блок костей", "блок кости", "bone block"));
        config.items.put("minecraft:cobblestone", List.of("булыжник", "cobblestone", "камень булыжный"));
        config.items.put("minecraft:enchanted_book", List.of("починка", "книга починки", "mending", "enchanted book"));
        config.items.put("minecraft:golden_carrot", List.of("золотая морковь", "golden carrot"));
        config.items.put("minecraft:quartz_block", List.of("кварцевый блок", "блок кварца", "quartz block"));
        config.items.put("minecraft:stone", List.of("камень", "stone"));
        return config;
    }
}
