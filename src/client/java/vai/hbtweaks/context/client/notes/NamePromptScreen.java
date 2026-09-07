package vai.hbtweaks.context.client.notes;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import vai.hbtweaks.context.client.contextmenu.editor.EditorButton;
import vai.hbtweaks.context.client.contextmenu.editor.EditorStyle;

import java.util.function.Consumer;

public class NamePromptScreen extends Screen {

    private static final int PANEL_W = 220;
    private static final int PANEL_H = 76;
    private static final int PAD = 8;

    private final Screen parent;
    private final String initial;
    private final Consumer<String> onConfirm;
    private SpruceTextFieldWidget field;

    public NamePromptScreen(Screen parent, Component title, String initial, Consumer<String> onConfirm) {
        super(title);
        this.parent = parent;
        this.initial = initial;
        this.onConfirm = onConfirm;
    }

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int l = left();
        int t = top();
        int inner = PANEL_W - PAD * 2;

        this.field = new SpruceTextFieldWidget(Position.of(l + PAD, t + PAD + 14), inner,
                EditorStyle.FIELD_H, this.getTitle());
        this.field.setText(this.initial);
        addRenderableWidget(this.field);

        int gap = 4;
        int bw = (inner - gap) / 2;
        int by = t + PANEL_H - PAD - EditorStyle.BTN_H;
        addRenderableWidget(new EditorButton(Position.of(l + PAD, by), bw, EditorStyle.BTN_H,
                CommonComponents.GUI_CANCEL, this::onClose));
        addRenderableWidget(new EditorButton(Position.of(l + PAD + bw + gap, by), inner - bw - gap,
                EditorStyle.BTN_H, CommonComponents.GUI_DONE, this::confirm));
    }

    private void confirm() {
        String name = this.field.getText().trim();
        if (name.isEmpty()) return;
        this.onConfirm.accept(name);
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int l = left();
        int t = top();
        graphics.fill(l - 1, t - 1, l + PANEL_W + 1, t + PANEL_H + 1, EditorStyle.PANEL_BORDER);
        graphics.fill(l, t, l + PANEL_W, t + PANEL_H, EditorStyle.PANEL_BG);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(this.font, this.getTitle(), left() + PAD, top() + PAD, EditorStyle.ACCENT, false);
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
