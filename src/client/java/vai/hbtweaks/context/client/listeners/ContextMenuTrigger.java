package vai.hbtweaks.context.client.listeners;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import vai.hbtweaks.context.HBTweaksContext;
import vai.hbtweaks.context.client.contextmenu.ContextMenu;
import vai.hbtweaks.context.client.effects.EffectsBank;
import vai.hbtweaks.context.client.network.EffectPayloads;
import vai.hbtweaks.context.client.contextmenu.CustomContextMenuLoader;
import vai.hbtweaks.context.client.contextmenu.editor.DeleteConfirmScreen;
import vai.hbtweaks.context.client.contextmenu.editor.MenuLocation;
import vai.hbtweaks.context.client.keyboard.WritersBank;
import vai.hbtweaks.context.client.Util;
import vai.hbtweaks.context.client.config.HBConfig;
import vai.hbtweaks.context.client.mouse.MouseTracker;
import vai.hbtweaks.context.client.mouse.MouseTrackerEntityClickUpCallback;
import vai.hbtweaks.context.client.mouse.ClickType;
import vai.hbtweaks.context.client.mouse.ScreenType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static vai.hbtweaks.context.client.Util.hasDev;
import static vai.hbtweaks.context.client.Util.hasPerm;

public class ContextMenuTrigger implements MouseTrackerEntityClickUpCallback, ScreenMouseEvents.AfterMouseClick
{

    private static ContextMenu contextMenu = null;
    private static java.util.UUID effectsRequestedFor = null;

    public static final Path CUSTOM_MENU = Paths.get("custom_menu.yml");
    public static final Path CUSTOM_MENU_SELF = Paths.get("custom_menu_self.yml");

    public static Map<String, Object> customMenu = CustomContextMenuLoader.readYaml(CUSTOM_MENU);
    public static Map<String, Object> customMenuSelf = CustomContextMenuLoader.readYaml(CUSTOM_MENU_SELF);

    public static void onFileEdited(Path file, Map<String, Object> root) {
        if (file.equals(CUSTOM_MENU))
            customMenu = root;
        else if (file.equals(CUSTOM_MENU_SELF))
            customMenuSelf = root;
    }

    public static boolean handleDelete() {
        if (!ContextMenu.EDIT_ENABLED || !ContextMenu.editMode || contextMenu == null || !contextMenu.isVisible())
            return false;
        Minecraft mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        MenuLocation.DeleteRef ref = contextMenu.getHoveredDeletable(mx, my);
        if (ref == null)
            return false;
        Screen parent = mc.screen;
        contextMenu.close();
        contextMenu = null;
        mc.setScreen(new DeleteConfirmScreen(parent, ref));
        return true;
    }

    private boolean isCommandAvailable(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        return mc.player.connection.getCommands().getRoot().getChild(command) != null;
    }

    public static String getMCName(Player player) {
        LocalPlayer mainPlayer = Minecraft.getInstance().player;
        if (mainPlayer != null) {
            PlayerInfo pi = mainPlayer.connection.getPlayerInfo(player.getUUID());
            if (pi != null)
                return pi.getProfile().name();
        }
        return null;
    }

    private ContextMenu makeReputContextMenu(Player player) {
        return makeReputAvisContextMenu(player, "reput");
    }

    private ContextMenu makeAvisContextMenu(Player player) {
        return makeReputAvisContextMenu(player, "avis");
    }

    private ContextMenu makeReputAvisContextMenu(Player player, String cmdPart) {
        ContextMenu context = new ContextMenu(0, 0, player);
        context.addCommandItem(Component.literal("OK ✔").withStyle(ChatFormatting.GREEN), cmdPart + "ok %mcname%");
        context.addCommandItem(Component.literal("NO -").withStyle(ChatFormatting.GRAY), cmdPart + "no %mcname%");
        context.addCommandItem(Component.literal("KO ✘").withStyle(ChatFormatting.RED), cmdPart + "ko %mcname%");
        return context;
    }

