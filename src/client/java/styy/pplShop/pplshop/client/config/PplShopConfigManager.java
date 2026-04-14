package styy.pplShop.pplshop.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class PplShopConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("PPLShop");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String ITEM_ALIASES_FILE = "item_aliases.json";
    private static final String ITEM_ALIASES_USER_FILE = "item_aliases_user.json";
    private static final String CURRENCY_ALIASES_FILE = "currency_aliases.json";
    private static final String PARSER_RULES_FILE = "parser_rules.json";
    private static final String UX_SETTINGS_FILE = "ux_settings.json";

    private final Path configDir = FabricLoader.getInstance().getConfigDir().resolve("pplshop");
    private final Map<Path, FileTime> knownModificationTimes = new HashMap<>();
    private final ItemAliasConfigLoader itemAliasConfigLoader = new ItemAliasConfigLoader();

    private ItemAliasConfig itemAliasConfig = ItemAliasConfig.defaults();
    private CurrencyAliasConfig currencyAliasConfig = CurrencyAliasConfig.defaults();
    private ParserRulesConfig parserRulesConfig = ParserRulesConfig.defaults();
    private RefreshUxConfig refreshUxConfig = RefreshUxConfig.defaults();

    public void loadAll() {
        try {
            Files.createDirectories(this.configDir);
            this.ensureDefaultItemAliases();
            this.ensureUserItemAliasesTemplate();
            this.ensureDefault(CURRENCY_ALIASES_FILE, CurrencyAliasConfig.defaults());
            this.ensureDefault(PARSER_RULES_FILE, ParserRulesConfig.defaults());
            this.ensureDefault(UX_SETTINGS_FILE, RefreshUxConfig.defaults());
            this.reloadAllInternal();
        } catch (IOException exception) {
            LOGGER.error("Failed to initialize config directory {}", this.configDir, exception);
        }
    }

    public boolean reloadIfChanged() {
        boolean changed = false;
        try {
            for (Path path : this.configPaths()) {
                FileTime current = Files.exists(path) ? Files.getLastModifiedTime(path) : FileTime.fromMillis(0L);
                FileTime known = this.knownModificationTimes.get(path);
                if (known == null || current.compareTo(known) > 0) {
                    changed = true;
                    break;
                }
            }

            if (changed) {
                this.reloadAllInternal();
                LOGGER.info("Reloaded PPLShop configs from {}", this.configDir);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to reload configs", exception);
        }
        return changed;
    }

    public Path configDir() {
        return this.configDir;
    }

    public ItemAliasConfig itemAliasConfig() {
        return this.itemAliasConfig;
    }

    public List<InvalidAliasTarget> invalidItemAliasTargets() {
        return List.copyOf(this.itemAliasConfig.invalidTargets);
    }

    public CurrencyAliasConfig currencyAliasConfig() {
        return this.currencyAliasConfig;
    }

    public ParserRulesConfig parserRulesConfig() {
        return this.parserRulesConfig;
    }

    public RefreshUxConfig refreshUxConfig() {
        return this.refreshUxConfig;
    }

    public void saveRefreshUxConfig(RefreshUxConfig nextConfig) {
        RefreshUxConfig sanitized = nextConfig == null ? RefreshUxConfig.defaults() : nextConfig;
        sanitized.sanitize();
        Path path = this.configDir.resolve(UX_SETTINGS_FILE);
        try {
            Files.createDirectories(this.configDir);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(sanitized, writer);
            }
            this.refreshUxConfig = sanitized;
            this.knownModificationTimes.put(path, Files.getLastModifiedTime(path));
        } catch (IOException exception) {
            LOGGER.error("Failed to save PPLShop UX config {}", path, exception);
        }
    }

    private Path[] configPaths() {
        return new Path[]{
                this.configDir.resolve(ITEM_ALIASES_FILE),
                this.configDir.resolve(ITEM_ALIASES_USER_FILE),
                this.configDir.resolve(CURRENCY_ALIASES_FILE),
                this.configDir.resolve(PARSER_RULES_FILE),
                this.configDir.resolve(UX_SETTINGS_FILE)
        };
    }

    private void reloadAllInternal() throws IOException {
        Path itemAliasesPath = this.configDir.resolve(ITEM_ALIASES_FILE);
        Path userItemAliasesPath = this.configDir.resolve(ITEM_ALIASES_USER_FILE);
        this.currencyAliasConfig = this.readOrDefault(this.configDir.resolve(CURRENCY_ALIASES_FILE), CurrencyAliasConfig.class, CurrencyAliasConfig.defaults());
        this.parserRulesConfig = this.readOrDefault(this.configDir.resolve(PARSER_RULES_FILE), ParserRulesConfig.class, ParserRulesConfig.defaults());
        this.refreshUxConfig = this.readOrDefault(this.configDir.resolve(UX_SETTINGS_FILE), RefreshUxConfig.class, RefreshUxConfig.defaults());
        this.refreshUxConfig.sanitize();
        this.itemAliasConfig = this.readMergedItemAliasConfig(
                itemAliasesPath,
                userItemAliasesPath,
                this.parserRulesConfig,
                ItemAliasConfig.defaults()
        );
        this.logItemAliasDiagnostics(itemAliasesPath, userItemAliasesPath, this.itemAliasConfig, this.parserRulesConfig);

        for (Path path : this.configPaths()) {
            this.knownModificationTimes.put(path, Files.exists(path) ? Files.getLastModifiedTime(path) : FileTime.fromMillis(0L));
        }
    }

    private void ensureDefaultItemAliases() throws IOException {
        Path path = this.configDir.resolve(ITEM_ALIASES_FILE);
        if (Files.exists(path)) {
            LOGGER.info("PPLShop base item aliases file found: {}", path);
            return;
        }

        LOGGER.warn("PPLShop base item aliases file not found: {}", path);
        String resourcePath = "/assets/pplshop/default-config/" + ITEM_ALIASES_FILE;
        try (InputStream stream = PplShopConfigManager.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                Files.copy(stream, path);
                LOGGER.info("Wrote default base item aliases file to {}", path);
                return;
            }
        }

        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(ItemAliasConfig.defaults().items, writer);
        }
        LOGGER.info("Wrote generated default base item aliases file to {}", path);
    }

    private void ensureUserItemAliasesTemplate() throws IOException {
        Path path = this.configDir.resolve(ITEM_ALIASES_USER_FILE);
        if (Files.exists(path)) {
            LOGGER.info("PPLShop user item aliases file found: {}", path);
            return;
        }

        LOGGER.warn("PPLShop user item aliases file not found: {}", path);
        try (Writer writer = Files.newBufferedWriter(path)) {
            writer.write("{\n}\n");
        }
        LOGGER.info("Wrote empty user item aliases template to {}", path);
    }

    private void ensureDefault(String fileName, Object defaults) throws IOException {
        Path path = this.configDir.resolve(fileName);
        if (Files.exists(path)) {
            return;
        }

        String resourcePath = "/assets/pplshop/default-config/" + fileName;
        try (InputStream stream = PplShopConfigManager.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                Files.copy(stream, path);
                return;
            }
        }

        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(defaults, writer);
        }
    }

    private ItemAliasConfig readMergedItemAliasConfig(Path basePath, Path userPath, ParserRulesConfig rules, ItemAliasConfig defaults) throws IOException {
        ItemAliasConfig bundledConfig = this.readBundledItemAliasConfig(rules, defaults);
        ItemAliasConfig diskConfig = this.readSingleItemAliasConfig(basePath, new ItemAliasConfig(), false);
        ItemAliasConfig userConfig = this.readSingleItemAliasConfig(userPath, new ItemAliasConfig(), false);

        MergeStats mergeStats = new MergeStats();
        ItemAliasConfig merged = this.mergeItemAliases(bundledConfig, diskConfig, rules, mergeStats);
        merged = this.mergeItemAliases(merged, userConfig, rules, mergeStats);

        LOGGER.info("PPLShop item aliases loaded: bundled item ids={}, disk override item ids={}, user override item ids={}, total merged item ids={}, total aliases after merge={}, normalized aliases={}, duplicate aliases dropped={}",
                bundledConfig.itemCount(),
                diskConfig.itemCount(),
                userConfig.itemCount(),
                merged.itemCount(),
                merged.aliasCount(),
                merged.normalizedAliasCount(),
                mergeStats.duplicatesDropped()
        );
        if (!merged.invalidTargets.isEmpty()) {
            String invalidSummary = merged.invalidTargets.stream()
                    .map(target -> target.bucketId() + " -> " + target.runtimeItemId())
                    .distinct()
                    .sorted()
                    .toList()
                    .toString();
            LOGGER.warn("Skipped {} invalid alias target entries while loading PPLShop item aliases: {}", merged.invalidTargets.size(), invalidSummary);
        }
        return merged;
    }

    private ItemAliasConfig readBundledItemAliasConfig(ParserRulesConfig rules, ItemAliasConfig defaults) throws IOException {
        String resourcePath = "/assets/pplshop/default-config/" + ITEM_ALIASES_FILE;
        try (InputStream stream = PplShopConfigManager.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOGGER.warn("PPLShop bundled item aliases resource not found at load time: {}", resourcePath);
                ItemAliasConfig fallback = defaults.copy();
                fallback.rebuildNormalizedAliasIndex(rules);
                return fallback;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                ItemAliasConfigLoader.LoadResult result = this.itemAliasConfigLoader.load("bundled:" + ITEM_ALIASES_FILE, reader, rules, defaults, true);
                if (result.parseFailed()) {
                    LOGGER.error(result.parseErrorSummary());
                    LOGGER.warn("Using default bundled item aliases: {} item ids, {} total aliases", result.config().itemCount(), result.config().aliasCount());
                    return result.config();
                }
                if (!result.invalidTargets().isEmpty()) {
                    LOGGER.warn("Validated {} invalid alias target entries in bundled {}", result.invalidTargets().size(), ITEM_ALIASES_FILE);
                }
                LOGGER.info("Loaded bundled item aliases: {} item ids, {} total aliases", result.config().itemCount(), result.config().aliasCount());
                return result.config();
            }
        }
    }

    private void logItemAliasDiagnostics(Path basePath, Path userPath, ItemAliasConfig config, ParserRulesConfig rules) {
        String resourcePath = "/assets/pplshop/default-config/" + ITEM_ALIASES_FILE;
        LOGGER.info("PPLShop alias diagnostics: bundled resource={}, base override path={}, user override path={}",
                String.valueOf(PplShopConfigManager.class.getResource(resourcePath)),
                basePath.toAbsolutePath(),
                userPath.toAbsolutePath()
        );
        LOGGER.info("PPLShop alias diagnostics: merged item ids={}, merged aliases={}, merged normalized aliases={}",
                config.itemCount(),
                config.aliasCount(),
                config.normalizedAliasCount()
        );
        LOGGER.info("PPLShop alias diagnostics: first 5 normalized keys={}",
                config.normalizedAliasToItemId.keySet().stream().limit(5).toList()
        );
        this.logAliasProbe("воронка", config, rules);
        this.logAliasProbe("кораллы блоки", config, rules);
        this.logAliasProbe("вазы", config, rules);
    }

    private void logAliasProbe(String alias, ItemAliasConfig config, ParserRulesConfig rules) {
        String normalized = NormalizationUtils.normalizeWithoutSorting(alias, rules);
        String resolvedBucket = config.normalizedAliasToItemId.get(normalized);
        LOGGER.info("PPLShop alias diagnostics: probe='{}' normalized='{}' -> {}", alias, normalized, resolvedBucket == null ? "<missing>" : resolvedBucket);
    }

    private ItemAliasConfig readSingleItemAliasConfig(Path path, ItemAliasConfig defaults, boolean isBaseFile) throws IOException {
        if (!Files.exists(path)) {
            if (isBaseFile) {
                LOGGER.warn("PPLShop base item aliases file not found at load time: {}", path);
                return defaults.copy();
            }

            LOGGER.warn("PPLShop user item aliases file not found at load time: {}", path);
            ItemAliasConfig empty = new ItemAliasConfig();
            empty.rebuildNormalizedAliasIndex(this.parserRulesConfig);
            return empty;
        }

        LOGGER.info("PPLShop item aliases file found: {}", path);
        ItemAliasConfigLoader.LoadResult result = this.itemAliasConfigLoader.load(path, this.parserRulesConfig, defaults, isBaseFile);
        if (result.parseFailed()) {
            LOGGER.error(result.parseErrorSummary());
            if (isBaseFile) {
                LOGGER.warn("Using default base item aliases: {} item ids, {} total aliases", result.config().itemCount(), result.config().aliasCount());
            } else {
                LOGGER.warn("Using empty user item aliases for {}", path);
            }
            return result.config();
        }

        if (!result.invalidTargets().isEmpty()) {
            LOGGER.warn("Validated {} invalid alias target entries in {}", result.invalidTargets().size(), path.getFileName());
        }
        LOGGER.info("Loaded item aliases from {}: {} item ids, {} total aliases", path, result.config().itemCount(), result.config().aliasCount());
        return result.config();
    }

    private List<String> mergeAliasLists(List<String> existing, List<String> incoming) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (existing != null) {
            merged.addAll(existing);
        }
        if (incoming != null) {
            merged.addAll(incoming);
        }
        return new ArrayList<>(merged);
    }
    private ItemAliasConfig mergeItemAliases(ItemAliasConfig baseConfig, ItemAliasConfig userConfig, ParserRulesConfig rules, MergeStats mergeStats) {
        LinkedHashMap<String, LinkedHashMap<String, String>> aliasesByItemId = new LinkedHashMap<>();
        LinkedHashMap<String, String> reverseAliasMap = new LinkedHashMap<>();

        this.mergeAliasSource(baseConfig.items, aliasesByItemId, reverseAliasMap, rules, mergeStats, false);
        this.mergeAliasSource(userConfig.items, aliasesByItemId, reverseAliasMap, rules, mergeStats, true);

        ItemAliasConfig merged = new ItemAliasConfig();
        for (Map.Entry<String, LinkedHashMap<String, String>> entry : aliasesByItemId.entrySet()) {
            merged.items.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }
        merged.normalizedAliasToItemId = reverseAliasMap;
        merged.targetMetadata.putAll(baseConfig.targetMetadata);
        merged.targetMetadata.putAll(userConfig.targetMetadata);
        merged.invalidTargets.addAll(baseConfig.invalidTargets);
        merged.invalidTargets.addAll(userConfig.invalidTargets);
        return merged;
    }

    private void mergeAliasSource(
            Map<String, List<String>> source,
            Map<String, LinkedHashMap<String, String>> aliasesByItemId,
            Map<String, String> reverseAliasMap,
            ParserRulesConfig rules,
            MergeStats mergeStats,
            boolean allowOverride
    ) {
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            LinkedHashMap<String, String> itemAliases = aliasesByItemId.computeIfAbsent(entry.getKey(), ignored -> new LinkedHashMap<>());
            if (entry.getValue() == null) {
                continue;
            }

            for (String alias : entry.getValue()) {
                if (alias == null || alias.isBlank()) {
                    mergeStats.incrementDuplicatesDropped();
                    continue;
                }

                String normalizedAlias = NormalizationUtils.normalizeForLookup(alias, rules);
                if (normalizedAlias.isBlank()) {
                    mergeStats.incrementDuplicatesDropped();
                    continue;
                }

                String existingItemId = reverseAliasMap.get(normalizedAlias);
                if (existingItemId != null) {
                    if (existingItemId.equals(entry.getKey())) {
                        mergeStats.incrementDuplicatesDropped();
                        continue;
                    }
                    if (!allowOverride) {
                        LOGGER.warn("PPLShop duplicate normalized item alias '{}' for {} and {}. Keeping first mapping.", normalizedAlias, existingItemId, entry.getKey());
                        mergeStats.incrementDuplicatesDropped();
                        continue;
                    }

                    LinkedHashMap<String, String> previousAliases = aliasesByItemId.get(existingItemId);
                    if (previousAliases != null) {
                        previousAliases.remove(normalizedAlias);
                    }
                }

                if (itemAliases.containsKey(normalizedAlias)) {
                    mergeStats.incrementDuplicatesDropped();
                    continue;
                }

                itemAliases.put(normalizedAlias, alias);
                reverseAliasMap.put(normalizedAlias, entry.getKey());
            }
        }
    }

    private <T> T readOrDefault(Path path, Class<T> type, T defaults) throws IOException {
        if (!Files.exists(path)) {
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(defaults, writer);
            }
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            T loaded = GSON.fromJson(reader, type);
            return loaded != null ? loaded : defaults;
        } catch (Exception exception) {
            LOGGER.error("Failed to parse config {}, using defaults", path, exception);
            return defaults;
        }
    }

    private static final class MergeStats {
        private int duplicatesDropped;

        int duplicatesDropped() {
            return this.duplicatesDropped;
        }

        void incrementDuplicatesDropped() {
            this.duplicatesDropped++;
        }
    }
}

