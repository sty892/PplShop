package styy.pplShop.pplshop.client.model;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.util.List;

public record ShopSignFingerprint(
        Identifier dimensionId,
        BlockPos pos,
        SignSide side,
        int normalizedTextHash,
        String relationSignature
) {
    public ShopSignFingerprint {
        pos = pos.toImmutable();
        relationSignature = relationSignature == null ? "" : relationSignature;
    }

    public static ShopSignFingerprint create(
            Identifier dimensionId,
            BlockPos pos,
            SignTextSnapshot snapshot,
            SignContainerRelation relation,
            ParserRulesConfig rules
    ) {
        List<String> lines = snapshot.lines();
        String normalizedText = NormalizationUtils.normalizeForLookup(String.join("\n", lines), rules);
        String relationSignature = relation == null ? "" : relation.signature();
        return new ShopSignFingerprint(
                dimensionId,
                pos,
                snapshot.side(),
                normalizedText.hashCode(),
                relationSignature
        );
    }
}
