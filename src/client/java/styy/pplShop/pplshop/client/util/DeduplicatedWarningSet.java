package styy.pplShop.pplshop.client.util;

import java.util.LinkedHashSet;
import java.util.Set;

public final class DeduplicatedWarningSet {
    private final Set<String> seenKeys = new LinkedHashSet<>();

    public boolean shouldEmit(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return this.seenKeys.add(key);
    }

    public int size() {
        return this.seenKeys.size();
    }

    public void clear() {
        this.seenKeys.clear();
    }
}
