package vai.hbtweaks.context.client.contextmenu;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import vai.hbtweaks.context.HBTweaksContext;
import vai.hbtweaks.context.client.config.HBConfig;
import vai.hbtweaks.context.client.contextmenu.editor.AddCommandScreen;
import vai.hbtweaks.context.client.contextmenu.editor.AddScriptScreen;
import vai.hbtweaks.context.client.contextmenu.editor.AddSubmenuScreen;
import vai.hbtweaks.context.client.contextmenu.editor.MenuLocation;
import vai.hbtweaks.context.client.listeners.ContextMenuTrigger;
import vai.hbtweaks.context.client.script.ScriptRunner;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Hand-drawn right-click menu about one player. A menu is a list of rows (items), each possibly
 * opening a submenu to its right on hover, optionally followed by a note block and by the small
 * "+" toggle that enters edit mode.
 * <p>
 * Menus are composed with the chainable add methods; the row types themselves are private.
 * NoteBlock and CheckboxItem are the only extension points. Submenus are built eagerly when the
 * root menu opens, but only the open one is rendered.
 * <p>
 * Edit mode (global, shared by every menu) shows, on each row coming from a custom YAML file,
 * a cluster of six controls: move into the submenu above, move to parent, move up, move down,
 * edit, delete. Built-in root rows get an eye toggle instead, hiding them outside edit mode.
 * <p>
 * The width is computed lazily: every mutator must call markWidthDirty, and ensureWidth then
 * recomputes it at most once per frame.
 */
public class ContextMenu {

    private static final int MIN_WIDTH = 20;
    private static final int ICON_SIZE = 16;

    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_TEXT_HOVER = 0xFFFFFFFF;
    private static final int COLOR_BG = 0xE0101010;
    private static final int COLOR_HOVER = 0xFF2255AA;
    private static final int COLOR_BORDER = 0xFF3A3A3A;
    private static final int COLOR_TOGGLE_BG = 0xFF2A2A2A;

    private static final int TOGGLE_SIZE = 9;
    private static final int TOGGLE_GAP = 1;

    /** Width of one column of the edit cluster; each column holds two half-height controls. */
    private static final int EDIT_CELL = 8;
    private static final int EDIT_CLUSTER_W = EDIT_CELL * 3;

    // Vanilla book page size
    private static final int NOTE_WIDTH = 114;
    private static final int NOTE_HEIGHT = 126;
    private static final int NOTE_LINE = 9;
    private static final int NOTE_PAD = 3;

    /** Note preview drawn below the rows, or null. */
    private NoteBlock noteBlock = null;
    /** Whether this is the top-level menu; only its built-in rows can be hidden. */
    private boolean rootMenu = false;

    /** The six controls of a row's edit cluster. */
    private enum EditControl { UP, DOWN, INTO, TO_PARENT, EDIT, DELETE }

    /** Compact rows, from HBConfig.menuStyle, read once at construction. */
    private final boolean minimalStyle;

    // Minimal style is temporarily ignored while editing
    private boolean minimal() { return this.minimalStyle && !editMode; }
    private int itemHeight() { return minimal() ? 12 : 16; }
    private int paddingX() { return minimal() ? 4 : 6; }
    private int arrowRightPad() { return minimal() ? 6 : 8; }
    private int textOffsetY() { return minimal() ? 2 : 4; }

    private int x;
    private int y;

    /** Master switch for every editing feature (add rows, "+" toggle, Delete key). */
    public static boolean EDIT_ENABLED = true;
    /** Whether edit mode is on. Global so it survives the menu rebuild that follows each edit. */
    public static boolean editMode = false;

    private final List<MenuItem> items = new ArrayList<>();
    /**
     * Parallel to items: the custom YAML entry each row comes from, or null for built-in rows.
     * A non-null entry makes the row editable in edit mode.
     */
    private final List<MenuLocation.DeleteRef> itemDelete = new ArrayList<>();
    private boolean hasEditToggle = false;
    private boolean visible = false;
    private int width = MIN_WIDTH;
    private boolean widthDirty = true;
    /** Submenu currently shown to the right of this menu, or null. */
    private ContextMenu openSubmenu = null;

    /** Wrapped lines of the note page, cached until the page text changes. */
    private List<FormattedCharSequence> noteLines = null;
    private String noteLinesFor = null;

    /** Per-row hidden state, refreshed by ensureWidth so rendering does not scan the config list each frame. */
    private boolean[] hiddenFlags = new boolean[0];

    private Player player;

    /**
     * @param x the left position in GUI pixels; ignored for submenus, placed when opened
     * @param y the top position in GUI pixels; ignored for submenus, placed when opened
     * @param target the player the menu is about, used by commands and placeholders
     */
    public ContextMenu(int x, int y, Player target) {
        this.x = x;
        this.y = y;
        this.player = target;

        this.minimalStyle = HBConfig.get().menuStyle == HBConfig.MenuStyle.MINIMAL;
    }

    public ContextMenu addActionItem(String label, Runnable action) {
        return this.addActionItem(Component.literal(label), action);
    }

    /**
     * Adds a row running arbitrary code when clicked, then closing the menu.
     *
     * @param label the row label
     * @param action the code to run
     * @return this menu
     */
    public ContextMenu addActionItem(Component label, Runnable action) {
        return push(new ActionItem(label, action));
    }

    /** Appends a row with no YAML entry attached. Every add method goes through here. */
    private ContextMenu push(MenuItem item) {
        this.items.add(item);
        this.itemDelete.add(null);
        markWidthDirty();
        return this;
    }

    /**
     * Attaches a custom YAML entry to the last added row, making it editable in edit mode.
     *
     * @param ref the entry the row was built from
     * @return this menu
     */
    public ContextMenu markLastDeletable(MenuLocation.DeleteRef ref) {
        if (!this.itemDelete.isEmpty())
            this.itemDelete.set(this.itemDelete.size() - 1, ref);
        markWidthDirty();
        return this;
    }

