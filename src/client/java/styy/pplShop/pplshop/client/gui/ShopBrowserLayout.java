package styy.pplShop.pplshop.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class ShopBrowserLayout {
    private static final int OUTER_PADDING = 10;
    private static final int TOP_BAR_Y = 20;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_GAP = 8;
    private static final int TITLE_Y = 6;
    private static final int FOUND_Y = 48;
    private static final int LOW_ENTRY_HINT_Y = 62;
    private static final int GRID_TOP = 80;
    private static final int GRID_BOTTOM_PADDING = 10;
    private static final int STATUS_BADGE_SIZE = 14;

    private static final int REFRESH_MIN_WIDTH = 82;
    private static final int REFRESH_MAX_WIDTH = 126;
    private static final int SORT_MIN_WIDTH = 108;
    private static final int SORT_MAX_WIDTH = 176;
    private static final int CLEAR_MIN_WIDTH = 112;
    private static final int CLEAR_MAX_WIDTH = 164;
    private static final int SEARCH_MIN_WIDTH = 190;
    private static final int SEARCH_PREFERRED_WIDTH = 248;
    private static final int SEARCH_MAX_WIDTH = 280;

    private ShopBrowserLayout() {
    }

    public static Layout calculate(TextRenderer textRenderer, int screenWidth, int screenHeight, Text refreshLabel, Text sortLabel, Text clearLabel) {
        int barX = OUTER_PADDING;
        int barY = TOP_BAR_Y;
        int barWidth = Math.max(0, screenWidth - (OUTER_PADDING * 2));

        int refreshWidth = preferredWidth(textRenderer, refreshLabel, REFRESH_MIN_WIDTH, REFRESH_MAX_WIDTH);
        int sortWidth = preferredWidth(textRenderer, sortLabel, SORT_MIN_WIDTH, SORT_MAX_WIDTH);
        int clearWidth = preferredWidth(textRenderer, clearLabel, CLEAR_MIN_WIDTH, CLEAR_MAX_WIDTH);
        Widths widths = fitToAvailableWidth(barWidth, refreshWidth, sortWidth, clearWidth);

        int leftLimit = barX + widths.refreshWidth() + CONTROL_GAP;
        int clearX = barX + barWidth - widths.clearWidth();
        int maxSearchWidth = clearX - CONTROL_GAP - widths.sortWidth() - CONTROL_GAP - leftLimit;
        int searchWidth = maxSearchWidth <= SEARCH_MIN_WIDTH
                ? Math.max(96, maxSearchWidth)
                : Math.min(SEARCH_MAX_WIDTH, Math.min(maxSearchWidth, SEARCH_PREFERRED_WIDTH));
        int idealSearchX = (screenWidth - searchWidth) / 2;
        int minSearchX = leftLimit;
        int maxSearchX = clearX - CONTROL_GAP - widths.sortWidth() - CONTROL_GAP - searchWidth;
        int searchX = MathHelper.clamp(idealSearchX, minSearchX, Math.max(minSearchX, maxSearchX));
        int refreshX = searchX - CONTROL_GAP - widths.refreshWidth();
        int sortX = searchX + searchWidth + CONTROL_GAP;

        Bounds refreshBounds = new Bounds(refreshX, barY, widths.refreshWidth(), CONTROL_HEIGHT);
        Bounds searchBounds = new Bounds(searchX, barY, Math.max(0, searchWidth), CONTROL_HEIGHT);
        Bounds sortBounds = new Bounds(sortX, barY, Math.max(0, widths.sortWidth()), CONTROL_HEIGHT);
        Bounds clearBounds = new Bounds(clearX, barY, widths.clearWidth(), CONTROL_HEIGHT);
        Bounds statusBadgeBounds = new Bounds(screenWidth - OUTER_PADDING - STATUS_BADGE_SIZE - 8, screenHeight - OUTER_PADDING - STATUS_BADGE_SIZE, STATUS_BADGE_SIZE, STATUS_BADGE_SIZE);
        Bounds discordSupportBounds = new Bounds(statusBadgeBounds.x() - CONTROL_GAP - STATUS_BADGE_SIZE, statusBadgeBounds.y(), STATUS_BADGE_SIZE, STATUS_BADGE_SIZE);

        return new Layout(refreshBounds, searchBounds, sortBounds, clearBounds, statusBadgeBounds, discordSupportBounds, TITLE_Y, FOUND_Y, LOW_ENTRY_HINT_Y, GRID_TOP, GRID_BOTTOM_PADDING);
    }

    private static int preferredWidth(TextRenderer textRenderer, Text label, int minWidth, int maxWidth) {
        int measuredWidth = textRenderer.getWidth(label) + 18;
        return Math.max(minWidth, Math.min(maxWidth, measuredWidth));
    }

    private static Widths fitToAvailableWidth(int barWidth, int refreshWidth, int sortWidth, int clearWidth) {
        int totalGaps = CONTROL_GAP * 3;
        int maxControlWidth = Math.max(REFRESH_MIN_WIDTH + SORT_MIN_WIDTH + CLEAR_MIN_WIDTH, barWidth - totalGaps - SEARCH_MIN_WIDTH);
        int currentWidth = refreshWidth + sortWidth + clearWidth;
        if (currentWidth <= maxControlWidth) {
            return new Widths(refreshWidth, sortWidth, clearWidth);
        }

        int overflow = currentWidth - maxControlWidth;
        int nextClearWidth = reduceWidth(clearWidth, CLEAR_MIN_WIDTH, overflow);
        overflow -= clearWidth - nextClearWidth;
        clearWidth = nextClearWidth;

        int nextSortWidth = reduceWidth(sortWidth, SORT_MIN_WIDTH, overflow);
        overflow -= sortWidth - nextSortWidth;
        sortWidth = nextSortWidth;

        refreshWidth = reduceWidth(refreshWidth, REFRESH_MIN_WIDTH, overflow);
        return new Widths(refreshWidth, sortWidth, clearWidth);
    }

    private static int reduceWidth(int currentWidth, int minWidth, int overflow) {
        if (overflow <= 0 || currentWidth <= minWidth) {
            return currentWidth;
        }
        int reducible = currentWidth - minWidth;
        return currentWidth - Math.min(reducible, overflow);
    }

    public record Layout(
            Bounds refreshBounds,
            Bounds searchBounds,
            Bounds sortBounds,
            Bounds clearBounds,
            Bounds statusBadgeBounds,
            Bounds discordSupportBounds,
            int titleY,
            int foundY,
            int lowEntryHintY,
            int gridTop,
            int gridBottomPadding
    ) {
    }

    public record Bounds(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private record Widths(int refreshWidth, int sortWidth, int clearWidth) {
    }
}
