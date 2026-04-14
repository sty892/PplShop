package styy.pplShop.pplshop.client.world;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import styy.pplShop.pplshop.client.gui.PriceColorResolver;
import styy.pplShop.pplshop.client.model.SignSide;

import java.util.LinkedHashMap;
import java.util.Map;

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
        VertexConsumer consumer = context.consumers().getBuffer(HIGHLIGHT_LAYER);

        Map<String, ActiveHighlightState.HighlightedTarget> targets = new LinkedHashMap<>();
        for (ActiveHighlightState.HighlightedTarget target : state.targets()) {
            HighlightTargetRelation relation = target.relation();
            if (relation.linkedContainer() && relation.containerPos() != null) {
                targets.putIfAbsent("c|" + relation.containerPos().asLong(), target);
            } else if (relation.signPos() != null) {
                targets.putIfAbsent("s|" + relation.signPos().asLong() + "|" + relation.signSide().name(), target);
            }
        }

        for (ActiveHighlightState.HighlightedTarget target : targets.values()) {
            boolean focused = state.isFocused(target.entry());
            float[] outlineColor = this.outlineColor(target.priceColors(), focused);
            if (target.relation().linkedContainer() && target.relation().containerPos() != null) {
                this.renderContainerOutline(matrices, consumer, cameraPos, target, outlineColor);
            } else {
                float[] signFaceColor = this.signFaceColor(target.priceColors(), focused);
                this.renderSignOutline(context.world(), matrices, consumer, cameraPos, target, outlineColor, signFaceColor);
            }
        }
    }

    private void renderContainerOutline(
            MatrixStack matrices,
            VertexConsumer consumer,
            Vec3d cameraPos,
            ActiveHighlightState.HighlightedTarget target,
            float[] outlineColor
    ) {
        HighlightTargetRelation relation = target.relation();
        if (!relation.linkedContainer() || relation.containerPos() == null) {
            return;
        }

        Box box = new Box(relation.containerPos()).expand(0.018D).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        VertexRendering.drawBox(matrices, consumer, box, outlineColor[0], outlineColor[1], outlineColor[2], outlineColor[3]);
    }

    private void renderSignOutline(
            ClientWorld world,
            MatrixStack matrices,
            VertexConsumer consumer,
            Vec3d cameraPos,
            ActiveHighlightState.HighlightedTarget target,
            float[] outlineColor,
            float[] signFaceColor
    ) {
        HighlightTargetRelation relation = target.relation();
        if (relation.signPos() == null) {
            return;
        }

        Box faceBox = this.createSignFaceBox(world, relation);
        if (faceBox != null) {
            Box shifted = faceBox.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            VertexRendering.drawBox(matrices, consumer, shifted, signFaceColor[0], signFaceColor[1], signFaceColor[2], signFaceColor[3]);
            return;
        }

        Box fallback = new Box(relation.signPos()).expand(0.02D).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        VertexRendering.drawBox(matrices, consumer, fallback, outlineColor[0], outlineColor[1], outlineColor[2], outlineColor[3]);
    }

    private Box createSignFaceBox(ClientWorld world, HighlightTargetRelation relation) {
        if (world == null || relation == null || relation.signPos() == null) {
            return null;
        }

        Direction faceDirection = this.resolveSignFaceDirection(world, relation);
        if (faceDirection == null) {
            return null;
        }

        Box base = new Box(relation.signPos());
        double minY = base.minY + (relation.hangingSign() ? 0.08D : 0.18D);
        double maxY = base.minY + (relation.hangingSign() ? 0.94D : 0.82D);
        double inset = 0.12D;
        double thickness = 0.025D;
        double faceOffset = 0.006D;

        return switch (faceDirection) {
            case NORTH -> new Box(base.minX + inset, minY, base.minZ - faceOffset, base.maxX - inset, maxY, base.minZ + thickness);
            case SOUTH -> new Box(base.minX + inset, minY, base.maxZ - thickness, base.maxX - inset, maxY, base.maxZ + faceOffset);
            case WEST -> new Box(base.minX - faceOffset, minY, base.minZ + inset, base.minX + thickness, maxY, base.maxZ - inset);
            case EAST -> new Box(base.maxX - thickness, minY, base.minZ + inset, base.maxX + faceOffset, maxY, base.maxZ - inset);
            default -> null;
        };
    }

    private Direction resolveSignFaceDirection(ClientWorld world, HighlightTargetRelation relation) {
        if (relation.preferredFaceDirection() != null) {
            return relation.signSide() == SignSide.FRONT ? relation.preferredFaceDirection() : relation.preferredFaceDirection().getOpposite();
        }

        BlockState state = world.getBlockState(relation.signPos());
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            return relation.signSide() == SignSide.FRONT ? facing : facing.getOpposite();
        }
        if (state.contains(Properties.ROTATION)) {
            Direction facing = this.directionFromRotation(state.get(Properties.ROTATION));
            return relation.signSide() == SignSide.FRONT ? facing : facing.getOpposite();
        }
        return null;
    }

    private Direction directionFromRotation(int rotation) {
        int quadrant = Math.floorMod(Math.round(rotation / 4.0F), 4);
        return switch (quadrant) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
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

    private static float[] rgba(int rgb, float alpha) {
        return new float[]{
                (rgb >> 16 & 0xFF) / 255.0F,
                (rgb >> 8 & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F,
                alpha
        };
    }
}
