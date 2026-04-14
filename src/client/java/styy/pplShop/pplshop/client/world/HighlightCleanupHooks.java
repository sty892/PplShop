package styy.pplShop.pplshop.client.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import styy.pplShop.pplshop.client.model.ShopSignEntry;

import java.util.ArrayList;
import java.util.List;

public final class HighlightCleanupHooks {
    public boolean shouldClearForWorld(MinecraftClient client) {
        return client.world == null || client.player == null;
    }

    public void pruneInvalidTargets(MinecraftClient client, ActiveHighlightState state, double autoClearDistance) {
        List<String> invalidRemovals = new ArrayList<>();
        List<String> proximityRemovals = new ArrayList<>();
        for (ActiveHighlightState.HighlightedTarget target : state.targets()) {
            ShopSignEntry entry = target.entry();
            if (!client.world.getRegistryKey().getValue().equals(entry.dimensionId())) {
                invalidRemovals.add(entry.cacheKey());
                continue;
            }
            if (!client.world.isChunkLoaded(entry.pos()) || client.world.getBlockEntity(entry.pos()) == null) {
                invalidRemovals.add(entry.cacheKey());
                continue;
            }

            HighlightTargetRelation relation = HighlightTargetRelation.fromEntry(entry);
            if (relation.linkedContainer() && relation.containerPos() != null) {
                if (!client.world.isChunkLoaded(relation.containerPos()) || client.world.getBlockEntity(relation.containerPos()) == null) {
                    invalidRemovals.add(entry.cacheKey());
                    continue;
                }
            }

            if (!this.shouldKeepByInteraction(client, entry) && this.closestRelevantDistance(client, entry) <= autoClearDistance) {
                proximityRemovals.add(entry.cacheKey());
            }
        }

        for (String cacheKey : invalidRemovals) {
            state.remove(cacheKey, ActiveHighlightState.MutationTrigger.OTHER);
        }
        for (String cacheKey : proximityRemovals) {
            state.remove(cacheKey, ActiveHighlightState.MutationTrigger.PROXIMITY);
        }
    }

    private boolean shouldKeepByInteraction(MinecraftClient client, ShopSignEntry entry) {
        HighlightTargetRelation relation = HighlightTargetRelation.fromEntry(entry);
        if (!relation.linkedContainer() || relation.containerPos() == null) {
            return false;
        }
        if (client.currentScreen != null && relation.containerPos().equals(statefulContainerPos(client, relation.containerPos()))) {
            return true;
        }
        return client.crosshairTarget instanceof BlockHitResult blockHitResult
                && relation.containerPos().equals(blockHitResult.getBlockPos());
    }

    private Vec3d anchorVec(ShopSignEntry entry) {
        HighlightTargetRelation relation = HighlightTargetRelation.fromEntry(entry);
        if (relation.linkedContainer() && relation.containerPos() != null) {
            return Vec3d.ofCenter(relation.containerPos());
        }
        return Vec3d.ofCenter(entry.pos());
    }

    private double closestRelevantDistance(MinecraftClient client, ShopSignEntry entry) {
        double signDistance = client.player.getPos().distanceTo(Vec3d.ofCenter(entry.pos()));
        HighlightTargetRelation relation = HighlightTargetRelation.fromEntry(entry);
        if (relation.linkedContainer() && relation.containerPos() != null) {
            double containerDistance = client.player.getPos().distanceTo(this.anchorVec(entry));
            return Math.min(signDistance, containerDistance);
        }
        return signDistance;
    }

    private net.minecraft.util.math.BlockPos statefulContainerPos(MinecraftClient client, net.minecraft.util.math.BlockPos fallback) {
        return client.crosshairTarget instanceof BlockHitResult blockHitResult ? blockHitResult.getBlockPos() : fallback;
    }
}
