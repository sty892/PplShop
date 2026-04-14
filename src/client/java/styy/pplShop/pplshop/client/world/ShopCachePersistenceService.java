package styy.pplShop.pplshop.client.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import styy.pplShop.pplshop.client.model.ItemResolutionTrace;
import styy.pplShop.pplshop.client.model.ItemResolutionResultType;
import styy.pplShop.pplshop.client.model.ParseStatus;
import styy.pplShop.pplshop.client.model.ParsedItem;
import styy.pplShop.pplshop.client.model.ParsedPrice;
import styy.pplShop.pplshop.client.model.ShopSignClassificationType;
import styy.pplShop.pplshop.client.model.ShopSignDiagnosticReason;
import styy.pplShop.pplshop.client.model.ShopSignDiagnostics;
import styy.pplShop.pplshop.client.model.ShopSignEntry;
import styy.pplShop.pplshop.client.model.SignContainerRelation;
import styy.pplShop.pplshop.client.model.SignSide;
import styy.pplShop.pplshop.client.model.SignTextSnapshot;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopCachePersistenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger("PPLShop");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int SNAPSHOT_SCHEMA_VERSION = 1;
    private static final String CACHE_DIR = "pplshop-cache";

    private final Path rootDir;

    public ShopCachePersistenceService() {
        this(FabricLoader.getInstance().getGameDir().resolve(CACHE_DIR));
    }

    ShopCachePersistenceService(Path rootDir) {
        this.rootDir = rootDir;
    }

    public PersistedSnapshot load(WorldCacheIdentity identity) {
        if (identity == null) {
            return PersistedSnapshot.empty();
        }

        Path path = this.snapshotPath(identity);
        if (!Files.exists(path)) {
            return PersistedSnapshot.empty();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            SnapshotFile snapshotFile = GSON.fromJson(reader, SnapshotFile.class);
            if (snapshotFile == null || snapshotFile.metadata == null) {
                return PersistedSnapshot.empty();
            }
            List<ShopSignEntry> entries = new ArrayList<>();
            if (snapshotFile.entries != null) {
                for (SnapshotEntry entry : snapshotFile.entries) {
                    ShopSignEntry restored = restoreEntry(entry);
                    if (restored != null) {
                        entries.add(restored);
                    }
                }
            }
            return new PersistedSnapshot(snapshotFile.metadata, List.copyOf(entries), true);
        } catch (JsonParseException | IllegalStateException exception) {
            LOGGER.error("Failed to parse persisted PPLShop cache snapshot {}", path, exception);
            return PersistedSnapshot.empty();
        } catch (IOException exception) {
            LOGGER.error("Failed to load persisted PPLShop cache snapshot {}", path, exception);
            return PersistedSnapshot.empty();
        }
    }

    public void save(WorldCacheIdentity identity, ShopSnapshotMetadata metadata, List<ShopSignEntry> entries) {
        if (identity == null || metadata == null) {
            return;
        }

        Path path = this.snapshotPath(identity);
        try {
            Files.createDirectories(path.getParent());
            SnapshotFile snapshotFile = new SnapshotFile();
            snapshotFile.metadata = metadata;
            snapshotFile.entries = (entries == null ? List.<ShopSignEntry>of() : entries).stream()
                    .map(ShopCachePersistenceService::snapshotEntry)
                    .toList();
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(snapshotFile, writer);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to save persisted PPLShop cache snapshot {}", path, exception);
        }
    }

    public ShopSnapshotMetadata buildMetadata(WorldCacheIdentity identity, long lastRefreshTimeMillis, int entryCount, int lastKnownChunkCount, int lastKnownSignCount) {
        if (identity == null) {
            return new ShopSnapshotMetadata(SNAPSHOT_SCHEMA_VERSION, "", "", "", "", lastRefreshTimeMillis, entryCount, lastKnownChunkCount, lastKnownSignCount);
        }
        return new ShopSnapshotMetadata(
                SNAPSHOT_SCHEMA_VERSION,
                identity.worldKey(),
                identity.displayName(),
                identity.sessionFingerprint(),
                identity.dimensionId().toString(),
                lastRefreshTimeMillis,
                entryCount,
                lastKnownChunkCount,
                lastKnownSignCount
        );
    }

    private Path snapshotPath(WorldCacheIdentity identity) {
        return this.rootDir.resolve(sanitize(identity.worldKey())).resolve(sanitize(identity.dimensionId().toString()) + ".json");
    }

    private static SnapshotEntry snapshotEntry(ShopSignEntry entry) {
        SnapshotEntry snapshot = new SnapshotEntry();
        snapshot.dimensionId = stringValue(entry.dimensionId());
        snapshot.x = entry.pos().getX();
        snapshot.y = entry.pos().getY();
        snapshot.z = entry.pos().getZ();
        snapshot.lines = List.copyOf(entry.snapshot().lines());
        snapshot.hanging = entry.snapshot().hanging();
        snapshot.side = entry.snapshot().side().name();
        snapshot.rawCombinedText = entry.rawCombinedText();
        snapshot.scanTick = entry.scanTick();

        snapshot.parsedItemRaw = entry.parsedItem().rawText();
        snapshot.parsedItemNormalized = entry.parsedItem().normalizedText();
        snapshot.parsedItemId = stringValue(entry.parsedItem().itemId());
        snapshot.parsedItemAlias = entry.parsedItem().matchedAlias();
        snapshot.parsedItemConfidence = entry.parsedItem().parseConfidence();
        snapshot.parsedItemStatus = entry.parsedItem().parseStatus().name();
        snapshot.parsedItemResultType = entry.parsedItem().resultType().name();
        snapshot.parsedItemSafeResult = entry.parsedItem().safeResult();
        snapshot.traceSelectedCandidate = entry.parsedItem().resolutionTrace().selectedCandidate();
        snapshot.traceSelectedCandidateSource = entry.parsedItem().resolutionTrace().selectedCandidateSource();
        snapshot.traceSelectedResolver = entry.parsedItem().resolutionTrace().selectedResolver();
        snapshot.traceSelectedItemId = entry.parsedItem().resolutionTrace().selectedItemId();
        snapshot.traceMatchedAlias = entry.parsedItem().resolutionTrace().matchedAlias();
        snapshot.traceFallbackReason = entry.parsedItem().resolutionTrace().fallbackReason();
        snapshot.traceSuggestedAliases = List.copyOf(entry.parsedItem().resolutionTrace().suggestedAliases());
        snapshot.traceConsideredCandidates = List.copyOf(entry.parsedItem().resolutionTrace().consideredCandidates());
        snapshot.traceRejectedCandidates = List.copyOf(entry.parsedItem().resolutionTrace().rejectedCandidates());
        snapshot.resolvedBucketId = entry.parsedItem().resolvedBucketId();
        snapshot.resolvedSubtypeKey = entry.parsedItem().resolvedSubtypeKey();
        snapshot.displayNameOverride = entry.parsedItem().displayNameOverride();

        snapshot.parsedPriceRaw = entry.parsedPrice().rawText();
        snapshot.parsedPriceAmount = entry.parsedPrice().amount();
        snapshot.parsedPriceCurrencyKey = entry.parsedPrice().currencyKey();
        snapshot.parsedPriceCurrencyItemId = stringValue(entry.parsedPrice().currencyItemId());
        snapshot.parsedPriceConfidence = entry.parsedPrice().parseConfidence();
        snapshot.parsedPriceStatus = entry.parsedPrice().parseStatus().name();
        snapshot.parsedPriceNormalizedDisplay = entry.parsedPrice().normalizedDisplayText();
        snapshot.parsedPriceAlias = entry.parsedPrice().matchedAlias();
        snapshot.parsedPriceQuantityAmount = entry.parsedPrice().quantityAmount();
        snapshot.parsedPriceQuantityUnitKey = entry.parsedPrice().quantityUnitKey();
        snapshot.parsedPriceQuantityItemCount = entry.parsedPrice().quantityItemCount();

        snapshot.classification = entry.diagnostics().classification().name();
        snapshot.primaryReason = entry.diagnostics().primaryReason().name();
        snapshot.cacheReason = entry.diagnostics().cacheReason().name();
        snapshot.ownerLineIndex = entry.diagnostics().ownerLineIndex();
        snapshot.priceLineIndex = entry.diagnostics().priceLineIndex();
        snapshot.itemLines = List.copyOf(entry.diagnostics().itemLines());
        snapshot.linkedContainerType = entry.diagnostics().linkedContainerType();
        snapshot.linkedContainerRelation = entry.diagnostics().linkedContainerRelation();
        snapshot.fingerprintSummary = entry.diagnostics().fingerprintSummary();
        snapshot.containerBlockId = stringValue(entry.relation().containerBlockId());
        snapshot.containerX = entry.relation().containerPos() == null ? 0 : entry.relation().containerPos().getX();
        snapshot.containerY = entry.relation().containerPos() == null ? 0 : entry.relation().containerPos().getY();
        snapshot.containerZ = entry.relation().containerPos() == null ? 0 : entry.relation().containerPos().getZ();
        snapshot.containerLinked = entry.relation().linked();
        snapshot.containerRelationKind = entry.relation().relationKind();
        return snapshot;
    }

    private static ShopSignEntry restoreEntry(SnapshotEntry entry) {
        if (entry == null || entry.dimensionId == null || entry.side == null) {
            return null;
        }

        Identifier dimensionId = Identifier.tryParse(entry.dimensionId);
        if (dimensionId == null) {
            return null;
        }

        ParsedItem parsedItem = new ParsedItem(
                safe(entry.parsedItemRaw),
                safe(entry.parsedItemNormalized),
                parseIdentifier(entry.parsedItemId),
                blankToNull(entry.parsedItemAlias),
                entry.parsedItemConfidence,
                parseEnum(entry.parsedItemStatus, ParseStatus.UNKNOWN, ParseStatus.class),
                new ItemResolutionTrace(
                        safe(entry.traceSelectedCandidate),
                        safe(entry.traceSelectedCandidateSource),
                        safe(entry.traceSelectedResolver),
                        safe(entry.traceSelectedItemId),
                        safe(entry.traceMatchedAlias),
                        safe(entry.traceFallbackReason),
                        copyOf(entry.traceSuggestedAliases),
                        copyOf(entry.traceConsideredCandidates),
                        copyOf(entry.traceRejectedCandidates)
                ),
                parseEnum(entry.parsedItemResultType, ItemResolutionResultType.UNKNOWN, ItemResolutionResultType.class),
                entry.parsedItemSafeResult,
                safe(entry.resolvedBucketId),
                safe(entry.resolvedSubtypeKey),
                safe(entry.displayNameOverride)
        );
        ParsedPrice parsedPrice = new ParsedPrice(
                safe(entry.parsedPriceRaw),
                entry.parsedPriceAmount,
                blankToNull(entry.parsedPriceCurrencyKey),
                parseIdentifier(entry.parsedPriceCurrencyItemId),
                entry.parsedPriceConfidence,
                parseEnum(entry.parsedPriceStatus, ParseStatus.UNKNOWN, ParseStatus.class),
                safe(entry.parsedPriceNormalizedDisplay),
                safe(entry.parsedPriceAlias),
                entry.parsedPriceQuantityAmount,
                safe(entry.parsedPriceQuantityUnitKey),
                entry.parsedPriceQuantityItemCount
        );
        ShopSignDiagnostics diagnostics = new ShopSignDiagnostics(
                parseEnum(entry.classification, ShopSignClassificationType.NOT_SHOP, ShopSignClassificationType.class),
                parseEnum(entry.primaryReason, ShopSignDiagnosticReason.NONE, ShopSignDiagnosticReason.class),
                parseEnum(entry.cacheReason, ShopSignDiagnosticReason.NONE, ShopSignDiagnosticReason.class),
                entry.ownerLineIndex,
                entry.priceLineIndex,
                copyOf(entry.itemLines),
                safe(entry.linkedContainerType),
                safe(entry.linkedContainerRelation),
                safe(entry.fingerprintSummary)
        );
        SignContainerRelation relation = entry.containerLinked
                ? new SignContainerRelation(
                true,
                parseIdentifier(entry.containerBlockId),
                new BlockPos(entry.containerX, entry.containerY, entry.containerZ),
                safe(entry.containerRelationKind)
        )
                : SignContainerRelation.missing();
        return new ShopSignEntry(
                dimensionId,
                new BlockPos(entry.x, entry.y, entry.z),
                new SignTextSnapshot(copyOf(entry.lines), entry.hanging, parseEnum(entry.side, SignSide.FRONT, SignSide.class)),
                parsedItem,
                parsedPrice,
                safe(entry.rawCombinedText),
                entry.scanTick,
                diagnostics,
                relation
        );
    }

    private static <T extends Enum<T>> T parseEnum(String rawValue, T fallback, Class<T> enumType) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, rawValue);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static Identifier parseIdentifier(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return Identifier.tryParse(rawValue);
    }

    private static String stringValue(Identifier identifier) {
        return identifier == null ? "" : identifier.toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static List<String> copyOf(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('\\', '_')
                .replace('/', '_')
                .replace(':', '_')
                .replace('|', '_')
                .replace(' ', '_');
    }

    public record PersistedSnapshot(ShopSnapshotMetadata metadata, List<ShopSignEntry> entries, boolean loadedFromDisk) {
        public PersistedSnapshot {
            metadata = metadata == null ? new ShopSnapshotMetadata(SNAPSHOT_SCHEMA_VERSION, "", "", "", "", 0L, 0, 0, 0) : metadata;
            entries = entries == null ? List.of() : List.copyOf(entries);
        }

        public static PersistedSnapshot empty() {
            return new PersistedSnapshot(new ShopSnapshotMetadata(SNAPSHOT_SCHEMA_VERSION, "", "", "", "", 0L, 0, 0, 0), List.of(), false);
        }
    }

    private static final class SnapshotFile {
        private ShopSnapshotMetadata metadata;
        private List<SnapshotEntry> entries = List.of();
    }

    private static final class SnapshotEntry {
        private String dimensionId;
        private int x;
        private int y;
        private int z;
        private List<String> lines = List.of();
        private boolean hanging;
        private String side;
        private String rawCombinedText;
        private long scanTick;
        private String parsedItemRaw;
        private String parsedItemNormalized;
        private String parsedItemId;
        private String parsedItemAlias;
        private int parsedItemConfidence;
        private String parsedItemStatus;
        private String parsedItemResultType;
        private boolean parsedItemSafeResult;
        private String traceSelectedCandidate;
        private String traceSelectedCandidateSource;
        private String traceSelectedResolver;
        private String traceSelectedItemId;
        private String traceMatchedAlias;
        private String traceFallbackReason;
        private List<String> traceSuggestedAliases = List.of();
        private List<String> traceConsideredCandidates = List.of();
        private List<String> traceRejectedCandidates = List.of();
        private String resolvedBucketId;
        private String resolvedSubtypeKey;
        private String displayNameOverride;
        private String parsedPriceRaw;
        private Integer parsedPriceAmount;
        private String parsedPriceCurrencyKey;
        private String parsedPriceCurrencyItemId;
        private int parsedPriceConfidence;
        private String parsedPriceStatus;
        private String parsedPriceNormalizedDisplay;
        private String parsedPriceAlias;
        private Integer parsedPriceQuantityAmount;
        private String parsedPriceQuantityUnitKey;
        private Integer parsedPriceQuantityItemCount;
        private String classification;
        private String primaryReason;
        private String cacheReason;
        private int ownerLineIndex;
        private int priceLineIndex;
        private List<String> itemLines = List.of();
        private String linkedContainerType;
        private String linkedContainerRelation;
        private String fingerprintSummary;
        private String containerBlockId;
        private int containerX;
        private int containerY;
        private int containerZ;
        private boolean containerLinked;
        private String containerRelationKind;
    }
}
