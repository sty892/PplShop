package styy.pplShop.pplshop.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import styy.pplShop.pplshop.client.world.RefreshUiState;
import styy.pplShop.pplshop.client.world.ShopDataState;

import java.util.ArrayList;
import java.util.List;

public final class RefreshStatusBadge {
    private ShopBrowserLayout.Bounds bounds = new ShopBrowserLayout.Bounds(0, 0, 0, 0);

    public void setBounds(ShopBrowserLayout.Bounds bounds) {
        this.bounds = bounds == null ? new ShopBrowserLayout.Bounds(0, 0, 0, 0) : bounds;
    }

    public void render(DrawContext context, RefreshUiState state) {
        VisualState visualState = VisualState.from(state);
        int x = this.bounds.x();
        int y = this.bounds.y();
        int width = this.bounds.width();
        int height = this.bounds.height();

        context.fill(x, y, x + width, y + height, visualState.fillColor);
        context.drawBorder(x - 1, y - 1, width + 2, height + 2, visualState.borderColor);
        this.drawClockIcon(context, x, y, width, height, visualState.iconColor);

        if (state != null && state.refreshInProgress()) {
            context.fill(x + width - 4, y + 2, x + width - 2, y + 4, 0xFFF7F1B3);
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.bounds.contains(mouseX, mouseY);
    }

    public void renderTooltip(DrawContext context, TextRenderer textRenderer, RefreshUiState state, int mouseX, int mouseY) {
        context.drawTooltip(textRenderer, this.buildTooltip(state), mouseX, mouseY);
    }

    private List<Text> buildTooltip(RefreshUiState state) {
        VisualState visualState = VisualState.from(state);
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.translatable(visualState.titleKey).formatted(visualState.formatting));
        tooltip.add(Text.translatable("screen.pplshop.status_badge.tooltip.updated", this.formatAge(state == null ? Long.MAX_VALUE : state.snapshotAgeMillis())).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(visualState.recommendationKey).formatted(Formatting.DARK_GRAY));
        if (state != null && state.isLowEntrySnapshot()) {
            tooltip.add(Text.translatable("screen.pplshop.status_badge.tooltip.low_entries_player", state.entryCount(), state.minimumExpectedEntries()).formatted(Formatting.GRAY));
        }
        if (state != null && state.refreshInProgress()) {
            tooltip.add(Text.translatable("screen.pplshop.status_badge.tooltip.refreshing_player").formatted(Formatting.GRAY));
        }
        return tooltip;
    }

    private String formatAge(long ageMillis) {
        if (ageMillis == Long.MAX_VALUE || ageMillis < 0L) {
            return Text.translatable("screen.pplshop.status_badge.age.unknown").getString();
        }

        long minutes = Math.max(1L, ageMillis / 60_000L);
        if (minutes < 60L) {
            return Text.translatable("screen.pplshop.status_badge.age.minutes", minutes).getString();
        }
        long hours = Math.max(1L, minutes / 60L);
        if (hours < 48L) {
            return Text.translatable("screen.pplshop.status_badge.age.hours", hours).getString();
        }
        long days = Math.max(1L, hours / 24L);
        return Text.translatable("screen.pplshop.status_badge.age.days", days).getString();
    }

    private void drawClockIcon(DrawContext context, int x, int y, int width, int height, int color) {
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        context.fill(centerX - 3, centerY - 4, centerX + 3, centerY - 3, color);
        context.fill(centerX - 4, centerY - 3, centerX - 3, centerY + 3, color);
        context.fill(centerX + 3, centerY - 3, centerX + 4, centerY + 3, color);
        context.fill(centerX - 3, centerY + 3, centerX + 3, centerY + 4, color);
        context.fill(centerX, centerY - 2, centerX + 1, centerY + 1, color);
        context.fill(centerX, centerY, centerX + 3, centerY + 1, color);
    }

    private enum VisualState {
        UNKNOWN("screen.pplshop.status_badge.unknown", "screen.pplshop.status_badge.tooltip.recommend.unknown_player", Formatting.GRAY, 0xCC6E727A, 0xFFAFB6C1, 0xFFF5F7FA),
        FRESH("screen.pplshop.status_badge.fresh", "screen.pplshop.status_badge.tooltip.recommend.fresh_player", Formatting.GREEN, 0xCC2F7A3D, 0xFF94DB8E, 0xFFF1FFF1),
        AGING("screen.pplshop.status_badge.aging", "screen.pplshop.status_badge.tooltip.recommend.aging_player", Formatting.YELLOW, 0xCC95731E, 0xFFF5D56E, 0xFFFFF8D9),
        STALE("screen.pplshop.status_badge.stale", "screen.pplshop.status_badge.tooltip.recommend.stale_player", Formatting.RED, 0xCC8D2F27, 0xFFF59A8B, 0xFFFFF0EC);

        private final String titleKey;
        private final String recommendationKey;
        private final Formatting formatting;
        private final int fillColor;
        private final int borderColor;
        private final int iconColor;

        VisualState(String titleKey, String recommendationKey, Formatting formatting, int fillColor, int borderColor, int iconColor) {
            this.titleKey = titleKey;
            this.recommendationKey = recommendationKey;
            this.formatting = formatting;
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            this.iconColor = iconColor;
        }

        private static VisualState from(RefreshUiState state) {
            if (state == null || state.dataState() == ShopDataState.NEVER_REFRESHED || !state.hasRefreshHistory()) {
                return UNKNOWN;
            }
            if (state.dataState() == ShopDataState.STALE || state.isLowEntrySnapshot()) {
                return STALE;
            }
            if (state.refreshInProgress() || state.dataState() == ShopDataState.AGING) {
                return AGING;
            }
            return FRESH;
        }
    }
}
