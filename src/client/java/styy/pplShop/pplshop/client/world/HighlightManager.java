package styy.pplShop.pplshop.client.world;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import styy.pplShop.pplshop.client.model.ShopSignEntry;

import java.util.Collection;
import java.util.List;

public final class HighlightManager {
    private final HighlightController controller = new HighlightController();

    public void setAutoClearDistance(double autoClearDistance) {
        this.controller.setAutoClearDistance(autoClearDistance);
    }

    public void setTarget(ShopSignEntry target) {
        this.setTarget(target, target == null ? List.of() : List.of(target));
    }

    public void setTarget(ShopSignEntry target, Collection<ShopSignEntry> comparisonPool) {
        this.setTargets(target == null ? List.of() : List.of(target), comparisonPool);
    }

    public void setTargets(Collection<ShopSignEntry> targets) {
        this.setTargets(targets, targets);
    }

    public void setTargets(Collection<ShopSignEntry> targets, Collection<ShopSignEntry> comparisonPool) {
        this.controller.setTargets(targets, comparisonPool);
    }

    public List<ShopSignEntry> targets() {
        return this.controller.targets();
    }

    public void clear() {
        this.controller.clear();
    }

    public void tick(MinecraftClient client) {
        this.controller.tick(client);
    }

    public void render(WorldRenderContext context) {
        this.controller.render(context);
    }

    public String backendId() {
        return this.controller.backendId();
    }

    public boolean experimentalDisplayEntityBackendAvailable() {
        return this.controller.experimentalDisplayEntityBackendAvailable();
    }

    public ActiveHighlightState.HighlightDebugSnapshot debugSnapshot() {
        return this.controller.debugSnapshot();
    }
}
