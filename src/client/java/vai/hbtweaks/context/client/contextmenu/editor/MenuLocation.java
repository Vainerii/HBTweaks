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

public final class MenuLocation {
    private final Path file;
    private final List<Integer> path;

    public MenuLocation(Path file, List<Integer> path) {
        this.file = file;
        this.path = path;
    }

    public MenuLocation child(int index) {
        List<Integer> p = new ArrayList<>(this.path);
        p.add(index);
        return new MenuLocation(this.file, p);
    }

    public Path file() {
        return this.file;
    }

    public boolean hasParent() {
        return !this.path.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolve(Map<String, Object> root) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) root.get("menu");
        for (int idx : this.path) {
            Map<String, Object> entry = list.get(idx);
            list = (List<Map<String, Object>>) entry.get("submenu");
        }
        return list;
    }

    private List<Map<String, Object>> resolveParent(Map<String, Object> root) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) root.get("menu");
        for (int i = 0; i < this.path.size() - 1; i++)
            list = (List<Map<String, Object>>) list.get(this.path.get(i)).get("submenu");
        return list;
    }

    public Map<String, Object> entryAt(int index) {
        Map<String, Object> root = CustomContextMenuLoader.readYaml(this.file);
        if (root == null) return null;
        List<Map<String, Object>> list = resolve(root);
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    public void addSubmenu(String name) {
        edit(root -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", name);
            entry.put("submenu", new ArrayList<>());
            resolve(root).add(entry);
        });
    }

    public void addCommand(String name, String command) {
        edit(root -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", name);
            entry.put("command", command);
            resolve(root).add(entry);
        });
    }

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

    public void moveIntoSubmenuAbove(int index) {
        edit(root -> {
            List<Map<String, Object>> list = resolve(root);
            if (index <= 0 || index >= list.size()) return;
            Object sub = list.get(index - 1).get("submenu");
            if (!(sub instanceof List)) return;
            ((List<Map<String, Object>>) sub).add(list.remove(index));
        });
    }

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

    public record DeleteRef(MenuLocation container, int index, String label) {
        public void delete() {
            this.container.deleteAt(this.index);
        }
    }
}
