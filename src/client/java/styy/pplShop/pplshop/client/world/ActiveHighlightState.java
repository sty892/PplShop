package styy.pplShop.pplshop.client.world;

import net.minecraft.util.math.BlockPos;
import styy.pplShop.pplshop.client.gui.PriceColorResolver;
import styy.pplShop.pplshop.client.model.ShopSignEntry;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ActiveHighlightState {
    private final Map<String, HighlightedTarget> targets = new LinkedHashMap<>();
    private String focusedTargetKey;
    private BlockPos lastLookedContainerPos;
    private boolean handledContainerScreenOpen;
    private MutationType lastMutationType = MutationType.CLEAR;
    private BlockPos lastMutationPos;
    private MutationTrigger lastMutationTrigger = MutationTrigger.OTHER;

    public void replaceTargets(Collection<HighlightedTarget> nextTargets) {
        this.targets.clear();
        this.focusedTargetKey = null;
        this.resetInteractionTracking();
        if (nextTargets == null || nextTargets.isEmpty()) {
            this.recordMutation(MutationType.CLEAR, null, MutationTrigger.OTHER);
            return;
        }

        BlockPos lastAddedPos = null;
        for (HighlightedTarget target : nextTargets) {
            if (target == null || target.entry() == null) {
                continue;
            }
            this.targets.put(target.entry().cacheKey(), target);
            if (this.focusedTargetKey == null) {
                this.focusedTargetKey = target.entry().cacheKey();
            }
            BlockPos anchorPos = this.anchorPos(target);
            if (anchorPos != null) {
                lastAddedPos = anchorPos;
            }
        }

        if (this.targets.isEmpty()) {
            this.recordMutation(MutationType.CLEAR, null, MutationTrigger.OTHER);
        } else {
            this.recordMutation(MutationType.ADD, lastAddedPos, MutationTrigger.OTHER);
        }
    }

    public void remove(String cacheKey) {
        this.remove(cacheKey, MutationTrigger.OTHER);
    }

    public void remove(String cacheKey, MutationTrigger trigger) {
        if (cacheKey == null) {
            return;
        }
        HighlightedTarget removed = this.targets.remove(cacheKey);
        if (removed == null) {
            return;
        }
        if (cacheKey.equals(this.focusedTargetKey)) {
            this.focusedTargetKey = this.targets.keySet().stream().findFirst().orElse(null);
        }
        this.recordMutation(MutationType.REMOVE, this.anchorPos(removed), trigger);
    }

    public boolean isEmpty() {
        return this.targets.isEmpty();
    }

    public boolean removeByContainerPos(BlockPos containerPos, MutationTrigger trigger) {
        if (containerPos == null) {
            return false;
        }

        List<String> cacheKeys = this.targets.values().stream()
                .filter(target -> target.relation().linkedContainer())
                .filter(target -> containerPos.equals(target.relation().containerPos()))
                .map(target -> target.entry().cacheKey())
                .toList();
        if (cacheKeys.isEmpty()) {
            return false;
        }

        for (String cacheKey : cacheKeys) {
            this.targets.remove(cacheKey);
        }
        if (cacheKeys.contains(this.focusedTargetKey)) {
            this.focusedTargetKey = this.targets.keySet().stream().findFirst().orElse(null);
        }
        if (containerPos.equals(this.lastLookedContainerPos)) {
            this.lastLookedContainerPos = null;
        }
        this.recordMutation(MutationType.REMOVE, containerPos, trigger);
        return true;
    }

    public void clear() {
        boolean hadTargets = !this.targets.isEmpty();
        this.targets.clear();
        this.focusedTargetKey = null;
        this.resetInteractionTracking();
        if (hadTargets) {
            this.recordMutation(MutationType.CLEAR, null, MutationTrigger.OTHER);
        }
    }

    public Collection<HighlightedTarget> targets() {
        return List.copyOf(this.targets.values());
    }

    public List<ShopSignEntry> entries() {
        return this.targets.values().stream().map(HighlightedTarget::entry).toList();
    }

    public HighlightedTarget target(String cacheKey) {
        return this.targets.get(cacheKey);
    }

    public HighlightedTarget focusedTarget() {
        return this.focusedTargetKey == null ? null : this.targets.get(this.focusedTargetKey);
    }

    public boolean containsKey(String cacheKey) {
        return this.targets.containsKey(cacheKey);
    }

    public boolean isFocused(ShopSignEntry entry) {
        return entry != null && entry.cacheKey().equals(this.focusedTargetKey);
    }

    public String focusedTargetKey() {
        return this.focusedTargetKey;
    }

    public void setFocusedTargetKey(String focusedTargetKey) {
        this.focusedTargetKey = focusedTargetKey == null || !this.targets.containsKey(focusedTargetKey)
                ? this.targets.keySet().stream().findFirst().orElse(null)
                : focusedTargetKey;
    }

    public BlockPos lastLookedContainerPos() {
        return this.lastLookedContainerPos;
    }

    public void setLastLookedContainerPos(BlockPos lastLookedContainerPos) {
        this.lastLookedContainerPos = lastLookedContainerPos == null ? null : lastLookedContainerPos.toImmutable();
    }

    public boolean handledContainerScreenOpen() {
        return this.handledContainerScreenOpen;
    }

    public void setHandledContainerScreenOpen(boolean handledContainerScreenOpen) {
        this.handledContainerScreenOpen = handledContainerScreenOpen;
    }

    public HighlightDebugSnapshot debugSnapshot() {
        LinkedHashSet<BlockPos> highlightedPositions = new LinkedHashSet<>();
        for (HighlightedTarget target : this.targets.values()) {
            BlockPos anchorPos = this.anchorPos(target);
            if (anchorPos != null) {
                highlightedPositions.add(anchorPos.toImmutable());
            }
        }
        return new HighlightDebugSnapshot(
                highlightedPositions.size(),
                this.targets.size(),
                List.copyOf(highlightedPositions),
                this.lastMutationType.name(),
                this.lastMutationPos,
                this.lastMutationTrigger.name()
        );
    }

    private BlockPos anchorPos(HighlightedTarget target) {
        if (target == null || target.relation() == null) {
            return null;
        }
        if (target.relation().linkedContainer() && target.relation().containerPos() != null) {
            return target.relation().containerPos();
        }
        return target.relation().signPos();
    }

    private void resetInteractionTracking() {
        this.lastLookedContainerPos = null;
        this.handledContainerScreenOpen = false;
    }

    private void recordMutation(MutationType mutationType, BlockPos mutationPos, MutationTrigger trigger) {
        this.lastMutationType = mutationType == null ? MutationType.CLEAR : mutationType;
        this.lastMutationPos = mutationPos == null ? null : mutationPos.toImmutable();
        this.lastMutationTrigger = trigger == null ? MutationTrigger.OTHER : trigger;
    }

    public record HighlightedTarget(ShopSignEntry entry, HighlightTargetRelation relation, PriceColorResolver.PriceColors priceColors) {
    }

    public record HighlightDebugSnapshot(
            int activeHighlightCount,
            int trackedTargetCount,
            List<BlockPos> highlightedPositions,
            String lastMutationEvent,
            BlockPos lastMutationPos,
            String lastMutationTrigger
    ) {
        public HighlightDebugSnapshot {
            highlightedPositions = highlightedPositions == null ? List.of() : highlightedPositions.stream()
                    .map(pos -> pos == null ? null : pos.toImmutable())
                    .filter(pos -> pos != null)
                    .toList();
            lastMutationEvent = lastMutationEvent == null || lastMutationEvent.isBlank() ? MutationType.CLEAR.name() : lastMutationEvent;
            lastMutationPos = lastMutationPos == null ? null : lastMutationPos.toImmutable();
            lastMutationTrigger = lastMutationTrigger == null || lastMutationTrigger.isBlank() ? MutationTrigger.OTHER.name() : lastMutationTrigger;
        }
    }

    private enum MutationType {
        ADD,
        REMOVE,
        CLEAR
    }

    public enum MutationTrigger {
        CONTAINER_OPEN,
        PROXIMITY,
        GUI_CLOSE,
        SEARCH_CHANGE,
        OTHER
    }
}
