package styy.pplShop.pplshop.client.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class BarrelInteractionResolver {
    public void updateLookedContainer(MinecraftClient client, ActiveHighlightState state) {
        if (client.world == null || !(client.crosshairTarget instanceof BlockHitResult blockHitResult)) {
            state.setLastLookedContainerPos(null);
            return;
        }

        this.matchLookedContainer(state, blockHitResult.getBlockPos());
    }

    public BlockPos openedRelatedContainerPos(MinecraftClient client, ActiveHighlightState state) {
        boolean handledScreenOpen = client.currentScreen instanceof HandledScreen<?>;
        BlockPos matchedContainerPos = null;
        if (handledScreenOpen
                && !state.handledContainerScreenOpen()
                && this.matchesOpenedFocusedContainer(state)) {
            matchedContainerPos = state.lastLookedContainerPos();
        }
        if (!handledScreenOpen && state.lastLookedContainerPos() != null) {
            ActiveHighlightState.HighlightedTarget refreshedFocus = state.focusedTarget();
            HighlightTargetRelation refreshedRelation = refreshedFocus == null ? null : refreshedFocus.relation();
            if (refreshedRelation == null
                    || !refreshedRelation.linkedContainer()
                    || !state.lastLookedContainerPos().equals(refreshedRelation.containerPos())) {
                state.setLastLookedContainerPos(null);
            }
        }
        state.setHandledContainerScreenOpen(handledScreenOpen);
        return matchedContainerPos == null ? null : matchedContainerPos.toImmutable();
    }

    void matchLookedContainer(ActiveHighlightState state, BlockPos lookedPos) {
        ContainerTarget match = this.findLookedContainer(this.toContainerTargets(state), lookedPos);
        if (match == null) {
            state.setLastLookedContainerPos(null);
            return;
        }
        state.setFocusedTargetKey(match.cacheKey());
        state.setLastLookedContainerPos(lookedPos);
    }

    ContainerTarget findLookedContainer(Iterable<ContainerTarget> targets, BlockPos lookedPos) {
        if (lookedPos == null) {
            return null;
        }

        for (ContainerTarget target : targets) {
            HighlightTargetRelation relation = target.relation();
            if (!relation.linkedContainer() || !lookedPos.equals(relation.containerPos())) {
                continue;
            }
            return target;
        }
        return null;
    }

    boolean matchesOpenedFocusedContainer(ActiveHighlightState state) {
        if (state == null) {
            return false;
        }
        ActiveHighlightState.HighlightedTarget focusedTarget = state.focusedTarget();
        HighlightTargetRelation focusedRelation = focusedTarget == null ? null : focusedTarget.relation();
        return this.matchesOpenedFocusedContainer(focusedRelation, state.lastLookedContainerPos());
    }

    boolean matchesOpenedFocusedContainer(HighlightTargetRelation focusedRelation, BlockPos openedContainerPos) {
        return focusedRelation != null
                && focusedRelation.linkedContainer()
                && openedContainerPos != null
                && openedContainerPos.equals(focusedRelation.containerPos());
    }

    private List<ContainerTarget> toContainerTargets(ActiveHighlightState state) {
        List<ContainerTarget> targets = new ArrayList<>();
        for (ActiveHighlightState.HighlightedTarget target : state.targets()) {
            targets.add(new ContainerTarget(target.entry().cacheKey(), target.relation()));
        }
        return targets;
    }

    record ContainerTarget(String cacheKey, HighlightTargetRelation relation) {
    }
}
