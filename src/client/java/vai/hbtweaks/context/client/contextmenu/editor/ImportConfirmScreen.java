package vai.hbtweaks.context.client.contextmenu.editor;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shows what an imported file adds before adding it. */
public class ImportConfirmScreen extends EditorScreen {
    private static final int WIDTH = 300;
    private static final int LINE_H = 10;
    private static final int MAX_LINES = 14;

    private final MenuLocation container;
    private final List<Map<String, Object>> entries;
    private final String author;
    private final Pattern authorPattern;
    private boolean replaceAuthor;

    public ImportConfirmScreen(Screen parent, MenuLocation container, List<Map<String, Object>> entries, String author) {
        this(parent, container, entries, author, true);
    }

    private ImportConfirmScreen(Screen parent, MenuLocation container, List<Map<String, Object>> entries,
                                String author, boolean replaceAuthor) {
        super(parent, Component.translatable("hbtweaks.context.editor.import_title"), WIDTH,
                panelHeight(entries, author));
        this.container = container;
        this.entries = entries;
        this.author = author;
        this.authorPattern = authorPattern(author);
        // Only offered when the exporter's name is actually in a command.
        this.replaceAuthor = replaceAuthor && this.authorPattern != null && mentionsAuthor(entries, this.authorPattern);
    }

    private static Pattern authorPattern(String author) {
        if (author == null) return null;
        return Pattern.compile("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(author) + "(?![A-Za-z0-9_])");
    }

    private static int panelHeight(List<Map<String, Object>> entries, String author) {
        int lines = Math.min(describe(entries, null).size(), MAX_LINES + 1);
        Pattern p = authorPattern(author);
        int option = p != null && mentionsAuthor(entries, p) ? EditorStyle.BTN_H + EditorStyle.ROW_GAP : 0;
        return EditorStyle.PADDING * 2 + 16 + lines * LINE_H + EditorStyle.ROW_GAP + option + EditorStyle.BTN_H;
    }

    @Override
    protected void build(SpruceContainerWidget panel) {
        int pad = EditorStyle.PADDING;
        int innerW = this.panelW - pad * 2;
        int y = pad + 16;

        List<Component> lines = describe(this.entries, this.replaceAuthor ? this.authorPattern : null);
        for (int i = 0; i < Math.min(lines.size(), MAX_LINES); i++) {
            panel.addChild(new SpruceLabelWidget(Position.of(panel, pad, y), lines.get(i), innerW));
            y += LINE_H;
        }
        if (lines.size() > MAX_LINES) {
            panel.addChild(new SpruceLabelWidget(Position.of(panel, pad, y),
                    Component.literal("… (+" + (lines.size() - MAX_LINES) + ")").withStyle(ChatFormatting.GRAY), innerW));
            y += LINE_H;
        }
        y += EditorStyle.ROW_GAP;

        if (this.authorPattern != null && mentionsAuthor(this.entries, this.authorPattern)) {
            Component label = Component.literal(this.replaceAuthor ? "[x] " : "[ ] ")
                    .append(Component.translatable("hbtweaks.context.editor.import_replace", this.author));
            panel.addChild(new EditorButton(Position.of(panel, pad, y), innerW, EditorStyle.BTN_H, label,
                    this::toggleReplace));
            y += EditorStyle.BTN_H + EditorStyle.ROW_GAP;
        }

        int btnW = (innerW - EditorStyle.ROW_GAP) / 2;
        panel.addChild(new EditorButton(Position.of(panel, pad, y), btnW, EditorStyle.BTN_H,
                Component.translatable("hbtweaks.context.editor.button.cancel"), this::onClose));
        panel.addChild(new EditorButton(Position.of(panel, pad + btnW + EditorStyle.ROW_GAP, y),
                btnW, EditorStyle.BTN_H, Component.translatable("hbtweaks.context.editor.button.import"), this::confirm));
    }

    private void toggleReplace() {
        this.minecraft.setScreen(new ImportConfirmScreen(this.parent, this.container, this.entries,
                this.author, !this.replaceAuthor));
    }

    private void confirm() {
        this.container.addEntries(this.replaceAuthor ? replaced(this.entries, this.authorPattern) : this.entries);
        this.done();
    }

    private static List<Component> describe(List<Map<String, Object>> entries, Pattern replace) {
        List<Component> out = new ArrayList<>();
        describe(entries, replace, "", out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void describe(List<Map<String, Object>> entries, Pattern replace, String indent, List<Component> out) {
        for (Map<String, Object> entry : entries) {
            if (entry.containsKey("info")) {
                out.add(Component.literal(indent + entry.get("info")).withStyle(ChatFormatting.GRAY));
                continue;
            }
            String label = indent + entry.get("label");
            Object command = entry.get("command");
            if (entry.containsKey("submenu")) {
                out.add(Component.literal(label + " ▸").withStyle(ChatFormatting.WHITE));
                describe((List<Map<String, Object>>) entry.get("submenu"), replace, indent + "  ", out);
            } else if (command instanceof List<?> script) {
                out.add(Component.literal(label).withStyle(ChatFormatting.WHITE));
                for (Object line : script)
                    out.add(Component.literal(indent + "    " + apply(String.valueOf(line), replace))
                            .withStyle(ChatFormatting.YELLOW));
            } else {
                out.add(Component.literal(label).withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(" → /" + apply(String.valueOf(command), replace))
                                .withStyle(ChatFormatting.YELLOW)));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean mentionsAuthor(List<Map<String, Object>> entries, Pattern author) {
        for (Map<String, Object> entry : entries) {
            Object command = entry.get("command");
            if (command instanceof List<?> script) {
                for (Object line : script)
                    if (author.matcher(String.valueOf(line)).find()) return true;
            } else if (command != null && author.matcher(String.valueOf(command)).find()) {
                return true;
            }
            if (entry.get("submenu") instanceof List<?> sub && mentionsAuthor((List<Map<String, Object>>) sub, author))
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> replaced(List<Map<String, Object>> entries, Pattern author) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            Map<String, Object> copy = new LinkedHashMap<>(entry);
            Object command = entry.get("command");
            if (command instanceof List<?> script) {
                List<String> lines = new ArrayList<>();
                for (Object line : script)
                    lines.add(apply(String.valueOf(line), author));
                copy.put("command", lines);
            } else if (command != null) {
                copy.put("command", apply(String.valueOf(command), author));
            }
            if (entry.get("submenu") instanceof List<?> sub)
                copy.put("submenu", replaced((List<Map<String, Object>>) sub, author));
            out.add(copy);
        }
        return out;
    }

    private static String apply(String text, Pattern author) {
        return author == null ? text : author.matcher(text).replaceAll(Matcher.quoteReplacement("%mymcname%"));
    }
}
