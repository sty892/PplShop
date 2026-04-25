package styy.pplShop.pplshop.client.parser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DebugDumpReplayToolTest {
    @Test
    void unresolvedDebugDumpCanBeReplayedWithoutMinecraftClient() throws IOException {
        DebugDumpReplayTool.Summary summary = DebugDumpReplayTool.replay(Path.of("2026-04-15_14-03-03_415__03-sign-unresolved-entries.txt"));

        assertEquals(333, summary.beforeUnresolvedCount());
        assertTrue(summary.resolvedByAlias() > 0);
        assertTrue(summary.resolvedByPrimaryMulti() > 0);
        assertTrue(summary.afterUnresolvedCount() < summary.beforeUnresolvedCount());
        System.out.println(summary);
    }

    @Test
    void latestLocalUnresolvedDebugDumpCanBeReplayedWhenPresent() throws IOException {
        Path dumpPath = Path.of("D:/modtrinth/profiles/PWGood 1.0.0/pplshop-debug/diagnostic-dump-2026-04-24_15-09-40_467/2026-04-24_15-09-40_467__03-sign-unresolved-entries.txt");
        assumeTrue(Files.exists(dumpPath), "latest local debug dump is not present on this machine");

        DebugDumpReplayTool.Summary summary = DebugDumpReplayTool.replay(dumpPath);

        assertEquals(311, summary.beforeUnresolvedCount());
        assertTrue(summary.resolvedByAlias() > 0);
        assertTrue(summary.resolvedByPrimaryMulti() > 0);
        assertTrue(summary.afterUnresolvedCount() < summary.beforeUnresolvedCount());
        System.out.println("latestLocal=" + summary);
    }
}
