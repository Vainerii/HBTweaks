package vai.hbtweaks.context.client.listeners;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import vai.hbtweaks.context.client.config.HBConfig;

// TODO Remove that, it will be done in plugin
public final class ChatCommandProtector {

    private ChatCommandProtector() {}

    public static void init() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!HBConfig.get().chatCommandProtector || !message.startsWith(":")) return true;

            Minecraft mc = Minecraft.getInstance();
            String name = message.substring(1).split(" ", 2)[0];
            if (name.isEmpty() || mc.player.connection.getCommands().getRoot().getChild(name) == null) return true;

            mc.gui.getChat().addClientSystemMessage(Component.literal("Vous avez écrit un \":\" à la place d'un \"/\"").withStyle(ChatFormatting.RED));
            return false;
        });
    }

}
