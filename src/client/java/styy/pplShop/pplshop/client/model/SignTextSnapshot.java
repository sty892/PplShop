package styy.pplShop.pplshop.client.model;

import java.util.List;

public record SignTextSnapshot(
        List<String> lines,
        boolean hanging,
        SignSide side
) {
}
