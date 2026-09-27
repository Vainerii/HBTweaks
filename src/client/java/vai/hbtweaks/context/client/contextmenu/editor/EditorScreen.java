package vai.hbtweaks.context.client.contextmenu.editor;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.SimpleColorBackground;
import dev.lambdaurora.spruceui.border.SimpleBorder;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base of the custom menu editor dialogs: a centred, bordered panel with the title at the top.
 * Subclasses fill the panel in build.
 */
abstract class EditorScreen extends SpruceScreen {
    protected final Screen parent;
    protected final int panelW;
    protected final int panelH;

    /**
     * @param parent the screen to return to
     * @param title the panel title
     * @param panelW the panel width, in GUI pixels
     * @param panelH the panel height, in GUI pixels
     */
    protected EditorScreen(Screen parent, Component title, int panelW, int panelH) {
        super(title);
        this.parent = parent;
        this.panelW = panelW;
        this.panelH = panelH;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.panelW) / 2;
        int y = (this.height - this.panelH) / 2;

        SpruceContainerWidget panel = new SpruceContainerWidget(Position.of(x, y), this.panelW, this.panelH);
        panel.setBackground(new SimpleColorBackground(EditorStyle.PANEL_BG));
        panel.setBorder(new SimpleBorder(1, EditorStyle.PANEL_BORDER));

        SpruceLabelWidget titleLabel = new SpruceLabelWidget(
                Position.of(panel, EditorStyle.PADDING, EditorStyle.PADDING),
                this.getTitle(), this.panelW - EditorStyle.PADDING * 2);
        titleLabel.setColor(EditorStyle.ACCENT);
        panel.addChild(titleLabel);

        build(panel);
        this.addRenderableWidget(panel);
    }

    /**
     * Adds the dialog's widgets to the panel. Positions are relative to the panel.
     *
     * @param panel the panel, already containing the title
     */
    protected abstract void build(SpruceContainerWidget panel);

    /** Returns to the parent screen, to be called once the dialog's action is applied. */
    protected void done() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
