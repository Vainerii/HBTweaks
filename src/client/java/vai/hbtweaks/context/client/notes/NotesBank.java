package vai.hbtweaks.context.client.notes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
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
        if (rp != null && !rp.getString().isBlank())
            return rp.getString();
        String mc = Util.getMCName(player);
        return mc == null ? player.getStringUUID() : mc;
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
