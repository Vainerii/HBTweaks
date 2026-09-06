package vai.hbtweaks.context.client.listeners;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
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
import vai.hbtweaks.context.client.contextmenu.DebugMenu;
import vai.hbtweaks.context.client.effects.EffectsBank;
import vai.hbtweaks.context.client.network.EffectPayloads;
import vai.hbtweaks.context.client.contextmenu.CustomContextMenuLoader;
import vai.hbtweaks.context.client.contextmenu.editor.AddCommandScreen;
import vai.hbtweaks.context.client.contextmenu.editor.AddScriptScreen;
import vai.hbtweaks.context.client.contextmenu.editor.AddSubmenuScreen;
import vai.hbtweaks.context.client.contextmenu.editor.DeleteConfirmScreen;
import vai.hbtweaks.context.client.contextmenu.editor.MenuLocation;
import vai.hbtweaks.context.client.keyboard.WritingStatusSender;
import vai.hbtweaks.context.client.Util;
import vai.hbtweaks.context.client.config.HBConfig;
import vai.hbtweaks.context.client.notes.NotesMenu;
import vai.hbtweaks.context.client.mouse.MouseTracker;
import vai.hbtweaks.context.client.mouse.MouseTrackerEntityClickUpCallback;
import vai.hbtweaks.context.client.mouse.ClickType;
import vai.hbtweaks.context.client.mouse.ScreenType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static vai.hbtweaks.context.client.Util.*;

public class ContextMenuTrigger implements MouseTrackerEntityClickUpCallback, ScreenMouseEvents.AfterMouseClick
{

    private static ContextMenu contextMenu = null;
    private static Runnable rebuilder = null;
    private static UUID effectsRequestedFor = null;

    private static ContextMenuTrigger instance;

    public ContextMenuTrigger() {
        instance = this;
    }

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

    private ContextMenu makeInfoContextMenu(Player player) {

        PlayerInfo pi = Minecraft.getInstance().player.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return null;
        ContextMenu infosContext = new ContextMenu(0, 0, player);
        infosContext.addInfoItem(pi.getTabListDisplayName());
        addMcNameItems(infosContext, player, Util.getVisibleMCName(player));
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

        if (hasPerm()) {
            ContextMenu options = new ContextMenu(0, 0, self);
            options.addCheckboxItem(new ContextMenu.CheckboxItem(Component.literal("Partager si je suis en train d'écrire")) {
                @Override public boolean isChecked() {
                    return HBConfig.get().shareTyping;
                }
                @Override protected void checked() {
                    HBConfig.get().shareTyping = true;
                    HBConfig.HANDLER.save();
                }
                @Override protected void unchecked() {
                    HBConfig.get().shareTyping = false;
                    HBConfig.HANDLER.save();
                    WritingStatusSender.stopWriting();
                }
            });
            /*
            options.addCheckboxItem(new ContextMenu.CheckboxItem(Component.literal("Montrer mon pseudo")) {
                @Override public boolean isChecked() {
                    return HBConfig.get().showMyName;
                }
                @Override protected void checked() {
                    HBConfig.get().showMyName = true;
                    HBConfig.HANDLER.save();
                }
                @Override protected void unchecked() {
                    HBConfig.get().showMyName = false;
                    HBConfig.HANDLER.save();
                }
            });*/
            context.addSubmenuItem("Options", options);
        }

        if (ContextMenuTrigger.customMenuSelf != null)
            context.merge(CustomContextMenuLoader.load(ContextMenuTrigger.customMenuSelf, Minecraft.getInstance().player, CUSTOM_MENU_SELF));

        if (hasDev())
            context.addSubmenuItem("Debug", DebugMenu.forSelf(self));

        context.addAddItem(new MenuLocation(CUSTOM_MENU_SELF, List.of()));
        context.withEditToggle();
        context.asRootMenu();

        return context;
    }

    private static Player firstVisiblePlayer(List<Entity> entities) {
        for (Entity e : entities) {
            if (e instanceof Player p && !p.isInvisible())
                return p;
        }
        return null;
    }

    @Override
    public void onClickUp(List<Entity> list, ClickType clickType, ScreenType screenType) {
        if (!HBConfig.get().contextMenus) return;
        if ((screenType == ScreenType.CHAT || screenType == ScreenType.CURSOR) && clickType == ClickType.RIGHT_CLICK) {
            if (isMouseOverChat()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            int x = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
            int y = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());

            try {
                Player targetPlayer = firstVisiblePlayer(list);
                if (targetPlayer == null) {
                    rebuilder = () -> {
                        if (ContextMenuTrigger.contextMenu != null)
                            ContextMenuTrigger.contextMenu.close();
                        ContextMenuTrigger.contextMenu = makeSelfContextMenu(Minecraft.getInstance().player, x, y);
                        ContextMenuTrigger.contextMenu.open();
                    };
                    rebuilder.run();
                    return;
                }
                if (!isReal(targetPlayer)) return;
                openForPlayer(targetPlayer, x, y);
            } catch (Exception e) {
                HBTweaksContext.LOGGER.error("Failed to open context menu", e);
                dispose();
            }
        }
    }

    public void openForPlayer(Player targetPlayer, int x, int y) {
        openForPlayer(targetPlayer, x, y, true);
    }

