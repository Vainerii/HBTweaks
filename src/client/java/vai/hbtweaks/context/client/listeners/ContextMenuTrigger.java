package vai.hbtweaks.context.client.listeners;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import vai.hbtweaks.context.client.contextmenu.ContextMenu;
import vai.hbtweaks.context.client.contextmenu.CustomContextMenuLoader;
import vai.hbtweaks.context.client.Util;
import vai.hbtweaks.context.client.mouse.MouseTracker;
import vai.hbtweaks.context.client.mouse.MouseTrackerEntityClickUpCallback;
import vai.hbtweaks.context.client.mouse.ClickType;
import vai.hbtweaks.context.client.mouse.ScreenType;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static vai.hbtweaks.context.client.Util.hasDev;
import static vai.hbtweaks.context.client.Util.hasPerm;

public class ContextMenuTrigger implements MouseTrackerEntityClickUpCallback, ScreenMouseEvents.AfterMouseClick
{

    private static ContextMenu contextMenu = null;
    public static Map<String, Object> customMenu = CustomContextMenuLoader.readYaml(Paths.get("custom_menu.yml"));
    public static Map<String, Object> customMenuSelf = CustomContextMenuLoader.readYaml(Paths.get("custom_menu_self.yml"));

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
        /*HerobrinePlayer hbp = HerobrinePlayer.getHerobrinePlayer(player);
        if (hbp != null) {
            context.addInfoItem("HbId: " + hbp.getHbId());
            context.addInfoItem("HbName: " + hbp.getHbName());
            context.addInfoItem("RpName: " + hbp.getRpName().getString());
            context.addInfoItem("McName: " + hbp.getMcName());
            context.addInfoItem("HbTitle: " + hbp.getHbTitle());
            context.addInfoItem("McLevel: " + hbp.getMcLevel());

            context.addInfoItem("BulbLit: " + hbp.getBulbLit());
            context.addInfoItem("BulbOff: " + hbp.getBulbOff());
            context.addInfoItem("BulbBroken: " + hbp.getBulbBroken());
            context.addInfoItem("BulbShattered: " + hbp.getBulbShattered());
        }*/
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
        //HerobrinePlayer hbp = HerobrinePlayer.getHerobrinePlayer(player);
        //if (hbp == null) {
        PlayerInfo pi = Minecraft.getInstance().player.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return null;
        ContextMenu infosContext = new ContextMenu(0, 0, player);
        infosContext.addInfoItem(pi.getTabListDisplayName());
        addMcNameItems(infosContext, player, ContextMenuTrigger.getMCName(player));
        return infosContext;
        /*} else {
            ContextMenu infosContext = new ContextMenu(0, 0, player);
            Component fullName = hbp.getHbTitle() != null
                    ? Component.literal(hbp.getHbTitle()).append(" ").append(hbp.getRpName())
                    : hbp.getRpName().copy();
            infosContext.addInfoItem(fullName);
            addMcNameItems(infosContext, player, hbp.getMcName());
            return infosContext;
        }*/
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
        emotes.addCommandItem("Applaudir", "emote clapclap");
        emotes.addCommandItem("S'agenouiller", "emote kneel");
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
            context.merge(CustomContextMenuLoader.load(ContextMenuTrigger.customMenuSelf, Minecraft.getInstance().player));

        return context;
    }

    @Override
    public void onClickUp(List<Entity> list, ClickType clickType, ScreenType screenType) {
        if (screenType == ScreenType.CHAT && clickType == ClickType.RIGHT_CLICK) {
            Minecraft mc = Minecraft.getInstance();
            int x = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
            int y = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());

            Entity target = list.isEmpty() ? null : list.getFirst();
            if (!(target instanceof Player)) {
                if (mc.player == null)
                    return;
                if (ContextMenuTrigger.contextMenu != null)
                    ContextMenuTrigger.contextMenu.close();
                ContextMenuTrigger.contextMenu = makeSelfContextMenu(mc.player, x, y);
                ContextMenuTrigger.contextMenu.open();
                return;
            }

            Player targetPlayer = (Player) target;
            if (mc.player.connection.getPlayerInfo(targetPlayer.getUUID()) == null)
                return;

            if (ContextMenuTrigger.contextMenu != null) {
                ContextMenuTrigger.contextMenu.close();
            }

            ContextMenuTrigger.contextMenu = new ContextMenu(x, y, targetPlayer);

            // == Infos ==
            ContextMenuTrigger.contextMenu.addSubmenuItem("Infos", makeInfoContextMenu(targetPlayer));

            // == Reput ==
            if (isCommandAvailable("reputok"))
                ContextMenuTrigger.contextMenu.addSubmenuItem("Reput", makeReputContextMenu(targetPlayer));

            // == Avis ==
            if (isCommandAvailable("avisok"))
                ContextMenuTrigger.contextMenu.addSubmenuItem("Avis", makeAvisContextMenu(targetPlayer));

            // == Items ==
            if (hasPerm())
                ContextMenuTrigger.contextMenu.addSubmenuItem("Items", makeInvContextMenu(targetPlayer));

            // == Custom ==
            if (ContextMenuTrigger.customMenu != null)
                ContextMenuTrigger.contextMenu.merge(CustomContextMenuLoader.load(ContextMenuTrigger.customMenu, targetPlayer));

            // == Debug ==
            if (hasDev())
                ContextMenuTrigger.contextMenu.addSubmenuItem("Debug", makeDebugContextMenu(targetPlayer));

            ContextMenuTrigger.contextMenu.open();
        }
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
        // Drop the menu once it has been closed (clicked away / item selected) or the mouse is grabbed.
        if (contextMenu != null
                && (!contextMenu.isVisible() || Minecraft.getInstance().mouseHandler.isMouseGrabbed())) {
            contextMenu.close();
            contextMenu = null;
        }

        if (contextMenu != null)
            contextMenu.render(graphics, tickDelta);
        else
            renderHoverName(graphics);
    }

    private static void renderHoverName(GuiGraphicsExtractor graphics) {
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

        int mouseX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int mouseY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        int boxX = mouseX + 8;
        int boxY = mouseY + 8;
        int pad = 2;
        int textW = mc.font.width(name);
        int lineH = mc.font.lineHeight;

        graphics.fill(boxX - 1, boxY - 1, boxX + textW + pad * 2 + 1, boxY + lineH + pad * 2 + 1, 0xFF3A3A3A);
        graphics.fill(boxX, boxY, boxX + textW + pad * 2, boxY + lineH + pad * 2, 0xE0101010);
        graphics.text(mc.font, name, boxX + pad, boxY + pad, 0xFFFFFFFF, false);
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
