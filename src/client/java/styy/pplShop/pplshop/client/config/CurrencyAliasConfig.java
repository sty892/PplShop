package styy.pplShop.pplshop.client.config;

import java.util.ArrayList;
import java.util.List;

public final class CurrencyAliasConfig {
    public List<CurrencyDefinition> currencies = new ArrayList<>();

    public static CurrencyAliasConfig defaults() {
        CurrencyAliasConfig config = new CurrencyAliasConfig();
        config.currencies.add(new CurrencyDefinition(
                "diamond_block",
                "minecraft:diamond_block",
                List.of("аб", "алмазный блок", "алм блок", "diamond block", "diamond blocks")
        ));
        config.currencies.add(new CurrencyDefinition(
                "diamond",
                "minecraft:diamond",
                List.of("алмаз", "алм", "алмазов", "diamond", "diamonds")
        ));
        return config;
    }

    public record CurrencyDefinition(String key, String item_id, List<String> aliases) {
    }
}
