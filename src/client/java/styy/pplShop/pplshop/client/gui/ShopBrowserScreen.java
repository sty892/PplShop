package styy.pplShop.pplshop.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import styy.pplShop.pplshop.client.config.CurrencyAliasConfig;
import styy.pplShop.pplshop.client.config.ItemAliasConfig;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.config.RefreshUxConfig;
import styy.pplShop.pplshop.client.debug.SignExportService;
import styy.pplShop.pplshop.client.model.ShopSignEntry;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;
import styy.pplShop.pplshop.client.world.HighlightManager;
import styy.pplShop.pplshop.client.world.RefreshTriggerSource;
import styy.pplShop.pplshop.client.world.RefreshUiState;
import styy.pplShop.pplshop.client.world.ShopCache;
import styy.pplShop.pplshop.client.world.ShopDataState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class ShopBrowserScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("PPLShop");
    private static final Identifier DISCORD_TEXTURE = Identifier.of("pplshop", "textures/gui/discord_support.png");
    private static final int DISCORD_TEXTURE_SIZE = 32;
    private static final String DISCORD_CONTACT = "styy8";
    private static final int ENTRY_WIDTH = 56;
    private static final int ENTRY_HEIGHT = 38;
    private static final int ENTRY_GAP = 4;
    private static final long SEARCH_DEBOUNCE_MS = 150L;

    private final ShopCache shopCache;
    private final HighlightManager highlightManager;
    private final ItemAliasConfig itemAliasConfig;
    private final CurrencyAliasConfig currencyAliasConfig;
    private final ParserRulesConfig parserRulesConfig;
    private final RefreshUxConfig refreshUxConfig;
    private final ShopBrowserSessionState sessionState;
    private final Supplier<RefreshUiState> refreshStateSupplier;
    private final Runnable guiRefreshAction;
    private final List<ShopEntryWidget> entryWidgets = new ArrayList<>();
    private final RefreshStatusBadge statusBadge = new RefreshStatusBadge();
    private final ItemThemeResolver itemThemeResolver = new ItemThemeResolver();
    private final ThemeKeywordSearch themeKeywordSearch = new ThemeKeywordSearch();

    private TextFieldWidget searchField;
    private ButtonWidget clearHighlightsButton;
    private ButtonWidget sortModeButton;
    private ButtonWidget refreshButton;
    private ShopBrowserLayout.Layout layout;
    private List<IndexedEntry> indexedEntries = List.of();
    private List<IndexedEntry> filteredIndexedEntries = List.of();
    private List<ShopSignEntry> filteredEntries = List.of();
    private Map<Identifier, PriceColorResolver.ItemPriceStats> itemPriceStats = Map.of();
    private ShopSignEntry hoveredEntry;
    private ShopEntryWidget.Model hoveredModel;
    private ShopSortMode sortMode = ShopSortMode.NAME;
    private ShopPriceSortBasis priceSortBasis = ShopPriceSortBasis.PER_UNIT;
    private int scrollRowOffset;
    private long lastCacheVersion = Long.MIN_VALUE;
    private String pendingNormalizedQuery = "";
    private String appliedNormalizedQuery = "";
    private long lastQueryChangedAt;
    private boolean scrollbarDragging;
    private int scrollbarDragOffsetY;
    private int scrollbarTrackX;
    private int scrollbarTrackY;
    private int scrollbarTrackHeight;
    private int scrollbarThumbY;
    private int scrollbarThumbHeight;
    private long discordCopiedAt;

    public ShopBrowserScreen(
            ShopCache shopCache,
            HighlightManager highlightManager,
            ItemAliasConfig itemAliasConfig,
            CurrencyAliasConfig currencyAliasConfig,
            ParserRulesConfig parserRulesConfig,
            RefreshUxConfig refreshUxConfig,
            ShopBrowserSessionState sessionState,
            Supplier<RefreshUiState> refreshStateSupplier,
            Runnable guiRefreshAction
    ) {
        super(Text.translatable("screen.pplshop.title"));
        this.shopCache = shopCache;
        this.highlightManager = highlightManager;
        this.itemAliasConfig = itemAliasConfig;
        this.currencyAliasConfig = currencyAliasConfig;
        this.parserRulesConfig = parserRulesConfig;
        this.refreshUxConfig = refreshUxConfig == null ? RefreshUxConfig.defaults() : refreshUxConfig;
        this.sessionState = sessionState == null ? new ShopBrowserSessionState() : sessionState;
        this.refreshStateSupplier = refreshStateSupplier;
        this.guiRefreshAction = guiRefreshAction;
    }

    @Override
    protected void init() {
        super.init();
        String existingSearch = this.searchField != null ? this.searchField.getText() : this.sessionState.searchText();
        this.priceSortBasis = this.defaultPriceSortBasis();
        this.sortMode = this.sessionState.sortMode().withPriceSortBasis(this.priceSortBasis);

        this.clearChildren();
        this.entryWidgets.clear();
        this.layout = this.computeLayout();
        this.statusBadge.setBounds(this.layout.statusBadgeBounds());

        this.searchField = new TextFieldWidget(
                this.textRenderer,
                this.layout.searchBounds().x(),
                this.layout.searchBounds().y(),
                this.layout.searchBounds().width(),
                this.layout.searchBounds().height(),
                Text.translatable("screen.pplshop.search")
        );
        this.searchField.setMaxLength(120);
        this.searchField.setPlaceholder(Text.translatable("screen.pplshop.search"));
        this.searchField.setText(existingSearch);
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);
        this.setInitialFocus(this.searchField);

        this.refreshButton = ButtonWidget.builder(Text.translatable("screen.pplshop.refresh"), button -> this.guiRefreshAction.run())
                .dimensions(this.layout.refreshBounds().x(), this.layout.refreshBounds().y(), this.layout.refreshBounds().width(), this.layout.refreshBounds().height())
                .build();
        this.addDrawableChild(this.refreshButton);

        this.sortModeButton = ButtonWidget.builder(this.sortModeLabel(), button -> this.cycleSortMode())
                .dimensions(this.layout.sortBounds().x(), this.layout.sortBounds().y(), this.layout.sortBounds().width(), this.layout.sortBounds().height())
                .build();
        this.addDrawableChild(this.sortModeButton);

        this.clearHighlightsButton = ButtonWidget.builder(Text.translatable("screen.pplshop.clear_highlight"), button -> this.highlightManager.clear())
                .dimensions(this.layout.clearBounds().x(), this.layout.clearBounds().y(), this.layout.clearBounds().width(), this.layout.clearBounds().height())
                .build();
        this.addDrawableChild(this.clearHighlightsButton);

        this.pendingNormalizedQuery = this.normalizeQuery(existingSearch);
        this.appliedNormalizedQuery = this.pendingNormalizedQuery;
        this.lastQueryChangedAt = Util.getMeasuringTimeMs();

        this.rebuildSearchIndex();
        this.applyFilter(false);
        this.scrollRowOffset = MathHelper.clamp(this.sessionState.scrollRowOffset(), 0, this.getMaxScrollRows());
        this.rebuildWidgetPool();
        this.updateVisibleWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        this.applyPendingSearchIfReady();
        if (this.lastCacheVersion != this.shopCache.version()) {
            this.clearVisibleWidgets();
            this.rebuildSearchIndex();
            this.applyFilter(false);
            this.rebuildWidgetPool();
            this.updateVisibleWidgets();
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.layout = this.computeLayout();
        this.statusBadge.setBounds(this.layout.statusBadgeBounds());
        this.rebuildWidgetPool();
        this.updateVisibleWidgets();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        this.persistSessionState();
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchField != null && this.searchField.isFocused() && this.searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchField != null && this.searchField.isFocused() && this.searchField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.hoveredEntry = null;
        this.hoveredModel = null;
        context.fillGradient(0, 0, this.width, this.height, 0xC215100B, 0xB00A0705);

        super.render(context, mouseX, mouseY, deltaTicks);

        ShopBrowserLayout.Layout activeLayout = this.activeLayout();
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, activeLayout.titleY(), 0xFFF3E6C8);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.pplshop.found", this.filteredEntries.size()), this.width / 2, activeLayout.foundY(), 0xFFCCCCCC);
        this.renderSnapshotSummary(context);
        this.statusBadge.render(context, this.currentRefreshState());
        this.renderDiscordSupport(context);

        for (ShopEntryWidget widget : this.entryWidgets) {
            if (widget.model() != null && widget.isMouseOver(mouseX, mouseY)) {
                this.hoveredModel = widget.model();
                this.hoveredEntry = widget.entry();
                break;
            }
        }

        this.renderScrollBar(context);

        if (this.isDiscordSupportHovered(mouseX, mouseY)) {
            this.renderDiscordSupportTooltip(context, mouseX, mouseY);
        } else if (this.statusBadge.isMouseOver(mouseX, mouseY)) {
            this.statusBadge.renderTooltip(context, this.textRenderer, this.currentRefreshState(), mouseX, mouseY);
        } else if (this.hoveredModel != null) {
            context.drawTooltip(this.textRenderer, this.buildTooltip(this.hoveredModel), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScrollRows = this.getMaxScrollRows();
        if (maxScrollRows <= 0 || verticalAmount == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int nextOffset = MathHelper.clamp(this.scrollRowOffset - (int) Math.signum(verticalAmount), 0, maxScrollRows);
        if (nextOffset == this.scrollRowOffset) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        this.scrollRowOffset = nextOffset;
        this.sessionState.setScrollRowOffset(this.scrollRowOffset);
        this.updateVisibleWidgets();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isPointInsideScrollBar(mouseX, mouseY)) {
            if (mouseY >= this.scrollbarThumbY && mouseY <= this.scrollbarThumbY + this.scrollbarThumbHeight) {
                this.scrollbarDragging = true;
                this.scrollbarDragOffsetY = (int) mouseY - this.scrollbarThumbY;
            } else {
                this.updateScrollFromThumbTop((int) mouseY - (this.scrollbarThumbHeight / 2));
            }
            return true;
        }
        if (button == 0 && this.isDiscordSupportHovered(mouseX, mouseY)) {
            MinecraftClient.getInstance().keyboard.setClipboard(DISCORD_CONTACT);
            this.discordCopiedAt = System.currentTimeMillis();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && this.scrollbarDragging) {
            this.updateScrollFromThumbTop((int) mouseY - this.scrollbarDragOffsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.scrollbarDragging) {
            this.scrollbarDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void onSearchChanged(String value) {
        this.sessionState.setSearchText(value);
        this.pendingNormalizedQuery = this.normalizeQuery(value);
        this.lastQueryChangedAt = Util.getMeasuringTimeMs();
        this.scrollRowOffset = 0;
        this.sessionState.setScrollRowOffset(0);
    }

    private void applyPendingSearchIfReady() {
        if (this.pendingNormalizedQuery.equals(this.appliedNormalizedQuery)) {
            return;
        }
        if (Util.getMeasuringTimeMs() - this.lastQueryChangedAt < SEARCH_DEBOUNCE_MS) {
            return;
        }
        this.applyFilter(true);
    }

    private void rebuildSearchIndex() {
        List<ShopSignEntry> source = this.shopCache.snapshot();
        List<IndexedEntry> rebuilt = new ArrayList<>(source.size());
        for (ShopSignEntry entry : source) {
            rebuilt.add(new IndexedEntry(entry, this.buildSearchBlob(entry), this.itemThemeResolver.resolveThemes(entry)));
        }
        this.itemPriceStats = PriceColorResolver.buildStats(source);
        this.indexedEntries = List.copyOf(rebuilt);
        this.filteredIndexedEntries = this.indexedEntries;
        this.lastCacheVersion = this.shopCache.version();
    }

    private void applyFilter(boolean scrollToTop) {
        Set<ShopTheme> previousMatchedThemes = this.themeKeywordSearch.resolve(this.appliedNormalizedQuery);
        String nextQuery = this.pendingNormalizedQuery;
        Set<ShopTheme> matchedThemes = this.themeKeywordSearch.resolve(nextQuery);
        List<IndexedEntry> filterSource = this.canFilterIncrementally(this.appliedNormalizedQuery, previousMatchedThemes, nextQuery, matchedThemes)
                ? this.filteredIndexedEntries
                : this.indexedEntries;

        this.appliedNormalizedQuery = nextQuery;
        this.filteredIndexedEntries = filterSource.stream()
                .filter(indexed -> this.matchesQuery(indexed, this.appliedNormalizedQuery, matchedThemes))
                .sorted((left, right) -> ShopEntryComparator.forMode(this.sortMode).compare(left.entry(), right.entry()))
                .toList();
        this.filteredEntries = this.filteredIndexedEntries.stream()
                .map(IndexedEntry::entry)
                .toList();
        this.logFilterSummary();
        if (scrollToTop) {
            this.scrollRowOffset = 0;
        }
        this.scrollRowOffset = MathHelper.clamp(this.scrollRowOffset, 0, this.getMaxScrollRows());
        this.sessionState.setScrollRowOffset(this.scrollRowOffset);
        this.updateVisibleWidgets();
    }

    private void rebuildWidgetPool() {
        int desired = this.getVisibleSlotCount();
        while (this.entryWidgets.size() < desired) {
            ShopEntryWidget widget = new ShopEntryWidget(0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, LOGGER.isDebugEnabled(), null, this::handleEntryClick);
            this.entryWidgets.add(widget);
            this.addDrawableChild(widget);
        }
        while (this.entryWidgets.size() > desired) {
            ShopEntryWidget removed = this.entryWidgets.remove(this.entryWidgets.size() - 1);
            this.remove(removed);
        }
    }

    private void updateVisibleWidgets() {
        this.rebuildWidgetPool();
        this.clearVisibleWidgets();
        int columns = this.getColumnCount();
        int totalRows = Math.max(0, (int) Math.ceil(this.filteredEntries.size() / (double) columns));
        int maxScrollRows = Math.max(0, totalRows - this.getVisibleRowCount());
        this.scrollRowOffset = MathHelper.clamp(this.scrollRowOffset, 0, maxScrollRows);

        int gridWidth = columns * ENTRY_WIDTH + Math.max(0, columns - 1) * ENTRY_GAP;
        int startX = (this.width - gridWidth) / 2;
        int gridTop = this.activeLayout().gridTop();

        for (int slotIndex = 0; slotIndex < this.entryWidgets.size(); slotIndex++) {
            int visibleRow = slotIndex / columns;
            int column = slotIndex % columns;
            int actualRow = this.scrollRowOffset + visibleRow;
            int entryIndex = actualRow * columns + column;
            int x = startX + column * (ENTRY_WIDTH + ENTRY_GAP);
            int y = gridTop + visibleRow * (ENTRY_HEIGHT + ENTRY_GAP);

            ShopEntryWidget widget = this.entryWidgets.get(slotIndex);
            widget.setPositionAndSize(x, y, ENTRY_WIDTH, ENTRY_HEIGHT);
            widget.setModel(entryIndex < this.filteredEntries.size() ? this.createModel(this.filteredEntries.get(entryIndex)) : null, this::handleEntryClick);
        }
    }

    private void clearVisibleWidgets() {
        for (ShopEntryWidget widget : this.entryWidgets) {
            widget.setModel(null, this::handleEntryClick);
        }
    }

    private void handleEntryClick(ShopSignEntry clickedEntry, int button) {
        if (button == 1) {
            Identifier resolvedItemId = clickedEntry.resolvedItemId();
            if (resolvedItemId != null) {
                List<ShopSignEntry> matchingEntries = this.shopCache.snapshot().stream()
                        .filter(entry -> resolvedItemId.equals(entry.resolvedItemId()))
                        .toList();
                if (!matchingEntries.isEmpty()) {
                    this.highlightManager.setTargets(matchingEntries, matchingEntries);
                }
            }
            this.close();
            return;
        }

        if (button == 0) {
            this.highlightManager.setTarget(clickedEntry, this.collectComparableEntries(clickedEntry));
            this.close();
        }
    }

    private List<ShopSignEntry> collectComparableEntries(ShopSignEntry clickedEntry) {
        Identifier resolvedItemId = clickedEntry.resolvedItemId();
        if (resolvedItemId == null || ShopEntryComparator.isUnknownEntry(clickedEntry)) {
            return List.of(clickedEntry);
        }

        List<ShopSignEntry> matchingEntries = this.shopCache.snapshot().stream()
                .filter(entry -> resolvedItemId.equals(entry.resolvedItemId()))
                .toList();
        return matchingEntries.isEmpty() ? List.of(clickedEntry) : matchingEntries;
    }

    private void cycleSortMode() {
        this.sortMode = this.sortMode.next(this.priceSortBasis);
        this.sessionState.setSortMode(this.sortMode);
        if (this.sortModeButton != null) {
            this.sortModeButton.setMessage(this.sortModeLabel());
        }
        this.applyFilter(true);
    }

    private boolean matchesQuery(IndexedEntry indexedEntry, String normalizedQuery, Set<ShopTheme> matchedThemes) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return true;
        }
        return indexedEntry.searchBlob().contains(normalizedQuery) || this.matchesThemeQuery(indexedEntry, matchedThemes);
    }

    private boolean matchesThemeQuery(IndexedEntry indexedEntry, Set<ShopTheme> matchedThemes) {
        if (matchedThemes == null || matchedThemes.isEmpty()) {
            return false;
        }
        for (ShopTheme theme : matchedThemes) {
            if (indexedEntry.themes().contains(theme)) {
                return true;
            }
        }
        return false;
    }

    private boolean canFilterIncrementally(String previousQuery, Set<ShopTheme> previousThemes, String nextQuery, Set<ShopTheme> nextThemes) {
        if (!previousThemes.equals(nextThemes)) {
            return false;
        }
        if (previousQuery == null || previousQuery.isBlank()) {
            return false;
        }
        if (nextQuery == null || nextQuery.isBlank()) {
            return false;
        }
        return nextQuery.startsWith(previousQuery);
    }

    private String buildSearchBlob(ShopSignEntry entry) {
        LinkedHashSet<String> normalizedTerms = new LinkedHashSet<>();
        for (String term : entry.rawSearchTerms()) {
            String normalized = NormalizationUtils.normalizeWithoutSorting(term, this.parserRulesConfig);
            if (!normalized.isBlank()) {
                normalizedTerms.add(normalized);
            }
        }

        if (entry.resolvedItemId() != null) {
            for (String alias : this.itemAliasConfig.items.getOrDefault(entry.resolvedItemId().toString(), List.of())) {
                String normalized = NormalizationUtils.normalizeWithoutSorting(alias, this.parserRulesConfig);
                if (!normalized.isBlank()) {
                    normalizedTerms.add(normalized);
                }
            }
        }
        if (!entry.parsedItem().resolvedBucketId().isBlank()) {
            for (String alias : this.itemAliasConfig.items.getOrDefault(entry.parsedItem().resolvedBucketId(), List.of())) {
                String normalized = NormalizationUtils.normalizeWithoutSorting(alias, this.parserRulesConfig);
                if (!normalized.isBlank()) {
                    normalizedTerms.add(normalized);
                }
            }
        }

        if (entry.parsedPrice().currencyKey() != null) {
            for (CurrencyAliasConfig.CurrencyDefinition definition : this.currencyAliasConfig.currencies) {
                if (!definition.key().equals(entry.parsedPrice().currencyKey())) {
                    continue;
                }
                for (String alias : definition.aliases()) {
                    String normalized = NormalizationUtils.normalizeWithoutSorting(alias, this.parserRulesConfig);
                    if (!normalized.isBlank()) {
                        normalizedTerms.add(normalized);
                    }
                }
            }
        }

        return String.join("\n", normalizedTerms);
    }

    private List<Text> buildTooltip(ShopEntryWidget.Model model) {
        ShopSignEntry entry = model.entry();
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal(model.displayName()));
        tooltip.add(Text.translatable("screen.pplshop.price", entry.priceDisplay()));
        tooltip.add(Text.empty());
        for (String line : entry.snapshot().lines()) {
            tooltip.add(Text.literal(line));
        }
        return tooltip;
    }

    private int getColumnCount() {
        return Math.max(1, (this.width - 48) / (ENTRY_WIDTH + ENTRY_GAP));
    }

    private int getVisibleRowCount() {
        ShopBrowserLayout.Layout activeLayout = this.activeLayout();
        int gridBottom = this.height - activeLayout.gridBottomPadding();
        int gridHeight = Math.max(ENTRY_HEIGHT, gridBottom - activeLayout.gridTop());
        return Math.max(1, gridHeight / (ENTRY_HEIGHT + ENTRY_GAP));
    }

    private int getVisibleSlotCount() {
        return this.getColumnCount() * this.getVisibleRowCount();
    }

    private int getMaxScrollRows() {
        int columns = this.getColumnCount();
        int totalRows = Math.max(0, (int) Math.ceil(this.filteredEntries.size() / (double) columns));
        return Math.max(0, totalRows - this.getVisibleRowCount());
    }

    private String normalizeQuery(String input) {
        return NormalizationUtils.normalizeWithoutSorting(input == null ? "" : input, this.parserRulesConfig);
    }

    private void logFilterSummary() {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        long unknownCount = this.filteredEntries.stream()
                .filter(ShopEntryComparator::isUnknownEntry)
                .count();
        LOGGER.debug("[GUI rebuild] total={} filtered={} unknown={} query='{}' theme={} sortMode={}",
                this.indexedEntries.size(),
                this.filteredEntries.size(),
                unknownCount,
                sanitize(this.appliedNormalizedQuery),
                this.themeKeywordSearch.resolve(this.appliedNormalizedQuery),
                this.sortMode);
    }

    public SignExportService.GuiDebugSnapshot captureGuiDebugSnapshot() {
        return new SignExportService.GuiDebugSnapshot(
                this.filteredEntries.stream().map(this::createModel).toList(),
                this.hoveredModel,
                this.hoveredFilteredIndex(),
                "shop_browser_screen_filtered"
        );
    }

    private int hoveredFilteredIndex() {
        if (this.hoveredModel == null) {
            return -1;
        }
        return this.filteredEntries.indexOf(this.hoveredModel.entry());
    }

    private static String sanitize(String raw) {
        return raw == null ? "" : raw.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private void renderScrollBar(DrawContext context) {
        int maxScrollRows = this.getMaxScrollRows();
        if (maxScrollRows <= 0) {
            this.scrollbarDragging = false;
            return;
        }

        ShopBrowserLayout.Layout activeLayout = this.activeLayout();
        this.scrollbarTrackX = this.width - 10;
        this.scrollbarTrackY = activeLayout.gridTop();
        this.scrollbarTrackHeight = Math.max(16, this.height - activeLayout.gridBottomPadding() - activeLayout.gridTop());
        int totalRows = Math.max(1, (int) Math.ceil(this.filteredEntries.size() / (double) this.getColumnCount()));
        int visibleRows = this.getVisibleRowCount();
        this.scrollbarThumbHeight = Math.max(12, (int) (this.scrollbarTrackHeight * (visibleRows / (double) totalRows)));
        int thumbTravel = Math.max(0, this.scrollbarTrackHeight - this.scrollbarThumbHeight);
        this.scrollbarThumbY = this.scrollbarTrackY + (maxScrollRows == 0 ? 0 : (int) Math.round(thumbTravel * (this.scrollRowOffset / (double) maxScrollRows)));

        context.fill(this.scrollbarTrackX, this.scrollbarTrackY, this.scrollbarTrackX + 4, this.scrollbarTrackY + this.scrollbarTrackHeight, 0x70302218);
        context.fill(this.scrollbarTrackX, this.scrollbarThumbY, this.scrollbarTrackX + 4, this.scrollbarThumbY + this.scrollbarThumbHeight, 0xD8E0B96A);
        context.drawBorder(this.scrollbarTrackX - 1, this.scrollbarTrackY - 1, 6, this.scrollbarTrackHeight + 2, 0x90523E2A);
    }

    private boolean isPointInsideScrollBar(double mouseX, double mouseY) {
        return mouseX >= this.scrollbarTrackX - 1
                && mouseX <= this.scrollbarTrackX + 5
                && mouseY >= this.scrollbarTrackY
                && mouseY <= this.scrollbarTrackY + this.scrollbarTrackHeight;
    }

    private void updateScrollFromThumbTop(int desiredThumbTop) {
        int maxScrollRows = this.getMaxScrollRows();
        if (maxScrollRows <= 0) {
            return;
        }

        int minThumbY = this.scrollbarTrackY;
        int maxThumbY = this.scrollbarTrackY + this.scrollbarTrackHeight - this.scrollbarThumbHeight;
        int clampedThumbY = MathHelper.clamp(desiredThumbTop, minThumbY, maxThumbY);
        double progress = maxThumbY == minThumbY ? 0.0D : (clampedThumbY - minThumbY) / (double) (maxThumbY - minThumbY);
        int nextOffset = (int) Math.round(progress * maxScrollRows);
        if (nextOffset != this.scrollRowOffset) {
            this.scrollRowOffset = nextOffset;
            this.sessionState.setScrollRowOffset(this.scrollRowOffset);
            this.updateVisibleWidgets();
        }
    }

    private void persistSessionState() {
        this.sessionState.setSearchText(this.searchField == null ? "" : this.searchField.getText());
        this.sessionState.setSortMode(this.sortMode);
        this.sessionState.setScrollRowOffset(this.scrollRowOffset);
    }

    private ShopEntryWidget.Model createModel(ShopSignEntry entry) {
        return ShopEntryWidget.Model.fromEntry(entry, PriceColorResolver.resolve(entry, this.itemPriceStats));
    }

    private Text sortModeLabel() {
        return Text.translatable("screen.pplshop.sort", Text.translatable(this.sortMode.translationKey()));
    }

    private void renderSnapshotSummary(DrawContext context) {
        RefreshUiState state = this.currentRefreshState();
        if (state.isLowEntrySnapshot()) {
            this.drawBanner(
                    context,
                    this.activeLayout().lowEntryHintY(),
                    Text.translatable("screen.pplshop.banner.low_entries_compact", state.entryCount(), state.minimumExpectedEntries()),
                    0xAA5E2A16,
                    0xFFFFD2A8
            );
        }
    }

    private void renderDiscordSupport(DrawContext context) {
        ShopBrowserLayout.Bounds bounds = this.activeLayout().discordSupportBounds();
        context.fill(bounds.x() - 1, bounds.y() - 1, bounds.x() + bounds.width() + 1, bounds.y() + bounds.height() + 1, 0xE05865F2);
        context.drawBorder(bounds.x() - 2, bounds.y() - 2, bounds.width() + 4, bounds.height() + 4, 0xFFE8EAFF);
        context.drawBorder(bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, 0xFF3742B8);
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                DISCORD_TEXTURE,
                bounds.x(),
                bounds.y(),
                0.0F,
                0.0F,
                bounds.width(),
                bounds.height(),
                DISCORD_TEXTURE_SIZE,
                DISCORD_TEXTURE_SIZE,
                DISCORD_TEXTURE_SIZE,
                DISCORD_TEXTURE_SIZE
        );
    }

    private boolean isDiscordSupportHovered(double mouseX, double mouseY) {
        return this.activeLayout().discordSupportBounds().contains(mouseX, mouseY);
    }

    private void renderDiscordSupportTooltip(DrawContext context, int mouseX, int mouseY) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.translatable("config.pplshop.discord.tooltip.issue"));
        tooltip.add(Text.translatable("config.pplshop.discord.tooltip.copy"));
        if (System.currentTimeMillis() - this.discordCopiedAt < 1800L) {
            tooltip.add(Text.translatable("config.pplshop.discord.tooltip.copied"));
        }
        context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
    }

    private void drawBanner(DrawContext context, int y, Text text, int background, int color) {
        int width = Math.min(this.width - 24, this.textRenderer.getWidth(text) + 16);
        int startX = (this.width - width) / 2;
        context.fill(startX, y - 2, startX + width, y + 10, background);
        context.drawBorder(startX, y - 2, width, 12, 0xA0603A20);
        context.drawCenteredTextWithShadow(this.textRenderer, text, this.width / 2, y, color);
    }

    private RefreshUiState currentRefreshState() {
        return this.refreshStateSupplier == null
                ? new RefreshUiState(ShopDataState.NEVER_REFRESHED, false, false, RefreshTriggerSource.NONE, 0L, Long.MAX_VALUE, 0L, this.shopCache.size(), 100, "", true)
                : this.refreshStateSupplier.get();
    }

    private ShopBrowserLayout.Layout activeLayout() {
        if (this.layout == null) {
            this.layout = this.computeLayout();
            this.statusBadge.setBounds(this.layout.statusBadgeBounds());
        }
        return this.layout;
    }

    private ShopBrowserLayout.Layout computeLayout() {
        return ShopBrowserLayout.calculate(
                this.textRenderer,
                this.width,
                this.height,
                Text.translatable("screen.pplshop.refresh"),
                this.widestSortModeLabel(),
                Text.translatable("screen.pplshop.clear_highlight")
        );
    }

    private Text widestSortModeLabel() {
        Text widest = this.sortModeLabel();
        int widestWidth = this.textRenderer.getWidth(widest);
        for (ShopSortMode mode : ShopSortMode.values()) {
            Text candidate = Text.translatable("screen.pplshop.sort", Text.translatable(mode.translationKey()));
            int candidateWidth = this.textRenderer.getWidth(candidate);
            if (candidateWidth > widestWidth) {
                widest = candidate;
                widestWidth = candidateWidth;
            }
        }
        return widest;
    }

    private ShopPriceSortBasis defaultPriceSortBasis() {
        return this.refreshUxConfig.preferredPriceBasis == null ? ShopPriceSortBasis.PER_UNIT : this.refreshUxConfig.preferredPriceBasis;
    }

    private record IndexedEntry(ShopSignEntry entry, String searchBlob, Set<ShopTheme> themes) {
    }
}
