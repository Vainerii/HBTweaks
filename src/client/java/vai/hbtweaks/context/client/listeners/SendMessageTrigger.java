package vai.hbtweaks.context.client.listeners;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import vai.hbtweaks.context.client.mixin.ChatScreenAccessor;
import vai.hbtweaks.context.client.mouse.MouseTrackerEntityClickUpCallback;
import vai.hbtweaks.context.client.mouse.ClickType;
import vai.hbtweaks.context.client.mouse.ScreenType;

import java.util.List;

public class SendMessageTrigger implements MouseTrackerEntityClickUpCallback {
    @Override
    public void onClickUp(List<Entity> list, ClickType clickType, ScreenType screenType) {
        if (screenType == ScreenType.CHAT && clickType == ClickType.MIDDLE_CLICK && !list.isEmpty()) {
            ChatScreen cs = (ChatScreen) Minecraft.getInstance().screen;
            Entity target = list.getFirst();
            if (target instanceof Player) {
                ((ChatScreenAccessor) cs).getInput().insertText(target.getName().getString());
                // TODO Add player name in chat at cursor position
            } else {
                ((ChatScreenAccessor) cs).getInput().insertText(target.getUUID().toString());
                // TODO Add entity uuid in chat at cursor position
            }
        }
    }
}
