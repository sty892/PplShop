package styy.pplShop.pplshop.client.world;

import styy.pplShop.pplshop.client.gui.ShopEntryComparator;
import styy.pplShop.pplshop.client.gui.ShopSortMode;
import styy.pplShop.pplshop.client.model.ShopSignEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShopCache {
    private final Map<String, ShopSignEntry> entries = new LinkedHashMap<>();
    private List<ShopSignEntry> snapshot = List.of();
    private long version;

    public void replaceAll(List<ShopSignEntry> scannedEntries) {
        this.entries.clear();
        for (ShopSignEntry entry : scannedEntries) {
            this.entries.put(entry.cacheKey(), entry);
        }
        this.rebuildSnapshot();
    }

    public void put(ShopSignEntry entry) {
        if (entry == null) {
            return;
        }
        ShopSignEntry previous = this.entries.put(entry.cacheKey(), entry);
        if (previous == entry) {
            return;
        }
        this.rebuildSnapshot();
    }

    public void remove(String cacheKey) {
        if (cacheKey == null) {
            return;
        }
        if (this.entries.remove(cacheKey) != null) {
            this.rebuildSnapshot();
        }
    }

    public List<ShopSignEntry> snapshot() {
        return this.snapshot;
    }

    public void clear() {
        this.entries.clear();
        this.snapshot = List.of();
        this.version++;
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    public List<ShopSignEntry> mutableSnapshot() {
        return new ArrayList<>(this.snapshot);
    }

    public long version() {
        return this.version;
    }

    private void rebuildSnapshot() {
        this.snapshot = this.entries.values().stream()
                .sorted(ShopEntryComparator.forMode(ShopSortMode.NAME))
                .toList();
        this.version++;
    }
}
