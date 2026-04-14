package styy.pplShop.pplshop.client.world;

import styy.pplShop.pplshop.client.model.ShopSignEntry;
import styy.pplShop.pplshop.client.model.ShopSignFingerprint;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShopSignParseCache {
    private final Map<String, CachedEntry> entries = new LinkedHashMap<>();

    public CachedEntry get(String cacheKey) {
        return this.entries.get(cacheKey);
    }

    public void put(String cacheKey, ShopSignFingerprint fingerprint, ShopSignEntry entry) {
        this.entries.put(cacheKey, new CachedEntry(fingerprint, entry));
    }

    public void remove(String cacheKey) {
        this.entries.remove(cacheKey);
    }

    public void clear() {
        this.entries.clear();
    }

    public record CachedEntry(ShopSignFingerprint fingerprint, ShopSignEntry entry) {
    }
}
