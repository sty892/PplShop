package styy.pplShop.pplshop.client.normalize;

import org.junit.jupiter.api.Test;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NormalizationUtilsTest {
    private final ParserRulesConfig rules = ParserRulesConfig.defaults();

    @Test
    void normalizeForLookupPreservesWordOrder() {
        assertEquals(
                "\u043a\u043e\u0440\u043d\u0438\u0441\u0442\u0430\u044f \u0437\u0435\u043c\u043b\u044f",
                NormalizationUtils.normalizeForLookup("\u041a\u043e\u0440\u043d\u0438\u0441\u0442\u0430\u044f \u0437\u0435\u043c\u043b\u044f", this.rules)
        );
    }

    @Test
    void normalizeForLookupStripsDecorativeUnicodeNoise() {
        assertEquals(
                "\u0444\u0435\u0439\u0435\u0440\u0432\u0435\u0440\u043a\u0438",
                NormalizationUtils.normalizeForLookup("\ud83d\udd25\u0424\u0435\u0439\u0435\u0440\u0432\u0435\u0440\u043a\u0438\ud83d\udd25", this.rules)
        );
        assertEquals(
                "schnitzel",
                NormalizationUtils.normalizeForLookup("==SCHNITZEL==", this.rules)
        );
    }

    @Test
    void verifiedAliasKeysNormalizeToExpectedLookupValues() {
        assertEquals("\u0431\u0430\u0437\u0430\u043b\u044c\u0442", NormalizationUtils.normalizeWithoutSorting("\u0411\u0430\u0437\u0430\u043b\u044c\u0442", this.rules));
        assertEquals("\u0431\u0443\u0442\u044b\u043b\u043e\u0447\u043a\u0438\u0435 \u043e\u043f\u044b\u0442\u0430", NormalizationUtils.normalizeWithoutSorting("\u0411\u0443\u0442\u044b\u043b\u043e\u0447\u043a\u0438\u0435 \u043e\u043f\u044b\u0442\u0430", this.rules));
        assertEquals("\u0431\u0443\u0442\u044b\u043b\u043e\u0447\u043a\u0438 \u043c\u0435\u0434\u0430", NormalizationUtils.normalizeWithoutSorting("\u0411\u0443\u0442\u044b\u043b\u043e\u0447\u043a\u0438 \u043c\u0435\u0434\u0430", this.rules));
        assertEquals("\u0437\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u043d\u044b\u0435 \u044f\u0431\u043b\u043e\u043a\u0438", NormalizationUtils.normalizeWithoutSorting("\u0417\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u043d\u044b\u0435 \u044f\u0431\u043b\u043e\u043a\u0438", this.rules));
        assertEquals("\u0442\u0443\u0444 \u043a\u0438\u0440\u043f\u0438\u0447\u0438", NormalizationUtils.normalizeWithoutSorting("\u0422\u0443\u0444 \u043a\u0438\u0440\u043f\u0438\u0447\u0438", this.rules));
        assertEquals("\u0448\u0430\u0431\u043b\u043e\u043d \u0448\u043f\u0438\u043b\u044c", NormalizationUtils.normalizeWithoutSorting("\u0428\u0430\u0431\u043b\u043e\u043d \u0448\u043f\u0438\u043b\u044c", this.rules));
        assertEquals("\u043c\u043e\u0438 \u044f\u0439\u0446\u0430", NormalizationUtils.normalizeWithoutSorting("\u041c\u043e\u0438 \u044f\u0439\u0446\u0430", this.rules));
        assertEquals("tnt", NormalizationUtils.normalizeWithoutSorting("TNT", this.rules));
        assertEquals("\u0432\u043e\u0440\u043e\u043d\u043a\u0430 \u0432\u043e\u0440\u043e\u043d\u043a\u0438", NormalizationUtils.normalizeWithoutSorting("\u0412\u043e\u0440\u043e\u043d\u043a\u0430 \u0432\u043e\u0440\u043e\u043d\u043a\u0438", this.rules));
        assertEquals("\u043d\u0435\u0432\u0438\u0434\u0438\u043c\u043e\u0441\u0442\u044c", NormalizationUtils.normalizeWithoutSorting("\u041d\u0435\u0432\u0438\u0434\u0438\u043c\u043e\u0441\u0442\u044c", this.rules));
        assertEquals("\u0438\u0441\u0446\u0435\u043b\u0435\u043d\u0438\u0435 ii", NormalizationUtils.normalizeWithoutSorting("\u0418\u0441\u0446\u0435\u043b\u0435\u043d\u0438\u0435 II", this.rules));
        assertEquals("\u043c\u0435\u0440\u0442\u0432\u044b\u0439 \u043a\u0443\u0441\u0442", NormalizationUtils.normalizeWithoutSorting("\u041c\u0451\u0440\u0442\u0432\u044b\u0439 \u043a\u0443\u0441\u0442", this.rules));
        assertEquals("\u043e\u0433\u043d\u0435\u043d\u043d\u044b\u0435 \u0441\u0442\u0435\u0440\u0436\u043d", NormalizationUtils.normalizeWithoutSorting("\u041e\u0433\u043d\u0435\u043d\u043d\u044b\u0435 \u0441\u0442\u0435\u0440\u0436\u043d", this.rules));
        assertEquals("\u0440\u043e\u0437 \u043b\u0435\u043f\u0435\u0441\u0442\u043a\u0438", NormalizationUtils.normalizeWithoutSorting("\u0420\u043e\u0437 \u043b\u0435\u043f\u0435\u0441\u0442\u043a\u0438", this.rules));
        assertEquals("\u043a\u043d\u0438\u0433\u0438 \u0437\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u0438\u044f", NormalizationUtils.normalizeWithoutSorting("\u041a\u043d\u0438\u0433\u0438 \u0437\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u0438\u044f", this.rules));
        assertEquals("\u0445\u043b\u0435\u0431", NormalizationUtils.normalizeWithoutSorting("\u0425\u043b\u0435\u0431", this.rules));
        assertEquals("\u0433\u0440\u0438\u0431\u043e\u0432\u043e\u0439 \u0441\u0443\u043f", NormalizationUtils.normalizeWithoutSorting("\u0413\u0440\u0438\u0431\u043e\u0432\u043e\u0439 \u0441\u0443\u043f", this.rules));
        assertEquals("\u043c\u0435\u0434\u044c", NormalizationUtils.normalizeWithoutSorting("\u041c\u0435\u0434\u044c", this.rules));
        assertEquals("\u043c\u0435\u0434\u043d\u044b\u0439 \u0431\u043b\u043e\u043a", NormalizationUtils.normalizeWithoutSorting("\u041c\u0435\u0434\u043d\u044b\u0439 \u0431\u043b\u043e\u043a", this.rules));
        assertEquals("\u0441\u0443\u0445\u043e\u0439 \u0433\u0430\u0441\u0442", NormalizationUtils.normalizeWithoutSorting("\u0421\u0443\u0445\u043e\u0439 \u0433\u0430\u0441\u0442", this.rules));
        assertEquals("\u043c\u043e\u043a\u0440\u0430\u044f \u0433\u0443\u0431\u043a\u0430", NormalizationUtils.normalizeWithoutSorting("\u041c\u043e\u043a\u0440\u0430\u044f \u0433\u0443\u0431\u043a\u0430", this.rules));
        assertEquals("\u0437\u0430\u0447\u0430\u0440 \u043a\u043d\u0438\u0433\u0430", NormalizationUtils.normalizeWithoutSorting("\u0417\u0430\u0447\u0430\u0440 \u043a\u043d\u0438\u0433\u0430", this.rules));
        assertEquals("\u043f\u0435\u0440\u0435\u043f\u043b\u0430\u0432\u043b\u0435\u043d\u043d\u044b\u0435 \u0440\u0443\u0434\u044b", NormalizationUtils.normalizeWithoutSorting("\u041f\u0435\u0440\u0435\u043f\u043b\u0430\u0432\u043b\u0435\u043d\u043d\u044b\u0435 \u0440\u0443\u0434\u044b", this.rules));
    }
}
