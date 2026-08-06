package vai.hbtweaks.context.client.contextmenu.editor;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AddSubmenuScreen extends EditorScreen {
    private final MenuLocation location;
    private final int editIndex;
    private final String initialName;
    private SpruceTextFieldWidget nameField;

    public AddSubmenuScreen(Screen parent, MenuLocation location) {
        this(parent, location, -1, "");
    }

    public AddSubmenuScreen(Screen parent, MenuLocation location, int editIndex, String name) {
        super(parent, Component.translatable("hbtweaks.context.editor.add_submenu"), 220, 92);
        this.location = location;
        this.editIndex = editIndex;
        this.initialName = name;
    }

    @Override
    protected void build(SpruceContainerWidget panel) {
        int pad = EditorStyle.PADDING;
        int innerW = this.panelW - pad * 2;
        int y = pad + 14;

        Component nameLabel = Component.translatable("hbtweaks.context.editor.name");
        panel.addChild(new SpruceLabelWidget(Position.of(panel, pad, y), nameLabel, innerW));
        y += 11;

        this.nameField = new SpruceTextFieldWidget(Position.of(panel, pad, y),
                innerW, EditorStyle.FIELD_H, nameLabel);
        if (this.editIndex >= 0)
            this.nameField.setText(this.initialName);
        panel.addChild(this.nameField);
        y += EditorStyle.FIELD_H + EditorStyle.ROW_GAP + 2;

        int btnW = (innerW - EditorStyle.ROW_GAP) / 2;
        panel.addChild(new EditorButton(Position.of(panel, pad, y), btnW, EditorStyle.BTN_H,
                Component.translatable("hbtweaks.context.editor.button.cancel"), this::onClose));
        panel.addChild(new EditorButton(Position.of(panel, pad + btnW + EditorStyle.ROW_GAP, y),
                btnW, EditorStyle.BTN_H, Component.translatable("hbtweaks.context.editor.button.add"), this::submit));
    }

    private void submit() {
        String name = this.nameField.getText().trim();
        if (name.isEmpty()) return;
        if (this.editIndex >= 0)
            this.location.rename(this.editIndex, name);
        else
            this.location.addSubmenu(name);
        this.done();
    }
}
