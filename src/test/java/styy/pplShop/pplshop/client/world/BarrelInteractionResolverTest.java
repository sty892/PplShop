package styy.pplShop.pplshop.client.world;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import styy.pplShop.pplshop.client.model.SignSide;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarrelInteractionResolverTest {
    private final BarrelInteractionResolver resolver = new BarrelInteractionResolver();

    @Test
    void unrelatedOpenedBarrelDoesNotClearFocusedHighlight() {
        HighlightTargetRelation focusedRelation = relation(new BlockPos(0, 64, 0), new BlockPos(0, 63, 0));
        assertFalse(this.resolver.matchesOpenedFocusedContainer(focusedRelation, new BlockPos(10, 63, 0)));
    }

    @Test
    void linkedOpenedBarrelClearsFocusedHighlight() {
        HighlightTargetRelation focusedRelation = relation(new BlockPos(0, 64, 0), new BlockPos(0, 63, 0));
        assertTrue(this.resolver.matchesOpenedFocusedContainer(focusedRelation, new BlockPos(0, 63, 0)));
    }

    @Test
    void lookedContainerSwitchesToExactMatchedTarget() {
        BarrelInteractionResolver.ContainerTarget match = this.resolver.findLookedContainer(
                List.of(
                        new BarrelInteractionResolver.ContainerTarget("first", relation(new BlockPos(0, 64, 0), new BlockPos(0, 63, 0))),
                        new BarrelInteractionResolver.ContainerTarget("second", relation(new BlockPos(10, 64, 0), new BlockPos(10, 63, 0)))
                ),
                new BlockPos(10, 63, 0)
        );

        assertEquals("second", match.cacheKey());
        assertEquals(new BlockPos(10, 63, 0), match.relation().containerPos());
    }

    @Test
    void unrelatedLookReturnsNoTarget() {
        BarrelInteractionResolver.ContainerTarget match = this.resolver.findLookedContainer(
                List.of(
                        new BarrelInteractionResolver.ContainerTarget("first", relation(new BlockPos(0, 64, 0), new BlockPos(0, 63, 0))),
                        new BarrelInteractionResolver.ContainerTarget("second", relation(new BlockPos(10, 64, 0), new BlockPos(10, 63, 0)))
                ),
                new BlockPos(99, 70, 99)
        );

        assertNull(match);
    }

    private static HighlightTargetRelation relation(BlockPos signPos, BlockPos containerPos) {
        return new HighlightTargetRelation(signPos, containerPos, true, SignSide.FRONT, false, null, "nearby");
    }
}
