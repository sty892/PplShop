package styy.pplShop.pplshop.client.world;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import styy.pplShop.pplshop.client.gui.PriceColorResolver;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ExperimentalGlowHighlightBackend implements HighlightBackend {
    private static final RenderPipeline HIGHLIGHT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation("pipeline/pplshop_experimental_glow")
                    .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, VertexFormat.DrawMode.LINES)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build()
    );

    private static final RenderLayer HIGHLIGHT_LAYER = RenderLayer.of(
            "pplshop_experimental_glow",
            1536,
            false,
            true,
            HIGHLIGHT_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder().build(false)
    );

    @Override
    public String id() {
        return "experimental_glow";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void render(WorldRenderContext context, ActiveHighlightState state) {
        if (state == null || state.isEmpty() || context.world() == null || context.consumers() == null || context.matrixStack() == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        VertexConsumer consumer = context.consumers().getBuffer(HIGHLIGHT_LAYER);
        BasicOutlineHighlightBackend basic = new BasicOutlineHighlightBackend();
        basic.render(context, state);

        Map<String, ActiveHighlightState.HighlightedTarget> targets = new LinkedHashMap<>();
        for (ActiveHighlightState.HighlightedTarget target : state.targets()) {
            HighlightTargetRelation relation = target.relation();
            if (relation.linkedContainer() && relation.containerPos() != null) {
                targets.putIfAbsent("c|" + relation.containerPos().asLong(), target);
            }
            if (relation.signPos() != null) {
                targets.putIfAbsent("s|" + relation.signPos().asLong() + "|" + relation.signSide().name(), target);
            }
        }

        for (ActiveHighlightState.HighlightedTarget target : targets.values()) {
            float[] glowColor = this.glowColor(target.priceColors(), state.isFocused(target.entry()));
            HighlightTargetRelation relation = target.relation();
            if (relation.linkedContainer() && relation.containerPos() != null) {
                Box box = new Box(relation.containerPos()).expand(0.085D).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                VertexRendering.drawBox(matrices, consumer, box, glowColor[0], glowColor[1], glowColor[2], glowColor[3]);
            }
            if (relation.signPos() != null) {
                Box box = new Box(relation.signPos()).expand(0.06D).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                VertexRendering.drawBox(matrices, consumer, box, glowColor[0], glowColor[1], glowColor[2], glowColor[3]);
            }
        }
    }

    private float[] glowColor(PriceColorResolver.PriceColors priceColors, boolean focused) {
        if (priceColors == null || !priceColors.tinted()) {
            return rgba(focused ? 0xFFF4D6 : 0xF5E4BC, focused ? 0.60F : 0.42F);
        }
        int rgb = mixRgb(priceColors.borderColor(), 0xFFFFFF, focused ? 0.30F : 0.18F);
        return rgba(rgb, focused ? 0.62F : 0.44F);
    }

    private static int mixRgb(int from, int to, float progress) {
        int red = Math.round(lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, progress));
        int green = Math.round(lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, progress));
        int blue = Math.round(lerp(from & 0xFF, to & 0xFF, progress));
        return red << 16 | green << 8 | blue;
    }

    private static float lerp(int from, int to, float progress) {
        return from + (to - from) * progress;
    }

    private static float[] rgba(int rgb, float alpha) {
        return new float[]{
                (rgb >> 16 & 0xFF) / 255.0F,
                (rgb >> 8 & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F,
                alpha
        };
    }
}
