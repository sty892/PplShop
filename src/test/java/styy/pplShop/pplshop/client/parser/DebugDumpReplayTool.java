package styy.pplShop.pplshop.client.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import styy.pplShop.pplshop.client.config.AliasTargetMappings;
import styy.pplShop.pplshop.client.config.CurrencyAliasConfig;
import styy.pplShop.pplshop.client.config.ItemAliasConfig;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.model.ItemResolutionResultType;
import styy.pplShop.pplshop.client.model.ParsedItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class DebugDumpReplayTool {
    private DebugDumpReplayTool() {
    }

    public static void main(String[] args) throws IOException {
        Path dumpPath = args.length == 0
                ? Path.of("2026-04-15_14-03-03_415__03-sign-unresolved-entries.txt")
                : Path.of(args[0]);
        Summary summary = replay(dumpPath);
        System.out.println(summary);
    }

    static Summary replay(Path dumpPath) throws IOException {
        ParserRulesConfig rules = ParserRulesConfig.defaults();
        CurrencyAliasConfig currencyAliases = CurrencyAliasConfig.defaults();
        ItemAliasResolver resolver = new ItemAliasResolver(loadItemAliasConfig(rules), currencyAliases, rules);

        List<DumpEntry> entries = readEntries(dumpPath);
        int resolvedByAlias = 0;
        int resolvedByMixed = 0;
        int stillConfirmedCustom = 0;
        int stillUnknown = 0;
        for (DumpEntry entry : entries) {
            ParsedItem parsedItem = resolver.resolveItemLines(entry.itemLines().isEmpty() ? List.of(entry.chosenCandidate()) : entry.itemLines());
            if (parsedItem.resultType() == ItemResolutionResultType.MIXED_ITEM) {
                resolvedByMixed++;
            } else if (parsedItem.isResolved()) {
                resolvedByAlias++;
            } else if (parsedItem.resolutionTrace().fallbackReason().startsWith("confirmed-unresolvable:")) {
                stillConfirmedCustom++;
            } else {
                stillUnknown++;
            }
        }
        return new Summary(entries.size(), resolvedByAlias, resolvedByMixed, stillConfirmedCustom, stillUnknown);
    }

    private static List<DumpEntry> readEntries(Path dumpPath) throws IOException {
        String text = Files.readString(dumpPath);
        List<DumpEntry> entries = new ArrayList<>();
        for (String block : text.split("(?m)^=== SIGN \\d+ ===$")) {
            if (block.isBlank()) {
                continue;
            }
            String chosenCandidate = "";
            List<String> itemLines = List.of();
            for (String line : block.lines().toList()) {
                if (line.startsWith("ChosenCandidate: ")) {
                    chosenCandidate = line.substring("ChosenCandidate: ".length()).trim();
                } else if (line.startsWith("ItemLines: ")) {
                    String rawItemLines = line.substring("ItemLines: ".length()).trim();
                    itemLines = rawItemLines.isBlank()
                            ? List.of()
                            : List.of(rawItemLines.split("\\s*\\|\\|\\s*"));
                }
            }
            entries.add(new DumpEntry(chosenCandidate, itemLines));
        }
        return entries;
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

    private record DumpEntry(String chosenCandidate, List<String> itemLines) {
    }

    record Summary(int beforeUnresolvedCount, int resolvedByAlias, int resolvedByMixed, int stillConfirmedCustom, int stillUnknown) {
        int afterUnresolvedCount() {
            return this.stillConfirmedCustom + this.stillUnknown;
        }

        @Override
        public String toString() {
            return "DebugDumpReplaySummary{"
                    + "beforeUnresolvedCount=" + this.beforeUnresolvedCount
                    + ", afterUnresolvedCount=" + this.afterUnresolvedCount()
                    + ", resolvedByAlias=" + this.resolvedByAlias
                    + ", resolvedByMixed=" + this.resolvedByMixed
                    + ", stillConfirmedCustom=" + this.stillConfirmedCustom
                    + ", stillUnknown=" + this.stillUnknown
                    + '}';
        }
    }
}