    private ContextMenu makeDebugContextMenu(Player player) {
        ContextMenu context = new ContextMenu(0, 0, player);
        PlayerInfo pi = Minecraft.getInstance().player.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return null;
        PropertyMap properties = pi.getProfile().properties();
        for (Map.Entry<String, Property> e : properties.entries()) {
            context.addActionItem(Component.literal(e.getKey()), () -> {
                Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.literal(e.getValue().name()));
                Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.literal(e.getValue().value()));
            });
        }
        return context;
    }

    private ContextMenu makeInvContextMenu(Player player) {
        ContextMenu context = new ContextMenu(0, 0, player);

        List<ItemStack> equipment = List.of(
                player.getItemBySlot(EquipmentSlot.MAINHAND),
                player.getItemBySlot(EquipmentSlot.OFFHAND),
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET)
        );

        for (ItemStack item : equipment) {
            if (item.isEmpty())
                context.addInfoItem(Component.literal("[VIDE]").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            else
                context.addItemStackItem(item);
        }
        return context;
    }

    // TODO temporary: manual writers-list toggling for testing
    private ContextMenu makeTestContextMenu(Player player) {
        ContextMenu context = new ContextMenu(0, 0, player);
        context.addActionItem(Component.literal("make writer").withStyle(ChatFormatting.GREEN),
                () -> WritersBank.startWriting(player));
        context.addActionItem(Component.literal("stop writer").withStyle(ChatFormatting.RED),
                () -> WritersBank.stopWriting(player));
        return context;
    }

    private ContextMenu makeInfoContextMenu(Player player) {

        PlayerInfo pi = Minecraft.getInstance().player.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return null;
        ContextMenu infosContext = new ContextMenu(0, 0, player);
        infosContext.addInfoItem(pi.getTabListDisplayName());
        addMcNameItems(infosContext, player, ContextMenuTrigger.getMCName(player));
        return infosContext;
    }

    private void addMcNameItems(ContextMenu menu, Player player, String mcName) {
        if (mcName != null) {
            menu.addInfoItem(Component.literal(mcName).withStyle(ChatFormatting.DARK_GRAY));
            // GMs also see player's fake Minecraft name.
            if (hasPerm() && Util.hasFakeName(player))
                menu.addInfoItem(Component.literal(Util.getFakeName(player)).withStyle(ChatFormatting.YELLOW));
        }
    }

    private ContextMenu makeSelfContextMenu(Player self, int x, int y) {
        ContextMenu context = new ContextMenu(x, y, self);

        context.addInfoItem(Component.literal("Vous-même").withStyle(ChatFormatting.GRAY));

        ContextMenu emotes = new ContextMenu(0, 0, self);
        emotes.addCommandItem("S'allonger", "lay");
        emotes.addCommandItem("S'asseoir", "sit");
        emotes.addCommandItem("Menu d'émote", "emote");
        context.addSubmenuItem("Emotes", emotes);

        context.addCommandItem("Marcher", "walk");

        ContextMenu roll = new ContextMenu(0, 0, self);
        roll.addCommandItem("1d20", "roll");
        roll.addCommandItem("1d6", "roll 1d6");
        roll.addCommandItem("2d6", "roll 2d6");
        roll.addCommandItem("1d100", "roll 1d100");
        context.addSubmenuItem("Roll", roll);

        context.addCommandItem("Stuff", "stuff");

        if (ContextMenuTrigger.customMenuSelf != null)
            context.merge(CustomContextMenuLoader.load(ContextMenuTrigger.customMenuSelf, Minecraft.getInstance().player, CUSTOM_MENU_SELF));

        context.addAddItem(new MenuLocation(CUSTOM_MENU_SELF, List.of()));
        context.withEditToggle();

        return context;
    }

    @Override
    public void onClickUp(List<Entity> list, ClickType clickType, ScreenType screenType) {
        if (screenType == ScreenType.CHAT && clickType == ClickType.RIGHT_CLICK) {
            if (isMouseOverChat()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            int x = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
            int y = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());

            try {
                Entity target = list.isEmpty() ? null : list.getFirst();
                if (!(target instanceof Player targetPlayer)) {
                    if (ContextMenuTrigger.contextMenu != null)
                        ContextMenuTrigger.contextMenu.close();
                    ContextMenuTrigger.contextMenu = makeSelfContextMenu(mc.player, x, y);
                    ContextMenuTrigger.contextMenu.open();
                    return;
                }

                if (mc.player.connection.getPlayerInfo(targetPlayer.getUUID()) == null)
                    return;

                if (ContextMenuTrigger.contextMenu != null)
                    ContextMenuTrigger.contextMenu.close();

                ContextMenuTrigger.contextMenu = new ContextMenu(x, y, targetPlayer);

                // == Infos ==
                addSubmenuIfPresent("Infos", makeInfoContextMenu(targetPlayer));

                // == Reput ==
                if (isCommandAvailable("reputok"))
                    addSubmenuIfPresent("Reput", makeReputContextMenu(targetPlayer));

                // == Avis ==
                if (isCommandAvailable("avisok"))
                    addSubmenuIfPresent("Avis", makeAvisContextMenu(targetPlayer));

                // == Items ==
                if (hasPerm())
                    addSubmenuIfPresent("Items", makeInvContextMenu(targetPlayer));

                // == Custom ==
                if (ContextMenuTrigger.customMenu != null)
                    ContextMenuTrigger.contextMenu.merge(CustomContextMenuLoader.load(ContextMenuTrigger.customMenu, targetPlayer, CUSTOM_MENU));

                // == Debug ==
                if (hasDev())
                    addSubmenuIfPresent("Debug", makeDebugContextMenu(targetPlayer));

                // == Test (temporary) ==
                //addSubmenuIfPresent("Test", makeTestContextMenu(targetPlayer));

                // == Add (edition) ==
                ContextMenuTrigger.contextMenu.addAddItem(new MenuLocation(CUSTOM_MENU, List.of()));
                ContextMenuTrigger.contextMenu.withEditToggle();

                ContextMenuTrigger.contextMenu.open();
            } catch (Exception e) {
                HBTweaksContext.LOGGER.error("Failed to open context menu", e);
                dispose();
            }
        }
    }

    private static void addSubmenuIfPresent(String label, ContextMenu submenu) {
        if (submenu != null)
            ContextMenuTrigger.contextMenu.addSubmenuItem(label, submenu);
    }

    private static boolean isMouseOverChat() {
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.options.chatScale().get();
        if (scale <= 0) return false;
        int width = ChatComponent.getWidth(mc.options.chatWidth().get());
        int height = (int) (ChatComponent.getHeight(mc.options.chatHeightFocused().get()) * scale);
        int bottom = mc.getWindow().getGuiScaledHeight() - 40;
        int top = bottom - height;
        double mx = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double my = mc.mouseHandler.getScaledYPos(mc.getWindow());
        return mx >= 0 && mx <= width && my >= top && my <= bottom;
    }

    public void renderOnScreen(GuiGraphicsExtractor graphics, DeltaTracker tickDelta) {
        render(graphics, tickDelta);
    }

    public static void dispose() {
        if (contextMenu != null) {
            contextMenu.close();
            contextMenu = null;
        }
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker tickDelta) {
        try {
            boolean leftDown = hasPerm() && GLFW.glfwGetMouseButton(
                    Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (!leftDown) effectsRequestedFor = null;

            if (contextMenu != null
                    && (!contextMenu.isVisible() || Minecraft.getInstance().mouseHandler.isMouseGrabbed())) {
                contextMenu.close();
                contextMenu = null;
            }
            if (contextMenu != null)
                contextMenu.render(graphics, tickDelta);
            else
                renderHoverName(graphics, leftDown);
        } catch (Exception e) {
            HBTweaksContext.LOGGER.error("Failed to render context menu / hover box", e);
            dispose();
        }
    }

    private static void renderHoverName(GuiGraphicsExtractor graphics, boolean leftDown) {
        if (isMouseOverChat()) return;
        Player target = null;
        for (Entity e : MouseTracker.getHoveredEntities()) {
            if (e instanceof Player p) {
                target = p;
                break;
            }
        }
        if (target == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection.getPlayerInfo(target.getUUID()) == null) return;

        Component name = Util.getRpName(target);
        if (name == null) return;

        double dist = mc.player.position().distanceTo(target.position());

        MutableComponent nameLine = name.copy()
                .append(Component.literal(" - ").withStyle(ChatFormatting.WHITE))
                .append(Util.distanceIndicator(dist))
                .append(Component.literal(" - ").withStyle(ChatFormatting.WHITE))
                .append(Util.writingIndicator(target));

        if (leftDown && !target.getUUID().equals(effectsRequestedFor)) {
            EffectPayloads.requestEffects(target.getUUID());
            effectsRequestedFor = target.getUUID();
        }

        List<Component> lines = new java.util.ArrayList<>();
        lines.add(nameLine);
        if (leftDown) {
            for (MobEffectInstance eff : EffectsBank.get(target)) {
                MutableComponent line = eff.getEffect().value().getDisplayName().copy();
                if (eff.getAmplifier() > 0)
                    line.append(" " + (eff.getAmplifier() + 1));
                lines.add(line);
            }
        }

        int pad = 2;
        int lineH = mc.font.lineHeight;
        int textW = 0;
        for (Component c : lines)
            textW = Math.max(textW, mc.font.width(c));
        int boxW = textW + pad * 2;
        int boxH = lineH * lines.size() + pad * 2;
        int margin = 5;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int boxX;
        int boxY;
        switch (HBConfig.get().hoverLocation) {
            case TOP_LEFT -> { boxX = margin; boxY = margin; }
            case TOP_RIGHT -> { boxX = screenW - boxW - margin; boxY = margin; }
            case BOTTOM_RIGHT -> { boxX = screenW - boxW - margin; boxY = screenH - boxH - margin; }
            default -> {
                boxX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow()) + 8;
                boxY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow()) + 8;
            }
        }

        graphics.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF3A3A3A);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xE0101010);
        for (int i = 0; i < lines.size(); i++)
            graphics.text(mc.font, lines.get(i), boxX + pad, boxY + pad + i * lineH, 0xFFFFFFFF, false);
    }

    @Override
    public boolean afterMouseClick(Screen screen, MouseButtonEvent event, boolean handled) {
        if (ContextMenuTrigger.contextMenu != null) {
            if (ContextMenuTrigger.contextMenu.mouseClicked(event.x(), event.y(), event.button())) {
                ContextMenuTrigger.contextMenu.close();
                ContextMenuTrigger.contextMenu = null;
            }
        }
        return false;
    }
}
