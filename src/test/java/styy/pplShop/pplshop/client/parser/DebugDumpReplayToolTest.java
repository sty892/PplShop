package styy.pplShop.pplshop.client.parser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugDumpReplayToolTest {
    @Test
    void unresolvedDebugDumpCanBeReplayedWithoutMinecraftClient() throws IOException {
        DebugDumpReplayTool.Summary summary = DebugDumpReplayTool.replay(Path.of("2026-04-15_14-03-03_415__03-sign-unresolved-entries.txt"));

        assertEquals(333, summary.beforeUnresolvedCount());
        assertTrue(summary.resolvedByAlias() > 0);
        assertTrue(summary.resolvedByMixed() > 0);
        assertTrue(summary.afterUnresolvedCount() < summary.beforeUnresolvedCount());
        System.out.println(summary);
    }
}
