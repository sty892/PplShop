package styy.pplShop.pplshop.client.world;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ShopSignValidator {
    private final ParserRulesConfig rules;
    private final Set<Block> allowedContainerBlocks = new HashSet<>();

    public ShopSignValidator(ParserRulesConfig rules) {
        this.rules = rules;
        for (String blockId : rules.allowed_container_block_ids) {
            Identifier identifier = Identifier.tryParse(blockId);
            if (identifier != null) {
                Block block = Registries.BLOCK.get(identifier);
                if (block != null) {
                    this.allowedContainerBlocks.add(block);
                }
            }
        }
    }

    public boolean isPotentialShopSign(ClientWorld world, BlockPos signPos, List<String> lines) {
        if (this.rules.require_near_barrel && !this.hasAllowedContainerNearby(world, signPos)) {
            return false;
        }

        return !this.shouldIgnore(lines);
    }

    private boolean hasAllowedContainerNearby(ClientWorld world, BlockPos signPos) {
        int radius = Math.max(0, this.rules.barrel_search_radius);
        for (BlockPos candidate : BlockPos.iterateOutwards(signPos, radius, radius, radius)) {
            BlockEntity blockEntity = world.getBlockEntity(candidate);
            if (blockEntity == null) {
                continue;
            }

            Block block = world.getBlockState(candidate).getBlock();
            if (this.allowedContainerBlocks.contains(block)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldIgnore(List<String> lines) {
        if (this.rules.ignored_sign_contains.isEmpty()) {
            return false;
        }

        String normalizedCombined = NormalizationUtils.normalizeWithoutSorting(String.join(" ", lines), this.rules);
        for (String ignored : this.rules.ignored_sign_contains) {
            String normalizedIgnored = NormalizationUtils.normalizeWithoutSorting(ignored, this.rules);
            if (!normalizedIgnored.isBlank() && normalizedCombined.contains(normalizedIgnored)) {
                return true;
            }
        }
        return false;
    }
}
