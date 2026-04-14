package styy.pplShop.pplshop.client.world;

import styy.pplShop.pplshop.client.model.ShopSignDiagnosticReason;
import styy.pplShop.pplshop.client.model.ShopSignEntry;
import styy.pplShop.pplshop.client.model.ShopSignFingerprint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NegativeShopCache {
    private final Map<String, NegativeEntry> entries = new LinkedHashMap<>();

    public NegativeEntry get(String cacheKey) {
        return this.entries.get(cacheKey);
    }

    public void put(String cacheKey, ShopSignFingerprint fingerprint, ShopSignDiagnosticReason reason, String rawText) {
        this.put(cacheKey, fingerprint, reason, rawText, null);
    }

    public void put(String cacheKey, ShopSignFingerprint fingerprint, ShopSignDiagnosticReason reason, String rawText, ShopSignEntry cachedEntry) {
        this.entries.put(cacheKey, new NegativeEntry(fingerprint, reason, rawText, cachedEntry));
    }

    public void remove(String cacheKey) {
        this.entries.remove(cacheKey);
    }

    public void clear() {
        this.entries.clear();
    }

    public List<NegativeEntry> snapshot() {
        return List.copyOf(this.entries.values());
    }

    public record NegativeEntry(ShopSignFingerprint fingerprint, ShopSignDiagnosticReason reason, String rawText, ShopSignEntry cachedEntry) {
    }
}
