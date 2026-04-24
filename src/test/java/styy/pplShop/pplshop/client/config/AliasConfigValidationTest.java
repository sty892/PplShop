package styy.pplShop.pplshop.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.junit.jupiter.api.Test;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class AliasConfigValidationTest {
    private static final List<Path> ALIAS_FILES = List.of(
            Path.of("src/client/resources/assets/pplshop/default-config/item_aliases.json"),
            Path.of("src/client/resources/assets/pplshop/default-config/item_aliases_user.json")
    );
    private static final Set<String> ALLOWED_CROSS_TARGET_ALIASES = Set.of(
            "арбуз",
            "арбузы",
            "банеры флаги",
            "ботинкискорость души",
            "василек",
            "высохший гаст",
            "глазурованная керамика",
            "глина",
            "голд яблоки",
            "горшок",
            "жареная курица",
            "зловещая бутылочка",
            "зловещие зелья",
            "золотоизумруды",
            "искаъженный фф багровый нарост",
            "керамика",
            "коралл",
            "коралловый блок",
            "красители",
            "красный розовый оранж тюльпан",
            "листва разная",
            "медовые соты",
            "морские фонар призмарин блоки",
            "набор красителей",
            "незерит лом",
            "низкая высокая сухая трава",
            "низкая трава",
            "обломок незерит",
            "печеный картофель",
            "пластинки",
            "плотная грязь",
            "плотный лед",
            "ресурсы из энда норм чары",
            "серый и розовый краситель",
            "скалкавая штука",
            "сухие гасты",
            "сухие листья",
            "сухой бетон",
            "счастливый гаст",
            "топоры",
            "трава",
            "хрустики блоки снега",
            "цветущая азалия",
            "яйца голубые коричневые"
    );

    @Test
    void bundledAliasJsonDoesNotContainDuplicateObjectKeys() throws IOException {
        for (Path path : ALIAS_FILES) {
            assertNoDuplicateJsonKeys(path);
        }
    }

    @Test
    void bundledAliasTargetsResolveToSyntacticallyKnownRuntimeItems() throws IOException {
        ItemAliasConfigLoader loader = new ItemAliasConfigLoader(itemId -> itemId != null);
        for (Path path : ALIAS_FILES) {
            ItemAliasConfigLoader.LoadResult result = loader.load(path, ParserRulesConfig.defaults(), new ItemAliasConfig(), false);
            assertFalse(result.parseFailed(), () -> result.parseErrorSummary());
            assertTrue(result.invalidTargets().isEmpty(), () -> "Invalid alias targets: " + result.invalidTargets());
        }
    }

    @Test
    void crossTargetAliasDuplicatesAreExplicitlyWhitelisted() throws IOException {
        ParserRulesConfig rules = ParserRulesConfig.defaults();
        Map<String, Set<String>> targetsByAlias = new LinkedHashMap<>();
        for (Path path : ALIAS_FILES) {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                JsonArray aliasArray = entry.getValue().isJsonObject()
                        ? entry.getValue().getAsJsonObject().getAsJsonArray("aliases")
                        : entry.getValue().getAsJsonArray();
                for (JsonElement aliasElement : aliasArray) {
                    String normalized = NormalizationUtils.normalizeForLookup(aliasElement.getAsString(), rules);
                    if (!normalized.isBlank()) {
                        targetsByAlias.computeIfAbsent(normalized, ignored -> new LinkedHashSet<>()).add(entry.getKey());
                    }
                }
            }
        }

        List<String> unexpectedDuplicates = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : targetsByAlias.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }
            if (sameRuntimeTarget(entry.getValue()) || ALLOWED_CROSS_TARGET_ALIASES.contains(entry.getKey())) {
                continue;
            }
            unexpectedDuplicates.add(entry.getKey() + " -> " + entry.getValue());
        }
        assertTrue(unexpectedDuplicates.isEmpty(), () -> "Unexpected cross-target alias duplicates: " + unexpectedDuplicates);
    }

    private static boolean sameRuntimeTarget(Set<String> bucketIds) {
        Set<String> runtimeTargets = new LinkedHashSet<>();
        for (String bucketId : bucketIds) {
            AliasTargetMetadata metadata = AliasTargetMappings.resolve(bucketId);
            runtimeTargets.add(metadata.runtimeItemId() == null ? "" : metadata.runtimeItemId().toString());
        }
        return runtimeTargets.size() == 1;
    }

    private static void assertNoDuplicateJsonKeys(Path path) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(Files.readString(path)))) {
            readAny(reader, path.toString());
        }
    }

    private static void readAny(JsonReader reader, String path) throws IOException {
        switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                Set<String> names = new LinkedHashSet<>();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (!names.add(name)) {
                        fail("Duplicate JSON key in " + path + ": " + name);
                    }
                    readAny(reader, path + "." + name);
                }
                reader.endObject();
            }
            case BEGIN_ARRAY -> {
                reader.beginArray();
                while (reader.hasNext()) {
                    readAny(reader, path + "[]");
                }
                reader.endArray();
            }
            default -> reader.skipValue();
        }
    }
}
