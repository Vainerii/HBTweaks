package vai.hbtweaks.context.client.contextmenu;

import java.util.List;

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

    private int x;
    private int y;

    private final List<MenuItem> items = new ArrayList<>();
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
        this.items.add(new ActionItem(label, action));
        recalcWidth();
        return this;
    }

    public Player getPlayer() {
        return player;
    }

    public ContextMenu addCommandItem(String label, String commandTemplate) {
        return this.addCommandItem(Component.literal(label), commandTemplate);
    }

    public ContextMenu addCommandItem(Component label, String commandTemplate) {
        this.items.add(new CommandItem(label, commandTemplate, player));
        recalcWidth();
        return this;
    }

    public ContextMenu addInfoItem(String label) {
        return this.addInfoItem(Component.literal(label));
    }

    public ContextMenu addInfoItem(Component label) {
        this.items.add(new InfoItem(label));
        recalcWidth();
        return this;
    }

    public ContextMenu addSubmenuItem(String label, ContextMenu submenu) {
        return this.addSubmenuItem(Component.literal(label), submenu);
    }

    public ContextMenu addSubmenuItem(Component label, ContextMenu submenu) {
        this.items.add(new SubmenuItem(label, submenu));
        recalcWidth();
        return this;
    }

    public ContextMenu addItemStackItem(ItemStack stack) {
        this.items.add(new ItemStackMenuItem(stack));
        recalcWidth();
        return this;
    }

    public ContextMenu addLinkItem(String label, String url) {
        return this.addLinkItem(Component.literal(label), url);
    }

    public ContextMenu addLinkItem(Component label, String url) {
        this.items.add(new LinkItem(label, url));
        recalcWidth();
        return this;
    }

    public ContextMenu addCopyItem(String label, String text) {
        return this.addCopyItem(Component.literal(label), text);
    }

    public ContextMenu addCopyItem(Component label, String text) {
        this.items.add(new CopyItem(label, text));
        recalcWidth();
        return this;
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

        int totalHeight = this.items.size() * ContextMenu.ITEM_HEIGHT;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        this.x = Math.min(this.x, screenW - this.width - 1);
        this.y = Math.min(this.y, screenH - totalHeight - 1);

        graphics.fill(this.x - 1,
                this.y - 1,
                this.x + this.width + 1,
                this.y + totalHeight + 1,
                ContextMenu.COLOR_BORDER);
        graphics.fill(this.x,
                this.y,
                this.x + this.width,
                this.y + totalHeight,
                ContextMenu.COLOR_BG);

        ContextMenu nextSubmenu = null;
        int nextSubmenuIndex = -1;
        ItemStack hoveredStack = null;

        for (int i = 0; i < this.items.size(); i++) {
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

            if (item instanceof SubmenuItem si) {
                graphics.text(mc.font, ">", this.x + this.width - ContextMenu.ARROW_RIGHT_PAD, itemY + 4, textColor, false);
                if (hovered) {
                    nextSubmenu = si.submenu;
                    nextSubmenuIndex = i;
                }
            }
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
            // Render the tooltip immediately (on top of the menu) instead of deferring it,
            // since our menu is drawn after the screen's deferred-tooltip pass.
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

        for (int i = 0; i < this.items.size(); i++) {
            int itemY = this.y + i * ContextMenu.ITEM_HEIGHT;
            if (isInsideRow(mx, my, itemY)) {
                MenuItem item = this.items.get(i);
                if (!(item instanceof SubmenuItem) && !(item instanceof InfoItem)) {
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
                && my < this.y + this.items.size() * ContextMenu.ITEM_HEIGHT;
    }

    public boolean containsMouseRecursive(int mx, int my) {
        if (this.containsMouse(mx, my))
            return true;
        for (MenuItem m : this.items) {
            if (m instanceof SubmenuItem) {
                if (((SubmenuItem)m).submenu.containsMouseRecursive(mx, my))
                    return true;
            }
        }
        return false;
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
            if (item instanceof SubmenuItem)
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
        recalcWidth();
        return this;
    }
}