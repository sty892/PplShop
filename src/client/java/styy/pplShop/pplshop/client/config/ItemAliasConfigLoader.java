package styy.pplShop.pplshop.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ItemAliasConfigLoader {
    private final ItemIdValidator itemIdValidator;

    public ItemAliasConfigLoader() {
        this(itemId -> itemId != null && Registries.ITEM.containsId(itemId) && Registries.ITEM.get(itemId) != Items.AIR);
    }

    ItemAliasConfigLoader(ItemIdValidator itemIdValidator) {
        this.itemIdValidator = itemIdValidator;
    }

    public LoadResult load(Path path, ParserRulesConfig rules, ItemAliasConfig defaults, boolean isBaseFile) throws IOException {
        if (!Files.exists(path)) {
            ItemAliasConfig missing = isBaseFile ? defaults.copy() : new ItemAliasConfig();
            missing.rebuildNormalizedAliasIndex(rules);
            return new LoadResult(missing, "", List.of(), false, false);
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            return this.load(path.getFileName().toString(), reader, rules, defaults, isBaseFile, true);
        } catch (Exception exception) {
            String summary = this.describeParseFailure(path.getFileName().toString(), exception);
            ItemAliasConfig fallback = isBaseFile ? defaults.copy() : new ItemAliasConfig();
            fallback.rebuildNormalizedAliasIndex(rules);
            return new LoadResult(fallback, summary, List.of(), true, false);
        }
    }

    public LoadResult load(String sourceName, Reader reader, ParserRulesConfig rules, ItemAliasConfig defaults, boolean isBaseFile) {
        return this.load(sourceName, reader, rules, defaults, isBaseFile, false);
    }

    private LoadResult load(
            String sourceName,
            Reader reader,
            ParserRulesConfig rules,
            ItemAliasConfig defaults,
            boolean isBaseFile,
            boolean loadedFromDisk
    ) {
        try {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                throw new JsonParseException(sourceName + " root must be a JSON object");
            }

            JsonObject rootObject = rootElement.getAsJsonObject();
            if (rootObject.has("items") && rootObject.get("items").isJsonObject()) {
                rootObject = rootObject.getAsJsonObject("items");
            }

            ItemAliasConfig loaded = new ItemAliasConfig();
            List<InvalidAliasTarget> invalidTargets = new ArrayList<>();
            for (var entry : rootObject.entrySet()) {
                EntryDefinition definition = this.readEntryDefinition(entry.getKey(), entry.getValue());
                AliasTargetMetadata metadata = AliasTargetMappings.resolve(
                        definition.bucketId(),
                        definition.runtimeItemId(),
                        definition.subtypeKey(),
                        definition.displayNameOverride()
                );
                if (metadata.runtimeItemId() == null) {
                    invalidTargets.add(new InvalidAliasTarget(sourceName, definition.bucketId(), definition.runtimeItemId(), "invalid identifier syntax"));
                    continue;
                }
                if (!this.itemIdValidator.exists(metadata.runtimeItemId())) {
                    invalidTargets.add(new InvalidAliasTarget(sourceName, definition.bucketId(), metadata.runtimeItemId().toString(), "item not present in registry"));
                    continue;
                }

                loaded.targetMetadata.put(metadata.bucketId(), metadata);
                loaded.items.merge(metadata.bucketId(), definition.aliases(), this::mergeAliasLists);
            }

            loaded.invalidTargets.addAll(invalidTargets);
            loaded.rebuildNormalizedAliasIndex(rules);
            return new LoadResult(loaded, "", invalidTargets, false, loadedFromDisk);
        } catch (Exception exception) {
            String summary = this.describeParseFailure(sourceName, exception);
            ItemAliasConfig fallback = isBaseFile ? defaults.copy() : new ItemAliasConfig();
            fallback.rebuildNormalizedAliasIndex(rules);
            return new LoadResult(fallback, summary, List.of(), true, false);
        }
    }

    private EntryDefinition readEntryDefinition(String bucketId, JsonElement element) {
        if (element.isJsonArray()) {
            return new EntryDefinition(bucketId, null, "", "", this.readAliases(bucketId, element.getAsJsonArray()));
        }
        if (!element.isJsonObject()) {
            throw new JsonParseException("Aliases for '" + bucketId + "' must be an array or object");
        }

        JsonObject object = element.getAsJsonObject();
        JsonElement aliasesElement = object.get("aliases");
        if (aliasesElement == null || !aliasesElement.isJsonArray()) {
            throw new JsonParseException("Aliases for '" + bucketId + "' must provide an aliases array");
        }

        String runtimeItemId = object.has("runtime_item_id") ? object.get("runtime_item_id").getAsString() : null;
        String subtypeKey = object.has("subtype") ? object.get("subtype").getAsString() : "";
        String displayNameOverride = object.has("display_name") ? object.get("display_name").getAsString() : "";
        return new EntryDefinition(bucketId, runtimeItemId, subtypeKey, displayNameOverride, this.readAliases(bucketId, aliasesElement.getAsJsonArray()));
    }

    private List<String> readAliases(String bucketId, JsonArray aliasArray) {
        List<String> aliases = new ArrayList<>(aliasArray.size());
        for (JsonElement aliasElement : aliasArray) {
            if (!aliasElement.isJsonPrimitive() || !aliasElement.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("Alias for '" + bucketId + "' must be a string");
            }
            aliases.add(aliasElement.getAsString());
        }
        return aliases;
    }

    private List<String> mergeAliasLists(List<String> existing, List<String> incoming) {
        ArrayList<String> merged = new ArrayList<>();
        if (existing != null) {
            merged.addAll(existing);
        }
        if (incoming != null) {
            for (String alias : incoming) {
                if (!merged.contains(alias)) {
                    merged.add(alias);
                }
            }
        }
        return merged;
    }

    private String describeParseFailure(String sourceName, Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return "Failed to parse " + sourceName + ": " + message;
    }

    public record LoadResult(
            ItemAliasConfig config,
            String parseErrorSummary,
            List<InvalidAliasTarget> invalidTargets,
            boolean parseFailed,
            boolean loadedFromDisk
    ) {
        public LoadResult {
            invalidTargets = invalidTargets == null ? List.of() : List.copyOf(invalidTargets);
        }
    }

    private record EntryDefinition(
            String bucketId,
            String runtimeItemId,
            String subtypeKey,
            String displayNameOverride,
            List<String> aliases
    ) {
    }

    interface ItemIdValidator {
        boolean exists(net.minecraft.util.Identifier itemId);
    }
}
