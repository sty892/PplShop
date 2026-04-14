package styy.pplShop.pplshop.client.world;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import styy.pplShop.pplshop.client.gui.PriceColorResolver;
import styy.pplShop.pplshop.client.model.ShopSignEntry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HighlightController {
    private static final boolean EXPERIMENTAL_GLOW_ENABLED = Boolean.getBoolean("pplshop.experimentalGlowHighlight");
    private final ActiveHighlightState activeState = new ActiveHighlightState();
    private final HighlightCleanupHooks cleanupHooks = new HighlightCleanupHooks();
    private final BarrelInteractionResolver interactionResolver = new BarrelInteractionResolver();
    private final HighlightBackend basicBackend = new BasicOutlineHighlightBackend();
    private final HighlightBackend experimentalBackend = new ExperimentalGlowHighlightBackend();
    private HighlightBackend activeBackend = EXPERIMENTAL_GLOW_ENABLED ? this.experimentalBackend : this.basicBackend;
    private double autoClearDistance = 3.0D;

    public void setAutoClearDistance(double autoClearDistance) {
        this.autoClearDistance = autoClearDistance;
    }

    public void setTargets(Collection<ShopSignEntry> targets, Collection<ShopSignEntry> comparisonPool) {
        List<ShopSignEntry> comparisonEntries = comparisonPool == null
                ? List.of()
                : comparisonPool.stream().filter(Objects::nonNull).toList();
        Map<Identifier, PriceColorResolver.ItemPriceStats> statsByItemId = PriceColorResolver.buildStats(comparisonEntries);
        List<ActiveHighlightState.HighlightedTarget> nextTargets = targets == null
                ? List.of()
                : targets.stream()
                .filter(Objects::nonNull)
                .map(target -> new ActiveHighlightState.HighlightedTarget(
                        target,
                        HighlightTargetRelation.fromEntry(target),
                        PriceColorResolver.resolve(target, statsByItemId)
                ))
                .toList();
        this.activeState.replaceTargets(nextTargets);
    }

    public List<ShopSignEntry> targets() {
        return this.activeState.entries();
    }

    public void clear() {
        this.activeState.clear();
        this.basicBackend.clear();
        this.experimentalBackend.clear();
    }

    public void tick(MinecraftClient client) {
        if (this.activeState.isEmpty()) {
            return;
        }
        if (this.cleanupHooks.shouldClearForWorld(client)) {
            this.clear();
            return;
        }

        this.interactionResolver.updateLookedContainer(client, this.activeState);
        BlockPos openedContainerPos = this.interactionResolver.openedRelatedContainerPos(client, this.activeState);
        if (openedContainerPos != null
                && this.activeState.removeByContainerPos(openedContainerPos, ActiveHighlightState.MutationTrigger.CONTAINER_OPEN)
                && this.activeState.isEmpty()) {
            this.basicBackend.clear();
            this.experimentalBackend.clear();
            return;
        }

        this.cleanupHooks.pruneInvalidTargets(client, this.activeState, this.autoClearDistance);
        if (this.activeState.isEmpty()) {
            this.clear();
        }
    }

    public void render(WorldRenderContext context) {
        if (this.activeState.isEmpty()) {
            return;
        }
        this.activeBackend.render(context, this.activeState);
    }

    public String backendId() {
        return this.activeBackend.id();
    }

    public boolean experimentalDisplayEntityBackendAvailable() {
        return false;
    }

    public boolean experimentalGlowBackendAvailable() {
        return this.experimentalBackend.isAvailable();
    }

    public ActiveHighlightState.HighlightDebugSnapshot debugSnapshot() {
        return this.activeState.debugSnapshot();
    }
}
