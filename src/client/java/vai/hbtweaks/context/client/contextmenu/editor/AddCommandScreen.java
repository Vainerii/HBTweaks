package vai.hbtweaks.context.client.contextmenu.editor;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Dialog creating or editing a single-command entry of a custom menu. The command field always
 * starts with "/", which is stripped before saving. The right column lists the available
 * placeholders.
 */
public class AddCommandScreen extends EditorScreen {
    /** Placeholders shown as help: the tag, then the translation key of its description. */
    private static final String[][] TAGS = {
            {"%mcname%", "hbtweaks.context.editor.tag.mcname"},
            {"%rpname%", "hbtweaks.context.editor.tag.rpname"},
            {"%blockpos%", "hbtweaks.context.editor.tag.blockpos"},
            {"%eyepos%", "hbtweaks.context.editor.tag.eyepos"},
            {"%uuid%", "hbtweaks.context.editor.tag.uuid"},
            {"%mymcname%", "hbtweaks.context.editor.tag.mymcname"},
            {"%myrpname%", "hbtweaks.context.editor.tag.myrpname"},
            {"%myblockpos%", "hbtweaks.context.editor.tag.myblockpos"},
            {"%myeyepos%", "hbtweaks.context.editor.tag.myeyepos"},
            {"%myuuid%", "hbtweaks.context.editor.tag.myuuid"},
    };

    private final MenuLocation location;
    /** Index of the edited entry, or -1 when creating a new one. */
    private final int editIndex;
    private final String initialName;
    private final String initialCommand;
    private SpruceTextFieldWidget nameField;
    private SpruceTextFieldWidget commandField;

    /**
     * Creation mode.
     *
     * @param parent the screen to return to
     * @param location the list the entry is appended to
     */
    public AddCommandScreen(Screen parent, MenuLocation location) {
        this(parent, location, -1, "", "");
    }

    /**
     * Edit mode when editIndex is 0 or more, prefilled with the current values.
     *
     * @param parent the screen to return to
     * @param location the list holding the entry
     * @param editIndex the entry index, or -1 to create
     * @param name the current label
     * @param command the current command, without "/"
     */
    public AddCommandScreen(Screen parent, MenuLocation location, int editIndex, String name, String command) {
        super(parent, Component.translatable("hbtweaks.context.editor.add_command"), 460, 160);
        this.location = location;
        this.editIndex = editIndex;
        this.initialName = name;
        this.initialCommand = command;
    }

    @Override
    protected void build(SpruceContainerWidget panel) {
        int pad = EditorStyle.PADDING;
        int leftW = 200;
        int y = pad + 16;

        Component nameLabel = Component.translatable("hbtweaks.context.editor.name");
        Component commandLabel = Component.translatable("hbtweaks.context.editor.command");

        panel.addChild(new SpruceLabelWidget(Position.of(panel, pad, y), nameLabel, leftW));
        y += 11;
        this.nameField = new SpruceTextFieldWidget(Position.of(panel, pad, y),
                leftW, EditorStyle.FIELD_H, nameLabel);
        panel.addChild(this.nameField);
        y += EditorStyle.FIELD_H + EditorStyle.ROW_GAP;

        panel.addChild(new SpruceLabelWidget(Position.of(panel, pad, y), commandLabel, leftW));
        y += 11;
        this.commandField = new SpruceTextFieldWidget(Position.of(panel, pad, y),
                leftW, EditorStyle.FIELD_H, commandLabel);
        this.commandField.setText("/");
        this.commandField.setTextPredicate(s -> s.startsWith("/"));
        panel.addChild(this.commandField);

        if (this.editIndex >= 0) {
            this.nameField.setText(this.initialName);
            this.commandField.setText("/" + this.initialCommand);
        }
        y += EditorStyle.FIELD_H + EditorStyle.ROW_GAP + 2;

        int btnW = (leftW - EditorStyle.ROW_GAP) / 2;
        panel.addChild(new EditorButton(Position.of(panel, pad, y), btnW, EditorStyle.BTN_H,
                Component.translatable("hbtweaks.context.editor.button.cancel"), this::onClose));
        panel.addChild(new EditorButton(Position.of(panel, pad + btnW + EditorStyle.ROW_GAP, y),
                btnW, EditorStyle.BTN_H, Component.translatable("hbtweaks.context.editor.button.add"), this::submit));

        int csX = pad + leftW + pad;
        int csW = this.panelW - csX - pad;
        int csY = pad + 16;
        SpruceLabelWidget csTitle = new SpruceLabelWidget(Position.of(panel, csX, csY),
                Component.translatable("hbtweaks.context.editor.tags"), csW);
        csTitle.setColor(EditorStyle.TEXT);
        panel.addChild(csTitle);
        csY += 12;
        for (String[] tag : TAGS) {
            Component line = Component.literal(tag[0]).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(" ").append(Component.translatable(tag[1])).withStyle(ChatFormatting.GRAY));
            SpruceLabelWidget lw = new SpruceLabelWidget(Position.of(panel, csX, csY), line, csW);
            panel.addChild(lw);
            csY += 11;
        }
    }

    /** Saves the entry if both fields are filled, otherwise stays open. */
    private void submit() {
        String name = this.nameField.getText().trim();
        String command = this.commandField.getText().trim();
        if (command.startsWith("/"))
            command = command.substring(1).trim();
        if (name.isEmpty() || command.isEmpty()) return;
        if (this.editIndex >= 0)
            this.location.replaceCommand(this.editIndex, name, command);
        else
            this.location.addCommand(name, command);
        this.done();
    }
}
