package vai.hbtweaks.context.client.contextmenu;

import java.util.List;

import net.minecraft.ChatFormatting;
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
import vai.hbtweaks.context.client.contextmenu.editor.AddCommandScreen;
import vai.hbtweaks.context.client.contextmenu.editor.AddSubmenuScreen;
import vai.hbtweaks.context.client.contextmenu.editor.MenuLocation;
import vai.hbtweaks.context.client.listeners.ContextMenuTrigger;

import java.util.ArrayList;
import java.util.UUID;

public class ContextMenu {

    private static final int ITEM_HEIGHT = 16;
    private static final int PADDING_X = 6;
    private static final int MIN_WIDTH = 20;
    private static final int ARROW_RIGHT_PAD = 8;

    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_TEXT_HOVER = 0xFFFFFFFF;
    private static final int COLOR_BG = 0xE0101010;
    private static final int COLOR_HOVER = 0xFF2255AA;
    private static final int COLOR_BORDER = 0xFF3A3A3A;
    private static final int COLOR_TOGGLE_BG = 0xFF2A2A2A;

    private static final int TOGGLE_SIZE = 9;
    private static final int TOGGLE_GAP = 1;

    private int x;
    private int y;

    public static boolean EDIT_ENABLED = true;
    public static boolean editMode = false;

    private final List<MenuItem> items = new ArrayList<>();
    private final List<MenuLocation.DeleteRef> itemDelete = new ArrayList<>();
    private boolean hasEditToggle = false;
    private boolean visible = false;
    private int width = MIN_WIDTH;
    private ContextMenu openSubmenu = null;

    private Player player;

    public ContextMenu(int x, int y, Player target) {
        this.x = x;
        this.y = y;
        this.player = target;
    }

    public ContextMenu addActionItem(String label, Runnable action) {
        return this.addActionItem(Component.literal(label), action);
    }

    public ContextMenu addActionItem(Component label, Runnable action) {
        return push(new ActionItem(label, action));
    }

    private ContextMenu push(MenuItem item) {
        this.items.add(item);
        this.itemDelete.add(null);
        recalcWidth();
        return this;
    }

