package styy.pplShop.pplshop.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import styy.pplShop.pplshop.client.PplshopClient;
import styy.pplShop.pplshop.client.config.AutoRefreshMode;
import styy.pplShop.pplshop.client.config.PplShopConfigManager;
import styy.pplShop.pplshop.client.config.RefreshUxConfig;

import java.util.ArrayList;
import java.util.List;

public final class PplShopConfigScreen extends Screen {
    private static final Identifier DISCORD_TEXTURE = Identifier.of("pplshop", "textures/gui/discord_support.png");
    private static final int DISCORD_ICON_SIZE = 24;
    private static final String DISCORD_CONTACT = "styy8";

    private final Screen parent;
    private final PplShopConfigManager configManager;
    private final RefreshUxConfig workingCopy;
    private final List<LabelRow> labelRows = new ArrayList<>();

    private TextFieldWidget minimumExpectedEntriesField;
    private TextFieldWidget refreshBudgetPerTickField;
    private int discordIconX;
    private int discordIconY;
    private long discordCopiedAt;

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
        this.renderDiscordSupport(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isDiscordSupportHovered(mouseX, mouseY)) {
            MinecraftClient.getInstance().keyboard.setClipboard(DISCORD_CONTACT);
            this.discordCopiedAt = System.currentTimeMillis();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    private void renderDiscordSupport(DrawContext context, int mouseX, int mouseY) {
        this.discordIconX = this.width - DISCORD_ICON_SIZE - 10;
        this.discordIconY = this.height - DISCORD_ICON_SIZE - 10;
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                DISCORD_TEXTURE,
                this.discordIconX,
                this.discordIconY,
                0.0F,
                0.0F,
                DISCORD_ICON_SIZE,
                DISCORD_ICON_SIZE,
                DISCORD_ICON_SIZE,
                DISCORD_ICON_SIZE
        );
        if (this.isDiscordSupportHovered(mouseX, mouseY)) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.translatable("config.pplshop.discord.tooltip.issue"));
            tooltip.add(Text.translatable("config.pplshop.discord.tooltip.copy"));
            tooltip.add(Text.literal(DISCORD_CONTACT));
            if (System.currentTimeMillis() - this.discordCopiedAt < 1800L) {
                tooltip.add(Text.translatable("config.pplshop.discord.tooltip.copied"));
            }
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private boolean isDiscordSupportHovered(double mouseX, double mouseY) {
        return mouseX >= this.discordIconX
                && mouseX <= this.discordIconX + DISCORD_ICON_SIZE
                && mouseY >= this.discordIconY
                && mouseY <= this.discordIconY + DISCORD_ICON_SIZE;
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
