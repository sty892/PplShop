package styy.pplShop.pplshop.client.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import styy.pplShop.pplshop.client.model.ShopSignEntry;
import styy.pplShop.pplshop.client.model.SignContainerRelation;
import styy.pplShop.pplshop.client.model.SignSide;

public record HighlightTargetRelation(
        BlockPos signPos,
        BlockPos containerPos,
        boolean linkedContainer,
        SignSide signSide,
        boolean hangingSign,
        Direction preferredFaceDirection,
        String relationKind
) {
    public HighlightTargetRelation {
        signPos = signPos == null ? null : signPos.toImmutable();
        containerPos = containerPos == null ? null : containerPos.toImmutable();
        signSide = signSide == null ? SignSide.FRONT : signSide;
        relationKind = relationKind == null ? "" : relationKind;
    }

    public static HighlightTargetRelation fromEntry(ShopSignEntry entry) {
        if (entry == null) {
            return new HighlightTargetRelation(null, null, false, SignSide.FRONT, false, null, "");
        }

        SignContainerRelation relation = entry.relation();
        return new HighlightTargetRelation(
                entry.pos(),
                relation.containerPos(),
                relation.linked() && relation.containerPos() != null,
                entry.snapshot().side(),
                entry.snapshot().hanging(),
                resolvePreferredFaceDirection(entry),
                relation.relationKind()
        );
    }

    private static Direction resolvePreferredFaceDirection(ShopSignEntry entry) {
        SignContainerRelation relation = entry.relation();
        if (!relation.linked() || relation.containerPos() == null) {
            return null;
        }

        int deltaX = entry.pos().getX() - relation.containerPos().getX();
        int deltaZ = entry.pos().getZ() - relation.containerPos().getZ();
        if (Math.abs(deltaX) >= Math.abs(deltaZ) && deltaX != 0) {
            return deltaX > 0 ? Direction.EAST : Direction.WEST;
        }
        if (deltaZ != 0) {
            return deltaZ > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return null;
    }
}
