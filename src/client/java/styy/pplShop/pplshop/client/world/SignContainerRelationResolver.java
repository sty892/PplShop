package styy.pplShop.pplshop.client.world;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.model.SignContainerRelation;
import styy.pplShop.pplshop.client.model.SignTextSnapshot;

import java.util.LinkedHashSet;
import java.util.Set;

public final class SignContainerRelationResolver {
    private final Set<Block> allowedContainerBlocks = new LinkedHashSet<>();

    public SignContainerRelationResolver(ParserRulesConfig rules) {
        for (String blockId : rules.allowed_container_block_ids) {
            Identifier identifier = Identifier.tryParse(blockId);
            if (identifier == null) {
                continue;
            }
            Block block = Registries.BLOCK.get(identifier);
            if (block != null) {
                this.allowedContainerBlocks.add(block);
            }
        }
    }

    public SignContainerRelation resolve(ClientWorld world, BlockPos signPos, SignTextSnapshot snapshot, int radius) {
        if (world == null) {
            return SignContainerRelation.missing();
        }

        BlockPos bestPos = null;
        Block bestBlock = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.iterateOutwards(signPos, Math.max(0, radius), Math.max(0, radius), Math.max(0, radius))) {
            BlockEntity blockEntity = world.getBlockEntity(candidate);
            if (blockEntity == null) {
                continue;
            }

            Block block = world.getBlockState(candidate).getBlock();
            if (!this.allowedContainerBlocks.contains(block)) {
                continue;
            }

            double distance = candidate.getSquaredDistance(signPos);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = candidate.toImmutable();
                bestBlock = block;
            }
        }

        if (bestPos == null || bestBlock == null) {
            return SignContainerRelation.missing();
        }

        Identifier containerId = Registries.BLOCK.getId(bestBlock);
        return new SignContainerRelation(true, containerId, bestPos, snapshot.hanging() ? "hanging-nearby" : "nearby");
    }
}
