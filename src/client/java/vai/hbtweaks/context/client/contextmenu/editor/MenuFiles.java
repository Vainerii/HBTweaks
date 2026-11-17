package vai.hbtweaks.context.client.contextmenu.editor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import vai.hbtweaks.context.HBTweaksContext;
import vai.hbtweaks.context.client.contextmenu.CustomContextMenuLoader;
import vai.hbtweaks.context.client.listeners.ContextMenuTrigger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Export custom menu entries through files from the system window
 * Export use the custom_menu.yml layout + author name
 */
public final class MenuFiles {

    private static final String DESCRIPTION = "Menu HBTweaks (*.yml)";

    private MenuFiles() {}

    public static void exportEntry(Map<String, Object> entry) {
        if (entry == null) return;
        Object label = entry.containsKey("label") ? entry.get("label") : entry.get("info");
        String name = String.valueOf(label).replaceAll("[^\\p{L}\\p{N} _-]", "_").trim();
        if (name.isEmpty()) name = "menu";

        String path;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            path = TinyFileDialogs.tinyfd_saveFileDialog("Exporter", name + ".yml", filters(stack), DESCRIPTION);
        }
        if (path == null) return;
        if (!path.endsWith(".yml") && !path.endsWith(".yaml"))
            path += ".yml";

        Map<String, Object> root = new LinkedHashMap<>();
        Minecraft mc = Minecraft.getInstance();
        String author = mc.player == null ? null : ContextMenuTrigger.getMCName(mc.player);
        if (author != null)
            root.put("author", author);
        root.put("menu", new ArrayList<>(List.of(entry)));

        Path file = Path.of(path);
        try {
            Files.writeString(file, yaml().dump(root));
        } catch (IOException e) {
            HBTweaksContext.LOGGER.error("Failed to export menu to {}", file, e);
            chat("Échec de l'export : " + file.getFileName());
            return;
        }
        chat("Exporté : " + file.getFileName());
    }

    public static void importInto(MenuLocation container) {
        String path;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            path = TinyFileDialogs.tinyfd_openFileDialog("Importer", "", filters(stack), DESCRIPTION, false);
        }
        if (path == null) return;

        Map<String, Object> root = CustomContextMenuLoader.readYaml(Path.of(path));
        List<Map<String, Object>> entries = root == null ? List.of() : validEntries(root.get("menu"));
        if (entries.isEmpty()) {
            chat("Fichier de menu invalide ou vide");
            return;
        }
        Object author = root.get("author");
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ImportConfirmScreen(mc.screen, container, entries,
                author instanceof String s && !s.isBlank() ? s : null));
    }

    /** Keeps only entries the menu loader can read */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> validEntries(Object list) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!(list instanceof List<?> items)) return out;
        for (Object o : items) {
            if (!(o instanceof Map<?, ?> m)) continue;
            Map<String, Object> entry = (Map<String, Object>) m;
            if (entry.get("info") instanceof String) {
                out.add(entry);
                continue;
            }
            if (!(entry.get("label") instanceof String)) continue;
            if (entry.containsKey("submenu")) {
                entry.put("submenu", validEntries(entry.get("submenu")));
                out.add(entry);
            } else if (entry.get("command") instanceof String || entry.get("command") instanceof List) {
                out.add(entry);
            }
        }
        return out;
    }

    static Yaml yaml() {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        opts.setIndent(2);
        return new Yaml(opts);
    }

    private static PointerBuffer filters(MemoryStack stack) {
        PointerBuffer filters = stack.mallocPointer(2);
        filters.put(stack.UTF8("*.yml"));
        filters.put(stack.UTF8("*.yaml"));
        return filters.flip();
    }

    private static void chat(String text) {
        Minecraft.getInstance().gui.getChat().addClientSystemMessage(
                Component.literal(text).withStyle(ChatFormatting.GRAY));
    }
}
