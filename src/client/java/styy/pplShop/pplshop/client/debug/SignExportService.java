package styy.pplShop.pplshop.client.debug;

import net.fabricmc.loader.api.FabricLoader;
import styy.pplShop.pplshop.client.gui.PriceColorResolver;
import styy.pplShop.pplshop.client.gui.ShopEntryComparator;
import styy.pplShop.pplshop.client.gui.ShopEntryWidget;
import styy.pplShop.pplshop.client.gui.ItemThemeResolver;
import styy.pplShop.pplshop.client.model.ItemResolutionTrace;
import styy.pplShop.pplshop.client.model.ShopSignEntry;
import styy.pplShop.pplshop.client.world.ActiveHighlightState;
import styy.pplShop.pplshop.client.world.LoadedSignScanner;
import styy.pplShop.pplshop.client.world.RefreshUiState;
import styy.pplShop.pplshop.client.world.ShopSnapshotMetadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SignExportService {
    private static final DateTimeFormatter DIRECTORY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS");
    private static final String NONE = "none";
    private static final String DEBUG_ROOT_DIR = "pplshop-debug";
    private static final String BARRIER_ITEM_ID = "minecraft:barrier";
    private final ItemThemeResolver themeResolver = new ItemThemeResolver();

    public Path exportFullDiagnostics(
            List<ShopSignEntry> entries,
            GuiDebugSnapshot guiSnapshot,
            LoadedSignScanner.DebugSnapshot scannerDebugSnapshot,
            RefreshUiState refreshUiState,
            ShopSnapshotMetadata snapshotMetadata,
            ActiveHighlightState.HighlightDebugSnapshot highlightDebugSnapshot
    ) throws IOException {
        DebugExportSession session = this.createExportSession("diagnostic-dump");
        GuiDebugSnapshot effectiveGuiSnapshot = guiSnapshot == null ? this.createCacheGuiSnapshot(entries) : guiSnapshot;

        session.writeFile("00-overview", this.buildOverviewText(entries, effectiveGuiSnapshot, refreshUiState, snapshotMetadata));
        session.writeFile("01-sign-summary", this.buildSnapshotSummary(entries));
        session.writeFile("02-sign-parsed-entries", this.buildParsedEntriesText(entries));
        session.writeFile("03-sign-unresolved-entries", this.buildUnresolvedEntriesText(entries));
        session.writeFile("04-sign-candidate-resolution-trace", this.buildAliasTraceText(entries));
        session.writeFile("05-gui-summary", this.buildGuiSummaryText(effectiveGuiSnapshot));
        session.writeFile("06-gui-filtered-entries", this.buildFilteredEntriesText(effectiveGuiSnapshot.filteredEntries()));
        session.writeFile("07-gui-problem-entries", this.buildProblemEntriesText(effectiveGuiSnapshot.filteredEntries()));
        session.writeFile("08-gui-hovered-entry", this.buildHoveredEntryText(effectiveGuiSnapshot.hoveredEntry(), effectiveGuiSnapshot.hoveredIndex()));
        session.writeFile("09-gui-parsed-entries", this.buildGuiParsedEntriesText(effectiveGuiSnapshot.filteredEntries()));
        session.writeFile("10-gui-unresolved-entries", this.buildGuiUnresolvedEntriesText(effectiveGuiSnapshot.filteredEntries()));
        session.writeFile("11-gui-candidate-resolution-trace", this.buildGuiAliasTraceText(effectiveGuiSnapshot.filteredEntries()));
        session.writeFile("12-classifier-cache-debug", this.buildClassifierCacheDebugText(scannerDebugSnapshot, refreshUiState, snapshotMetadata));
        session.writeFile("13-highlight-state-debug", this.buildHighlightStateDebugText(highlightDebugSnapshot));
        session.writeFile("14-potential-false-positives", this.buildPotentialFalsePositiveText(entries));
        return session.exportDir();
    }

    public GuiDebugSnapshot createCacheGuiSnapshot(List<ShopSignEntry> entries) {
        List<ShopSignEntry> safeEntries = entries == null ? List.of() : List.copyOf(entries);
        Map<net.minecraft.util.Identifier, PriceColorResolver.ItemPriceStats> statsByItemId = PriceColorResolver.buildStats(safeEntries);
        List<ShopEntryWidget.Model> models = safeEntries.stream()
                .map(entry -> ShopEntryWidget.Model.fromEntry(entry, PriceColorResolver.resolve(entry, statsByItemId)))
                .toList();
        return new GuiDebugSnapshot(models, null, -1, "cache_snapshot_full_list");
    }

    public static String describeFailure(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }

    private DebugExportSession createExportSession(String prefix) throws IOException {
        Path rootDir = FabricLoader.getInstance().getGameDir().resolve(DEBUG_ROOT_DIR);
        Files.createDirectories(rootDir);

        String timestamp = LocalDateTime.now().format(DIRECTORY_TIMESTAMP);
        Path exportDir = rootDir.resolve(prefix + "-" + timestamp);
        Files.createDirectories(exportDir);
        return new DebugExportSession(exportDir, timestamp);
    }

    private String buildOverviewText(List<ShopSignEntry> entries, GuiDebugSnapshot guiSnapshot, RefreshUiState refreshUiState, ShopSnapshotMetadata snapshotMetadata) {
        List<ShopEntryWidget.Model> filteredEntries = guiSnapshot.filteredEntries();
        List<ShopEntryWidget.Model> problemEntries = this.collectProblemEntries(filteredEntries);
        long unresolvedSigns = entries.stream()
                .filter(ShopEntryComparator::isUnknownEntry)
                .count();

        StringBuilder builder = new StringBuilder();
        builder.append("=== PPLSHOP DIAGNOSTIC DUMP ===").append(System.lineSeparator());
        builder.append("ExportRoot: ").append(DEBUG_ROOT_DIR).append(System.lineSeparator());
        builder.append("ExportedAt: ").append(LocalDateTime.now()).append(System.lineSeparator());
        builder.append("SignEntries: ").append(entries.size()).append(System.lineSeparator());
        builder.append("ResolvedSignEntries: ").append(entries.size() - unresolvedSigns).append(System.lineSeparator());
        builder.append("UnresolvedSignEntries: ").append(unresolvedSigns).append(System.lineSeparator());
        builder.append("GuiSnapshotSource: ").append(guiSnapshot.sourceDescription()).append(System.lineSeparator());
        builder.append("GuiFilteredEntries: ").append(filteredEntries.size()).append(System.lineSeparator());
        builder.append("GuiProblemEntries: ").append(problemEntries.size()).append(System.lineSeparator());
        builder.append("GuiHoveredIndex: ").append(guiSnapshot.hoveredIndex()).append(System.lineSeparator());
        builder.append("GuiHoveredPresent: ").append(guiSnapshot.hoveredEntry() != null).append(System.lineSeparator());
        if (refreshUiState != null) {
            builder.append("SnapshotLoadedFromDisk: ").append(refreshUiState.snapshotLoadedFromDisk()).append(System.lineSeparator());
            builder.append("RefreshInProgress: ").append(refreshUiState.refreshInProgress()).append(System.lineSeparator());
            builder.append("RefreshTriggerSource: ").append(refreshUiState.triggerSource()).append(System.lineSeparator());
            builder.append("SnapshotAgeMillis: ").append(refreshUiState.snapshotAgeMillis()).append(System.lineSeparator());
            builder.append("DataState: ").append(refreshUiState.dataState()).append(System.lineSeparator());
        }
        if (snapshotMetadata != null) {
            builder.append("SnapshotWorld: ").append(snapshotMetadata.worldDisplayName()).append(System.lineSeparator());
            builder.append("SnapshotDimension: ").append(snapshotMetadata.dimensionId()).append(System.lineSeparator());
            builder.append("SnapshotLastRefresh: ").append(snapshotMetadata.lastRefreshTimeMillis()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildSnapshotSummary(List<ShopSignEntry> entries) {
        StringBuilder builder = new StringBuilder();
        long unresolvedCount = entries.stream().filter(ShopEntryComparator::isUnknownEntry).count();
        builder.append("=== PPLSHOP SIGN EXPORT ===").append(System.lineSeparator());
        builder.append("TotalEntries: ").append(entries.size()).append(System.lineSeparator());
        builder.append("ResolvedEntries: ").append(entries.size() - unresolvedCount).append(System.lineSeparator());
        builder.append("UnresolvedEntries: ").append(unresolvedCount).append(System.lineSeparator());
        builder.append("ExportedAt: ").append(LocalDateTime.now()).append(System.lineSeparator());
        return builder.toString();
    }

    private String buildParsedEntriesText(List<ShopSignEntry> entries) {
        StringBuilder builder = new StringBuilder();
        if (entries.isEmpty()) {
            builder.append("No cached signs found.").append(System.lineSeparator());
            return builder.toString();
        }

        for (int i = 0; i < entries.size(); i++) {
            this.appendSnapshotEntry(builder, i, entries.get(i));
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildUnresolvedEntriesText(List<ShopSignEntry> entries) {
        StringBuilder builder = new StringBuilder();
        List<ShopSignEntry> unresolvedEntries = entries.stream()
                .filter(ShopEntryComparator::isUnknownEntry)
                .toList();
        if (unresolvedEntries.isEmpty()) {
            builder.append("No unresolved entries found.").append(System.lineSeparator());
            return builder.toString();
        }

        for (int i = 0; i < unresolvedEntries.size(); i++) {
            this.appendSnapshotEntry(builder, i, unresolvedEntries.get(i));
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildAliasTraceText(List<ShopSignEntry> entries) {
        StringBuilder builder = new StringBuilder();
        if (entries.isEmpty()) {
            builder.append("No alias trace data available.").append(System.lineSeparator());
            return builder.toString();
        }

        for (int i = 0; i < entries.size(); i++) {
            ShopSignEntry entry = entries.get(i);
            builder.append("=== TRACE ").append(i).append(" ===").append(System.lineSeparator());
            builder.append("DisplayName: ").append(entry.displayName()).append(System.lineSeparator());
            builder.append("ResolvedItemId: ").append(entry.resolvedItemId()).append(System.lineSeparator());
            this.appendTrace(builder, entry.parsedItem().resolutionTrace());
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildGuiSummaryText(GuiDebugSnapshot guiSnapshot) {
        StringBuilder builder = new StringBuilder();
        List<ShopEntryWidget.Model> filteredEntries = guiSnapshot.filteredEntries();
        List<ShopEntryWidget.Model> problemEntries = this.collectProblemEntries(filteredEntries);
        builder.append("=== PPLSHOP GUI DEBUG EXPORT ===").append(System.lineSeparator());
        builder.append("Source: ").append(guiSnapshot.sourceDescription()).append(System.lineSeparator());
        builder.append("TotalFilteredEntries: ").append(filteredEntries.size()).append(System.lineSeparator());
        builder.append("ProblemEntries: ").append(problemEntries.size()).append(System.lineSeparator());
        builder.append("HoveredIndex: ").append(guiSnapshot.hoveredIndex()).append(System.lineSeparator());
        builder.append("HoveredPresent: ").append(guiSnapshot.hoveredEntry() != null).append(System.lineSeparator());
        builder.append("ExportedAt: ").append(LocalDateTime.now()).append(System.lineSeparator());
        return builder.toString();
    }

    private String buildFilteredEntriesText(List<ShopEntryWidget.Model> filteredEntries) {
        StringBuilder builder = new StringBuilder();
        if (filteredEntries.isEmpty()) {
            builder.append("No filtered GUI entries.").append(System.lineSeparator());
            return builder.toString();
        }

        for (int i = 0; i < filteredEntries.size(); i++) {
            this.appendGuiEntry(builder, i, filteredEntries.get(i));
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildProblemEntriesText(List<ShopEntryWidget.Model> filteredEntries) {
        StringBuilder builder = new StringBuilder();
        List<ShopEntryWidget.Model> problemEntries = this.collectProblemEntries(filteredEntries);
        if (problemEntries.isEmpty()) {
            builder.append("No fallback/barrier/problem entries in filtered list.").append(System.lineSeparator());
            return builder.toString();
        }

        for (int i = 0; i < problemEntries.size(); i++) {
            this.appendGuiEntry(builder, i, problemEntries.get(i));
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildHoveredEntryText(ShopEntryWidget.Model hoveredEntry, int hoveredIndex) {
        StringBuilder builder = new StringBuilder();
        if (hoveredEntry == null) {
            builder.append("HoveredEntry: none").append(System.lineSeparator());
            return builder.toString();
        }
        this.appendGuiEntry(builder, hoveredIndex, hoveredEntry);
        return builder.toString();
    }

    private String buildGuiParsedEntriesText(List<ShopEntryWidget.Model> filteredEntries) {
        return this.buildFilteredEntriesText(filteredEntries);
    }

    private String buildGuiUnresolvedEntriesText(List<ShopEntryWidget.Model> filteredEntries) {
        StringBuilder builder = new StringBuilder();
        List<ShopEntryWidget.Model> unresolvedEntries = filteredEntries.stream()
                .filter(model -> ShopEntryComparator.isUnknownEntry(model.entry()))
                .toList();
        if (unresolvedEntries.isEmpty()) {
            builder.append("No unresolved GUI entries found.").append(System.lineSeparator());
            return builder.toString();
        }

        for (int i = 0; i < unresolvedEntries.size(); i++) {
            this.appendGuiEntry(builder, i, unresolvedEntries.get(i));
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildGuiAliasTraceText(List<ShopEntryWidget.Model> filteredEntries) {
        StringBuilder builder = new StringBuilder();
        if (filteredEntries.isEmpty()) {
            builder.append("No GUI alias trace data available.").append(System.lineSeparator());
            return builder.toString();
        }

        for (int i = 0; i < filteredEntries.size(); i++) {
            ShopEntryWidget.Model model = filteredEntries.get(i);
            builder.append("=== GUI TRACE ").append(i).append(" ===").append(System.lineSeparator());
            builder.append("WidgetTitle: ").append(model.displayName()).append(System.lineSeparator());
            builder.append("WidgetInternalTitle: ").append(model.internalDisplayTitle()).append(System.lineSeparator());
            builder.append("PriceTintProgress: ").append(model.priceColors().progress()).append(System.lineSeparator());
            builder.append("PriceTintApplied: ").append(model.priceColors().tinted()).append(System.lineSeparator());
            this.appendTrace(builder, model.entry().parsedItem().resolutionTrace());
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private void appendSnapshotEntry(StringBuilder builder, int index, ShopSignEntry entry) {
        builder.append("=== SIGN ").append(index).append(" ===").append(System.lineSeparator());
        builder.append("Dimension: ").append(entry.dimensionId()).append(System.lineSeparator());
        builder.append("Pos: ")
                .append(entry.pos().getX()).append(' ')
                .append(entry.pos().getY()).append(' ')
                .append(entry.pos().getZ()).append(System.lineSeparator());
        builder.append("Type: ").append(entry.snapshot().hanging() ? "hanging sign" : "sign").append(System.lineSeparator());
        builder.append("Side: ").append(entry.snapshot().side().name()).append(System.lineSeparator());

        List<String> lines = entry.snapshot().lines();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            builder.append("Line").append(lineIndex + 1).append(": ").append(lines.get(lineIndex)).append(System.lineSeparator());
        }

        builder.append("Raw: ").append(entry.rawCombinedText()).append(System.lineSeparator());
        builder.append("ParsedItemRaw: ").append(entry.parsedItem().rawText()).append(System.lineSeparator());
        builder.append("ParsedItemNormalized: ").append(entry.parsedItem().normalizedText()).append(System.lineSeparator());
        builder.append("ParsedItemAlias: ").append(entry.parsedItem().matchedAlias()).append(System.lineSeparator());
        builder.append("ParsedItemId: ").append(entry.parsedItem().itemId()).append(System.lineSeparator());
        builder.append("ParsedItemResultType: ").append(entry.parsedItem().resultType()).append(System.lineSeparator());
        builder.append("ParsedItemSafe: ").append(entry.parsedItem().safeResult()).append(System.lineSeparator());
        builder.append("ResolvedBucketId: ").append(entry.parsedItem().resolvedBucketId()).append(System.lineSeparator());
        builder.append("ResolvedSubtypeKey: ").append(entry.parsedItem().resolvedSubtypeKey()).append(System.lineSeparator());
        builder.append("DisplayNameOverride: ").append(entry.parsedItem().displayNameOverride()).append(System.lineSeparator());
        builder.append("ResolvedItemId: ").append(entry.resolvedItemId()).append(System.lineSeparator());
        builder.append("Themes: ").append(this.themeResolver.resolveThemes(entry)).append(System.lineSeparator());
        builder.append("ItemStatus: ").append(entry.parsedItem().parseStatus()).append(System.lineSeparator());
        builder.append("DisplayName: ").append(entry.displayName()).append(System.lineSeparator());
        builder.append("ParsedPrice: ").append(entry.parsedPrice().normalizedDisplayText()).append(System.lineSeparator());
        builder.append("PriceAmount: ").append(entry.parsedPrice().amount()).append(System.lineSeparator());
        builder.append("UnknownEntry: ").append(ShopEntryComparator.isUnknownEntry(entry)).append(System.lineSeparator());
        builder.append("Classification: ").append(entry.diagnostics().classification()).append(System.lineSeparator());
        builder.append("PrimaryReason: ").append(entry.diagnostics().primaryReason()).append(System.lineSeparator());
        builder.append("CacheReason: ").append(entry.diagnostics().cacheReason()).append(System.lineSeparator());
        builder.append("LinkedContainerType: ").append(entry.diagnostics().linkedContainerType()).append(System.lineSeparator());
        builder.append("LinkedContainerRelation: ").append(entry.diagnostics().linkedContainerRelation()).append(System.lineSeparator());
        builder.append("OwnerLineIndex: ").append(entry.diagnostics().ownerLineIndex()).append(System.lineSeparator());
        builder.append("PriceLineIndex: ").append(entry.diagnostics().priceLineIndex()).append(System.lineSeparator());
        builder.append("ItemLines: ").append(String.join(" || ", entry.diagnostics().itemLines())).append(System.lineSeparator());
        builder.append("Fingerprint: ").append(entry.diagnostics().fingerprintSummary()).append(System.lineSeparator());
        this.appendTrace(builder, entry.parsedItem().resolutionTrace());
    }

    private void appendGuiEntry(StringBuilder builder, int index, ShopEntryWidget.Model model) {
        ShopSignEntry entry = model.entry();
        builder.append("=== GUI ENTRY ").append(index).append(" ===").append(System.lineSeparator());
        builder.append("IndexInFilteredList: ").append(index).append(System.lineSeparator());
        builder.append("RawSignLines: ").append(model.rawSignLines()).append(System.lineSeparator());
        builder.append("RawSignText: ").append(model.rawSignText()).append(System.lineSeparator());
        builder.append("ShopEntryDisplayTitle: ").append(model.displayNameBeforeWidget()).append(System.lineSeparator());
        builder.append("ShopEntryPriceText: ").append(entry.priceDisplay()).append(System.lineSeparator());
        builder.append("ParsedPriceText: ").append(entry.parsedPrice().normalizedDisplayText()).append(System.lineSeparator());
        builder.append("ParsedPriceAmount: ").append(entry.parsedPrice().amount()).append(System.lineSeparator());
        builder.append("ParsedItemRaw: ").append(model.parsedItemRaw()).append(System.lineSeparator());
        builder.append("ParsedItemAlias: ").append(model.parsedItemAlias()).append(System.lineSeparator());
        builder.append("ParsedItemId: ").append(model.parsedItemId()).append(System.lineSeparator());
        builder.append("ParsedItemResultType: ").append(entry.parsedItem().resultType()).append(System.lineSeparator());
        builder.append("ParsedItemSafe: ").append(entry.parsedItem().safeResult()).append(System.lineSeparator());
        builder.append("ResolvedBucketId: ").append(entry.parsedItem().resolvedBucketId()).append(System.lineSeparator());
        builder.append("ResolvedSubtypeKey: ").append(entry.parsedItem().resolvedSubtypeKey()).append(System.lineSeparator());
        builder.append("Themes: ").append(this.themeResolver.resolveThemes(entry)).append(System.lineSeparator());
        builder.append("DisplayNameOverride: ").append(entry.parsedItem().displayNameOverride()).append(System.lineSeparator());
        builder.append("ResolvedItemId: ").append(model.resolvedItemId()).append(System.lineSeparator());
        builder.append("ItemStatus: ").append(model.itemStatus()).append(System.lineSeparator());
        builder.append("WidgetItemId: ").append(model.widgetItemId()).append(System.lineSeparator());
        builder.append("RenderedItemStackItemId: ").append(model.widgetStackItemId()).append(System.lineSeparator());
        builder.append("WidgetTitle: ").append(model.displayName()).append(System.lineSeparator());
        builder.append("WidgetPriceText: ").append(entry.priceDisplay()).append(System.lineSeparator());
        builder.append("PriceTintApplied: ").append(model.priceColors().tinted()).append(System.lineSeparator());
        builder.append("PriceTintProgress: ").append(model.priceColors().progress()).append(System.lineSeparator());
        builder.append("FallbackUsed: ").append(model.fallbackUsed()).append(System.lineSeparator());
        builder.append("FallbackReason: ").append(model.fallbackReason()).append(System.lineSeparator());
        builder.append("DiagnosticReason: ").append(model.diagnosticReason()).append(System.lineSeparator());
        builder.append("Classification: ").append(entry.diagnostics().classification()).append(System.lineSeparator());
        builder.append("PrimaryReason: ").append(entry.diagnostics().primaryReason()).append(System.lineSeparator());
        builder.append("CacheReason: ").append(entry.diagnostics().cacheReason()).append(System.lineSeparator());
        this.appendTrace(builder, entry.parsedItem().resolutionTrace());
        builder.append("SignPos: ")
                .append(entry.pos().getX()).append(' ')
                .append(entry.pos().getY()).append(' ')
                .append(entry.pos().getZ()).append(System.lineSeparator());
        builder.append("OwnerOrNick: ").append(this.guessOwnerOrNick(entry)).append(System.lineSeparator());
    }

    private void appendTrace(StringBuilder builder, ItemResolutionTrace trace) {
        builder.append("ChosenCandidate: ").append(trace.selectedCandidate()).append(System.lineSeparator());
        builder.append("ChosenCandidateSource: ").append(trace.selectedCandidateSource()).append(System.lineSeparator());
        builder.append("ChosenResolver: ").append(trace.selectedResolver()).append(System.lineSeparator());
        builder.append("ChosenItemId: ").append(trace.selectedItemId()).append(System.lineSeparator());
        builder.append("TraceMatchedAlias: ").append(trace.matchedAlias()).append(System.lineSeparator());
        builder.append("TraceFallbackReason: ").append(trace.fallbackReason()).append(System.lineSeparator());
        builder.append("TraceSuggestedAliases: ").append(String.join(" || ", trace.suggestedAliases())).append(System.lineSeparator());
        builder.append("ConsideredCandidates: ").append(String.join(" || ", trace.consideredCandidates())).append(System.lineSeparator());
        builder.append("RejectedCandidates: ").append(String.join(" || ", trace.rejectedCandidates())).append(System.lineSeparator());
    }

    private List<ShopEntryWidget.Model> collectProblemEntries(List<ShopEntryWidget.Model> filteredEntries) {
        List<ShopEntryWidget.Model> problemEntries = new ArrayList<>();
        for (ShopEntryWidget.Model model : filteredEntries) {
            boolean widgetBarrier = this.isBarrier(model.widgetItemId());
            boolean barrierEntry = widgetBarrier || this.isBarrier(model.widgetStackItemId());
            boolean diagnosticProblem = !NONE.equalsIgnoreCase(this.safeString(model.diagnosticReason()));
            if (model.fallbackUsed()
                    || widgetBarrier
                    || model.resolvedItemId() == null
                    || barrierEntry
                    || diagnosticProblem) {
                problemEntries.add(model);
            }
        }
        return problemEntries;
    }

    private boolean isBarrier(Object itemId) {
        return BARRIER_ITEM_ID.equals(this.safeString(itemId));
    }

    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }

    private String guessOwnerOrNick(ShopSignEntry entry) {
        List<String> lines = entry.snapshot().lines();
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.equals(entry.parsedItem().rawText())) {
                continue;
            }
            if (trimmed.equals(entry.priceDisplay())) {
                continue;
            }
            return trimmed;
        }
        return "";
    }

    private String buildClassifierCacheDebugText(LoadedSignScanner.DebugSnapshot debugSnapshot, RefreshUiState refreshUiState, ShopSnapshotMetadata snapshotMetadata) {
        StringBuilder builder = new StringBuilder();
        if (debugSnapshot == null) {
            builder.append("No classifier/cache debug data available.").append(System.lineSeparator());
            return builder.toString();
        }

        builder.append("=== SNAPSHOT / REFRESH ===").append(System.lineSeparator());
        if (refreshUiState != null) {
            builder.append("SnapshotLoadedFromDisk: ").append(refreshUiState.snapshotLoadedFromDisk()).append(System.lineSeparator());
            builder.append("SnapshotAgeMillis: ").append(refreshUiState.snapshotAgeMillis()).append(System.lineSeparator());
            builder.append("RefreshInProgress: ").append(refreshUiState.refreshInProgress()).append(System.lineSeparator());
            builder.append("RefreshTriggerSource: ").append(refreshUiState.triggerSource()).append(System.lineSeparator());
            builder.append("ProcessedEntriesPerTick: ").append(debugSnapshot.lastProcessedChunks() + debugSnapshot.lastProcessedSigns()).append(System.lineSeparator());
        }
        if (snapshotMetadata != null) {
            builder.append("SnapshotWorldKey: ").append(snapshotMetadata.worldKey()).append(System.lineSeparator());
            builder.append("SnapshotWorldDisplay: ").append(snapshotMetadata.worldDisplayName()).append(System.lineSeparator());
            builder.append("SnapshotDimension: ").append(snapshotMetadata.dimensionId()).append(System.lineSeparator());
            builder.append("SnapshotEntryCount: ").append(snapshotMetadata.entryCount()).append(System.lineSeparator());
            builder.append("SnapshotKnownChunkCount: ").append(snapshotMetadata.lastKnownChunkCount()).append(System.lineSeparator());
            builder.append("SnapshotKnownSignCount: ").append(snapshotMetadata.lastKnownSignCount()).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());

        builder.append("=== NEGATIVE CACHE ===").append(System.lineSeparator());
        if (debugSnapshot.negativeEntries().isEmpty()) {
            builder.append("No negative cache entries.").append(System.lineSeparator());
        } else {
            for (var entry : debugSnapshot.negativeEntries()) {
                builder.append("Reason: ").append(entry.reason()).append(System.lineSeparator());
                builder.append("RawText: ").append(entry.rawText()).append(System.lineSeparator());
                builder.append("Fingerprint: ")
                        .append(entry.fingerprint().normalizedTextHash())
                        .append('|')
                        .append(entry.fingerprint().relationSignature())
                        .append(System.lineSeparator())
                        .append(System.lineSeparator());
            }
        }

        builder.append("=== RECENT EVENTS ===").append(System.lineSeparator());
        if (debugSnapshot.recentEvents().isEmpty()) {
            builder.append("No recent cache/classifier events.").append(System.lineSeparator());
        } else {
            for (String event : debugSnapshot.recentEvents()) {
                builder.append(event).append(System.lineSeparator());
            }
        }
        builder.append(System.lineSeparator());
        builder.append("=== QUEUE / TIMINGS ===").append(System.lineSeparator());
        builder.append("QueuedChunks: ").append(debugSnapshot.queuedChunks()).append(System.lineSeparator());
        builder.append("QueuedSigns: ").append(debugSnapshot.queuedSigns()).append(System.lineSeparator());
        builder.append("KnownLoadedChunks: ").append(debugSnapshot.knownLoadedChunks()).append(System.lineSeparator());
        builder.append("KnownSigns: ").append(debugSnapshot.knownSigns()).append(System.lineSeparator());
        builder.append("LastProcessedChunks: ").append(debugSnapshot.lastProcessedChunks()).append(System.lineSeparator());
        builder.append("LastProcessedSigns: ").append(debugSnapshot.lastProcessedSigns()).append(System.lineSeparator());
        builder.append("CacheHits: ").append(debugSnapshot.cacheHitCount()).append(System.lineSeparator());
        builder.append("NegativeCacheHits: ").append(debugSnapshot.negativeCacheHitCount()).append(System.lineSeparator());
        builder.append("DirtyReparses: ").append(debugSnapshot.dirtyReparseCount()).append(System.lineSeparator());
        builder.append("FullRebuildTasks: ").append(debugSnapshot.fullRebuildCount()).append(System.lineSeparator());
        builder.append("LastCollectionPhaseNanos: ").append(debugSnapshot.lastChunkPhaseNanos()).append(System.lineSeparator());
        builder.append("LastSignPhaseNanos: ").append(debugSnapshot.lastSignPhaseNanos()).append(System.lineSeparator());
        builder.append("TotalCollectionPhaseNanos: ").append(debugSnapshot.totalChunkPhaseNanos()).append(System.lineSeparator());
        builder.append("TotalSignPhaseNanos: ").append(debugSnapshot.totalSignPhaseNanos()).append(System.lineSeparator());
        return builder.toString();
    }

    private String buildPotentialFalsePositiveText(List<ShopSignEntry> entries) {
        StringBuilder builder = new StringBuilder();
        List<ShopSignEntry> suspicious = entries.stream()
                .filter(entry -> entry.resolvedItemId() != null)
                .filter(entry -> entry.parsedItem().resultType().name().contains("SHORTLIST")
                        || !entry.parsedItem().resolutionTrace().suggestedAliases().isEmpty()
                        || !entry.parsedItem().resolutionTrace().rejectedCandidates().isEmpty())
                .toList();
        if (suspicious.isEmpty()) {
            builder.append("No suspicious resolved entries found.").append(System.lineSeparator());
            return builder.toString();
        }

        for (int index = 0; index < suspicious.size(); index++) {
            ShopSignEntry entry = suspicious.get(index);
            builder.append("=== SUSPECT ").append(index).append(" ===").append(System.lineSeparator());
            builder.append("DisplayName: ").append(entry.displayName()).append(System.lineSeparator());
            builder.append("ResolvedItemId: ").append(entry.resolvedItemId()).append(System.lineSeparator());
            builder.append("ResultType: ").append(entry.parsedItem().resultType()).append(System.lineSeparator());
            builder.append("MatchedAlias: ").append(entry.parsedItem().matchedAlias()).append(System.lineSeparator());
            builder.append("SelectedCandidate: ").append(entry.parsedItem().resolutionTrace().selectedCandidate()).append(System.lineSeparator());
            builder.append("SelectedResolver: ").append(entry.parsedItem().resolutionTrace().selectedResolver()).append(System.lineSeparator());
            builder.append("SuggestedAliases: ").append(String.join(" || ", entry.parsedItem().resolutionTrace().suggestedAliases())).append(System.lineSeparator());
            builder.append("RejectedCandidates: ").append(String.join(" || ", entry.parsedItem().resolutionTrace().rejectedCandidates())).append(System.lineSeparator());
            builder.append("RawLines: ").append(String.join(" || ", entry.snapshot().lines())).append(System.lineSeparator());
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildHighlightStateDebugText(ActiveHighlightState.HighlightDebugSnapshot highlightDebugSnapshot) {
        StringBuilder builder = new StringBuilder();
        builder.append("=== HIGHLIGHT STATE DEBUG ===").append(System.lineSeparator());
        if (highlightDebugSnapshot == null) {
            builder.append("ActiveHighlightCount: 0").append(System.lineSeparator());
            builder.append("HighlightedBlockPosEntries:").append(System.lineSeparator());
            builder.append("none").append(System.lineSeparator());
            builder.append("LastMutationEvent: CLEAR").append(System.lineSeparator());
            builder.append("LastMutationPos: none").append(System.lineSeparator());
            builder.append("LastMutationTrigger: OTHER").append(System.lineSeparator());
            return builder.toString();
        }

        builder.append("ActiveHighlightCount: ").append(highlightDebugSnapshot.activeHighlightCount()).append(System.lineSeparator());
        builder.append("HighlightedBlockPosEntries:").append(System.lineSeparator());
        if (highlightDebugSnapshot.highlightedPositions().isEmpty()) {
            builder.append("none").append(System.lineSeparator());
        } else {
            for (var pos : highlightDebugSnapshot.highlightedPositions()) {
                builder.append(this.formatBlockPos(pos)).append(System.lineSeparator());
            }
        }
        builder.append("LastMutationEvent: ").append(highlightDebugSnapshot.lastMutationEvent()).append(System.lineSeparator());
        builder.append("LastMutationPos: ").append(this.formatBlockPos(highlightDebugSnapshot.lastMutationPos())).append(System.lineSeparator());
        builder.append("LastMutationTrigger: ").append(highlightDebugSnapshot.lastMutationTrigger()).append(System.lineSeparator());
        return builder.toString();
    }

    private String formatBlockPos(net.minecraft.util.math.BlockPos pos) {
        if (pos == null) {
            return "none";
        }
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    public record GuiDebugSnapshot(
            List<ShopEntryWidget.Model> filteredEntries,
            ShopEntryWidget.Model hoveredEntry,
            int hoveredIndex,
            String sourceDescription
    ) {
        public GuiDebugSnapshot {
            filteredEntries = filteredEntries == null ? List.of() : List.copyOf(filteredEntries);
            hoveredIndex = hoveredEntry == null ? -1 : hoveredIndex;
            sourceDescription = sourceDescription == null || sourceDescription.isBlank() ? "unknown" : sourceDescription;
        }
    }

    private record DebugExportSession(Path exportDir, String timestamp) {
        private void writeFile(String logicalName, String content) throws IOException {
            String fileName = this.timestamp + "__" + logicalName + ".txt";
            Files.writeString(this.exportDir.resolve(fileName), content, StandardCharsets.UTF_8);
        }
    }
}
