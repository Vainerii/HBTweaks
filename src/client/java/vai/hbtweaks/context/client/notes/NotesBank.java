package vai.hbtweaks.context.client.notes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import vai.hbtweaks.context.HBTweaksContext;
import vai.hbtweaks.context.client.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Notes taken on other players, RP name as key so a player acting as an NPC
 * have the notes of that NPC. Everything in a single JSON file
 */
public final class NotesBank {

    private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("player_notes.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, List<String>> NOTES = new LinkedHashMap<>();
    private static boolean loaded = false;

    private NotesBank() {}

    public static String keyOf(Player player) {
        var rp = Util.getRpName(player);
        if (rp != null && !clean(rp.getString()).isBlank())
            return clean(rp.getString());
        String mc = Util.getMCName(player);
        return mc == null ? player.getStringUUID() : clean(mc);
    }

    //Remove formating
    private static String clean(String name) {
        String stripped = ChatFormatting.stripFormatting(name);
        return stripped == null ? "" : stripped.trim();
    }

    public static List<String> keys() {
        load();
        List<String> keys = new ArrayList<>(NOTES.keySet());
        keys.sort(String.CASE_INSENSITIVE_ORDER);
        return keys;
    }

    public static boolean has(String key) {
        load();
        return NOTES.containsKey(key);
    }

    public static void delete(String key) {
        load();
        if (NOTES.remove(key) != null)
            save();
    }

    public static void rename(String from, String to) {
        load();
        List<String> pages = NOTES.get(from);
        if (pages == null || from.equals(to)) return;
        displace(to);
        NOTES.remove(from);
        NOTES.put(to, pages);
        save();
    }

    public static void duplicate(String key, String to) {
        load();
        List<String> pages = NOTES.get(key);
        if (pages == null) return;
        displace(to);
        NOTES.put(to, new ArrayList<>(pages));
        save();
    }

    private static void displace(String key) {
        List<String> existing = NOTES.remove(key);
        if (existing == null) return;
        String target = key + "_old";
        for (int n = 2; NOTES.containsKey(target); n++)
            target = key + "_old" + n;
        NOTES.put(target, existing);
    }

    public static List<String> get(Player player) {
        return get(keyOf(player));
    }

    public static List<String> get(String key) {
        load();
        return NOTES.getOrDefault(key, List.of());
    }

    public static void set(String key, List<String> pages) {
        load();
        if (pages.isEmpty())
            NOTES.remove(key);
        else
            NOTES.put(key, new ArrayList<>(pages));
        save();
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(FILE), JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                JsonObject entry = e.getValue().getAsJsonObject();
                if (!entry.has("pages")) continue;
                List<String> pages = new ArrayList<>();
                entry.getAsJsonArray("pages").forEach(p -> pages.add(p.getAsString()));
                NOTES.put(e.getKey(), pages);
            }
        } catch (Exception e) {
            HBTweaksContext.LOGGER.error("Failed to read {}", FILE, e);
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        for (Map.Entry<String, List<String>> e : NOTES.entrySet()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("updated", now);
            JsonArray pages = new JsonArray();
            e.getValue().forEach(pages::add);
            entry.add("pages", pages);
            root.add(e.getKey(), entry);
        }
        try {
            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root));
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            HBTweaksContext.LOGGER.error("Failed to write {}", FILE, e);
        }
    }
}
