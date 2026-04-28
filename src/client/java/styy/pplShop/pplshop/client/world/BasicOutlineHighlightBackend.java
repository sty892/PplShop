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

public final class BasicOutlineHighlightBackend implements HighlightBackend {
    private static final RenderPipeline HIGHLIGHT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation("pipeline/pplshop_basic_highlight")
                    .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, VertexFormat.DrawMode.LINES)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build()
    );

    private static final RenderLayer HIGHLIGHT_LAYER = RenderLayer.of(
            "pplshop_basic_highlight",
            1536,
            false,
            true,
            HIGHLIGHT_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder().build(false)
    );

    static RenderLayer highlightLayer() {
        return HIGHLIGHT_LAYER;
    }

    @Override
    public String id() {
        return "basic_outline";
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
        VertexConsumer fillConsumer = context.consumers().getBuffer(RenderLayer.getDebugFilledBox());
        VertexConsumer consumer = context.consumers().getBuffer(HIGHLIGHT_LAYER);

        for (ActiveHighlightState.HighlightedTarget target : HighlightGeometry.uniqueTargets(state).values()) {
            boolean focused = state.isFocused(target.entry());
            float[] outlineColor = this.outlineColor(target.priceColors(), focused);
            if (target.relation().linkedContainer() && target.relation().containerPos() != null) {
                float[] fillColor = this.fillColor(target.priceColors(), focused);
                this.renderContainerOutline(matrices, fillConsumer, consumer, cameraPos, target, fillColor, outlineColor);
            } else {
                float[] signFaceColor = this.signFaceColor(target.priceColors(), focused);
                float[] fillColor = this.fillColor(target.priceColors(), focused);
                this.renderSignOutline(context.world(), matrices, fillConsumer, consumer, cameraPos, target, fillColor, outlineColor, signFaceColor);
            }
        }
    }

    private void renderContainerOutline(
            MatrixStack matrices,
            VertexConsumer fillConsumer,
            VertexConsumer consumer,
            Vec3d cameraPos,
            ActiveHighlightState.HighlightedTarget target,
            float[] fillColor,
            float[] outlineColor
    ) {
        HighlightTargetRelation relation = target.relation();
        if (!relation.linkedContainer() || relation.containerPos() == null) {
            return;
        }

        Box box = new Box(relation.containerPos()).expand(0.018D).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        this.drawFilledOutline(matrices, fillConsumer, consumer, box, fillColor, outlineColor);
    }

    private void renderSignOutline(
            net.minecraft.client.world.ClientWorld world,
            MatrixStack matrices,
            VertexConsumer fillConsumer,
            VertexConsumer consumer,
            Vec3d cameraPos,
            ActiveHighlightState.HighlightedTarget target,
            float[] fillColor,
            float[] outlineColor,
            float[] signFaceColor
    ) {
        HighlightTargetRelation relation = target.relation();
        if (relation.signPos() == null) {
            return;
        }

        Box faceBox = HighlightGeometry.createSignFaceBox(world, relation);
        if (faceBox != null) {
            Box shifted = faceBox.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            this.drawFilledOutline(matrices, fillConsumer, consumer, shifted, fillColor, signFaceColor);
            return;
        }

        Box fallback = new Box(relation.signPos()).expand(0.02D).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        this.drawFilledOutline(matrices, fillConsumer, consumer, fallback, fillColor, outlineColor);
    }

    private void drawFilledOutline(
            MatrixStack matrices,
            VertexConsumer fillConsumer,
            VertexConsumer outlineConsumer,
            Box box,
            float[] fillColor,
            float[] outlineColor
    ) {
        VertexRendering.drawFilledBox(
                matrices,
                fillConsumer,
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ,
                fillColor[0],
                fillColor[1],
                fillColor[2],
                fillColor[3]
        );
        VertexRendering.drawBox(matrices, outlineConsumer, box, outlineColor[0], outlineColor[1], outlineColor[2], outlineColor[3]);
    }

    private float[] outlineColor(PriceColorResolver.PriceColors priceColors, boolean focused) {
        if (priceColors == null || !priceColors.tinted()) {
            return rgba(focused ? 0xFFE6B5 : 0xEBCB92, focused ? 0.98F : 0.90F);
        }
        return rgba(priceColors.borderColor() & 0xFFFFFF, focused ? 0.99F : 0.92F);
    }

    private float[] signFaceColor(PriceColorResolver.PriceColors priceColors, boolean focused) {
        if (priceColors == null || !priceColors.tinted()) {
            return rgba(focused ? 0xFFF2C8 : 0xF1D8A8, focused ? 1.0F : 0.94F);
        }
        int color = priceColors.priceTextColor() & 0xFFFFFF;
        return rgba(color, focused ? 0.98F : 0.90F);
    }

    private float[] fillColor(PriceColorResolver.PriceColors priceColors, boolean focused) {
        if (priceColors == null || !priceColors.tinted()) {
            return rgba(focused ? 0xFFE6B5 : 0xEBCB92, focused ? 0.20F : 0.14F);
        }
        return rgba(priceColors.priceTextColor() & 0xFFFFFF, focused ? 0.18F : 0.12F);
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
