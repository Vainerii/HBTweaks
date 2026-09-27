package vai.hbtweaks.context.client.contextmenu.editor;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Confirmation dialog before deleting an entry from a custom menu file.
 */
public class DeleteConfirmScreen extends EditorScreen {
    private final MenuLocation.DeleteRef ref;

    public DeleteConfirmScreen(Screen parent, MenuLocation.DeleteRef ref) {
        super(parent, Component.translatable("hbtweaks.context.editor.delete_title"), 240, 84);
        this.ref = ref;
    }

    @Override
    protected void build(SpruceContainerWidget panel) {
        int pad = EditorStyle.PADDING;
        int innerW = this.panelW - pad * 2;
        int y = pad + 16;

        panel.addChild(new SpruceLabelWidget(Position.of(panel, pad, y),
                Component.translatable("hbtweaks.context.editor.delete_confirm", this.ref.label()), innerW));
        y += 16 + EditorStyle.ROW_GAP;

        int btnW = (innerW - EditorStyle.ROW_GAP) / 2;
        panel.addChild(new EditorButton(Position.of(panel, pad, y), btnW, EditorStyle.BTN_H,
                Component.translatable("hbtweaks.context.editor.button.cancel"), this::onClose));
        panel.addChild(new EditorButton(Position.of(panel, pad + btnW + EditorStyle.ROW_GAP, y),
                btnW, EditorStyle.BTN_H, Component.translatable("hbtweaks.context.editor.button.delete"), this::confirm,
                EditorStyle.DANGER, EditorStyle.DANGER_HOVER));
    }

    private void confirm() {
        this.ref.delete();
        this.done();
    }
}
