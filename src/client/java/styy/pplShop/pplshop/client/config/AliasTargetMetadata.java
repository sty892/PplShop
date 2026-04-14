package styy.pplShop.pplshop.client.config;

import net.minecraft.util.Identifier;

public record AliasTargetMetadata(
        String bucketId,
        Identifier runtimeItemId,
        String subtypeKey,
        String displayNameOverride
) {
    public AliasTargetMetadata {
        bucketId = bucketId == null ? "" : bucketId;
        subtypeKey = subtypeKey == null ? "" : subtypeKey;
        displayNameOverride = displayNameOverride == null ? "" : displayNameOverride;
    }

    public static AliasTargetMetadata direct(String bucketId, Identifier runtimeItemId) {
        return new AliasTargetMetadata(bucketId, runtimeItemId, "", "");
    }
}
