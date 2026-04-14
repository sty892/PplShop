package styy.pplShop.pplshop.client.gui;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import styy.pplShop.pplshop.client.model.ShopSignEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PriceColorResolver {
    private static final int NEUTRAL_CARD_OVERLAY = 0x00000000;
    private static final int NEUTRAL_BORDER = 0xFF66513A;
    private static final int NEUTRAL_PRICE_TEXT = 0xFFE8D49A;
    private static final int CARD_CHEAP = 0x342A6A2E;
    private static final int CARD_EXPENSIVE = 0x343E201C;
    private static final int BORDER_CHEAP = 0xFF6BA36B;
    private static final int BORDER_EXPENSIVE = 0xFFB66E63;
    private static final int PRICE_CHEAP = 0xFF9FDE9C;
    private static final int PRICE_EXPENSIVE = 0xFFF0A38F;

    private PriceColorResolver() {
    }

    public static Map<Identifier, ItemPriceStats> buildStats(List<ShopSignEntry> entries) {
        Map<Identifier, MutableStats> collected = new HashMap<>();
        for (ShopSignEntry entry : entries) {
            if (entry == null || ShopEntryComparator.isUnknownEntry(entry) || entry.resolvedItemId() == null || entry.parsedPrice().amount() == null) {
                continue;
            }
            collected.computeIfAbsent(entry.resolvedItemId(), ignored -> new MutableStats())
                    .accept(entry.parsedPrice().amount());
        }

        Map<Identifier, ItemPriceStats> result = new HashMap<>();
        for (Map.Entry<Identifier, MutableStats> entry : collected.entrySet()) {
            result.put(entry.getKey(), entry.getValue().freeze());
        }
        return Map.copyOf(result);
    }

    public static PriceColors resolve(ShopSignEntry entry, Map<Identifier, ItemPriceStats> statsByItemId) {
        if (entry == null || ShopEntryComparator.isUnknownEntry(entry) || entry.resolvedItemId() == null || entry.parsedPrice().amount() == null) {
            return PriceColors.neutral();
        }

        ItemPriceStats stats = statsByItemId.get(entry.resolvedItemId());
        if (stats == null || !stats.hasRange()) {
            return PriceColors.neutral();
        }

        float progress = MathHelper.clamp((entry.parsedPrice().amount() - stats.minPrice()) / (float) (stats.maxPrice() - stats.minPrice()), 0.0F, 1.0F);
        return new PriceColors(
                mixColor(CARD_CHEAP, CARD_EXPENSIVE, progress),
                mixColor(BORDER_CHEAP, BORDER_EXPENSIVE, progress),
                mixColor(PRICE_CHEAP, PRICE_EXPENSIVE, progress),
                progress,
                true
        );
    }

    private static int mixColor(int from, int to, float progress) {
        int alpha = Math.round(lerp(channel(from, 24), channel(to, 24), progress));
        int red = Math.round(lerp(channel(from, 16), channel(to, 16), progress));
        int green = Math.round(lerp(channel(from, 8), channel(to, 8), progress));
        int blue = Math.round(lerp(channel(from, 0), channel(to, 0), progress));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int channel(int color, int shift) {
        return color >> shift & 0xFF;
    }

    private static float lerp(int from, int to, float progress) {
        return from + (to - from) * progress;
    }

    public record ItemPriceStats(int minPrice, int maxPrice, int sampleCount) {
        public boolean hasRange() {
            return this.sampleCount > 1 && this.maxPrice > this.minPrice;
        }
    }

    public record PriceColors(int cardOverlayColor, int borderColor, int priceTextColor, float progress, boolean tinted) {
        public static PriceColors neutral() {
            return new PriceColors(NEUTRAL_CARD_OVERLAY, NEUTRAL_BORDER, NEUTRAL_PRICE_TEXT, 0.5F, false);
        }
    }

    private static final class MutableStats {
        private int minPrice = Integer.MAX_VALUE;
        private int maxPrice = Integer.MIN_VALUE;
        private int sampleCount;

        void accept(int amount) {
            this.minPrice = Math.min(this.minPrice, amount);
            this.maxPrice = Math.max(this.maxPrice, amount);
            this.sampleCount++;
        }

        ItemPriceStats freeze() {
            return new ItemPriceStats(this.minPrice, this.maxPrice, this.sampleCount);
        }
    }
}
