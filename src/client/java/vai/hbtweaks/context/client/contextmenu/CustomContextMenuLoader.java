package vai.hbtweaks.context.client.contextmenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.yaml.snakeyaml.Yaml;
import vai.hbtweaks.context.client.contextmenu.editor.MenuLocation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns the user's custom menu YAML files into ContextMenu instances. Expected format:
 * <pre>
 * menu:
 *   - info: "Plain text row"
 *   - label: "Command"
 *     command: "msg %mcname% Hello"
 *   - label: "Script"
 *     command:
 *       - "Hello %rpname%"
 *       - "[wait:2s]"
 *       - "/wave"
 *   - label: "Submenu"
 *     submenu:
 *       - ...
 * </pre>
 * Every parsed row is marked deletable with its MenuLocation, so edit mode can act on it, and
 * every submenu gets its own "add" row.
 */
public class CustomContextMenuLoader {
    private CustomContextMenuLoader() {}

    /**
     * @param path the YAML file
     * @param player the player the menu targets
     * @return the menu, or null if the file cannot be read
     */
    public static ContextMenu load(Path path, Player player) {
        Map<String, Object> root = readYaml(path);
        if (root == null) return null;
        return load(root, player, path);
    }

    /**
     * @param path the YAML file
     * @return the parsed root mapping, or null if the file is missing or invalid
     */
    public static Map<String, Object> readYaml(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            Yaml yaml = new Yaml();
            return yaml.load(is);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param root the parsed YAML root
     * @param player the player the menu targets
     * @param file the file the root came from, used to address entries for editing
     * @return the menu built from the root "menu" list
     */
    public static ContextMenu load(Map<String, Object> root, Player player, Path file) {
        ContextMenu cm = new ContextMenu(0, 0, player);
        MenuLocation location = new MenuLocation(file, List.of());
        parseItems(cm, (List<Map<String, Object>>) root.get("menu"), location);
        return cm;
    }

    private static void parseItems(ContextMenu menu, List<Map<String, Object>> items, MenuLocation container) {
        for (int i = 0; i < items.size(); i++) {
            parseEntry(menu, items.get(i), container, i);
        }
    }

    /**
     * Adds one YAML entry to the menu: an info row, a submenu (parsed recursively), or a command
     * row. A command given as a list becomes a script row. Entries matching none are ignored.
     */
    private static void parseEntry(ContextMenu menu, Map<String, Object> entry, MenuLocation container, int index) {
        if (entry.containsKey("info")) {
            String text = (String) entry.get("info");
            menu.addInfoItem(Component.literal(text));
            menu.markLastDeletable(new MenuLocation.DeleteRef(container, index, text));
            return;
        }

        String labelStr = (String) entry.get("label");
        Component label = Component.literal(labelStr);

        if (entry.containsKey("submenu")) {
            Object sub = entry.get("submenu");
            MenuLocation childLocation = container.child(index);
            ContextMenu submenu = new ContextMenu(0, 0, menu.getPlayer());
            parseItems(submenu, (List<Map<String, Object>>) sub, childLocation);
            submenu.addAddItem(childLocation);
            menu.addSubmenuItem(label, submenu);
            menu.markLastDeletable(new MenuLocation.DeleteRef(container, index, labelStr));
        } else if (entry.containsKey("command")) {
            Object command = entry.get("command");
            if (command instanceof List<?> list) {
                List<String> lines = new ArrayList<>();
                for (Object o : list)
                    lines.add(String.valueOf(o));
                menu.addScriptItem(label, lines);
            } else {
                menu.addCommandItem(label, String.valueOf(command));
            }
            menu.markLastDeletable(new MenuLocation.DeleteRef(container, index, labelStr));
        }
    }
}
