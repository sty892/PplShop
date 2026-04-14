package styy.pplShop.pplshop.client.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;

public record WorldCacheIdentity(
        String worldKey,
        String displayName,
        Identifier dimensionId,
        String sessionFingerprint
) {
    public WorldCacheIdentity {
        worldKey = safe(worldKey);
        displayName = safe(displayName);
        sessionFingerprint = safe(sessionFingerprint);
    }

    public static WorldCacheIdentity resolve(MinecraftClient client) {
        if (client == null || client.world == null) {
            return null;
        }

        Identifier dimensionId = client.world.getRegistryKey().getValue();
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null && serverInfo.address != null && !serverInfo.address.isBlank()) {
            String address = serverInfo.address.trim().toLowerCase();
            return new WorldCacheIdentity(
                    "multiplayer:" + address,
                    address,
                    dimensionId,
                    address
            );
        }

        IntegratedServer integratedServer = client.getServer();
        if (integratedServer != null) {
            String levelName = integratedServer.getSaveProperties().getLevelName();
            String safeLevelName = levelName == null || levelName.isBlank() ? "singleplayer" : levelName.trim();
            return new WorldCacheIdentity(
                    "singleplayer:" + safeLevelName.toLowerCase(),
                    safeLevelName,
                    dimensionId,
                    safeLevelName
            );
        }

        return new WorldCacheIdentity(
                "unknown:" + dimensionId,
                "unknown",
                dimensionId,
                dimensionId.toString()
        );
    }

    public String scopedKey() {
        return this.worldKey + "|" + this.dimensionId;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
