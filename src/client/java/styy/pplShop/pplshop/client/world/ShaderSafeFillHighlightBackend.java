package styy.pplShop.pplshop.client.world;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import styy.pplShop.pplshop.client.gui.PriceColorResolver;

import java.util.Map;

public final class ShaderSafeFillHighlightBackend implements HighlightBackend {
    @Override
    public String id() {
        return "shader_safe_fill";
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
        VertexConsumer lineConsumer = context.consumers().getBuffer(BasicOutlineHighlightBackend.highlightLayer());

        for (Map.Entry<String, ActiveHighlightState.HighlightedTarget> entry : HighlightGeometry.uniqueTargets(state).entrySet()) {
            ActiveHighlightState.HighlightedTarget target = entry.getValue();
            boolean focused = state.isFocused(target.entry());
            float[] outlineColor = this.outlineColor(target.priceColors(), focused);
            float[] fillColor = this.fillColor(target.priceColors(), focused);
            HighlightTargetRelation relation = target.relation();
            if (relation.linkedContainer() && relation.containerPos() != null) {
                Box shifted = new Box(relation.containerPos()).expand(0.025D).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                this.drawFilledOutline(matrices, fillConsumer, lineConsumer, shifted, fillColor, outlineColor);
            } else if (relation.signPos() != null) {
                Box signBox = HighlightGeometry.createSignFaceBox(context.world(), relation);
                Box shifted = (signBox == null ? new Box(relation.signPos()).expand(0.03D) : signBox.expand(0.01D))
                        .offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                this.drawFilledOutline(matrices, fillConsumer, lineConsumer, shifted, fillColor, outlineColor);
            }
        }
    }

    private void drawFilledOutline(
            MatrixStack matrices,
            VertexConsumer fillConsumer,
            VertexConsumer lineConsumer,
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
        VertexRendering.drawBox(matrices, lineConsumer, box, outlineColor[0], outlineColor[1], outlineColor[2], outlineColor[3]);
    }

    private float[] outlineColor(PriceColorResolver.PriceColors priceColors, boolean focused) {
        if (priceColors == null || !priceColors.tinted()) {
            return rgba(focused ? 0xFFE6B5 : 0xEBCB92, focused ? 0.98F : 0.90F);
        }
        return rgba(priceColors.borderColor() & 0xFFFFFF, focused ? 0.99F : 0.92F);
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
