package vai.hbtweaks.context.client.notes;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Copy of the vanilla writable book screen without the Sign button, and saves to NotesBank instead
 */
public class NotesEditScreen extends Screen {

    private static final int TEXT_WIDTH = 114;
    private static final int TEXT_HEIGHT = 126;
    private static final int IMAGE_WIDTH = 192;
    private static final int IMAGE_HEIGHT = 192;
    private static final int MAX_PAGES = 100;

    private final Screen parent;
    private final String key;
    private final List<String> pages = new ArrayList<>();

    private int currentPage;
    private PageButton forwardButton;
    private PageButton backButton;
    private Component numberOfPages = CommonComponents.EMPTY;
    private MultiLineEditBox page;

    public NotesEditScreen(Screen parent, String key, List<String> pages) {
        super(Component.literal(key));
        this.parent = parent;
        this.key = key;
        this.pages.addAll(pages);
        if (this.pages.isEmpty())
            this.pages.add("");
    }

    private int getNumPages() {
        return this.pages.size();
    }

    private int backgroundLeft() {
        return (this.width - IMAGE_WIDTH) / 2;
    }

    private int backgroundTop() {
        return 2;
    }

    private int menuControlsTop() {
        return backgroundTop() + IMAGE_HEIGHT + 2;
    }

    @Override
    protected void init() {
        int left = backgroundLeft();
        int top = backgroundTop();

        this.page = MultiLineEditBox.builder()
                .setShowDecorations(false)
                .setTextColor(-16777216)
                .setCursorColor(-16777216)
                .setShowBackground(false)
                .setTextShadow(false)
                .setX((this.width - TEXT_WIDTH) / 2 - 8)
                .setY(28)
                .build(this.font, 122, 134, CommonComponents.EMPTY);
        this.page.setCharacterLimit(1024);
        this.page.setLineLimit(TEXT_HEIGHT / 9);
        this.page.setValueListener(value -> this.pages.set(this.currentPage, value));
        addRenderableWidget(this.page);
        updatePageContent();

        this.numberOfPages = getPageNumberMessage();
        this.backButton = addRenderableWidget(
                new PageButton(left + 43, top + 157, false, button -> pageBack(), true));
        this.forwardButton = addRenderableWidget(
                new PageButton(left + 116, top + 157, true, button -> pageForward(), true));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            saveChanges();
            this.minecraft.setScreen(this.parent);
        }).pos(this.width / 2 - 49, menuControlsTop()).width(98).build());

        updateButtonVisibility();
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(this.page);
    }

    private Component getPageNumberMessage() {
        return Component.translatable("book.pageIndicator", this.currentPage + 1, getNumPages())
                .withColor(-16777216).withoutShadow();
    }

    private void pageBack() {
        if (this.currentPage > 0) {
            this.currentPage--;
            updatePageContent();
        }
        updateButtonVisibility();
    }

    private void pageForward() {
        if (this.currentPage < getNumPages() - 1) {
            this.currentPage++;
        } else {
            if (getNumPages() < MAX_PAGES)
                this.pages.add("");
            if (this.currentPage < getNumPages() - 1)
                this.currentPage++;
        }
        updatePageContent();
        updateButtonVisibility();
    }

    private void updatePageContent() {
        this.page.setValue(this.pages.get(this.currentPage), true);
        this.numberOfPages = getPageNumberMessage();
    }

    private void updateButtonVisibility() {
        this.backButton.visible = this.currentPage > 0;
    }

    private void eraseEmptyTrailingPages() {
        ListIterator<String> it = this.pages.listIterator(this.pages.size());
        while (it.hasPrevious() && it.previous().isBlank())
            it.remove();
    }

    private void saveChanges() {
        eraseEmptyTrailingPages();
        NotesBank.set(this.key, this.pages);
    }

    @Override
    public void onClose() {
        saveChanges();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case 266 -> {
                this.backButton.onPress(event);
                return true;
            }
            case 267 -> {
                this.forwardButton.onPress(event);
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        visitText(graphics.textRenderer());
    }

    private void visitText(ActiveTextCollector collector) {
        collector.accept(TextAlignment.RIGHT, backgroundLeft() + 148, backgroundTop() + 16, this.numberOfPages);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BookViewScreen.BOOK_LOCATION,
                backgroundLeft(), backgroundTop(), 0.0F, 0.0F, IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);
    }
}
