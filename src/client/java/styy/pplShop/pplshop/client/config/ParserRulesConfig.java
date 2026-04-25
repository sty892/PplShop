package styy.pplShop.pplshop.client.config;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class ParserRulesConfig {
    public List<Integer> preferred_item_lines = List.of(0, 1, 2, 3);
    public List<Integer> preferred_price_lines = List.of(3, 2, 1, 0);
    public List<String> price_prefixes = List.of("цена", "price", "стоимость");
    public List<String> item_prefixes = List.of("товар", "item", "предмет", "sell", "selling");
    public List<String> ignored_characters = List.of(",", ";", ":", "-", "_", "|");
    public List<String> quantity_suffixes = List.of("x", "*", "шт", "штук", "pcs", "pc", "pieces");
    public List<String> price_joiner_words = List.of("за", "for");
    public boolean require_near_barrel = true;
    public int barrel_search_radius = 1;
    public List<String> allowed_container_block_ids = List.of("minecraft:barrel");
    public List<String> ignored_sign_contains = List.of(
            "\u043f\u0440\u0430\u0432\u0438\u043b\u0430",
            "pepeland",
            "\u0440\u0435\u043a\u043b\u0430\u043c\u0430",
            "\u043e\u0431\u044a\u044f\u0432\u043b\u0435\u043d\u0438\u0435",
            "\u0430\u0440\u0435\u043d\u0434\u0430",
            "\u043e\u0431\u043c\u0435\u043d",
            "\u043f\u0438\u0437\u0434\u0430",
            "\u043f\u043e\u043f\u0430",
            "\u043c\u0435\u0444\u0435\u0434\u0440\u043e\u043d\u0447\u0438\u043a",
            "femboy milk",
            "\u0444\u0435\u043c\u0431\u043e\u0439\u0441\u043a\u043e\u0435 \u043c\u043e\u043b\u043e\u043a\u043e",
            "\u0441\u043a\u0438\u043d\u044b \u043d\u0430 \u0437\u0430\u043a\u0430\u0437",
            "\u0441 \u043b\u044e\u0431\u043e\u0432\u044c\u044e",
            "\u0434\u0435\u043d\u044c\u0433\u0438 \u043d\u0430 \u0432\u0435\u0442\u0435\u0440",
            "\u0430\u0440\u0442 \u0441 \u0432\u0430\u043c\u0438",
            "\u043f\u0430\u043b\u044c \u043eff",
            "\u0442\u0430\u0431\u043b\u0438\u0447\u043a\u0430 \u0441 \u0432\u044b"
    );
    public List<String> blacklisted_sign_contains = List.of(
            "\u0440\u0430\u0437\u043d\u043e\u0435",
            "\u0446\u0435\u043d\u043d\u043e\u0441\u0442\u0438",
            "\u0444\u0438\u0433\u0443\u0440\u0430",
            "\u0441\u043a\u0438\u043d",
            "\u0432\u044b\u043a\u0443\u043f\u043b\u0435\u043d\u043e",
            "\u0444\u0430\u043d\u0444\u0438\u043a \u043f\u0440\u043e \u0431\u0435\u0440\u0435\u0437\u0443",
            "\u0431\u0435\u0440\u0435\u0437\u043e\u0432\u044b\u0439 \u0434\u043e\u043c",
            "\u043f\u0438\u0432\u043e",
            "\u0430\u043d\u0430\u043b\u044c\u043d\u0430\u044f \u043f\u0440\u043e\u0431\u043a\u0430",
            "\u0436\u0436\u0435\u043d\u044b\u0439 \u0440\u0438\u0441",
            "\u043a\u043d\u0438\u0436\u043d\u044b\u0439 \u043f\u043e\u043d\u043e\u0441",
            "\u0447\u0430\u0441\u044b rolex",
            "\u0432\u043e\u043b\u0448\u0435\u0431\u043d\u0430\u044f \u0442\u0440\u0430\u0432\u0430",
            "\u0432\u043e\u043b\u0448\u0435\u0431\u043d\u044b\u0439 \u043d\u0430\u0431\u043e\u0440",
            "\u0434\u0440\u0430\u043a\u043e\u043d\u0438\u0439 \u0441\u0435\u0442",
            "\u043b\u0435\u0433\u0435\u043d\u0434\u0430\u0440\u043d\u044b\u0439 \u0441\u0435\u0442",
            "\u043a\u0430\u0441\u0442\u043e\u043c\u043d\u044b\u0435 \u043d\u0435\u0437\u0435\u0440",
            "\u043f\u0435\u0441 \u043c\u0435\u043d\u044c\u0448\u0435\u0432 \u0430\u0442\u043e\u043c\u0430",
            "\u043f\u0451\u0441 \u043c\u0435\u043d\u044c\u0448\u0435\u0432 \u0430\u0442\u043e\u043c\u0430",
            "\u043f\u043e\u0442\u043e\u043f \u043d\u0430 \u0441\u0435\u0440\u0432\u0435\u0440",
            "\u0433\u0430\u0441\u0442\u044b \u043a \u043f\u0438\u0432\u0443",
            "\u0431\u043b\u043e\u043a \u0431\u0443\u0431\u0443",
            "\u0430\u043a\u0441\u043e\u0441\u043e\u043f\u043b\u0435",
            "\u0440\u0430\u0431\u043e\u0442\u0430\u044e\u0449\u0438\u0435 1 \u0441\u043b\u043e\u0442",
            "\u0440\u0430\u0431\u043e\u0442\u0430\u044e\u0449\u0438\u0435 1\u0441\u043b\u043e\u0442",
            "\u0448\u0430\u043b\u043a\u0435\u0440 \u0440\u043e\u0437",
            "\u0448\u0430\u043b\u043a\u0435\u0440\u0430 \u0431\u043b\u043e\u043a\u043e\u0432",
            "\u043d\u0430\u0431\u043e\u0440 \u044e\u043d\u043e\u0433\u043e \u0440\u0435\u0434\u0441\u0442\u043e\u0443\u043d\u0435\u0440\u0430",
            "\u0444\u0435\u043c\u0431\u043e\u0439",
            "\u043f\u043e\u0439\u043b\u043e",
            "\u0431\u0440\u043e\u0441\u044f\u043d\u043a\u0430",
            "pooshka",
            "\u0434\u043e\u0445\u043e\u0434\u044f\u0433\u0438",
            "\u0444\u0438\u043b\u043e\u0441\u043e\u0432\u0441\u043a\u0438\u0439 \u043a\u0430\u043c\u0435\u043d\u044c",
            "\u043a\u0443\u0441\u043e\u0447\u0435\u043a \u0444 \u0438\u044e\u043b\u044c\u0441\u043a\u043e\u0433\u043e \u043d\u0435\u0431\u0430",
            "\u043a\u043e\u043d\u0441\u0442\u0440\u0443\u043a\u0442\u043e\u0440 \u043f\u0430\u043a\u0435\u0442",
            "\u0436\u0435\u043b\u0435\u0437\u043d\u044b\u0435 \u0432\u0435\u0449\u0438",
            "\u043b\u0443\u0442 \u0438\u0437",
            "\u043f\u0435\u0440\u0435\u043f\u043b\u0430\u0432\u043b\u0435\u043d\u043d\u044b\u0435 \u0440\u0443\u0434\u044b",
            "\u043a\u043e\u043b\u0435\u043a\u0446\u0438\u044f \u0440\u0443\u0434",
            "\u043f\u043e\u0434\u0430\u0440\u043a\u0438 \u0432 \u0431\u043e\u0447\u043a\u0435",
            "\u0444\u0444 "
    );
    public Map<String, String> token_replacements = new LinkedHashMap<>();
    public boolean sort_tokens_for_lookup = false;
    public boolean scan_all_lines_for_item = true;
    public boolean scan_all_lines_for_price = true;
    public double highlight_clear_distance = 1.0D;
    public int scan_interval_ticks = 40;

    public static ParserRulesConfig defaults() {
        ParserRulesConfig config = new ParserRulesConfig();
        config.token_replacements.put("красн", "красный");
        config.token_replacements.put("алм.", "алм");
        config.token_replacements.put("алмазных", "алмазный");
        config.token_replacements.put("алмазов", "алмаз");
        config.token_replacements.put("blocks", "block");
        config.token_replacements.put("diamonds", "diamond");
        return config;
    }

    public List<String> combinedBlacklistedSignContains() {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (this.ignored_sign_contains != null) {
            merged.addAll(this.ignored_sign_contains);
        }
        if (this.blacklisted_sign_contains != null) {
            merged.addAll(this.blacklisted_sign_contains);
        }
        return new ArrayList<>(merged);
    }

}
