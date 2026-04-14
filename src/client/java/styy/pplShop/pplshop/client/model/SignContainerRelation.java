package styy.pplShop.pplshop.client.model;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record SignContainerRelation(
        boolean linked,
        Identifier containerBlockId,
        BlockPos containerPos,
        String relationKind
) {
    public SignContainerRelation {
        containerPos = containerPos == null ? null : containerPos.toImmutable();
        relationKind = relationKind == null ? "" : relationKind;
    }

    public static SignContainerRelation missing() {
        return new SignContainerRelation(false, null, null, "");
    }

    public String signature() {
        String pos = this.containerPos == null ? "none" : this.containerPos.asLong() + "";
        String blockId = this.containerBlockId == null ? "none" : this.containerBlockId.toString();
        return blockId + "|" + pos + "|" + this.relationKind;
    }
}
