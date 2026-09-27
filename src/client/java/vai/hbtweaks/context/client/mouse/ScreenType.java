package vai.hbtweaks.context.client.mouse;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import vai.hbtweaks.context.client.screen.CursorScreen;

/**
 * Coarse category of the open screen, passed along with mouse click events. Constants are
 * matched in declaration order, so more specific screens must come before their parents.
 */
public enum ScreenType {
    INVENTORY(InventoryScreen.class),
    // WARN: CreativeModeInventoryScreen extends AbstractContainerScreen, so CONTAINER matches it
    // first and CREATIVE_INVENTORY is never returned.
    CONTAINER(AbstractContainerScreen.class),
    CHAT(ChatScreen.class),
    PAUSE(PauseScreen.class),
    CREATIVE_INVENTORY(CreativeModeInventoryScreen.class),
    CURSOR(CursorScreen.class),
    UNKNOWN(Screen.class);

    private final Class<? extends Screen> screenClass;

    ScreenType(Class<? extends Screen> screenClass) {
        this.screenClass = screenClass;
    }

    /**
     * @param screen the open screen, may be null
     * @return the first matching type, UNKNOWN if none matches, or null if no screen is open
     */
    public static ScreenType fromScreen(Screen screen) {
        if (screen == null) return null;
        for (ScreenType type : values()) {
            if (type == UNKNOWN) continue;
            if (type.screenClass.isInstance(screen)) return type;
        }
        return UNKNOWN;
    }
}
