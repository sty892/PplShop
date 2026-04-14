package styy.pplShop.pplshop.client.gui;

public final class ShopBrowserSessionState {
    private String searchText = "";
    private ShopSortMode sortMode = ShopSortMode.NAME;
    private ShopTheme theme = ShopTheme.ALL;
    private int scrollRowOffset;

    public String searchText() {
        return this.searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText == null ? "" : searchText;
    }

    public ShopSortMode sortMode() {
        return this.sortMode;
    }

    public void setSortMode(ShopSortMode sortMode) {
        this.sortMode = sortMode == null ? ShopSortMode.NAME : sortMode;
    }

    public ShopTheme theme() {
        return this.theme;
    }

    public void setTheme(ShopTheme theme) {
        this.theme = theme == null ? ShopTheme.ALL : theme;
    }

    public int scrollRowOffset() {
        return this.scrollRowOffset;
    }

    public void setScrollRowOffset(int scrollRowOffset) {
        this.scrollRowOffset = Math.max(0, scrollRowOffset);
    }

    public void clear() {
        this.searchText = "";
        this.sortMode = ShopSortMode.NAME;
        this.theme = ShopTheme.ALL;
        this.scrollRowOffset = 0;
    }
}
