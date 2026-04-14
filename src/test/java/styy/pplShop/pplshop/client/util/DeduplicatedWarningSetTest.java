package styy.pplShop.pplshop.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeduplicatedWarningSetTest {
    @Test
    void duplicateKeysOnlyEmitOnce() {
        DeduplicatedWarningSet warnings = new DeduplicatedWarningSet();

        assertTrue(warnings.shouldEmit("missing-registry-item|minecraft:not_real"));
        assertFalse(warnings.shouldEmit("missing-registry-item|minecraft:not_real"));
        assertTrue(warnings.shouldEmit("missing-registry-item|minecraft:other"));
    }
}
