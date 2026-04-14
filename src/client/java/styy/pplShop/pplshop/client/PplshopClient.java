package styy.pplShop.pplshop.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import styy.pplShop.pplshop.client.config.PplShopConfigManager;
import styy.pplShop.pplshop.client.config.RefreshUxConfig;
import styy.pplShop.pplshop.client.debug.SignExportService;
import styy.pplShop.pplshop.client.gui.PplShopConfigScreen;
import styy.pplShop.pplshop.client.gui.ShopBrowserScreen;
import styy.pplShop.pplshop.client.gui.ShopBrowserSessionState;
import styy.pplShop.pplshop.client.gui.ShopEntryComparator;
import styy.pplShop.pplshop.client.model.ShopSignDiagnosticReason;
import styy.pplShop.pplshop.client.parser.ShopSignParser;
import styy.pplShop.pplshop.client.world.HighlightManager;
import styy.pplShop.pplshop.client.world.LoadedSignScanner;
import styy.pplShop.pplshop.client.world.RefreshTriggerSource;
import styy.pplShop.pplshop.client.world.RefreshUiState;
import styy.pplShop.pplshop.client.world.ShopCache;
import styy.pplShop.pplshop.client.world.ShopCachePersistenceService;
import styy.pplShop.pplshop.client.world.ShopDataState;
import styy.pplShop.pplshop.client.world.ShopSnapshotMetadata;
import styy.pplShop.pplshop.client.world.SnapshotFreshnessPolicy;
import styy.pplShop.pplshop.client.world.WorldCacheIdentity;

import java.io.IOException;
import java.nio.file.Path;

