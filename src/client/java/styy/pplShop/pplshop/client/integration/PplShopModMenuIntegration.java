package styy.pplShop.pplshop.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import styy.pplShop.pplshop.client.PplshopClient;

public final class PplShopModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            PplshopClient client = PplshopClient.instance();
            return client == null ? parent : client.createConfigScreen(parent);
        };
    }
}