    public ContextMenu markLastDeletable(MenuLocation.DeleteRef ref) {
        if (!this.itemDelete.isEmpty())
            this.itemDelete.set(this.itemDelete.size() - 1, ref);
        return this;
    }

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
        return push(new AddMenuItem(
                Component.translatable("hbtweaks.context.editor.add").withStyle(ChatFormatting.GREEN), sub));
    }

    private static ContextMenu submenuOf(MenuItem item) {
        if (item instanceof SubmenuItem si) return si.submenu;
        if (item instanceof AddMenuItem ai) return ai.submenu;
        return null;
    }

    public ContextMenu withEditToggle() {
        this.hasEditToggle = EDIT_ENABLED
                && !vai.hbtweaks.context.client.config.HBConfig.get().hidePlusBox;
        return this;
    }

    private int effectiveItemCount() {
        int n = this.items.size();
        if (!editMode && n > 0 && this.items.get(n - 1) instanceof AddMenuItem) n--;
        return n;
    }

    private int toggleX() {
        return this.x + 1;
    }

    private int toggleY() {
        return this.y + effectiveItemCount() * ContextMenu.ITEM_HEIGHT + TOGGLE_GAP;
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

    public ContextMenu addCommandItem(Component label, String commandTemplate) {
        return push(new CommandItem(label, commandTemplate, player));
    }

    public ContextMenu addInfoItem(String label) {
        return this.addInfoItem(Component.literal(label));
    }

    public ContextMenu addInfoItem(Component label) {
        return push(new InfoItem(label));
    }

    public ContextMenu addSubmenuItem(String label, ContextMenu submenu) {
        return this.addSubmenuItem(Component.literal(label), submenu);
    }

    public ContextMenu addSubmenuItem(Component label, ContextMenu submenu) {
        return push(new SubmenuItem(label, submenu));
    }

    public ContextMenu addItemStackItem(ItemStack stack) {
        return push(new ItemStackMenuItem(stack));
    }

    public ContextMenu addLinkItem(String label, String url) {
        return this.addLinkItem(Component.literal(label), url);
    }

    public ContextMenu addLinkItem(Component label, String url) {
        return push(new LinkItem(label, url));
    }

    public ContextMenu addCopyItem(String label, String text) {
        return this.addCopyItem(Component.literal(label), text);
    }

    public ContextMenu addCopyItem(Component label, String text) {
        return push(new CopyItem(label, text));
    }

    public void open() {
        this.visible = true;
    }

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

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, DeltaTracker tickDelta) {
        if (!this.visible) return;

        Minecraft mc = Minecraft.getInstance();

        int itemCount = effectiveItemCount();
        int itemsHeight = itemCount * ContextMenu.ITEM_HEIGHT;
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
            int itemY = this.y + i * ContextMenu.ITEM_HEIGHT;

            boolean hovered = isInsideRow(mouseX, mouseY, itemY);
            if (item instanceof InfoItem) {
                hovered = false;
            }

            if (hovered) {
                graphics.fill(this.x, itemY, this.x + this.width, itemY + ContextMenu.ITEM_HEIGHT, ContextMenu.COLOR_HOVER);
            }

            int textColor = hovered ? ContextMenu.COLOR_TEXT_HOVER : ContextMenu.COLOR_TEXT;

            if (item instanceof ItemStackMenuItem ism) {
                graphics.item(ism.stack, this.x + ContextMenu.PADDING_X, itemY);
                graphics.text(mc.font, item.getLabel(),
                        this.x + ContextMenu.PADDING_X + ContextMenu.ITEM_HEIGHT + 2,
                        itemY + 4, textColor, false);
                if (hovered) {
                    hoveredStack = ism.stack; // on mémorise, on ne rend pas encore
                }
            } else {
                graphics.text(mc.font, item.getLabel(),
                        this.x + ContextMenu.PADDING_X, itemY + 4, textColor, false);
            }

            ContextMenu sub = submenuOf(item);
            if (sub != null) {
                graphics.text(mc.font, ">", this.x + this.width - ContextMenu.ARROW_RIGHT_PAD, itemY + 4, textColor, false);
                if (hovered) {
                    nextSubmenu = sub;
                    nextSubmenuIndex = i;
                }
            }
        }

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
                this.openSubmenu.y = this.y + nextSubmenuIndex * ContextMenu.ITEM_HEIGHT;
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
            return false;
        }

        int itemCount = effectiveItemCount();
        for (int i = 0; i < itemCount; i++) {
            int itemY = this.y + i * ContextMenu.ITEM_HEIGHT;
            if (isInsideRow(mx, my, itemY)) {
                MenuItem item = this.items.get(i);
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

    public boolean containsMouse(int mx, int my) {
        return mx >= this.x
                && mx < this.x + this.width
                && my >= this.y
                && my < this.y + effectiveItemCount() * ContextMenu.ITEM_HEIGHT;
    }

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
            if (isInsideRow(mx, my, this.y + i * ContextMenu.ITEM_HEIGHT))
                return this.itemDelete.get(i);
        }
        return null;
    }

    private boolean isInsideRow(int mx, int my, int rowY) {
        return mx >= this.x
                && mx < this.x + this.width
                && my >= rowY
                && my < rowY + ContextMenu.ITEM_HEIGHT;
    }

    private void recalcWidth() {
        Minecraft mc = Minecraft.getInstance();
        int max = 0;
        for (MenuItem item : this.items) {
            int lw = mc.font.width(item.getLabel());
            if (submenuOf(item) != null)
                lw += ContextMenu.ARROW_RIGHT_PAD + 4;
            if (item instanceof ItemStackMenuItem)
                lw += ContextMenu.ITEM_HEIGHT; // place pour l'icône 16x16
            if (lw > max)
                max = lw;
        }
        this.width = Math.max(ContextMenu.MIN_WIDTH, max + ContextMenu.PADDING_X * 2);
    }

    private interface MenuItem {
        Component getLabel();
        void onClick();
    }

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

            String c = replaceString(this.command, player);

            //HBTweaksContext.LOGGER.info("COMMAND RUN : " + c);
            mc.player.connection.sendCommand(c);
        }
    }

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

    public ContextMenu merge(ContextMenu cm) {
        this.items.addAll(cm.items);
        this.itemDelete.addAll(cm.itemDelete);
        recalcWidth();
        return this;
    }
}