package styy.pplShop.pplshop.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import styy.pplShop.pplshop.client.PplshopClient;
import styy.pplShop.pplshop.client.config.AutoRefreshMode;
import styy.pplShop.pplshop.client.config.PplShopConfigManager;
import styy.pplShop.pplshop.client.config.RefreshUxConfig;

import java.util.ArrayList;
import java.util.List;

public final class PplShopConfigScreen extends Screen {
    private final Screen parent;
    private final PplShopConfigManager configManager;
    private final RefreshUxConfig workingCopy;
    private final List<LabelRow> labelRows = new ArrayList<>();

    private TextFieldWidget minimumExpectedEntriesField;
    private TextFieldWidget refreshBudgetPerTickField;
    private TextFieldWidget maxRefreshBudgetPerTickField;

    public PplShopConfigScreen(Screen parent, PplShopConfigManager configManager) {
        super(Text.translatable("config.pplshop.title"));
        this.parent = parent;
        this.configManager = configManager;
        this.workingCopy = copyOf(configManager.refreshUxConfig());
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();
        this.labelRows.clear();

        int panelWidth = Math.min(560, this.width - 32);
        int labelWidth = Math.max(180, panelWidth - 170);
        int controlWidth = 140;
        int startX = (this.width - panelWidth) / 2;
        int controlX = startX + panelWidth - controlWidth;
        int y = 44;

        y = this.addBooleanRow(startX, controlX, labelWidth, y, Text.translatable("config.pplshop.show_stale_cache_warning"), Text.translatable("config.pplshop.show_stale_cache_warning.tooltip"), () -> this.workingCopy.showStaleCacheWarning = !this.workingCopy.showStaleCacheWarning, () -> this.workingCopy.showStaleCacheWarning);
        y = this.addEnumRow(startX, controlX, labelWidth, y, Text.translatable("config.pplshop.auto_refresh_mode"), Text.translatable("config.pplshop.auto_refresh_mode.tooltip"), this::cycleAutoRefreshMode, () -> Text.translatable(this.workingCopy.autoRefreshMode.translationKey()));
        y = this.addBooleanRow(startX, controlX, labelWidth, y, Text.translatable("config.pplshop.auto_refresh_if_snapshot_too_small"), Text.translatable("config.pplshop.auto_refresh_if_snapshot_too_small.tooltip"), () -> this.workingCopy.autoRefreshIfSnapshotTooSmall = !this.workingCopy.autoRefreshIfSnapshotTooSmall, () -> this.workingCopy.autoRefreshIfSnapshotTooSmall);
        y = this.addBooleanRow(startX, controlX, labelWidth, y, Text.translatable("config.pplshop.persist_cache_between_sessions"), Text.translatable("config.pplshop.persist_cache_between_sessions.tooltip"), () -> this.workingCopy.persistCacheBetweenSessions = !this.workingCopy.persistCacheBetweenSessions, () -> this.workingCopy.persistCacheBetweenSessions);
        y = this.addBooleanRow(startX, controlX, labelWidth, y, Text.translatable("config.pplshop.suppress_manual_refresh_warning"), Text.translatable("config.pplshop.suppress_manual_refresh_warning.tooltip"), () -> this.workingCopy.suppressManualRefreshWarning = !this.workingCopy.suppressManualRefreshWarning, () -> this.workingCopy.suppressManualRefreshWarning);
        y = this.addEnumRow(startX, controlX, labelWidth, y, Text.translatable("config.pplshop.preferred_price_basis"), Text.translatable("config.pplshop.preferred_price_basis.tooltip"), this::cyclePreferredPriceBasis, () -> Text.translatable(this.workingCopy.preferredPriceBasis.translationKey()));
        y = this.addNumberRow(startX, controlX, labelWidth, y, Text.translatable("config.pplshop.minimum_expected_entries"), Text.translatable("config.pplshop.minimum_expected_entries.tooltip"), String.valueOf(this.workingCopy.minimumExpectedEntries), field -> this.minimumExpectedEntriesField = field);
        y = this.addNumberRow(startX, controlX, labelWidth, y, Text.translatable("config.pplshop.refresh_budget_per_tick"), Text.translatable("config.pplshop.refresh_budget_per_tick.tooltip"), String.valueOf(this.workingCopy.refreshBudgetPerTick), field -> this.refreshBudgetPerTickField = field);
        y = this.addBooleanRow(startX, controlX, labelWidth, y, Text.translatable("config.pplshop.adaptive_refresh_budget"), Text.translatable("config.pplshop.adaptive_refresh_budget.tooltip"), () -> this.workingCopy.adaptiveRefreshBudget = !this.workingCopy.adaptiveRefreshBudget, () -> this.workingCopy.adaptiveRefreshBudget);
        y = this.addNumberRow(startX, controlX, labelWidth, y, Text.translatable("config.pplshop.max_refresh_budget_per_tick"), Text.translatable("config.pplshop.max_refresh_budget_per_tick.tooltip"), String.valueOf(this.workingCopy.maxRefreshBudgetPerTick), field -> this.maxRefreshBudgetPerTickField = field);

        int buttonY = Math.min(this.height - 28, y + 12);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.saveAndClose())
                .dimensions(this.width / 2 - 154, buttonY, 150, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> this.close())
                .dimensions(this.width / 2 + 4, buttonY, 150, 20)
                .build());
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, 0xFFF3E6C8);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("config.pplshop.subtitle"), this.width / 2, 28, 0xFFBEB7AA);

        int panelWidth = Math.min(560, this.width - 32);
        int panelHeight = Math.max(180, this.height - 76);
        int panelX = (this.width - panelWidth) / 2;
        context.fill(panelX, 38, panelX + panelWidth, 38 + panelHeight, 0xAA17120E);
        context.drawBorder(panelX, 38, panelWidth, panelHeight, 0xA04A3622);

        for (LabelRow row : this.labelRows) {
            String trimmed = this.textRenderer.trimToWidth(row.label().getString(), row.width());
            context.drawTextWithShadow(this.textRenderer, trimmed, row.x(), row.y() + 6, 0xFFF0E3C3);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private int addBooleanRow(int x, int controlX, int labelWidth, int y, Text label, Text tooltip, Runnable toggleAction, BooleanSupplier valueSupplier) {
        this.labelRows.add(new LabelRow(x + 8, y, labelWidth - 12, label));
        this.addDrawableChild(ButtonWidget.builder(toggleLabel(valueSupplier.getAsBoolean()), button -> {
                    toggleAction.run();
                    button.setMessage(toggleLabel(valueSupplier.getAsBoolean()));
                })
                .dimensions(controlX, y, 140, 20)
                .tooltip(Tooltip.of(tooltip))
                .build());
        return y + 24;
    }

    private int addEnumRow(int x, int controlX, int labelWidth, int y, Text label, Text tooltip, Runnable cycleAction, TextSupplier textSupplier) {
        this.labelRows.add(new LabelRow(x + 8, y, labelWidth - 12, label));
        this.addDrawableChild(ButtonWidget.builder(textSupplier.get(), button -> {
                    cycleAction.run();
                    button.setMessage(textSupplier.get());
                })
                .dimensions(controlX, y, 140, 20)
                .tooltip(Tooltip.of(tooltip))
                .build());
        return y + 24;
    }

    private int addNumberRow(int x, int controlX, int labelWidth, int y, Text label, Text tooltip, String initialValue, FieldConsumer consumer) {
        this.labelRows.add(new LabelRow(x + 8, y, labelWidth - 12, label));
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, controlX, y, 140, 20, Text.empty());
        field.setText(initialValue);
        field.setTooltip(Tooltip.of(tooltip));
        this.addDrawableChild(field);
        consumer.accept(field);
        return y + 24;
    }

    private void cycleAutoRefreshMode() {
        AutoRefreshMode[] values = AutoRefreshMode.values();
        this.workingCopy.autoRefreshMode = values[(this.workingCopy.autoRefreshMode.ordinal() + 1) % values.length];
    }

    private void cyclePreferredPriceBasis() {
        this.workingCopy.preferredPriceBasis = this.workingCopy.preferredPriceBasis.next();
    }

    private void saveAndClose() {
        this.workingCopy.minimumExpectedEntries = parseInt(this.minimumExpectedEntriesField.getText(), this.workingCopy.minimumExpectedEntries);
        this.workingCopy.refreshBudgetPerTick = parseInt(this.refreshBudgetPerTickField.getText(), this.workingCopy.refreshBudgetPerTick);
        this.workingCopy.maxRefreshBudgetPerTick = parseInt(this.maxRefreshBudgetPerTickField.getText(), this.workingCopy.maxRefreshBudgetPerTick);
        this.workingCopy.sanitize();
        this.configManager.saveRefreshUxConfig(this.workingCopy);
        if (PplshopClient.instance() != null) {
            PplshopClient.instance().onRefreshUxConfigChanged();
        }
        this.close();
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception exception) {
            return fallback;
        }
    }

    private static Text toggleLabel(boolean value) {
        return Text.translatable(value ? "config.pplshop.enabled" : "config.pplshop.disabled");
    }

    private static RefreshUxConfig copyOf(RefreshUxConfig source) {
        RefreshUxConfig copy = new RefreshUxConfig();
        copy.showStaleCacheWarning = source.showStaleCacheWarning;
        copy.autoRefreshMode = source.autoRefreshMode;
        copy.suppressManualRefreshWarning = source.suppressManualRefreshWarning;
        copy.autoRefreshIfSnapshotTooSmall = source.autoRefreshIfSnapshotTooSmall;
        copy.minimumExpectedEntries = source.minimumExpectedEntries;
        copy.persistCacheBetweenSessions = source.persistCacheBetweenSessions;
        copy.refreshBudgetPerTick = source.refreshBudgetPerTick;
        copy.adaptiveRefreshBudget = source.adaptiveRefreshBudget;
        copy.maxRefreshBudgetPerTick = source.maxRefreshBudgetPerTick;
        copy.preferredPriceBasis = source.preferredPriceBasis;
        copy.sanitize();
        return copy;
    }

    private record LabelRow(int x, int y, int width, Text label) {
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    @FunctionalInterface
    private interface TextSupplier {
        Text get();
    }

    @FunctionalInterface
    private interface FieldConsumer {
        void accept(TextFieldWidget field);
    }
}
