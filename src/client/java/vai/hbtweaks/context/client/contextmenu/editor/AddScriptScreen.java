package vai.hbtweaks.context.client.contextmenu.editor;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextAreaWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class AddScriptScreen extends EditorScreen {
    private static final String[][] TAGS = {
            {"[wait:5s]", "hbtweaks.context.editor.tag.wait"},
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
    private final int editIndex;
    private final String initialName;
    private final List<String> initialLines;
    private SpruceTextFieldWidget nameField;
    private SpruceTextAreaWidget scriptArea;

    public AddScriptScreen(Screen parent, MenuLocation location) {
        this(parent, location, -1, "", List.of());
    }

    public AddScriptScreen(Screen parent, MenuLocation location, int editIndex, String name, List<String> lines) {
        super(parent, Component.translatable("hbtweaks.context.editor.add_script"), 460, 200);
        this.location = location;
        this.editIndex = editIndex;
        this.initialName = name;
        this.initialLines = lines;
    }

    @Override
    protected void build(SpruceContainerWidget panel) {
        int pad = EditorStyle.PADDING;
        int leftW = 200;
        int y = pad + 16;

        Component nameLabel = Component.translatable("hbtweaks.context.editor.name");
        Component scriptLabel = Component.translatable("hbtweaks.context.editor.script");

        panel.addChild(new SpruceLabelWidget(Position.of(panel, pad, y), nameLabel, leftW));
        y += 11;
        this.nameField = new SpruceTextFieldWidget(Position.of(panel, pad, y), leftW, EditorStyle.FIELD_H, nameLabel);
        panel.addChild(this.nameField);
        y += EditorStyle.FIELD_H + EditorStyle.ROW_GAP;

        panel.addChild(new SpruceLabelWidget(Position.of(panel, pad, y), scriptLabel, leftW));
        y += 11;
        int areaH = 80;
        this.scriptArea = new SpruceTextAreaWidget(Position.of(panel, pad, y), leftW, areaH, scriptLabel);
        panel.addChild(this.scriptArea);
        y += areaH + EditorStyle.ROW_GAP + 2;

        if (this.editIndex >= 0) {
            this.nameField.setText(this.initialName);
            this.scriptArea.setLines(new ArrayList<>(this.initialLines));
        }

        int btnW = (leftW - EditorStyle.ROW_GAP) / 2;
        panel.addChild(new EditorButton(Position.of(panel, pad, y), btnW, EditorStyle.BTN_H,
                Component.translatable("hbtweaks.context.editor.button.cancel"), this::onClose));
        panel.addChild(new EditorButton(Position.of(panel, pad + btnW + EditorStyle.ROW_GAP, y),
                btnW, EditorStyle.BTN_H, Component.translatable("hbtweaks.context.editor.button.add"), this::submit));

        int csX = pad + leftW + pad;
        int csW = this.panelW - csX - pad;
        int csY = pad + 16;
        SpruceLabelWidget csTitle = new SpruceLabelWidget(Position.of(panel, csX, csY), Component.translatable("hbtweaks.context.editor.tags"), csW);
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

    private void submit() {
        String name = this.nameField.getText().trim();
        List<String> lines = new ArrayList<>();
        for (String line : this.scriptArea.getLines()) {
            String l = line.replace("\n", "").stripTrailing();
            if (!l.isBlank()) lines.add(l);
        }
        if (name.isEmpty() || lines.isEmpty()) return;
        if (this.editIndex >= 0)
            this.location.replaceScript(this.editIndex, name, lines);
        else
            this.location.addScript(name, lines);
        this.done();
    }
}