    /**
     * Adds the green "add" row, whose submenu opens the dialogs creating a submenu, command or
     * script in the given YAML list. The row is only shown in edit mode, and must be the last
     * one since effectiveItemCount drops it by position.
     *
     * @param container the YAML list new entries are appended to
     * @return this menu
     */
    public ContextMenu addAddItem(MenuLocation container) {
        if (!EDIT_ENABLED) return this;
        ContextMenu sub = new ContextMenu(0, 0, this.player);
        sub.addActionItem(Component.translatable("hbtweaks.context.editor.add_submenu"), () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new AddSubmenuScreen(mc.screen, container));
        });
        sub.addActionItem(Component.translatable("hbtweaks.context.editor.add_command"), () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new AddCommandScreen(mc.screen, container));
        });
        sub.addActionItem(Component.translatable("hbtweaks.context.editor.add_script"), () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new AddScriptScreen(mc.screen, container));
        });
        return push(new AddMenuItem(
                Component.translatable("hbtweaks.context.editor.add").withStyle(ChatFormatting.GREEN), sub));
    }

    /**
     * @return the submenu opened by a row, or null if it opens none
     */
    private static ContextMenu submenuOf(MenuItem item) {
        if (item instanceof SubmenuItem si) return si.submenu;
        if (item instanceof AddMenuItem ai) return ai.submenu;
        return null;
    }

    /**
     * Shows the "+" edit mode toggle below the menu, unless hidden in the config.
     *
     * @return this menu
     */
    public ContextMenu withEditToggle() {
        this.hasEditToggle = EDIT_ENABLED && !HBConfig.get().hidePlusBox;
        return this;
    }

    /**
     * Marks this menu as the top-level one, enabling row hiding.
     *
     * @return this menu
     */
    public ContextMenu asRootMenu() {
        this.rootMenu = true;
        markWidthDirty();
        return this;
    }

    /** Only built-in rows of the root menu can be hidden; custom rows are deleted instead. */
    private boolean isHideable(int i) {
        return this.rootMenu
                && this.itemDelete.get(i) == null
                && !(this.items.get(i) instanceof AddMenuItem);
    }

    /** Hidden rows are stored by label in HBConfig.hiddenMenus. */
    private static boolean isHidden(MenuItem item) {
        return HBConfig.get().hiddenMenus.contains(item.getLabel().getString());
    }

    private static void toggleHidden(MenuItem item) {
        String label = item.getLabel().getString();
        List<String> hidden = HBConfig.get().hiddenMenus;
        if (!hidden.remove(label))
            hidden.add(label);
        HBConfig.HANDLER.save();
        ContextMenuTrigger.rebuildAfterEdit();
    }

    /** Removes hidden rows from the root menu when it opens. Skipped in edit mode, where they show with a closed eye. */
    private void applyHidden() {
        if (!this.rootMenu || editMode) return;
        for (int i = this.items.size() - 1; i >= 0; i--) {
            if (isHideable(i) && isHidden(this.items.get(i))) {
                this.items.remove(i);
                this.itemDelete.remove(i);
            }
        }
        markWidthDirty();
    }

    /**
     * @return the number of drawn rows: every row, minus the trailing "add" row outside edit mode
     */
    private int effectiveItemCount() {
        int n = this.items.size();
        if (!editMode && n > 0 && this.items.get(n - 1) instanceof AddMenuItem) n--;
        return n;
    }

    private int toggleX() {
        return this.x + 1;
    }

    private int toggleY() {
        return this.y + effectiveItemCount() * itemHeight() + noteBlockHeight() + TOGGLE_GAP;
    }

    private boolean isInsideToggle(int mx, int my) {
        int sx = toggleX();
        int sy = toggleY();
        return mx >= sx && mx < sx + TOGGLE_SIZE && my >= sy && my < sy + TOGGLE_SIZE;
    }

    public Player getPlayer() {
        return player;
    }

    public ContextMenu addCommandItem(String label, String commandTemplate) {
        return this.addCommandItem(Component.literal(label), commandTemplate);
    }

    /**
     * Adds a row sending a server command when clicked. Placeholders are resolved at click time.
     *
     * @param label the row label
     * @param commandTemplate the command without "/", placeholders allowed
     * @return this menu
     * @see #replaceString(String, Player)
     */
    public ContextMenu addCommandItem(Component label, String commandTemplate) {
        return push(new CommandItem(label, commandTemplate, player));
    }

    public ContextMenu addInfoItem(String label) {
        return this.addInfoItem(Component.literal(label));
    }

    /**
     * Adds a non-interactive text row.
     *
     * @param label the text
     * @return this menu
     */
    public ContextMenu addInfoItem(Component label) {
        return push(new InfoItem(label));
    }

    public ContextMenu addSubmenuItem(String label, ContextMenu submenu) {
        return this.addSubmenuItem(Component.literal(label), submenu);
    }

    /**
     * Adds a row opening a submenu on hover.
     *
     * @param label the row label
     * @param submenu the submenu
     * @return this menu
     */
    public ContextMenu addSubmenuItem(Component label, ContextMenu submenu) {
        return push(new SubmenuItem(label, submenu));
    }

    /**
     * Adds a checkbox row, which toggles without closing the menu.
     *
     * @param item the checkbox
     * @return this menu
     */
    public ContextMenu addCheckboxItem(CheckboxItem item) {
        return push(item);
    }

    /**
     * Shows the current page of a note below the rows, sized like a vanilla book page.
     *
     * @param block the note source
     * @return this menu
     */
    public ContextMenu withNoteBlock(NoteBlock block) {
        this.noteBlock = block;
        markWidthDirty();
        return this;
    }

    /**
     * Adds the row with the previous / next page arrows and the edit pen of a note.
     *
     * @param block the note source, usually also passed to withNoteBlock
     * @return this menu
     */
    public ContextMenu addNoteBar(NoteBlock block) {
        return push(new NoteBarItem(block));
    }

    /**
     * Source of a paged note shown in a menu. Keeps its own page index, clamped to the current
     * page count, so pages can change underneath it. See NotesMenu for the implementation.
     */
    public abstract static class NoteBlock {
        /**
         * @return the note's pages, possibly empty; read on every frame, so it must be cheap
         */
        public abstract List<String> pages();

        private int page = 0;

        public int page() {
            return Math.min(this.page, Math.max(0, pages().size() - 1));
        }

        /**
         * @return the page count, at least 1 so an empty note still shows "1/1"
         */
        public int pageCount() {
            return Math.max(1, pages().size());
        }

        public boolean hasPrev() {
            return page() > 0;
        }

        public boolean hasNext() {
            return page() < pages().size() - 1;
        }

        public void prev() {
            if (hasPrev()) this.page = page() - 1;
        }

        public void next() {
            if (hasNext()) this.page = page() + 1;
        }

        /** Opens the note editor. */
        public abstract void edit();

        String text() {
            List<String> p = pages();
            return p.isEmpty() ? "" : p.get(page());
        }
    }

    /**
     * @return the height of the note block including its top separator, 0 if there is none
     */
    private int noteBlockHeight() {
        return this.noteBlock == null ? 0 : 1 + NOTE_HEIGHT + NOTE_PAD * 2;
    }

    private int noteBlockY() {
        return this.y + effectiveItemCount() * itemHeight();
    }

    /**
     * Adds a row showing an item with its icon and name, and its tooltip on hover.
     *
     * @param stack the item
     * @return this menu
     */
    public ContextMenu addItemStackItem(ItemStack stack) {
        return push(new ItemStackMenuItem(stack));
    }

    /**
     * Adds a row running a multi-line script through ScriptRunner when clicked.
     *
     * @param label the row label
     * @param lines the script lines
     * @return this menu
     */
    public ContextMenu addScriptItem(Component label, java.util.List<String> lines) {
        return push(new ScriptItem(label, new ArrayList<>(lines), player));
    }

    public ContextMenu addLinkItem(String label, String url) {
        return this.addLinkItem(Component.literal(label), url);
    }

    /**
     * Adds a row opening a URL in the system browser when clicked.
     *
     * @param label the row label
     * @param url the URL
     * @return this menu
     */
    public ContextMenu addLinkItem(Component label, String url) {
        return push(new LinkItem(label, url));
    }

    public ContextMenu addCopyItem(String label, String text) {
        return this.addCopyItem(Component.literal(label), text);
    }

    /**
     * Adds a row copying text to the clipboard when clicked.
     *
     * @param label the row label
     * @param text the copied text
     * @return this menu
     */
    public ContextMenu addCopyItem(Component label, String text) {
        return push(new CopyItem(label, text));
    }

    /** Shows the menu, dropping hidden rows first if it is the root menu. */
    public void open() {
        applyHidden();
        this.visible = true;
    }

    /** Hides the menu and closes its open submenu, recursively. */
    public void close() {
        this.visible = false;
        if (this.openSubmenu != null) {
            this.openSubmenu.close();
            this.openSubmenu = null;
        }
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        render(graphics,
                (int) mc.mouseHandler.getScaledXPos(mc.getWindow()),
                (int) mc.mouseHandler.getScaledYPos(mc.getWindow()),
                tickDelta);
    }

    /**
     * Draws the menu and its open submenu. Also does the hover logic: the hovered submenu row
     * opens its submenu, and the open submenu is closed once the mouse leaves both menus. The
     * menu is clamped to stay on screen.
     */
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, DeltaTracker tickDelta) {
        if (!this.visible) return;

        Minecraft mc = Minecraft.getInstance();

        ensureWidth(); // bc of edit mode

        int itemCount = effectiveItemCount();
        int itemsHeight = itemCount * itemHeight() + noteBlockHeight();
        int footprint = itemsHeight + (this.hasEditToggle ? TOGGLE_GAP + TOGGLE_SIZE : 0);

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        this.x = Math.min(this.x, screenW - this.width - 1);
        this.y = Math.min(this.y, screenH - footprint - 1);

        graphics.fill(this.x - 1,
                this.y - 1,
                this.x + this.width + 1,
                this.y + itemsHeight + 1,
                ContextMenu.COLOR_BORDER);
        graphics.fill(this.x,
                this.y,
                this.x + this.width,
                this.y + itemsHeight,
                ContextMenu.COLOR_BG);

        ContextMenu nextSubmenu = null;
        int nextSubmenuIndex = -1;
        ItemStack hoveredStack = null;

        for (int i = 0; i < itemCount; i++) {
            MenuItem item = this.items.get(i);
            int itemY = this.y + i * itemHeight();

            boolean hovered = isInsideRow(mouseX, mouseY, itemY);
            if (item instanceof InfoItem) {
                hovered = false;
            }

            if (hovered) {
                graphics.fill(this.x, itemY, this.x + this.width, itemY + itemHeight(), ContextMenu.COLOR_HOVER);
            }

            int textColor = hovered ? ContextMenu.COLOR_TEXT_HOVER : ContextMenu.COLOR_TEXT;

            if (item instanceof NoteBarItem bar) {
                drawNoteBar(graphics, mc, bar.block, itemY, mouseX, mouseY);
            } else if (item instanceof ItemStackMenuItem ism) {
                graphics.item(ism.stack, this.x + paddingX(), itemY);
                graphics.text(mc.font, item.getLabel(),
                        this.x + paddingX() + ContextMenu.ICON_SIZE + 2,
                        itemY + textOffsetY(), textColor, false);
                if (hovered) {
                    hoveredStack = ism.stack; // on mémorise, on ne rend pas encore
                }
            } else {
                graphics.text(mc.font, item.getLabel(),
                        this.x + paddingX(), itemY + textOffsetY(), textColor, false);
            }
            boolean showCluster = editMode && this.itemDelete.get(i) != null;

            ContextMenu sub = submenuOf(item);
            if (sub != null) {
                drawSubmenuArrow(graphics, itemY, showCluster);
                if (hovered) {
                    nextSubmenu = sub;
                    nextSubmenuIndex = i;
                }
            }
            if (showCluster)
                drawEditCluster(graphics, mc, i, itemY, mouseX, mouseY);
            else if (editMode && isHideable(i))
                drawEyeToggle(graphics, i, itemY, mouseX, mouseY);
        }

        if (this.noteBlock != null)
            drawNoteBlock(graphics, mc);

        if (this.hasEditToggle) {
            int sx = toggleX();
            int sy = toggleY();
            boolean tHover = mouseX >= sx && mouseX < sx + TOGGLE_SIZE && mouseY >= sy && mouseY < sy + TOGGLE_SIZE;
            graphics.fill(sx - 1, sy - 1, sx + TOGGLE_SIZE + 1, sy + TOGGLE_SIZE + 1, ContextMenu.COLOR_BORDER);
            graphics.fill(sx, sy, sx + TOGGLE_SIZE, sy + TOGGLE_SIZE, tHover ? ContextMenu.COLOR_HOVER : ContextMenu.COLOR_TOGGLE_BG);
            String glyph = editMode ? "-" : "+";
            int gw = mc.font.width(glyph);
            graphics.text(mc.font, glyph, sx + (TOGGLE_SIZE - gw + 1) / 2, sy + 1,
                    tHover ? ContextMenu.COLOR_TEXT_HOVER : ContextMenu.COLOR_TEXT, false);
        }

        if (nextSubmenu != null) {
            if (this.openSubmenu != nextSubmenu) {
                if (this.openSubmenu != null)
                    this.openSubmenu.close();
                this.openSubmenu = nextSubmenu;
                this.openSubmenu.x = this.x + this.width;
                this.openSubmenu.y = this.y + nextSubmenuIndex * itemHeight();
                this.openSubmenu.open();
            }
        } else {
            if (this.openSubmenu != null && !this.openSubmenu.containsMouseRecursive(mouseX, mouseY)) {
                this.openSubmenu.close();
                this.openSubmenu = null;
            }
        }

        if (hoveredStack != null) {
            List<ClientTooltipComponent> components = new ArrayList<>();
            for (Component line : Screen.getTooltipFromItem(mc, hoveredStack)) {
                components.add(ClientTooltipComponent.create(line.getVisualOrderText()));
            }
            hoveredStack.getTooltipImage().ifPresent(image -> components.add(1, ClientTooltipComponent.create(image)));
            graphics.tooltip(mc.font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }

        if (this.openSubmenu != null) {
            this.openSubmenu.render(graphics, mouseX, mouseY, tickDelta);
        }
    }

    /**
     * Handles a click on the menu or its open submenu. Checkboxes, note bar controls, edit
     * controls and the "+" toggle act without closing the menu.
     *
     * @param mouseX the mouse x, in GUI pixels
     * @param mouseY the mouse y, in GUI pixels
     * @param button the mouse button
     * @return true if a row action ran and the whole menu tree should be closed
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) {
            return false;
        }

        if (this.openSubmenu != null && this.openSubmenu.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (this.hasEditToggle && isInsideToggle(mx, my)) {
            editMode = !editMode;
            ContextMenuTrigger.rebuildAfterEdit();
            return false;
        }

        if (handleEditClick(mx, my))
            return false; // keep menu open

        if (editMode) {
            int count = effectiveItemCount();
            for (int i = 0; i < count; i++) {
                int itemY = this.y + i * itemHeight();
                if (isHideable(i) && isInsideEye(mx, my, i, itemY)) {
                    toggleHidden(this.items.get(i));
                    return false; // keep menu open
                }
            }
        }

        int itemCount = effectiveItemCount();
        for (int i = 0; i < itemCount; i++) {
            int itemY = this.y + i * itemHeight();
            if (isInsideRow(mx, my, itemY)) {
                MenuItem item = this.items.get(i);
                if (item instanceof NoteBarItem bar) {
                    NoteZone zone = hitNoteBar(mx, my, itemY);
                    if (zone == NoteZone.EDIT) {
                        bar.block.edit();
                        close();
                        return true;
                    }
                    if (zone == NoteZone.PREV) bar.block.prev();
                    if (zone == NoteZone.NEXT) bar.block.next();
                    if (zone != null)
                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    return false; // Keep the menu open
                }
                if (item instanceof CheckboxItem) {
                    item.onClick();
                    return false; // Keep the menu open
                }
                if (submenuOf(item) == null && !(item instanceof InfoItem)) {
                    item.onClick();
                    close();
                } else {
                    return false;
                }
                return true;
            }
        }

        if (!containsMouseRecursive(mx, my)) {
            close();
        }
        return false;
    }

    /**
     * @return true if the point is over this menu's rows or note block, not its submenus
     */
    public boolean containsMouse(int mx, int my) {
        return mx >= this.x
                && mx < this.x + this.width
                && my >= this.y
                && my < this.y + effectiveItemCount() * itemHeight() + noteBlockHeight();
    }

    /**
     * @return true if the point is over this menu or any of its submenus, open or not
     */
    public boolean containsMouseRecursive(int mx, int my) {
        if (this.containsMouse(mx, my))
            return true;
        for (MenuItem m : this.items) {
            ContextMenu sub = submenuOf(m);
            if (sub != null && sub.containsMouseRecursive(mx, my))
                return true;
        }
        return false;
    }

    /**
     * Finds the custom YAML entry under the mouse, looking in the open submenu first.
     *
     * @return the hovered entry, or null if the hovered row is built-in or nothing is hovered
     */
    public MenuLocation.DeleteRef getHoveredDeletable(int mx, int my) {
        if (!this.visible)
            return null;
        if (this.openSubmenu != null) {
            MenuLocation.DeleteRef r = this.openSubmenu.getHoveredDeletable(mx, my);
            if (r != null)
                return r;
        }
        int itemCount = effectiveItemCount();
        for (int i = 0; i < itemCount; i++) {
            if (isInsideRow(mx, my, this.y + i * itemHeight()))
                return this.itemDelete.get(i);
        }
        return null;
    }

    private boolean isInsideRow(int mx, int my, int rowY) {
        return mx >= this.x
                && mx < this.x + this.width
                && my >= rowY
                && my < rowY + itemHeight();
    }

    /** Space kept right of the edit cluster for the submenu arrow. */
    private static final int EDIT_ARROW_W = 9;

    private int editClusterX(int i) {
        return this.x + this.width - EDIT_CLUSTER_W - 1 - EDIT_ARROW_W;
    }

    /** A row can move into the row above only if that row is a custom submenu. */
    private boolean canMoveInto(int i) {
        return i > 0
                && this.itemDelete.get(i - 1) != null
                && this.items.get(i - 1) instanceof SubmenuItem;
    }

    private boolean canMoveToParent(int i) {
        MenuLocation.DeleteRef ref = this.itemDelete.get(i);
        return ref != null && ref.container().hasParent();
    }

    private boolean isEditable(int i) {
        MenuItem m = this.items.get(i);
        return m instanceof CommandItem || m instanceof ScriptItem || m instanceof SubmenuItem;
    }

    /**
     * Whether row i can swap with row i + delta: both must come from the same YAML list. The
     * reference comparison works because rows parsed from the same list share one MenuLocation.
     */
    private boolean hasSwapNeighbour(int i, int delta) {
        int j = i + delta;
        if (j < 0 || j >= this.items.size())
            return false;
        MenuLocation.DeleteRef me = this.itemDelete.get(i);
        MenuLocation.DeleteRef other = this.itemDelete.get(j);
        return other != null && me != null && other.container() == me.container();
    }

    private boolean canMoveUp(int i) { return hasSwapNeighbour(i, -1); }
    private boolean canMoveDown(int i) { return hasSwapNeighbour(i, 1); }

    /**
     * Edit control under the mouse on row i. The cluster is a 3 by 2 grid: into / to parent,
     * up / down, edit / delete. Disabled controls are reported as null.
     */
    private EditControl hitEditControl(int mx, int my, int i) {
        if (!editMode || i < 0 || i >= this.itemDelete.size() || this.itemDelete.get(i) == null)
            return null;
        int itemY = this.y + i * itemHeight();
        int cx = editClusterX(i);
        int half = itemHeight() / 2;
        boolean col0 = mx >= cx && mx < cx + EDIT_CELL;
        boolean col1 = mx >= cx + EDIT_CELL && mx < cx + 2 * EDIT_CELL;
        boolean col2 = mx >= cx + 2 * EDIT_CELL && mx < cx + EDIT_CLUSTER_W;
        boolean top = my >= itemY && my < itemY + half;
        boolean bot = my >= itemY + half && my < itemY + itemHeight();
        if (col0 && top) return canMoveInto(i) ? EditControl.INTO : null;
        if (col0 && bot) return canMoveToParent(i) ? EditControl.TO_PARENT : null;
        if (col1 && top) return canMoveUp(i) ? EditControl.UP : null;
        if (col1 && bot) return canMoveDown(i) ? EditControl.DOWN : null;
        if (col2 && top) return isEditable(i) ? EditControl.EDIT : null;
        if (col2 && bot) return EditControl.DELETE;
        return null;
    }

    /** Tints of the edit mode sprites: enabled, disabled, and the red delete icon. */
    private static final int EDIT_ICON = 0xFFFFFFFF;
    private static final int EDIT_ICON_DISABLED = 0xFF4A4A4A;
    private static final int EDIT_ICON_DELETE = 0xFFE05555;

    private void drawEditCluster(GuiGraphicsExtractor graphics, Minecraft mc, int i, int itemY, int mouseX, int mouseY) {
        int cx = editClusterX(i);
        int half = itemHeight() / 2;
        EditControl hover = hitEditControl(mouseX, mouseY, i);
        drawIcon(graphics, ICON_INTO, cx, itemY, half, hover == EditControl.INTO,
                canMoveInto(i) ? EDIT_ICON : EDIT_ICON_DISABLED);
        drawIcon(graphics, ICON_TO_PARENT, cx, itemY + half, half, hover == EditControl.TO_PARENT,
                canMoveToParent(i) ? EDIT_ICON : EDIT_ICON_DISABLED);
        drawIcon(graphics, ICON_UP, cx + EDIT_CELL, itemY, half, hover == EditControl.UP,
                canMoveUp(i) ? EDIT_ICON : EDIT_ICON_DISABLED);
        drawIcon(graphics, ICON_DOWN, cx + EDIT_CELL, itemY + half, half, hover == EditControl.DOWN,
                canMoveDown(i) ? EDIT_ICON : EDIT_ICON_DISABLED);
        drawIcon(graphics, ICON_EDIT, cx + 2 * EDIT_CELL, itemY, half, hover == EditControl.EDIT,
                isEditable(i) ? EDIT_ICON : EDIT_ICON_DISABLED);
        drawIcon(graphics, ICON_DELETE, cx + 2 * EDIT_CELL, itemY + half, half, hover == EditControl.DELETE,
                EDIT_ICON_DELETE);
    }

    // The eye toggle sits in the bottom cell of the cluster's last column, where delete would be.
    private int eyeX(int i) {
        return editClusterX(i) + 2 * EDIT_CELL;
    }

    private int eyeY(int itemY) {
        return itemY + itemHeight() / 2;
    }

    private boolean isInsideEye(int mx, int my, int i, int itemY) {
        int ex = eyeX(i);
        int ey = eyeY(itemY);
        return mx >= ex && mx < ex + EDIT_CELL && my >= ey && my < ey + itemHeight() / 2;
    }

    private void drawEyeToggle(GuiGraphicsExtractor graphics, int i, int itemY, int mouseX, int mouseY) {
        boolean hidden = i < this.hiddenFlags.length ? this.hiddenFlags[i] : isHidden(this.items.get(i));
        drawIcon(graphics, hidden ? ICON_HIDDEN : ICON_VISIBLE,
                eyeX(i), eyeY(itemY), itemHeight() / 2,
                isInsideEye(mouseX, mouseY, i, itemY),
                hidden ? EDIT_ICON_DISABLED : EDIT_ICON);
    }

    /** Draws the separator and the current note page, cut to what fits in a book page. */
    private void drawNoteBlock(GuiGraphicsExtractor graphics, Minecraft mc) {
        graphics.fill(this.x, noteBlockY(), this.x + this.width, noteBlockY() + 1, ContextMenu.COLOR_BORDER);

        int bx = this.x + paddingX();
        int by = noteBlockY() + 1 + NOTE_PAD;

        if (this.noteBlock.pages().isEmpty()) {
            graphics.text(mc.font,
                    Component.translatable("hbtweaks.context.notes.empty")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    bx, by, ContextMenu.COLOR_TEXT, false);
            return;
        }

        String text = this.noteBlock.text();
        if (this.noteLines == null || !text.equals(this.noteLinesFor)) {
            this.noteLinesFor = text;
            this.noteLines = mc.font.split(Component.literal(text), NOTE_WIDTH);
        }
        int max = NOTE_HEIGHT / NOTE_LINE;
        for (int i = 0; i < Math.min(this.noteLines.size(), max); i++)
            graphics.text(mc.font, this.noteLines.get(i), bx, by + i * NOTE_LINE, ContextMenu.COLOR_TEXT, false);
    }

    /**
     * @param name the sprite name, from textures/gui/sprites/icon
     * @return the sprite identifier
     */
    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath("hb-tweaks-context", "icon/" + name);
    }

    private static final Identifier ICON_INTO = icon("up_right_arrow");
    private static final Identifier ICON_TO_PARENT = icon("down_left_arrow");
    private static final Identifier ICON_UP = icon("move_up");
    private static final Identifier ICON_DOWN = icon("move_down");
    private static final Identifier ICON_EDIT = icon("edit");
    private static final Identifier ICON_DELETE = icon("remove");
    private static final Identifier ICON_SUBMENU = icon("right_arrow");
    private static final Identifier ICON_LEFT = icon("left_arrow");
    private static final Identifier ICON_VISIBLE = icon("visible");
    private static final Identifier ICON_HIDDEN = icon("hidden");

    private void drawSubmenuArrow(GuiGraphicsExtractor graphics, int itemY, boolean besideCluster) {
        int size = Math.min(itemHeight() - 4, 7);
        int ax = besideCluster ? this.x + this.width - size - 1 : this.x + this.width - arrowRightPad();
        int ay = itemY + (itemHeight() - size) / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON_SUBMENU, ax, ay, size, size);
    }

    /** Draws a tinted sprite centred in an edit cell, with a light highlight when hovered. */
    private void drawIcon(GuiGraphicsExtractor graphics, Identifier id, int cellX, int cellY, int cellH, boolean hover, int tint) {
        if (hover)
            graphics.fill(cellX, cellY, cellX + EDIT_CELL, cellY + cellH, 0x40FFFFFF);
        int s = Math.min(EDIT_CELL, cellH) - 1;
        int x = cellX + (EDIT_CELL - s + 1) / 2;
        int y = cellY + (cellH - s + 1) / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, s, s, tint);
    }

    private static final Identifier[] LEGEND_ICONS =
            { ICON_INTO, ICON_TO_PARENT, ICON_UP, ICON_DOWN, ICON_EDIT, ICON_DELETE };
    private static final String[] LEGEND_KEYS = {
            "hbtweaks.context.editor.legend.into",
            "hbtweaks.context.editor.legend.to_parent",
            "hbtweaks.context.editor.legend.up",
            "hbtweaks.context.editor.legend.down",
            "hbtweaks.context.editor.legend.edit",
            "hbtweaks.context.editor.legend.delete",
    };

    // Idk what other tint can be good
    private static final int[] LEGEND_TINT =
            { EDIT_ICON, EDIT_ICON, EDIT_ICON, EDIT_ICON, EDIT_ICON, EDIT_ICON_DELETE };

    // Bottom-right legend fir edit mode
    public static void renderEditLegend(GuiGraphicsExtractor graphics) {
        if (!editMode) return;
        Minecraft mc = Minecraft.getInstance();
        int iconSz = 8;
        int gap = 3;
        int lineH = 10;
        Component[] labels = new Component[LEGEND_KEYS.length];
        int textW = 0;
        for (int i = 0; i < LEGEND_KEYS.length; i++) {
            labels[i] = Component.translatable(LEGEND_KEYS[i]);
            textW = Math.max(textW, mc.font.width(labels[i]));
        }
        int boxW = gap + iconSz + gap + textW + gap;
        int boxH = labels.length * lineH + gap;
        int margin = 5;
        int boxX = mc.getWindow().getGuiScaledWidth() - boxW - margin;
        int boxY = mc.getWindow().getGuiScaledHeight() - boxH - margin;

        graphics.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, COLOR_BORDER);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, COLOR_BG);
        for (int i = 0; i < labels.length; i++) {
            int ly = boxY + 2 + i * lineH;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LEGEND_ICONS[i], boxX + gap, ly, iconSz, iconSz, LEGEND_TINT[i]);
            graphics.text(mc.font, labels[i], boxX + gap + iconSz + gap, ly + 1, COLOR_TEXT, false);
        }
    }

    /**
     * Applies the edit control under the mouse. Moves are written to the YAML file at once and
     * the menu is rebuilt; edit and delete open a dialog.
     *
     * @return true if a control was clicked
     */
    private boolean handleEditClick(int mx, int my) {
        if (!editMode) return false;
        for (int i = 0; i < effectiveItemCount(); i++) {
            EditControl ctrl = hitEditControl(mx, my, i);
            if (ctrl == null) continue;
            MenuLocation.DeleteRef ref = this.itemDelete.get(i);
            switch (ctrl) {
                case UP -> { ref.container().move(ref.index(), -1); ContextMenuTrigger.rebuildAfterEdit(); }
                case DOWN -> { ref.container().move(ref.index(), 1); ContextMenuTrigger.rebuildAfterEdit(); }
                case INTO -> { ref.container().moveIntoSubmenuAbove(ref.index()); ContextMenuTrigger.rebuildAfterEdit(); }
                case TO_PARENT -> { ref.container().moveToParent(ref.index()); ContextMenuTrigger.rebuildAfterEdit(); }
                case EDIT -> ContextMenuTrigger.requestEdit(ref);
                case DELETE -> ContextMenuTrigger.requestDelete(ref);
            }
            return true;
        }
        return false;
    }

    private void markWidthDirty() {
        this.widthDirty = true;
    }

    /**
     * Recomputes the width from the widest row, including room for icons, submenu arrows and the
     * edit cluster, and refreshes hiddenFlags. Only runs when marked dirty.
     */
    private void ensureWidth() {
        if (!this.widthDirty) return;
        this.widthDirty = false;

        Minecraft mc = Minecraft.getInstance();
        int max = 0;
        this.hiddenFlags = new boolean[this.items.size()];
        for (int i = 0; i < this.items.size(); i++) {
            MenuItem item = this.items.get(i);
            this.hiddenFlags[i] = isHidden(item);
            int lw = mc.font.width(item.getLabel());
            if (submenuOf(item) != null) lw += arrowRightPad() + 4;
            if (item instanceof ItemStackMenuItem) lw += ContextMenu.ICON_SIZE; // place pour l'icône 16x16
            if (editMode && i < this.itemDelete.size() && (this.itemDelete.get(i) != null || isHideable(i)))
                lw += EDIT_CLUSTER_W + EDIT_ARROW_W + 2;
            if (lw > max) max = lw;
        }
        this.width = Math.max(ContextMenu.MIN_WIDTH, max + paddingX() * 2);
        if (this.noteBlock != null)
            this.width = Math.max(this.width, NOTE_WIDTH + paddingX() * 2);
    }

    /** A row of the menu. */
    private interface MenuItem {
        Component getLabel();
        void onClick();
    }

    /** Row running arbitrary code. */
    private static final class ActionItem implements MenuItem {
        private final Component label;
        private final Runnable action;

        ActionItem(Component label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        @Override
        public Component getLabel() {
            return this.label;
        }

        @Override
        public void onClick() {
            this.action.run();
        }
    }

    /**
     * Row sending a server command. Refuses to run, with a chat message, when the command needs
     * the target's position but the target is not loaded.
     */
    private static final class CommandItem implements MenuItem {

        private final Component label;
        private final String command;
        private final Player player;

        CommandItem(Component label, String command, Player player) {
            this.label = label;
            this.command = command;
            this.player = player;
        }

        @Override
        public Component getLabel() {
            return this.label;
        }

        @Override
        public void onClick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null)
                return;

            UUID playerUUID = player.getUUID();
            PlayerInfo pi = mc.player.connection.getPlayerInfo(playerUUID);
            if (pi == null)
                return;

            boolean needsPos = this.command.contains("%blockpos%") || this.command.contains("%eyepos%");
            if (needsPos && (mc.level == null || mc.level.getPlayerByUUID(playerUUID) == null)) {
                mc.gui.getChat().addClientSystemMessage(
                        Component.literal("Le joueur est trop loin pour exécuter cette commande.").withStyle(ChatFormatting.RED));
                return;
            }

            String c = replaceString(this.command, player);

            //HBTweaksContext.LOGGER.info("COMMAND RUN : " + c);
            mc.player.connection.sendCommand(c);
        }
    }

    // WARN: %mcname% and %mymcname% resolve to the real Minecraft name. In a script line sent as a
    // chat message (no leading "/"), this reveals it publicly, bypassing the fake name masking.
    // WARN: %mcname% and %rpname% throw a NullPointerException if the target left the tab list or
    // has no display name. ScriptRunner does not catch it, so a delayed script line can crash the
    // client tick.
    /**
     * Resolves the placeholders of a command or script line. %mcname%, %rpname%, %blockpos%,
     * %eyepos% and %uuid% refer to the target; the same names prefixed with "my" refer to the
     * local player.
     *
     * @param c the template
     * @param player the target
     * @return the resolved string
     */
    public static String replaceString(String c, Player player) {
        PlayerInfo pi = Minecraft.getInstance().player.connection.getPlayerInfo(player.getUUID());

        if (c.contains("%mcname%"))
            c = c.replace("%mcname%", ContextMenuTrigger.getMCName(player));
        if (c.contains("%rpname%"))
            c = c.replace("%rpname%", pi.getTabListDisplayName().getString());
        if (c.contains("%blockpos%"))
            c = c.replace("%blockpos%", "%s %s %s".formatted(player.getBlockX(), player.getBlockY(), player.getBlockZ()));
        if (c.contains("%eyepos%"))
            c = c.replace("%eyepos%", "%s %s %s".formatted(player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z));
        if (c.contains("%uuid%"))
            c = c.replace("%uuid%", player.getStringUUID());

        LocalPlayer me = Minecraft.getInstance().player;
        if (c.contains("%mymcname%"))
            c = c.replace("%mymcname%", ContextMenuTrigger.getMCName(me));
        if (c.contains("%myrpname%"))
            c = c.replace("%myrpname%", me.connection.getPlayerInfo(me.getUUID()).getTabListDisplayName().getString());
        if (c.contains("%myblockpos%"))
            c = c.replace("%myblockpos%","%s %s %s".formatted(me.getBlockX(), me.getBlockY(), me.getBlockZ()));
        if (c.contains("%myeyepos%"))
            c = c.replace("%myeyepos%", "%s %s %s".formatted(me.getEyePosition().x, me.getEyePosition().y, me.getEyePosition().z));
        if (c.contains("%myuuid%"))
            c = c.replace("%myuuid%", me.getStringUUID());

        return c;
    }

    /** Row queueing a multi-line script in ScriptRunner. */
    private static final class ScriptItem implements MenuItem {
        private final Component label;
        private final java.util.List<String> lines;
        private final Player player;

        ScriptItem(Component label, java.util.List<String> lines, Player player) {
            this.label = label;
            this.lines = lines;
            this.player = player;
        }

        @Override
        public Component getLabel() {
            return this.label;
        }

        @Override
        public void onClick() {
            ScriptRunner.enqueue(this.lines, this.player);
        }
    }

    /** Row opening a submenu on hover; clicking it does nothing. */
    private static final class SubmenuItem implements MenuItem {
        private final Component label;
        final ContextMenu submenu;

        SubmenuItem(Component label, ContextMenu submenu) {
            this.label = label;
            this.submenu = submenu;
        }

        @Override
        public Component getLabel() {
            return this.label;
        }

        @Override
        public void onClick() {
            // Nothing
        }
    }

    /**
     * The green "add" row of edit mode. Distinct from SubmenuItem so it is never hideable,
     * editable, or counted outside edit mode.
     */
    private static final class AddMenuItem implements MenuItem {
        private final Component label;
        final ContextMenu submenu;

        AddMenuItem(Component label, ContextMenu submenu) {
            this.label = label;
            this.submenu = submenu;
        }

        @Override
        public Component getLabel() {
            return this.label;
        }

        @Override
        public void onClick() {
            // Nothing
        }
    }

    /**
     * Checkbox row. The state lives outside the menu: subclasses read it in isChecked and write
     * it in checked / unchecked. Clicking toggles it and keeps the menu open.
     */
    public abstract static class CheckboxItem implements MenuItem {
        private final Component label;

        protected CheckboxItem(Component label) {
            this.label = label;
        }

        /**
         * @return the current state, read every frame to draw the box
         */
        public abstract boolean isChecked();

        /** Called when the box gets checked. */
        protected abstract void checked();

        /** Called when the box gets unchecked. */
        protected abstract void unchecked();

        @Override
        public Component getLabel() {
            return Component.literal(isChecked() ? "☑ " : "☐ ").append(this.label);
        }

        @Override
        public void onClick() {
            if (isChecked())
                unchecked();
            else
                checked();
        }
    }

    /** Single row holding the prev/edit/next controls of the Note menu. */
    private static final class NoteBarItem implements MenuItem {
        private final NoteBlock block;

        NoteBarItem(NoteBlock block) {
            this.block = block;
        }

        @Override
        public Component getLabel() {
            return Component.literal("  " + (block.page() + 1) + "/" + block.pageCount() + "  ");
        }

        @Override
        public void onClick() {
            // Handled per zone
        }
    }

    /** Sizes of the note bar controls, in GUI pixels. */
    private static final int NOTE_ARROW = 9;
    private static final int NOTE_PEN = 12;
    private static final int NOTE_ARROW_GAP = 3;

    /** Clickable zones of the note bar: previous page, next page, edit. */
    private enum NoteZone { PREV, NEXT, EDIT }

    private int noteZoneX(NoteZone zone) {
        return switch (zone) {
            case PREV -> this.x + paddingX();
            case NEXT -> this.x + paddingX() + NOTE_ARROW + NOTE_ARROW_GAP;
            case EDIT -> this.x + this.width - paddingX() - NOTE_PEN;
        };
    }

    private int noteZoneW(NoteZone zone) {
        return zone == NoteZone.EDIT ? NOTE_PEN : NOTE_ARROW;
    }

    /**
     * @return the note bar zone under the mouse, or null
     */
    private NoteZone hitNoteBar(int mx, int my, int rowY) {
        if (my < rowY || my >= rowY + itemHeight()) return null;
        for (NoteZone zone : NoteZone.values()) {
            int zx = noteZoneX(zone);
            if (mx >= zx && mx < zx + noteZoneW(zone)) return zone;
        }
        return null;
    }

    /** Draws the arrows on the left, the page counter in the middle, and the edit pen on the right. */
    private void drawNoteBar(GuiGraphicsExtractor graphics, Minecraft mc, NoteBlock block,
                             int rowY, int mouseX, int mouseY) {
        NoteZone hover = hitNoteBar(mouseX, mouseY, rowY);
        int ay = rowY + (itemHeight() - NOTE_ARROW) / 2;

        boolean prev = block.hasPrev();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON_LEFT,
                noteZoneX(NoteZone.PREV), ay, NOTE_ARROW, NOTE_ARROW,
                !prev ? EDIT_ICON_DISABLED : hover == NoteZone.PREV ? EDIT_ICON : ContextMenu.COLOR_TEXT);

        boolean next = block.hasNext();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON_SUBMENU,
                noteZoneX(NoteZone.NEXT), ay, NOTE_ARROW, NOTE_ARROW,
                !next ? EDIT_ICON_DISABLED : hover == NoteZone.NEXT ? EDIT_ICON : ContextMenu.COLOR_TEXT);

        int py = rowY + (itemHeight() - NOTE_PEN) / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON_EDIT,
                noteZoneX(NoteZone.EDIT), py, NOTE_PEN, NOTE_PEN,
                hover == NoteZone.EDIT ? EDIT_ICON : ContextMenu.COLOR_TEXT);

        String label = (block.page() + 1) + "/" + block.pageCount();
        graphics.text(mc.font, label,
                this.x + (this.width - mc.font.width(label)) / 2, rowY + textOffsetY(),
                ContextMenu.COLOR_TEXT, false);
    }

    /** Plain text row, never highlighted. */
    private static final class InfoItem implements MenuItem {
        private final Component label;

        InfoItem(Component label) {
            this.label = label;
        }

        @Override
        public Component getLabel() {
            return this.label;
        }

        @Override public void onClick() {
            // Nothing
        }
    }

    /** Row showing an item icon and name, with the item tooltip on hover. */
    private static final class ItemStackMenuItem implements MenuItem {
        private final ItemStack stack;

        ItemStackMenuItem(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public Component getLabel() {
            return stack.getHoverName();
        }

        @Override
        public void onClick() {
            // Pas d'action
        }
    }

    /** Row opening a URL in the system browser. */
    private static final class LinkItem implements MenuItem {
        private final Component label;
        private final String url;

        LinkItem(Component label, String url) {
            this.label = label;
            this.url = url;
        }

        @Override
        public Component getLabel() {
            return this.label;
        }

        @Override
        public void onClick() {
            try {
                Util.getPlatform().openUri(new java.net.URI(this.url));
            } catch (java.net.URISyntaxException e) {
                HBTweaksContext.LOGGER.error("Invalid URL: {}", this.url, e);
            }
        }
    }

    /** Row copying text to the clipboard. */
    private static final class CopyItem implements MenuItem {
        private final Component label;
        private final String text;

        CopyItem(Component label, String text) {
            this.label = label;
            this.text = text;
        }

        @Override
        public Component getLabel() {
            return this.label;
        }

        @Override
        public void onClick() {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.text);
        }
    }

    /**
     * Appends every row of another menu, keeping their YAML entries. Used to add the custom
     * rows to a built-in menu.
     *
     * @param cm the menu to take the rows from
     * @return this menu
     */
    public ContextMenu merge(ContextMenu cm) {
        this.items.addAll(cm.items);
        this.itemDelete.addAll(cm.itemDelete);
        markWidthDirty();
        return this;
    }
}