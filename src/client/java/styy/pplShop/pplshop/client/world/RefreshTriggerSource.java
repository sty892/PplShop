package styy.pplShop.pplshop.client.world;

public enum RefreshTriggerSource {
    NONE,
    WORLD_JOIN,
    MANUAL_CONFIRMED,
    GUI_BUTTON,
    GUI_LOW_ENTRY_AUTOREFRESH,
    SCHEDULED;

    public String translationKey() {
        return "message.pplshop.refresh_trigger." + this.name().toLowerCase();
    }
}
