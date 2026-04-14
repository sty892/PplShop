package styy.pplShop.pplshop.client.world;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public interface HighlightBackend {
    String id();

    boolean isAvailable();

    default void render(WorldRenderContext context, ActiveHighlightState state) {
    }

    default void clear() {
    }
}
