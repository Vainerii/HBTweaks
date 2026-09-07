package vai.hbtweaks.context.client.notes;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import vai.hbtweaks.context.client.contextmenu.editor.EditorButton;
import vai.hbtweaks.context.client.contextmenu.editor.EditorStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotesScreen extends Screen {

    private static final int PANEL_W = 380;
    private static final int PANEL_H = 210;
    private static final int LIST_W = 130;
    private static final int ROW_H = 12;
    private static final int PAD = 6;

    private static final int COLOR_BG = 0xEE101010;
    private static final int COLOR_BORDER = 0xFF404040;
    private static final int COLOR_TEXT = 0xFFDDDDDD;
    private static final int COLOR_DIM = 0xFF999999;
    private static final int COLOR_SEL = 0xFF2255AA;
    private static final int COLOR_HOVER = 0x40FFFFFF;

    // Vanilla book page sizes
    private static final int NOTE_WIDTH = 114;
    private static final int NOTE_LINE = 9;
    private static final int NOTE_BOX_PAD = 4;
    private static final int NOTE_HEIGHT = 9*14;
    private static final int NOTE_NAV_GAP = 6;
    private static final int COLOR_BOX_BG = 0xFF080808;

    private final Screen parent;
    private final List<String> shown = new ArrayList<>();

    private SpruceTextFieldWidget search;
    private String searchText = "";
    private String selected = null;
    private int page = 0;
    private int scroll = 0;

    private EditorButton renameBtn;
    private EditorButton duplicateBtn;
    private EditorButton editBtn;
    private EditorButton deleteBtn;

    public NotesScreen(Screen parent) {
        super(Component.translatable("hbtweaks.context.notes.title"));
        this.parent = parent;
    }

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - PANEL_H) / 2;
    }

    private int listTop() {
        return top() + PAD + 22;
    }

    private int listHeight() {
        return PANEL_H - (PAD + 22) - (PAD + 24);
    }

    private int visibleRows() {
        return listHeight() / ROW_H;
    }

    @Override
    protected void init() {
        int l = left();
        int t = top();

        this.search = new SpruceTextFieldWidget(Position.of(l + PAD, t + PAD), LIST_W, EditorStyle.FIELD_H,
                Component.translatable("hbtweaks.context.notes.search"));
        this.search.setChangedListener(v -> {
            this.searchText = v;
            this.scroll = 0;
            refresh();
        });
        addRenderableWidget(this.search);

        int by = t + PANEL_H - PAD - EditorStyle.BTN_H;
        int gap = 4;
        int usable = PANEL_W - PAD * 2 - gap * 4;
        int bw = usable / 5;
        int bx = l + PAD;

        addRenderableWidget(new EditorButton(Position.of(bx, by), bw + 1, EditorStyle.BTN_H,
                Component.translatable("hbtweaks.context.notes.new"), this::promptNew));
        bx += bw + 1 + gap;

        this.renameBtn = addRenderableWidget(new EditorButton(Position.of(bx, by), bw + 1, EditorStyle.BTN_H,
                Component.translatable("hbtweaks.context.notes.rename"), this::promptRename));
        bx += bw + 1 + gap;

        this.duplicateBtn = addRenderableWidget(new EditorButton(Position.of(bx, by), bw, EditorStyle.BTN_H,
                Component.translatable("hbtweaks.context.notes.duplicate"), this::promptDuplicate));
        bx += bw + gap;

        this.editBtn = addRenderableWidget(new EditorButton(Position.of(bx, by), bw, EditorStyle.BTN_H,
                Component.translatable("hbtweaks.context.notes.edit"), this::edit));
        bx += bw + gap; //inline this

        this.deleteBtn = addRenderableWidget(new EditorButton(Position.of(bx, by), l + PANEL_W - PAD - bx,
                EditorStyle.BTN_H, Component.translatable("hbtweaks.context.notes.delete"), this::promptDelete));

        this.search.setText(this.searchText);
        refresh();
    }

    private void refresh() {
        this.shown.clear();
        String filter = this.search == null ? "" : this.search.getText().trim().toLowerCase(Locale.ROOT);
        for (String key : NotesBank.keys())
            if (filter.isEmpty() || key.toLowerCase(Locale.ROOT).contains(filter))
                this.shown.add(key);

        if (this.selected != null && !this.shown.contains(this.selected))
            this.selected = null;
        if (this.selected == null && !this.shown.isEmpty())
            select(this.shown.getFirst());

        int max = Math.max(0, this.shown.size() - visibleRows());
        this.scroll = Math.min(this.scroll, max);
        updateButtons();
    }

    private void select(String key) {
        this.selected = key;
        this.page = 0;
        updateButtons();
    }

    private void updateButtons() {
        if (this.renameBtn == null) return;
        boolean any = this.selected != null;
        this.renameBtn.setActive(any);
        this.duplicateBtn.setActive(any);
        this.editBtn.setActive(any);
        this.deleteBtn.setActive(any);
    }

    private List<String> pages() {
        return this.selected == null ? List.of() : NotesBank.get(this.selected);
    }

    private void promptNew() {
        this.minecraft.setScreen(new NamePromptScreen(this,
                Component.translatable("hbtweaks.context.notes.new"), "", name -> {
            if (!NotesBank.has(name))
                NotesBank.set(name, List.of(""));
            this.selected = name;
            this.page = 0;
            refresh();
        }));
    }

    private void promptRename() {
        if (this.selected == null) return;
        String from = this.selected;
        this.minecraft.setScreen(new NamePromptScreen(this,
                Component.translatable("hbtweaks.context.notes.rename"), from, name -> {
            NotesBank.rename(from, name);
            this.selected = name;
            refresh();
        }));
    }

    private void promptDuplicate() {
        if (this.selected == null) return;
        String from = this.selected;
        this.minecraft.setScreen(new NamePromptScreen(this,
                Component.translatable("hbtweaks.context.notes.duplicate"), from + " (2)", name -> {
            NotesBank.duplicate(from, name);
            this.selected = name;
            refresh();
        }));
    }

    private void edit() {
        if (this.selected == null) return;
        this.minecraft.setScreen(new NotesEditScreen(this, this.selected, NotesBank.get(this.selected)));
    }

    private void promptDelete() {
        if (this.selected == null) return;
        String key = this.selected;
        this.minecraft.setScreen(new ConfirmScreen(ok -> {
            if (ok) {
                NotesBank.delete(key);
                this.selected = null;
            }
            this.minecraft.setScreen(this);
            refresh();
        },
                Component.translatable("hbtweaks.context.notes.delete"),
                Component.translatable("hbtweaks.context.notes.delete_confirm", key)));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();

        int l = left();
        if (mx >= l + PAD && mx < l + PAD + LIST_W && my >= listTop() && my < listTop() + listHeight()) {
            int index = this.scroll + (my - listTop()) / ROW_H;
            if (index >= 0 && index < this.shown.size()) {
                select(this.shown.get(index));
                return true;
            }
        }

        List<String> pages = pages();
        if (!pages.isEmpty()) {
            int bw = NOTE_WIDTH + NOTE_BOX_PAD * 2;
            String counter = (Math.min(this.page, pages.size() - 1) + 1) + "/" + pages.size();
            int prevW = this.font.width("<");
            int nextW = this.font.width(">");
            int navW = prevW + NOTE_NAV_GAP + nextW + NOTE_NAV_GAP * 2 + this.font.width(counter);
            int nx = boxLeft() + (bw - navW) / 2;
            int ny = boxTop() + boxHeight() + 4;

            if (my >= ny && my < ny + this.font.lineHeight) {
                if (mx >= nx && mx < nx + prevW && this.page > 0) {
                    this.page--;
                    return true;
                }
                int nextX = nx + prevW + NOTE_NAV_GAP;
                if (mx >= nextX && mx < nextX + nextW && this.page < pages.size() - 1) {
                    this.page++;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, this.shown.size() - visibleRows());
        this.scroll = Math.clamp(this.scroll - (int) Math.signum(scrollY), 0, max);
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int l = left();
        int t = top();
        graphics.fill(l - 1, t - 1, l + PANEL_W + 1, t + PANEL_H + 1, COLOR_BORDER);
        graphics.fill(l, t, l + PANEL_W, t + PANEL_H, COLOR_BG);
        // Divider between the list and the preview, stopping above the button row.
        int dx = l + PAD + LIST_W + PAD / 2;
        graphics.fill(dx, t + PAD, dx + 1, t + PANEL_H - PAD - EditorStyle.BTN_H - 4, COLOR_BORDER);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        renderList(graphics, mouseX, mouseY);
        renderPreview(graphics);
    }

    private void renderList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = left() + PAD;
        int top = listTop();
        int rows = visibleRows();

        if (this.shown.isEmpty()) {
            graphics.text(this.font, Component.translatable("hbtweaks.context.notes.none"),
                    l, top, COLOR_DIM, false);
            return;
        }

        for (int i = 0; i < rows && this.scroll + i < this.shown.size(); i++) {
            String key = this.shown.get(this.scroll + i);
            int ry = top + i * ROW_H;
            boolean hovered = mouseX >= l && mouseX < l + LIST_W && mouseY >= ry && mouseY < ry + ROW_H;

            if (key.equals(this.selected))
                graphics.fill(l, ry, l + LIST_W, ry + ROW_H, COLOR_SEL);
            else if (hovered)
                graphics.fill(l, ry, l + LIST_W, ry + ROW_H, COLOR_HOVER);

            graphics.text(this.font, trim(key, LIST_W - 4), l + 2, ry + 2, COLOR_TEXT, false);
        }

    }

    private int boxLeft() {
        int sectionX = left() + PAD + LIST_W + PAD;
        int sectionW = left() + PANEL_W - PAD - sectionX;
        return sectionX + (sectionW - (NOTE_WIDTH + NOTE_BOX_PAD * 2)) / 2;
    }

    private int boxTop() {
        return listTop();
    }

    private int boxHeight() {
        return NOTE_HEIGHT + NOTE_BOX_PAD * 2;
    }

    private void renderPreview(GuiGraphicsExtractor graphics) {
        int sectionX = left() + PAD + LIST_W + PAD;

        if (this.selected == null) {
            graphics.text(this.font, Component.translatable("hbtweaks.context.notes.no_selection"),
                    sectionX, boxTop(), COLOR_DIM, false);
            return;
        }

        graphics.text(this.font, trim(this.selected, PANEL_W - LIST_W - PAD * 3),
                sectionX, top() + PAD + 5, COLOR_TEXT, false);

        int bx = boxLeft();
        int bw = NOTE_WIDTH + NOTE_BOX_PAD * 2;
        int by = boxTop();
        int bh = boxHeight();
        graphics.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, COLOR_BORDER);
        graphics.fill(bx, by, bx + bw, by + bh, COLOR_BOX_BG);

        List<String> pages = pages();
        int tx = bx + NOTE_BOX_PAD;
        int ty = by + NOTE_BOX_PAD;

        if (pages.isEmpty()) {
            graphics.text(this.font, Component.translatable("hbtweaks.context.notes.empty"),
                    tx, ty, COLOR_DIM, false);
            return;
        }

        int shownPage = Math.min(this.page, pages.size() - 1);
        List<FormattedCharSequence> lines =
                this.font.split(Component.literal(pages.get(shownPage)), NOTE_WIDTH);
        int max = (bh - NOTE_BOX_PAD * 2) / NOTE_LINE;
        for (int i = 0; i < Math.min(lines.size(), max); i++)
            graphics.text(this.font, lines.get(i), tx, ty + i * NOTE_LINE, COLOR_TEXT, false);

        String counter = (shownPage + 1) + "/" + pages.size();
        int navW = this.font.width("<") + NOTE_NAV_GAP + this.font.width(">")
                + NOTE_NAV_GAP * 2 + this.font.width(counter);
        int nx = bx + (bw - navW) / 2;
        int ny = by + bh + 4;

        graphics.text(this.font, "<", nx, ny, this.page > 0 ? COLOR_TEXT : COLOR_DIM, false);
        graphics.text(this.font, ">", nx + this.font.width("<") + NOTE_NAV_GAP, ny,
                this.page < pages.size() - 1 ? COLOR_TEXT : COLOR_DIM, false);
        graphics.text(this.font, counter,
                nx + this.font.width("<") + NOTE_NAV_GAP + this.font.width(">") + NOTE_NAV_GAP * 2, ny,
                COLOR_DIM, false);
    }

    private String trim(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        return this.font.plainSubstrByWidth(text, maxWidth - this.font.width("...")) + "...";
    }
}
