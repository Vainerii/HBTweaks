package vai.hbtweaks.context.client.contextmenu.editor;

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
import java.util.function.Consumer;

/**
 * Address of one menu list inside a custom menu YAML file: the file, plus the chain of submenu
 * indexes leading to it from the root "menu" list (an empty path is the root list). Entries in
 * that list are then addressed by index.
 * <p>
 * Every mutation re-reads the file, applies the change, writes it back and pushes the new
 * content to ContextMenuTrigger, so the cached menus never drift from the file.
 */
public final class MenuLocation {
    private final Path file;
    private final List<Integer> path;

    /**
     * @param file the YAML file
     * @param path submenu indexes from the root list, empty for the root list itself
     */
    public MenuLocation(Path file, List<Integer> path) {
        this.file = file;
        this.path = path;
    }

    /**
     * @param index index of a submenu entry in this list
     * @return the location of that submenu's list
     */
    public MenuLocation child(int index) {
        List<Integer> p = new ArrayList<>(this.path);
        p.add(index);
        return new MenuLocation(this.file, p);
    }

    public Path file() {
        return this.file;
    }

    /**
     * @return true if this list is a submenu, false for the root list
     */
    public boolean hasParent() {
        return !this.path.isEmpty();
    }

    /** Walks the path down from the root "menu" list to this location's list. */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolve(Map<String, Object> root) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) root.get("menu");
        for (int idx : this.path) {
            Map<String, Object> entry = list.get(idx);
            list = (List<Map<String, Object>>) entry.get("submenu");
        }
        return list;
    }

    /** Walks the path down to the list that contains this location's submenu entry. */
    private List<Map<String, Object>> resolveParent(Map<String, Object> root) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) root.get("menu");
        for (int i = 0; i < this.path.size() - 1; i++)
            list = (List<Map<String, Object>>) list.get(this.path.get(i)).get("submenu");
        return list;
    }

    /**
     * Reads one entry of this list straight from the file.
     *
     * @param index the entry index
     * @return the raw YAML entry, or null if the file or the entry does not exist
     */
    public Map<String, Object> entryAt(int index) {
        Map<String, Object> root = CustomContextMenuLoader.readYaml(this.file);
        if (root == null) return null;
        List<Map<String, Object>> list = resolve(root);
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    /**
     * Appends an empty submenu.
     *
     * @param name the submenu label
     */
    public void addSubmenu(String name) {
        edit(root -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", name);
            entry.put("submenu", new ArrayList<>());
            resolve(root).add(entry);
        });
    }

    /**
     * Appends a single-command entry.
     *
     * @param name the label
     * @param command the command, placeholders allowed
     */
    public void addCommand(String name, String command) {
        edit(root -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", name);
            entry.put("command", command);
            resolve(root).add(entry);
        });
    }

    /**
     * Appends a script entry, stored as a list under the "command" key.
     *
     * @param name the label
     * @param lines the script lines
     */
    public void addScript(String name, List<String> lines) {
        edit(root -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", name);
            entry.put("command", new ArrayList<>(lines));
            resolve(root).add(entry);
        });
    }

    public void deleteAt(int index) {
        edit(root -> {
            List<Map<String, Object>> list = resolve(root);
            if (index >= 0 && index < list.size())
                list.remove(index);
        });
    }

    /**
     * Swaps an entry with its neighbour. Does nothing if either index is out of bounds.
     *
     * @param index the entry index
     * @param delta -1 to move up, 1 to move down
     */
    public void move(int index, int delta) {
        edit(root -> {
            List<Map<String, Object>> list = resolve(root);
            int j = index + delta;
            if (index < 0 || index >= list.size() || j < 0 || j >= list.size()) return;
            Map<String, Object> tmp = list.get(index);
            list.set(index, list.get(j));
            list.set(j, tmp);
        });
    }

    /**
     * Moves an entry to the end of the submenu right above it, if that entry is a submenu.
     *
     * @param index the entry index
     */
    public void moveIntoSubmenuAbove(int index) {
        edit(root -> {
            List<Map<String, Object>> list = resolve(root);
            if (index <= 0 || index >= list.size()) return;
            Object sub = list.get(index - 1).get("submenu");
            if (!(sub instanceof List)) return;
            ((List<Map<String, Object>>) sub).add(list.remove(index));
        });
    }

    /**
     * Moves an entry out of this submenu into the parent list, right after the submenu itself.
     * Does nothing on the root list.
     *
     * @param index the entry index
     */
    public void moveToParent(int index) {
        if (!hasParent()) return;
        edit(root -> {
            List<Map<String, Object>> list = resolve(root);
            if (index < 0 || index >= list.size()) return;
            Map<String, Object> moved = list.remove(index);
            List<Map<String, Object>> parent = resolveParent(root);
            int k = this.path.get(this.path.size() - 1);
            parent.add(Math.min(k + 1, parent.size()), moved);
        });
    }

    public void rename(int index, String name) {
        edit(root -> {
            List<Map<String, Object>> list = resolve(root);
            if (index < 0 || index >= list.size()) return;
            list.get(index).put("label", name);
        });
    }

    /**
     * Replaces an entry with a single-command entry.
     *
     * @param index the entry index
     * @param name the new label
     * @param command the new command
     */
    public void replaceCommand(int index, String name, String command) {
        edit(root -> {
            List<Map<String, Object>> list = resolve(root);
            if (index < 0 || index >= list.size()) return;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", name);
            entry.put("command", command);
            list.set(index, entry);
        });
    }

    /**
     * Replaces an entry with a script entry.
     *
     * @param index the entry index
     * @param name the new label
     * @param lines the new script lines
     */
    public void replaceScript(int index, String name, List<String> lines) {
        edit(root -> {
            List<Map<String, Object>> list = resolve(root);
            if (index < 0 || index >= list.size()) return;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", name);
            entry.put("command", new ArrayList<>(lines));
            list.set(index, entry);
        });
    }

    /**
     * Read, mutate, write cycle shared by every mutation. A missing file or "menu" list is
     * created. On a write failure the cache is left untouched.
     *
     * @param mutator the change to apply to the parsed YAML root
     */
    private void edit(Consumer<Map<String, Object>> mutator) {
        Map<String, Object> root = CustomContextMenuLoader.readYaml(this.file);
        if (root == null)
            root = new LinkedHashMap<>();
        if (!(root.get("menu") instanceof List))
            root.put("menu", new ArrayList<>());

        mutator.accept(root);

        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        opts.setIndent(2);
        try {
            Files.writeString(this.file, new Yaml(opts).dump(root));
        } catch (IOException e) {
            HBTweaksContext.LOGGER.error("Failed to write custom menu {}", this.file, e);
            return;
        }

        ContextMenuTrigger.onFileEdited(this.file, root);
    }

    /**
     * One entry of a custom menu, as targeted by the edit mode controls (delete, edit).
     *
     * @param container the list holding the entry
     * @param index the entry index in that list
     * @param label the entry label, for display in confirmations
     */
    public record DeleteRef(MenuLocation container, int index, String label) {
        public void delete() {
            this.container.deleteAt(this.index);
        }
    }
}
