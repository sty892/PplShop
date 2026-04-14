package styy.pplShop.pplshop.client.config;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemAliasConfigLoaderTest {
    private final ItemAliasConfigLoader loader = new ItemAliasConfigLoader(itemId ->
            itemId != null && !"minecraft:not_a_real_item".equals(itemId.toString())
    );

    @Test
    void brokenUserAliasJsonFallsBackToEmptyConfig(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("item_aliases_user.json");
        Files.writeString(path, "{\n  \"minecraft:stone\": [\"stone\"\n");

        ItemAliasConfigLoader.LoadResult result = this.loader.load(path, ParserRulesConfig.defaults(), new ItemAliasConfig(), false);

        assertTrue(result.parseFailed());
        assertTrue(result.config().items.isEmpty());
        assertTrue(result.parseErrorSummary().contains("item_aliases_user.json"));
    }

    @Test
    void invalidRegistryItemIdsAreReportedAndDropped(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("item_aliases.json");
        Files.writeString(path, "{\n  \"minecraft:not_a_real_item\": [\"broken alias\"]\n}\n");

        ItemAliasConfigLoader.LoadResult result = this.loader.load(path, ParserRulesConfig.defaults(), new ItemAliasConfig(), false);

        assertFalse(result.parseFailed());
        assertEquals(1, result.invalidTargets().size());
        assertTrue(result.config().items.isEmpty());
        assertEquals("minecraft:not_a_real_item", result.invalidTargets().getFirst().bucketId());
    }

    @Test
    void legacyPotionBucketsUseValidRuntimePotionIds() {
        List<AliasTargetMetadata> mappings = List.of(
                AliasTargetMappings.resolve("minecraft:fire_resistance_potion"),
                AliasTargetMappings.resolve("minecraft:swiftness_potion"),
                AliasTargetMappings.resolve("minecraft:night_vision_potion"),
                AliasTargetMappings.resolve("minecraft:invisibility_potion"),
                AliasTargetMappings.resolve("minecraft:regeneration_potion")
        );

        for (AliasTargetMetadata metadata : mappings) {
            assertEquals(Identifier.of("minecraft", "potion"), metadata.runtimeItemId());
            assertFalse(metadata.subtypeKey().isBlank());
        }
    }
}
