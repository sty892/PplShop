package styy.pplShop.pplshop.client.world;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import styy.pplShop.pplshop.client.model.ShopSignEntry;
import styy.pplShop.pplshop.client.model.SignSide;
import styy.pplShop.pplshop.client.model.SignTextSnapshot;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveHighlightStateTest {
    @Test
    void removeByContainerPosOnlyRemovesMatchingBarrelHighlights() {
        BlockPos barrelA = new BlockPos(0, 63, 0);
        BlockPos barrelB = new BlockPos(10, 63, 0);
        ShopSignEntry first = entry(new BlockPos(0, 64, 0), barrelA);
        ShopSignEntry second = entry(new BlockPos(1, 64, 0), barrelA);
        ShopSignEntry third = entry(new BlockPos(10, 64, 0), barrelB);

        ActiveHighlightState state = new ActiveHighlightState();
        state.replaceTargets(List.of(target(first, barrelA), target(second, barrelA), target(third, barrelB)));

        assertTrue(state.removeByContainerPos(barrelA, ActiveHighlightState.MutationTrigger.CONTAINER_OPEN));
        assertEquals(1, state.entries().size());
        assertEquals(third.cacheKey(), state.entries().getFirst().cacheKey());
        assertEquals(third.cacheKey(), state.focusedTargetKey());

        ActiveHighlightState.HighlightDebugSnapshot snapshot = state.debugSnapshot();
        assertEquals(1, snapshot.activeHighlightCount());
        assertEquals(1, snapshot.trackedTargetCount());
        assertIterableEquals(List.of(barrelB), snapshot.highlightedPositions());
        assertEquals("REMOVE", snapshot.lastMutationEvent());
        assertEquals(barrelA, snapshot.lastMutationPos());
        assertEquals("CONTAINER_OPEN", snapshot.lastMutationTrigger());
    }

    @Test
    void removeByContainerPosIgnoresUntrackedBarrels() {
        BlockPos barrelA = new BlockPos(0, 63, 0);
        BlockPos barrelB = new BlockPos(10, 63, 0);
        ShopSignEntry entry = entry(new BlockPos(0, 64, 0), barrelA);

        ActiveHighlightState state = new ActiveHighlightState();
        state.replaceTargets(List.of(target(entry, barrelA)));

        assertFalse(state.removeByContainerPos(barrelB, ActiveHighlightState.MutationTrigger.CONTAINER_OPEN));
        assertEquals(1, state.entries().size());

        ActiveHighlightState.HighlightDebugSnapshot snapshot = state.debugSnapshot();
        assertEquals(1, snapshot.activeHighlightCount());
        assertEquals(1, snapshot.trackedTargetCount());
        assertIterableEquals(List.of(barrelA), snapshot.highlightedPositions());
        assertEquals("ADD", snapshot.lastMutationEvent());
        assertEquals("OTHER", snapshot.lastMutationTrigger());
    }

    private static ActiveHighlightState.HighlightedTarget target(ShopSignEntry entry, BlockPos barrelPos) {
        return new ActiveHighlightState.HighlightedTarget(
                entry,
                new HighlightTargetRelation(entry.pos(), barrelPos, true, entry.snapshot().side(), false, null, "nearby"),
                null
        );
    }

    private static ShopSignEntry entry(BlockPos signPos, BlockPos barrelPos) {
        try {
            ShopSignEntry entry = (ShopSignEntry) unsafe().allocateInstance(ShopSignEntry.class);
            setField(entry, "dimensionId", Identifier.of("minecraft", "overworld"));
            setField(entry, "pos", signPos.toImmutable());
            setField(entry, "snapshot", new SignTextSnapshot(List.of("Stone", "1 dia", "", "Seller"), false, SignSide.FRONT));
            return entry;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to create synthetic ShopSignEntry", exception);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = ShopSignEntry.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Unsafe unsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }
}