public class PplshopClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PPLShop");
    private static final long MANUAL_REFRESH_DEBOUNCE_MS = 750L;
    private static final long MANUAL_REFRESH_CONFIRM_WINDOW_MS = 5_000L;
    private static final long CACHE_PERSIST_DEBOUNCE_TICKS = 40L;
    private static final long GUI_AUTO_REFRESH_READY_DELAY_TICKS = 60L;
    private static final int HIDDEN_DEBUG_PRIMARY_KEY = GLFW.GLFW_KEY_DELETE;
    private static final int HIDDEN_DEBUG_SECONDARY_KEY = GLFW.GLFW_KEY_ENTER;
    private static final int HIDDEN_DEBUG_SECONDARY_KEYPAD = GLFW.GLFW_KEY_KP_ENTER;
    private static PplshopClient INSTANCE;

    private final PplShopConfigManager configManager = new PplShopConfigManager();
    private final LoadedSignScanner loadedSignScanner = new LoadedSignScanner();
    private final ShopCache shopCache = new ShopCache();
    private final HighlightManager highlightManager = new HighlightManager();
    private final SignExportService signExportService = new SignExportService();
    private final ShopCachePersistenceService cachePersistenceService = new ShopCachePersistenceService();
    private final SnapshotFreshnessPolicy snapshotFreshnessPolicy = new SnapshotFreshnessPolicy();
    private final ShopBrowserSessionState browserSessionState = new ShopBrowserSessionState();

    private ShopSignParser parser;
    private KeyBinding openMenuKeyBinding;
    private KeyBinding refreshCacheKeyBinding;
    private ClientWorld observedWorld;
    private Identifier observedDimensionId;
    private WorldCacheIdentity activeWorldIdentity;
    private ShopSnapshotMetadata snapshotMetadata = new ShopSnapshotMetadata(1, "", "", "", "", 0L, 0, 0, 0);
    private LoadedSignScanner.RefreshJob activeRefreshJob;
    private boolean snapshotLoadedFromDisk;
    private boolean hiddenDebugComboActive;
    private long lastManualRefreshRequestAt;
    private long manualRefreshConfirmDeadlineAt;
    private long lastRefreshCompletedAt;
    private long lastPersistedCacheVersion = Long.MIN_VALUE;
    private long lastPersistAttemptTick;
    private long lastLowEntryAutoRefreshTick = Long.MIN_VALUE;
    private long worldEnteredAtTick;
    private long tickCounter;
    private RefreshTriggerSource refreshTriggerSource = RefreshTriggerSource.NONE;
    private RefreshUiState cachedRefreshUiState;

    public static PplshopClient instance() {
        return INSTANCE;
    }

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        this.configManager.loadAll();
        this.rebuildParser();

        this.openMenuKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pplshop.open_browser",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.pplshop"
        ));
        this.refreshCacheKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pplshop.refresh_cache",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.pplshop"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(this::onChunkUnload);
        WorldRenderEvents.LAST.register(this.highlightManager::render);
        LOGGER.info("Initialized PPLShop client");
    }

    public Screen createConfigScreen(Screen parent) {
        return new PplShopConfigScreen(parent, this.configManager);
    }

    private void onClientTick(MinecraftClient client) {
        this.tickCounter++;
        this.highlightManager.tick(client);

        boolean configChanged = this.configManager.reloadIfChanged();
        if (configChanged) {
            this.rebuildParser();
            if (client.world != null && client.player != null) {
                this.loadedSignScanner.markWorldInvalidated("config_reload");
            }
            this.refreshCachedUiState();
        }

        this.refreshCacheOnWorldChange(client);
        this.tickRefreshLifecycle(client);
        this.handleHiddenDebugCombo(client);

        while (this.refreshCacheKeyBinding.wasPressed()) {
            this.requestManualRefresh(client);
        }

        while (this.openMenuKeyBinding.wasPressed()) {
            this.openShopMenu(client);
        }
    }

    private void onChunkLoad(ClientWorld world, WorldChunk chunk) {
        if (this.observedWorld != null && world == this.observedWorld && !this.loadedSignScanner.isWorldDirty()) {
            this.loadedSignScanner.markWorldInvalidated("chunk_load");
        }
    }

    private void onChunkUnload(ClientWorld world, WorldChunk chunk) {
        if (this.observedWorld != null && world == this.observedWorld && !this.loadedSignScanner.isWorldDirty()) {
            this.loadedSignScanner.markWorldInvalidated("chunk_unload");
        }
    }

    private void refreshCacheOnWorldChange(MinecraftClient client) {
        if (client.world == null || client.player == null || this.parser == null) {
            if (this.observedWorld != null) {
                this.persistSnapshotIfNeeded(true);
            }
            this.observedWorld = null;
            this.observedDimensionId = null;
            this.activeWorldIdentity = null;
            this.snapshotMetadata = new ShopSnapshotMetadata(1, "", "", "", "", 0L, 0, 0, 0);
            this.snapshotLoadedFromDisk = false;
            this.activeRefreshJob = null;
            this.refreshTriggerSource = RefreshTriggerSource.NONE;
            this.manualRefreshConfirmDeadlineAt = 0L;
            this.shopCache.clear();
            this.highlightManager.clear();
            this.loadedSignScanner.clearParsedEntryCache();
            this.lastPersistedCacheVersion = Long.MIN_VALUE;
            this.cachedRefreshUiState = null;
            this.browserSessionState.clear();
            return;
        }

        Identifier currentDimensionId = client.world.getRegistryKey().getValue();
        if (this.observedWorld == client.world && currentDimensionId.equals(this.observedDimensionId)) {
            return;
        }

        if (this.observedWorld != null) {
            this.persistSnapshotIfNeeded(true);
        }

        this.observedWorld = client.world;
        this.observedDimensionId = currentDimensionId;
        this.activeWorldIdentity = WorldCacheIdentity.resolve(client);
        this.highlightManager.clear();
        this.browserSessionState.clear();
        this.loadedSignScanner.clearParsedEntryCache();
        this.activeRefreshJob = null;
        this.refreshTriggerSource = RefreshTriggerSource.NONE;
        this.manualRefreshConfirmDeadlineAt = 0L;
        this.lastLowEntryAutoRefreshTick = Long.MIN_VALUE;
        this.worldEnteredAtTick = this.tickCounter;

        RefreshUxConfig refreshConfig = this.configManager.refreshUxConfig();
        ShopCachePersistenceService.PersistedSnapshot persistedSnapshot = refreshConfig.persistCacheBetweenSessions
                ? this.cachePersistenceService.load(this.activeWorldIdentity)
                : ShopCachePersistenceService.PersistedSnapshot.empty();
        if (persistedSnapshot.loadedFromDisk() && !persistedSnapshot.entries().isEmpty()) {
            this.shopCache.replaceAll(persistedSnapshot.entries());
            this.snapshotMetadata = persistedSnapshot.metadata();
            this.snapshotLoadedFromDisk = true;
            this.lastRefreshCompletedAt = persistedSnapshot.metadata().lastRefreshTimeMillis();
        } else {
            this.shopCache.clear();
            this.snapshotMetadata = this.cachePersistenceService.buildMetadata(this.activeWorldIdentity, 0L, 0, 0, 0);
            this.snapshotLoadedFromDisk = false;
            this.lastRefreshCompletedAt = 0L;
        }
        this.lastPersistedCacheVersion = this.shopCache.version();
        this.refreshCachedUiState();
    }

    private void tickRefreshLifecycle(MinecraftClient client) {
        if (client.world == null || client.player == null || this.activeWorldIdentity == null) {
            return;
        }

        if (this.activeRefreshJob != null) {
            this.activeRefreshJob.step(this.configManager.refreshUxConfig().refreshBudgetPerTick);
            if (this.activeRefreshJob.isDone()) {
                this.finishRefresh(client);
            }
        }

        this.persistSnapshotIfNeeded(false);
    }

    private void requestManualRefresh(MinecraftClient client) {
        if (client.world == null || client.player == null || this.parser == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastManualRefreshRequestAt < MANUAL_REFRESH_DEBOUNCE_MS) {
            return;
        }
        this.lastManualRefreshRequestAt = now;

        if (this.activeRefreshJob != null) {
            client.player.sendMessage(Text.translatable("message.pplshop.refresh_in_progress"), false);
            return;
        }

        RefreshUxConfig refreshConfig = this.configManager.refreshUxConfig();
        if (!refreshConfig.suppressManualRefreshWarning && now > this.manualRefreshConfirmDeadlineAt) {
            this.manualRefreshConfirmDeadlineAt = now + MANUAL_REFRESH_CONFIRM_WINDOW_MS;
            client.player.sendMessage(Text.translatable("message.pplshop.refresh_confirm"), false);
            return;
        }

        this.manualRefreshConfirmDeadlineAt = 0L;
        this.startRefresh(client, RefreshTriggerSource.MANUAL_CONFIRMED, true);
    }

    private void requestGuiRefresh(MinecraftClient client) {
        if (client.world == null || client.player == null || this.parser == null) {
            return;
        }
        this.startRefresh(client, RefreshTriggerSource.GUI_BUTTON, true);
    }

    private void startRefresh(MinecraftClient client, RefreshTriggerSource triggerSource, boolean announceStart) {
        if (client.world == null || client.player == null || this.parser == null) {
            return;
        }
        if (this.activeRefreshJob != null) {
            client.player.sendMessage(Text.translatable("message.pplshop.refresh_in_progress"), false);
            return;
        }

        this.activeRefreshJob = this.loadedSignScanner.beginRefresh(
                client,
                this.parser,
                this.configManager.parserRulesConfig(),
                this.tickCounter,
                ShopSignDiagnosticReason.FULL_REBUILD
        );
        this.refreshTriggerSource = triggerSource == null ? RefreshTriggerSource.NONE : triggerSource;
        this.refreshCachedUiState();
        if (announceStart && client.player != null) {
            client.player.sendMessage(Text.translatable("message.pplshop.refresh_started"), false);
        }
    }

    private void finishRefresh(MinecraftClient client) {
        if (this.activeRefreshJob == null) {
            return;
        }

        this.shopCache.replaceAll(this.activeRefreshJob.entries());
        this.loadedSignScanner.detachJob(this.activeRefreshJob);
        this.activeRefreshJob = null;
        this.loadedSignScanner.clearWorldDirty();
        this.lastRefreshCompletedAt = System.currentTimeMillis();
        this.snapshotLoadedFromDisk = false;
        this.snapshotMetadata = this.cachePersistenceService.buildMetadata(
                this.activeWorldIdentity,
                this.lastRefreshCompletedAt,
                this.shopCache.size(),
                this.loadedSignScanner.lastCollectedChunkCount(),
                this.loadedSignScanner.lastCollectedSignCount()
        );
        this.persistSnapshotIfNeeded(true);

        if (client.player != null) {
            long unresolvedCount = this.shopCache.snapshot().stream().filter(ShopEntryComparator::isUnknownEntry).count();
            client.player.sendMessage(Text.translatable("message.pplshop.refresh_finished", this.shopCache.size(), unresolvedCount), false);
        }
        this.refreshTriggerSource = RefreshTriggerSource.NONE;
        this.refreshCachedUiState();
    }

    private void persistSnapshotIfNeeded(boolean force) {
        if (this.activeWorldIdentity == null || !this.configManager.refreshUxConfig().persistCacheBetweenSessions) {
            return;
        }
        if (!force) {
            if (this.shopCache.version() == this.lastPersistedCacheVersion) {
                return;
            }
            if (this.tickCounter - this.lastPersistAttemptTick < CACHE_PERSIST_DEBOUNCE_TICKS) {
                return;
            }
        }

        long effectiveRefreshTime = this.lastRefreshCompletedAt > 0L
                ? this.lastRefreshCompletedAt
                : this.snapshotMetadata.lastRefreshTimeMillis();
        ShopSnapshotMetadata metadata = this.cachePersistenceService.buildMetadata(
                this.activeWorldIdentity,
                effectiveRefreshTime,
                this.shopCache.size(),
                this.snapshotMetadata.lastKnownChunkCount(),
                this.snapshotMetadata.lastKnownSignCount()
        );
        this.cachePersistenceService.save(this.activeWorldIdentity, metadata, this.shopCache.snapshot());
        this.snapshotMetadata = metadata;
        this.lastPersistedCacheVersion = this.shopCache.version();
        this.lastPersistAttemptTick = this.tickCounter;
    }

    private void exportFullDiagnosticDump(MinecraftClient client) {
        try {
            SignExportService.GuiDebugSnapshot guiSnapshot = client.currentScreen instanceof ShopBrowserScreen screen
                    ? screen.captureGuiDebugSnapshot()
                    : this.signExportService.createCacheGuiSnapshot(this.shopCache.snapshot());
            Path exportPath = this.signExportService.exportFullDiagnostics(
                    this.shopCache.snapshot(),
                    guiSnapshot,
                    this.loadedSignScanner.debugSnapshot(),
                    this.buildRefreshUiState(),
                    this.snapshotMetadata,
                    this.highlightManager.debugSnapshot()
            );
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("message.pplshop.debug_dump_saved", exportPath.toAbsolutePath().toString()), false);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to export diagnostic dump", exception);
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("message.pplshop.debug_dump_failed", SignExportService.describeFailure(exception)), false);
            }
        }
    }

    private void handleHiddenDebugCombo(MinecraftClient client) {
        if (client.getWindow() == null) {
            return;
        }

        long handle = client.getWindow().getHandle();
        boolean deletePressed = InputUtil.isKeyPressed(handle, HIDDEN_DEBUG_PRIMARY_KEY);
        boolean enterPressed = InputUtil.isKeyPressed(handle, HIDDEN_DEBUG_SECONDARY_KEY)
                || InputUtil.isKeyPressed(handle, HIDDEN_DEBUG_SECONDARY_KEYPAD);
        boolean comboPressed = deletePressed && enterPressed;
        if (!comboPressed) {
            this.hiddenDebugComboActive = false;
            return;
        }
        if (this.hiddenDebugComboActive) {
            return;
        }

        this.hiddenDebugComboActive = true;
        this.exportFullDiagnosticDump(client);
    }

    private void rebuildParser() {
        this.loadedSignScanner.clearParsedEntryCache();
        this.activeRefreshJob = null;
        this.refreshTriggerSource = RefreshTriggerSource.NONE;
        this.parser = new ShopSignParser(
                this.configManager.itemAliasConfig(),
                this.configManager.currencyAliasConfig(),
                this.configManager.parserRulesConfig()
        );
        this.highlightManager.setAutoClearDistance(this.configManager.parserRulesConfig().highlight_clear_distance);
        this.refreshCachedUiState();
    }

    public void onRefreshUxConfigChanged() {
        this.refreshCachedUiState();
    }

    private void openShopMenu(MinecraftClient client) {
        if (client.player == null || this.parser == null) {
            return;
        }

        this.maybeAutoRefreshFromGui(client);
        client.setScreen(new ShopBrowserScreen(
                this.shopCache,
                this.highlightManager,
                this.configManager.itemAliasConfig(),
                this.configManager.currencyAliasConfig(),
                this.configManager.parserRulesConfig(),
                this.configManager.refreshUxConfig(),
                this.browserSessionState,
                this::buildRefreshUiState,
                () -> this.requestGuiRefresh(MinecraftClient.getInstance())
        ));
    }

    private void maybeAutoRefreshFromGui(MinecraftClient client) {
        RefreshUxConfig refreshConfig = this.configManager.refreshUxConfig();
        if (this.activeRefreshJob != null || this.tickCounter - this.worldEnteredAtTick < GUI_AUTO_REFRESH_READY_DELAY_TICKS) {
            return;
        }

        long lastRefreshTime = this.lastRefreshCompletedAt > 0L
                ? this.lastRefreshCompletedAt
                : this.snapshotMetadata.lastRefreshTimeMillis();
        boolean lowSnapshot = this.shopCache.size() < refreshConfig.minimumExpectedEntries;
        boolean lowSnapshotAutoRefresh = refreshConfig.autoRefreshIfSnapshotTooSmall && lowSnapshot;
        boolean scheduledRefresh = refreshConfig.autoRefreshMode.shouldRefreshOnGuiOpen(lastRefreshTime, System.currentTimeMillis());
        if (!lowSnapshotAutoRefresh && !scheduledRefresh) {
            return;
        }
        if (this.tickCounter - this.lastLowEntryAutoRefreshTick < GUI_AUTO_REFRESH_READY_DELAY_TICKS) {
            return;
        }

        this.lastLowEntryAutoRefreshTick = this.tickCounter;
        this.startRefresh(client, lowSnapshotAutoRefresh ? RefreshTriggerSource.GUI_LOW_ENTRY_AUTOREFRESH : RefreshTriggerSource.SCHEDULED, false);
    }

    private RefreshUiState buildRefreshUiState() {
        if (this.cachedRefreshUiState == null) {
            this.refreshCachedUiState();
        }
        return this.cachedRefreshUiState;
    }

    private void refreshCachedUiState() {
        RefreshUxConfig refreshConfig = this.configManager.refreshUxConfig();
        long lastRefreshTime = this.lastRefreshCompletedAt > 0L
                ? this.lastRefreshCompletedAt
                : this.snapshotMetadata.lastRefreshTimeMillis();
        long ageMillis = this.snapshotFreshnessPolicy.snapshotAge(lastRefreshTime);
        ShopDataState dataState = this.snapshotFreshnessPolicy.classify(
                lastRefreshTime,
                this.activeRefreshJob != null,
                this.loadedSignScanner.isWorldDirty()
        );
        String worldDisplay = this.activeWorldIdentity != null
                ? this.activeWorldIdentity.displayName()
                : this.snapshotMetadata.worldDisplayName();
        this.cachedRefreshUiState = new RefreshUiState(
                dataState,
                this.snapshotLoadedFromDisk,
                this.activeRefreshJob != null,
                this.refreshTriggerSource,
                lastRefreshTime,
                ageMillis,
                this.snapshotFreshnessPolicy.staleThresholdMillis(),
                this.shopCache.size(),
                refreshConfig.minimumExpectedEntries,
                worldDisplay,
                refreshConfig.showStaleCacheWarning
        );
    }
}
