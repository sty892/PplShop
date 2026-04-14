package styy.pplShop.pplshop.client.model;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import styy.pplShop.pplshop.client.util.DeduplicatedWarningSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ShopSignEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger("PPLShop");
    private static final DeduplicatedWarningSet WARNINGS = new DeduplicatedWarningSet();

    private final Identifier dimensionId;
    private final BlockPos pos;
    private final SignTextSnapshot snapshot;
    private final ParsedItem parsedItem;
    private final ParsedPrice parsedPrice;
    private final String rawCombinedText;
    private final long scanTick;
    private final Identifier resolvedItemId;
    private final ShopSignDiagnostics diagnostics;
    private final SignContainerRelation relation;
    private final Double pricePerUnit;
    private ItemStack resolvedItemStack;
    private String resolvedDisplayName;

    public ShopSignEntry(
            Identifier dimensionId,
            BlockPos pos,
            SignTextSnapshot snapshot,
            ParsedItem parsedItem,
            ParsedPrice parsedPrice,
            String rawCombinedText,
            long scanTick
    ) {
        this(dimensionId, pos, snapshot, parsedItem, parsedPrice, rawCombinedText, scanTick, ShopSignDiagnostics.empty(), SignContainerRelation.missing());
    }

    public ShopSignEntry(
            Identifier dimensionId,
            BlockPos pos,
            SignTextSnapshot snapshot,
            ParsedItem parsedItem,
            ParsedPrice parsedPrice,
            String rawCombinedText,
            long scanTick,
            ShopSignDiagnostics diagnostics
    ) {
        this(dimensionId, pos, snapshot, parsedItem, parsedPrice, rawCombinedText, scanTick, diagnostics, SignContainerRelation.missing());
    }

    public ShopSignEntry(
            Identifier dimensionId,
            BlockPos pos,
            SignTextSnapshot snapshot,
            ParsedItem parsedItem,
            ParsedPrice parsedPrice,
            String rawCombinedText,
            long scanTick,
            ShopSignDiagnostics diagnostics,
            SignContainerRelation relation
    ) {
        this.dimensionId = dimensionId;
        this.pos = pos.toImmutable();
        this.snapshot = snapshot;
        this.parsedItem = parsedItem;
        this.parsedPrice = parsedPrice;
        this.rawCombinedText = rawCombinedText;
        this.scanTick = scanTick;
        this.resolvedItemId = parsedItem.isResolved() ? parsedItem.itemId() : null;
        this.diagnostics = diagnostics == null ? ShopSignDiagnostics.empty() : diagnostics;
        this.relation = relation == null ? SignContainerRelation.missing() : relation;
        this.pricePerUnit = computePricePerUnit(parsedPrice);
        this.resolvedItemStack = ItemStack.EMPTY;
        this.resolvedDisplayName = this.resolvedItemId == null
                ? translated("item.pplshop.unknown")
                : (parsedItem.displayNameOverride().isBlank() ? parsedItem.rawText() : parsedItem.displayNameOverride());
    }

    public Identifier dimensionId() {
        return this.dimensionId;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public SignTextSnapshot snapshot() {
        return this.snapshot;
    }

    public ParsedItem parsedItem() {
        return this.parsedItem;
    }

    public ParsedPrice parsedPrice() {
        return this.parsedPrice;
    }

    public String rawCombinedText() {
        return this.rawCombinedText;
    }

    public long scanTick() {
        return this.scanTick;
    }

    public Identifier resolvedItemId() {
        return this.resolvedItemId;
    }

    public ShopSignDiagnostics diagnostics() {
        return this.diagnostics;
    }

    public SignContainerRelation relation() {
        return this.relation;
    }

    public Double pricePerUnit() {
        return this.pricePerUnit;
    }

    public ItemStack resolvedItemStack() {
        if (this.resolvedItemId == null) {
            return ItemStack.EMPTY;
        }

        if (!this.resolvedItemStack.isEmpty()) {
            return this.resolvedItemStack.copy();
        }

        ItemStack resolvedNow = this.buildResolvedItemStack(this.resolvedItemId);
        if (!resolvedNow.isEmpty()) {
            this.cacheResolvedPresentation(resolvedNow);
            return resolvedNow.copy();
        }
        return this.resolvedItemStack.isEmpty() ? ItemStack.EMPTY : this.resolvedItemStack.copy();
    }

    public boolean hasResolvedItem() {
        return this.resolvedItemId != null;
    }

    public String displayName() {
        if (this.resolvedItemId == null) {
            return this.resolvedDisplayName;
        }

        if (!this.parsedItem.displayNameOverride().isBlank()) {
            return this.parsedItem.displayNameOverride();
        }

        ItemStack resolvedNow = this.resolvedItemStack();
        return resolvedNow.isEmpty() ? this.resolvedDisplayName : resolvedNow.getName().getString();
    }

    public String priceDisplay() {
        if (this.parsedPrice.normalizedDisplayText() != null && !this.parsedPrice.normalizedDisplayText().isBlank()) {
            return this.parsedPrice.normalizedDisplayText();
        }
        return translated("screen.pplshop.price_unknown");
    }

    public ItemStack createDisplayStack() {
        if (this.resolvedItemId == null) {
            return new ItemStack(Items.BARRIER);
        }

        ItemStack resolvedNow = this.resolvedItemStack();
        if (!resolvedNow.isEmpty()) {
            return resolvedNow;
        }

        if (WARNINGS.shouldEmit("gui-missing-stack|" + this.resolvedItemId)) {
            LOGGER.warn("[GUI] resolved item id present but stack missing: raw='{}' resolvedItemId={}", this.rawCombinedText, this.resolvedItemId);
        }
        return ItemStack.EMPTY;
    }

    public Text statusText() {
        return Text.literal(this.parsedItem.parseStatus() + " / " + this.parsedPrice.parseStatus());
    }

    public String cacheKey() {
        return this.dimensionId + "|" + this.pos.toShortString() + "|" + this.snapshot.side().name();
    }

    public List<String> rawSearchTerms() {
        List<String> terms = new ArrayList<>();
        terms.add(this.displayName());
        terms.add(this.rawCombinedText);
        terms.add(this.parsedItem.rawText());
        terms.add(this.parsedItem.normalizedText());
        terms.add(this.priceDisplay());
        terms.add(this.parsedPrice.rawText());
        if (this.resolvedItemId != null) {
            terms.add(this.resolvedItemId.toString());
        }
        if (this.parsedPrice.currencyKey() != null) {
            terms.add(this.parsedPrice.currencyKey());
        }
        if (this.parsedPrice.currencyItemId() != null) {
            terms.add(this.parsedPrice.currencyItemId().toString());
        }
        if (this.parsedItem.matchedAlias() != null) {
            terms.add(this.parsedItem.matchedAlias());
        }
        if (this.parsedPrice.matchedAlias() != null) {
            terms.add(this.parsedPrice.matchedAlias());
        }
        return terms.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
    }

    private ItemStack buildResolvedItemStack(Identifier itemId) {
        if (itemId == null) {
            return ItemStack.EMPTY;
        }

        Item item = Registries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) {
            if (WARNINGS.shouldEmit("missing-registry-item|" + itemId)) {
                LOGGER.warn("Alias matched but registry item missing: {}", itemId);
            }
            return ItemStack.EMPTY;
        }

        LOGGER.debug("[Alias] registry resolved item='{}'", itemId);
        ItemStack stack = new ItemStack(item);
        LOGGER.debug("[Alias] built ItemStack for {}", itemId);
        return stack;
    }

    private void cacheResolvedPresentation(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        this.resolvedItemStack = stack.copy();
        this.resolvedDisplayName = stack.getName().getString();
    }

    private static Double computePricePerUnit(ParsedPrice parsedPrice) {
        if (parsedPrice == null || parsedPrice.amount() == null) {
            return null;
        }
        if (parsedPrice.quantityItemCount() == null || parsedPrice.quantityItemCount() <= 0) {
            return (double) parsedPrice.amount();
        }
        return parsedPrice.amount() / (double) parsedPrice.quantityItemCount();
    }

    private static String translated(String key) {
        return Text.translatable(key).getString();
    }
}
