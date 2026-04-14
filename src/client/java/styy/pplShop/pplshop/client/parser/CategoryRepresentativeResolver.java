package styy.pplShop.pplshop.client.parser;

import net.minecraft.util.Identifier;
import styy.pplShop.pplshop.client.normalize.NormalizationUtils;

import java.util.List;

final class CategoryRepresentativeResolver {
    Match resolve(String normalizedPlain, String normalizedLookup, List<String> contextCandidates) {
        String normalized = !normalizedPlain.isBlank() ? normalizedPlain : normalizedLookup;
        List<String> tokens = NormalizationUtils.tokenize(normalized);
        if (tokens.isEmpty()) {
            return null;
        }

        boolean netherite = hasAny(tokens, "незер", "незерит", "незеритовый", "незеритовая", "незеритовые");
        boolean diamond = hasAny(tokens, "алм", "алмаз", "алмазный", "алмазная", "алмазные");
        boolean armor = hasAny(tokens, "сет", "броня", "доспех", "кастомная", "кастомный", "трим", "тримами");
        boolean tools = hasAny(tokens, "инструменты", "инструмент", "кирки", "кирка", "лопаты", "лопата");

        if (hasAny(tokens, "арбалет", "арбы")) {
            return new Match(Identifier.of("minecraft:crossbow"), "category:crossbow");
        }
        if (hasAny(tokens, "топор", "топоры")) {
            return new Match(Identifier.of(netherite ? "minecraft:netherite_axe" : "minecraft:diamond_axe"), "category:axe");
        }
        if (hasAny(tokens, "ботинки", "боты")) {
            return new Match(Identifier.of(netherite ? "minecraft:netherite_boots" : "minecraft:diamond_boots"), "category:boots");
        }
        if (tools) {
            return new Match(Identifier.of(netherite ? "minecraft:netherite_pickaxe" : "minecraft:diamond_pickaxe"), "category:tools");
        }
        if (armor) {
            return new Match(Identifier.of(netherite ? "minecraft:netherite_chestplate" : "minecraft:diamond_chestplate"), "category:armor");
        }
        if ((diamond || netherite) && contextHintsItem(contextCandidates)) {
            return new Match(Identifier.of(netherite ? "minecraft:netherite_chestplate" : "minecraft:diamond_chestplate"), "category:material_only_with_context");
        }
        return null;
    }

    private static boolean contextHintsItem(List<String> contextCandidates) {
        for (String candidate : contextCandidates) {
            List<String> tokens = NormalizationUtils.tokenize(candidate);
            if (hasAny(tokens, "броня", "сет", "инструменты", "топор", "кирка", "лопата", "ботинки", "арбалет")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAny(List<String> tokens, String... values) {
        for (String value : values) {
            for (String token : tokens) {
                if (token.equals(value) || token.startsWith(value) || value.startsWith(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    record Match(Identifier itemId, String reason) {
    }
}
