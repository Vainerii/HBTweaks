package vai.hbtweaks.context.client.mouse;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public enum ScreenType {
    INVENTORY(InventoryScreen.class),
    CONTAINER(AbstractContainerScreen.class),
    CHAT(ChatScreen.class),
    PAUSE(PauseScreen.class),
    CREATIVE_INVENTORY(CreativeModeInventoryScreen.class),
    UNKNOWN(Screen.class);

    private final Class<? extends Screen> screenClass;

    ScreenType(Class<? extends Screen> screenClass) {
        this.screenClass = screenClass;
    }

    public static ScreenType fromScreen(Screen screen) {
        if (screen == null) return null;
        for (ScreenType type : values()) {
            if (type == UNKNOWN) continue;
            if (type.screenClass.isInstance(screen)) return type;
        }
        return UNKNOWN;
    }
}
