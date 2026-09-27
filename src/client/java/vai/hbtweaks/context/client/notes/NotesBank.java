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

    /** Pages of every note, by key. Filled lazily on first access. */
    private static final Map<String, List<String>> NOTES = new LinkedHashMap<>();
    private static boolean loaded = false;

    private NotesBank() {}

    /**
     * Key under which a player's notes are stored: the roleplay name without formatting, falling
     * back to the Minecraft name, then to the UUID.
     *
     * @param player the player
     * @return the notes key
     */
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

    /**
     * @return every note key, sorted case-insensitively
     */
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

    /**
     * Moves a note to a new key. A note already at the target key is kept under a suffixed key
     * rather than overwritten.
     *
     * @param from the current key
     * @param to the new key
     */
    public static void rename(String from, String to) {
        load();
        List<String> pages = NOTES.get(from);
        if (pages == null || from.equals(to)) return;
        displace(to);
        NOTES.remove(from);
        NOTES.put(to, pages);
        save();
    }

    /**
     * Copies a note to another key. A note already at the target key is kept under a suffixed
     * key rather than overwritten.
     *
     * @param key the source key
     * @param to the target key
     */
    public static void duplicate(String key, String to) {
        load();
        List<String> pages = NOTES.get(key);
        if (pages == null) return;
        displace(to);
        NOTES.put(to, new ArrayList<>(pages));
        save();
    }

    /**
     * Moves the note at a key out of the way, to "key_old", "key_old2", and so on.
     *
     * @param key the key to free
     */
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

    /**
     * @param key the notes key
     * @return the note's pages, or an empty list if there is none
     */
    public static List<String> get(String key) {
        load();
        return NOTES.getOrDefault(key, List.of());
    }

    /**
     * Replaces a note's pages and saves. An empty list deletes the note.
     *
     * @param key the notes key
     * @param pages the new pages
     */
    public static void set(String key, List<String> pages) {
        load();
        if (pages.isEmpty())
            NOTES.remove(key);
        else
            NOTES.put(key, new ArrayList<>(pages));
        save();
    }

    /** Reads the file once. Entries without "pages" are skipped; a broken file is logged and ignored. */
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

    /**
     * Writes every note, each stamped with the current time. The file is written to a temporary
     * sibling then moved over the original, so a crash never leaves a truncated file.
     */
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