    public void openForPlayer(Player targetPlayer, int x, int y, boolean loaded) {
        if (!HBConfig.get().contextMenus) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection.getPlayerInfo(targetPlayer.getUUID()) == null)
            return;
        try {
            if (ContextMenuTrigger.contextMenu != null)
                ContextMenuTrigger.contextMenu.close();

            ContextMenuTrigger.contextMenu = new ContextMenu(x, y, targetPlayer);

            addSubmenuIfPresent("Infos", makeInfoContextMenu(targetPlayer));
            addSubmenuIfPresent("Notes", NotesMenu.forPlayer(targetPlayer));
            if (isCommandAvailable("reputok"))
                addSubmenuIfPresent("Reput", makeReputContextMenu(targetPlayer));
            if (isCommandAvailable("avisok"))
                addSubmenuIfPresent("Avis", makeAvisContextMenu(targetPlayer));
            if (loaded && hasPerm())
                addSubmenuIfPresent("Items", makeInvContextMenu(targetPlayer));
            if (ContextMenuTrigger.customMenu != null)
                ContextMenuTrigger.contextMenu.merge(CustomContextMenuLoader.load(ContextMenuTrigger.customMenu, targetPlayer, CUSTOM_MENU));
            if (hasDev())
                addSubmenuIfPresent("Debug", DebugMenu.forPlayer(targetPlayer));

            ContextMenuTrigger.contextMenu.addAddItem(new MenuLocation(CUSTOM_MENU, List.of()));
            ContextMenuTrigger.contextMenu.withEditToggle();
            ContextMenuTrigger.contextMenu.asRootMenu();
            ContextMenuTrigger.contextMenu.open();
            rebuilder = () -> openForPlayer(targetPlayer, x, y, loaded);
        } catch (Exception e) {
            HBTweaksContext.LOGGER.error("Failed to open context menu", e);
            dispose();
        }
    }

    public static void openMenuFor(Player target, int x, int y) {
        if (instance != null)
            instance.openForPlayer(target, x, y, true);
    }

    /** Opens the menu for connected but unloaded player */
    public static void openMenuForRemote(GameProfile profile, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        if (instance == null || mc.level == null)
            return;
        instance.openForPlayer(new RemotePlayer(mc.level, profile), x, y, false);
    }

    public static void rebuildAfterEdit() {
        if (rebuilder == null) return;
        try {
            rebuilder.run();
        } catch (Exception e) {
            HBTweaksContext.LOGGER.error("Failed to rebuild context menu", e);
            dispose();
        }
    }

    public static void requestDelete(MenuLocation.DeleteRef ref) {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen;
        dispose();
        mc.setScreen(new DeleteConfirmScreen(parent, ref));
    }

    public static void requestEdit(MenuLocation.DeleteRef ref) {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen;
        Map<String, Object> entry = ref.container().entryAt(ref.index());
        if (entry == null) return;
        String label = String.valueOf(entry.getOrDefault("label", ""));
        Object cmd = entry.get("command");
        dispose();
        if (cmd instanceof List<?> list) {
            List<String> lines = new java.util.ArrayList<>();
            for (Object o : list)
                lines.add(String.valueOf(o));
            mc.setScreen(new AddScriptScreen(parent, ref.container(), ref.index(), label, lines));
        } else if (cmd != null) {
            mc.setScreen(new AddCommandScreen(parent, ref.container(), ref.index(), label, String.valueOf(cmd)));
        } else if (entry.containsKey("submenu")) {
            mc.setScreen(new AddSubmenuScreen(parent, ref.container(), ref.index(), label));
        }
    }

    public static boolean isMenuOver(double mx, double my) {
        return contextMenu != null && contextMenu.isVisible() && contextMenu.containsMouseRecursive((int) mx, (int) my);
    }

    private static void addSubmenuIfPresent(String label, ContextMenu submenu) {
        if (submenu != null)
            ContextMenuTrigger.contextMenu.addSubmenuItem(label, submenu);
    }

    private static boolean isMouseOverChat() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof ChatScreen)) return false;
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
            if (contextMenu != null) {
                contextMenu.render(graphics, tickDelta);
                ContextMenu.renderEditLegend(graphics);
            } else
                renderHoverName(graphics, leftDown);
        } catch (Exception e) {
            HBTweaksContext.LOGGER.error("Failed to render context menu / hover box", e);
            dispose();
        }
    }

    private static void renderHoverName(GuiGraphicsExtractor graphics, boolean leftDown) {
        if (Minecraft.getInstance().options.hideGui) return;
        if (isMouseOverChat()) return;
        Player target = firstVisiblePlayer(MouseTracker.getHoveredEntities());
        if (target == null) return;
        if (!isReal(target)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection.getPlayerInfo(target.getUUID()) == null) return;

        Component name = Util.getRpName(target);
        if (name == null) return;

        double dist = mc.player.position().distanceTo(target.position());

        name = Util.getHead(target).copy().append(name);
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
        int boxH = lineH * lines.size() + pad * 2 - 1;
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

        boxX = Math.max(margin, Math.min(boxX, screenW - boxW - margin));
        boxY = Math.max(margin, Math.min(boxY, screenH - boxH - margin));

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
