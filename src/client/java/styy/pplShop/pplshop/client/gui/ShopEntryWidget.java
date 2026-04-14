package styy.pplShop.pplshop.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import styy.pplShop.pplshop.client.model.ParseStatus;
import styy.pplShop.pplshop.client.model.ShopSignEntry;

public final class ShopEntryWidget extends ClickableWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger("PPLShop");
    private static final PlayerFacingItemDisplayResolver DISPLAY_RESOLVER = new PlayerFacingItemDisplayResolver();

    private final boolean debugLoggingEnabled;
    private Model model;
    private PressAction pressAction;
    private boolean hoveredByMouse;
    private String lastRenderDebugSignature = "";

    public ShopEntryWidget(int x, int y, int width, int height, boolean debugLoggingEnabled, Model model, PressAction pressAction) {
        super(x, y, width, height, Text.literal(model == null ? "" : model.displayName()));
        this.debugLoggingEnabled = debugLoggingEnabled;
        this.setModel(model, pressAction);
    }

    public ShopSignEntry entry() {
        return this.model == null ? null : this.model.entry();
    }

    public Model model() {
        return this.model;
    }

    public void setPositionAndSize(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.setWidth(width);
        this.setHeight(height);
    }

    public void setModel(Model model, PressAction pressAction) {
        this.model = model;
        this.pressAction = pressAction;
        this.hoveredByMouse = false;
        this.visible = model != null;
        this.active = model != null;
        this.lastRenderDebugSignature = "";
        this.setMessage(Text.literal(model == null ? "" : model.displayName()));
        this.logWidgetBinding();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.model == null || this.pressAction == null || !this.visible || !this.active) {
            return false;
        }
        if (!this.isMouseOver(mouseX, mouseY) || (button != 0 && button != 1)) {
            return false;
        }

        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
        this.pressAction.onPress(this.model.entry(), button);
        return true;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (this.model == null || !this.visible) {
            return;
        }

        this.hoveredByMouse = this.isMouseOver(mouseX, mouseY);

        int background = this.hoveredByMouse ? 0xD0393127 : 0xBC201C17;
        if (this.model.priceColors().tinted()) {
            background = mixColor(background, this.model.priceColors().cardOverlayColor(), this.hoveredByMouse ? 0.9F : 1.0F);
        }
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, background);
        int borderColor = this.hoveredByMouse
                ? mixColor(this.model.priceColors().borderColor(), 0xFFE0B96A, 0.35F)
                : this.model.priceColors().borderColor();
        context.drawBorder(this.getX(), this.getY(), this.width, this.height, borderColor);
        context.createNewRootLayer();

        ItemStack stack = this.model.widgetStack();
        int iconX = this.getX() + (this.width - 16) / 2;
        int iconY = this.getY() + 3;
        boolean itemDrawCalled = false;
        if (!stack.isEmpty()) {
            context.drawItemWithoutEntity(stack, iconX, iconY);
            context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, iconX, iconY);
            itemDrawCalled = true;
        }
        this.logRenderState(stack, iconX, iconY, itemDrawCalled, 1.0F, 1.0F, false);
        context.createNewRootLayer();

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        String name = this.model.displayName();
        String price = this.model.entry().priceDisplay();
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(ellipsize(textRenderer, name, this.width - 4)), this.getX() + this.width / 2, this.getY() + 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(ellipsize(textRenderer, price, this.width - 4)), this.getX() + this.width / 2, this.getY() + 29, this.model.priceColors().priceTextColor());
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    private static String ellipsize(net.minecraft.client.font.TextRenderer textRenderer, String text, int maxWidth) {
        String trimmed = textRenderer.trimToWidth(text, Math.max(8, maxWidth));
        if (trimmed.length() < text.length() && maxWidth > textRenderer.getWidth("...")) {
            String shortened = textRenderer.trimToWidth(text, Math.max(8, maxWidth - textRenderer.getWidth("...")));
            return shortened + "...";
        }
        return trimmed;
    }

    private void logWidgetBinding() {
        if (this.model == null || !this.debugLoggingEnabled) {
            return;
        }

        String widgetPriceText = this.model.entry().priceDisplay();
        LOGGER.debug(
                "[GUI bind] raw='{}' displayTitle='{}' parsedPriceText='{}' priceText='{}' parsedItemId={} resolvedItemId={} widgetItemId={} renderedItemStackItemId={} widgetTitle='{}' widgetPriceText='{}' fallbackUsed={} fallbackReason='{}'",
                sanitize(this.model.rawSignText()),
                sanitize(this.model.displayNameBeforeWidget()),
                sanitize(this.model.entry().parsedPrice().normalizedDisplayText()),
                sanitize(widgetPriceText),
                this.model.parsedItemId(),
                this.model.resolvedItemId(),
                this.model.widgetItemId(),
                this.model.widgetStackItemId(),
                sanitize(this.model.displayName()),
                sanitize(widgetPriceText),
                this.model.fallbackUsed(),
                sanitize(this.model.fallbackReason())
        );

        LOGGER.debug(
                "[GUI bind detail] rawLines='{}' parsedItemRaw='{}' parsedItemAlias='{}' itemStatus={} widgetStack={} diagnosticReason='{}'",
                sanitize(this.model.rawSignLines()),
                sanitize(this.model.parsedItemRaw()),
                sanitize(this.model.parsedItemAlias()),
                this.model.itemStatus(),
                describeStack(this.model.widgetStack()),
                sanitize(this.model.diagnosticReason())
        );
    }

    private void logRenderState(ItemStack stack, int iconX, int iconY, boolean itemDrawCalled, float scaleX, float scaleY, boolean scissorActive) {
        if (this.model == null || !this.debugLoggingEnabled) {
            return;
        }

        String widgetPriceText = this.model.entry().priceDisplay();
        String debugSignature = sanitize(this.model.rawSignText())
                + "|" + String.valueOf(this.model.resolvedItemId())
                + "|" + String.valueOf(this.model.widgetItemId())
                + "|" + String.valueOf(this.model.widgetStackItemId())
                + "|" + describeStack(stack)
                + "|" + widgetPriceText
                + "|" + this.getX()
                + "|" + this.getY()
                + "|" + this.width
                + "|" + this.height
                + "|" + iconX
                + "|" + iconY
                + "|" + itemDrawCalled;
        if (debugSignature.equals(this.lastRenderDebugSignature)) {
            return;
        }

        this.lastRenderDebugSignature = debugSignature;
        LOGGER.debug(
                "[GUI render] raw='{}' displayTitle='{}' parsedPriceText='{}' priceText='{}' parsedItemId={} resolvedItemId={} widgetItemId={} renderedItemStackItemId={} widgetTitle='{}' widgetPriceText='{}' fallbackUsed={} fallbackReason='{}'",
                sanitize(this.model.rawSignText()),
                sanitize(this.model.displayNameBeforeWidget()),
                sanitize(this.model.entry().parsedPrice().normalizedDisplayText()),
                sanitize(widgetPriceText),
                this.model.parsedItemId(),
                this.model.resolvedItemId(),
                this.model.widgetItemId(),
                this.model.widgetStackItemId(),
                sanitize(this.model.displayName()),
                sanitize(widgetPriceText),
                this.model.fallbackUsed(),
                sanitize(this.model.fallbackReason())
        );

        LOGGER.debug(
                "[GUI render detail] rawLines='{}' parsedItemRaw='{}' parsedItemAlias='{}' itemStatus={} widgetRect={}x{} {}x{} icon={}x{} renderItemStackId={} scale={}x{} scissorActive={} itemDrawCalled={} widgetStack={} diagnosticReason='{}'",
                sanitize(this.model.rawSignLines()),
                sanitize(this.model.parsedItemRaw()),
                sanitize(this.model.parsedItemAlias()),
                this.model.itemStatus(),
                this.getX(),
                this.getY(),
                this.width,
                this.height,
                iconX,
                iconY,
                this.model.widgetStackItemId(),
                scaleX,
                scaleY,
                scissorActive,
                itemDrawCalled,
                describeStack(stack),
                sanitize(this.model.diagnosticReason())
        );
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "EMPTY";
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        return itemId + " x" + stack.getCount();
    }

    private static String sanitize(String raw) {
        return raw == null ? "" : raw.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static int mixColor(int from, int to, float progress) {
        int alpha = Math.round(lerp(channel(from, 24), channel(to, 24), progress));
        int red = Math.round(lerp(channel(from, 16), channel(to, 16), progress));
        int green = Math.round(lerp(channel(from, 8), channel(to, 8), progress));
        int blue = Math.round(lerp(channel(from, 0), channel(to, 0), progress));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int channel(int color, int shift) {
        return color >> shift & 0xFF;
    }

    private static float lerp(int from, int to, float progress) {
        return from + (to - from) * progress;
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(ShopSignEntry entry, int button);
    }

    public record Model(
            ShopSignEntry entry,
            String rawSignLines,
            String rawSignText,
            String parsedItemRaw,
            String parsedItemAlias,
            String displayNameBeforeWidget,
            Identifier parsedItemId,
            Identifier resolvedItemId,
            ParseStatus itemStatus,
            Identifier widgetItemId,
            Identifier widgetStackItemId,
            ItemStack widgetStack,
            String displayName,
            String internalDisplayTitle,
            PriceColorResolver.PriceColors priceColors,
            boolean fallbackUsed,
            String fallbackReason,
            String diagnosticReason
    ) {
        public Model {
            widgetStack = widgetStack == null ? ItemStack.EMPTY : widgetStack.copy();
            priceColors = priceColors == null ? PriceColorResolver.PriceColors.neutral() : priceColors;
        }

        public static Model fromEntry(ShopSignEntry entry, PriceColorResolver.PriceColors priceColors) {
            Identifier parsedItemId = entry.parsedItem().itemId();
            Identifier resolvedItemId = entry.resolvedItemId();
            String displayNameBeforeWidget = entry.displayName();
            PlayerFacingItemDisplayResolver.DisplayInfo presentation = DISPLAY_RESOLVER.resolve(entry);
            String displayName = presentation.playerFacingTitle();
            if (displayName == null || displayName.isBlank()) {
                displayName = presentation.internalTitle();
            }

            return new Model(
                    entry,
                    String.join(" || ", entry.snapshot().lines()),
                    entry.rawCombinedText(),
                    entry.parsedItem().rawText(),
                    entry.parsedItem().matchedAlias(),
                    displayNameBeforeWidget,
                    parsedItemId,
                    resolvedItemId,
                    entry.parsedItem().parseStatus(),
                    presentation.widgetItemId(),
                    presentation.widgetStackItemId(),
                    presentation.widgetStack(),
                    displayName,
                    presentation.internalTitle(),
                    priceColors,
                    presentation.fallbackUsed(),
                    presentation.fallbackReason(),
                    determineDiagnosticReason(resolvedItemId, presentation, displayName)
            );
        }

        @Override
        public ItemStack widgetStack() {
            return this.widgetStack.copy();
        }

        private static String determineDiagnosticReason(Identifier resolvedItemId, PlayerFacingItemDisplayResolver.DisplayInfo presentation, String displayName) {
            if (presentation.fallbackUsed()) {
                return presentation.fallbackReason();
            }
            if (resolvedItemId != null && presentation.widgetStack().isEmpty()) {
                return "alias matched but registry item missing";
            }
            if (resolvedItemId != null && presentation.widgetStackItemId() != null && !resolvedItemId.equals(presentation.widgetStackItemId())) {
                return "resolved item exists but widget item id mismatch";
            }
            if (displayName == null || displayName.isBlank() || Text.translatable("item.pplshop.unknown").getString().equals(displayName)) {
                return "title missing";
            }
            return "none";
        }
    }
}

