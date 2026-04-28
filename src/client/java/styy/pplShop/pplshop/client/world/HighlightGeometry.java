package styy.pplShop.pplshop.client.world;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import styy.pplShop.pplshop.client.model.SignSide;

import java.util.LinkedHashMap;
import java.util.Map;

final class HighlightGeometry {
    private HighlightGeometry() {
    }

    static Map<String, ActiveHighlightState.HighlightedTarget> uniqueTargets(ActiveHighlightState state) {
        Map<String, ActiveHighlightState.HighlightedTarget> targets = new LinkedHashMap<>();
        for (ActiveHighlightState.HighlightedTarget target : state.targets()) {
            HighlightTargetRelation relation = target.relation();
            if (relation.linkedContainer() && relation.containerPos() != null) {
                targets.putIfAbsent("c|" + relation.containerPos().asLong(), target);
            } else if (relation.signPos() != null) {
                targets.putIfAbsent("s|" + relation.signPos().asLong() + "|" + relation.signSide().name(), target);
            }
        }
        return targets;
    }

    static Box createSignFaceBox(ClientWorld world, HighlightTargetRelation relation) {
        if (world == null || relation == null || relation.signPos() == null) {
            return null;
        }

        Direction faceDirection = resolveSignFaceDirection(world, relation);
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

    private static Direction resolveSignFaceDirection(ClientWorld world, HighlightTargetRelation relation) {
        if (relation.preferredFaceDirection() != null) {
            return relation.signSide() == SignSide.FRONT ? relation.preferredFaceDirection() : relation.preferredFaceDirection().getOpposite();
        }

        BlockState state = world.getBlockState(relation.signPos());
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            return relation.signSide() == SignSide.FRONT ? facing : facing.getOpposite();
        }
        if (state.contains(Properties.ROTATION)) {
            Direction facing = directionFromRotation(state.get(Properties.ROTATION));
            return relation.signSide() == SignSide.FRONT ? facing : facing.getOpposite();
        }
        return null;
    }

    private static Direction directionFromRotation(int rotation) {
        int quadrant = Math.floorMod(Math.round(rotation / 4.0F), 4);
        return switch (quadrant) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }
}
