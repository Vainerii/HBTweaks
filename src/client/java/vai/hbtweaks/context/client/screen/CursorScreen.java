package vai.hbtweaks.context.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import vai.hbtweaks.context.client.HBTweaksContextClient;

/**
 * Invisible screen for free cursor
 */
public class CursorScreen extends Screen {

    public CursorScreen() {
        super(Component.empty());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) { }

    /** Pressing the cursor key again closes the screen, making the key a toggle. */
    @Override
    public boolean keyPressed(KeyEvent event) {
         if (HBTweaksContextClient.CURSOR_KEY.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
