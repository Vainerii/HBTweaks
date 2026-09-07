package vai.hbtweaks.context.client.contextmenu.editor;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.render.SpruceGuiGraphics;
import dev.lambdaurora.spruceui.widget.AbstractSpruceWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class EditorButton extends AbstractSpruceWidget {
    private final Component label;
    private final Runnable onClick;
    private final int accent;
    private final int baseBg;

    public EditorButton(Position position, int w, int h, Component label, Runnable onClick) {
        this(position, w, h, label, onClick, EditorStyle.BTN_BG, EditorStyle.BTN_HOVER);
    }

    public EditorButton(Position position, int w, int h, Component label, Runnable onClick, int bg, int hoverBg) {
        super(position);
        this.width = w;
        this.height = h;
        this.label = label;
        this.onClick = onClick;
        this.accent = hoverBg;
        this.baseBg = bg;
    }

    @Override
    protected boolean onMouseClick(MouseButtonEvent event, boolean dbl) {
        this.onClick.run();
        return true;
    }

    @Override
    protected void extractWidgetRenderState(SpruceGuiGraphics g, int mx, int my, float d) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();
        boolean enabled = this.isActive();
        boolean hovered = enabled && mx >= x && mx < x + w && my >= y && my < y + h;

        g.fill(x, y, x + w, y + h, hovered ? this.accent : this.baseBg);
        int border = EditorStyle.BTN_BORDER;
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);

        Font font = Minecraft.getInstance().font;
        int textW = font.width(this.label);
        int drawX = x + (w - textW) / 2;
        int drawY = y + (h - font.lineHeight) / 2;
        g.text(font, this.label, drawX, drawY, enabled ? 0xFFFFFFFF : EditorStyle.TEXT_DIM, false);
    }
}
