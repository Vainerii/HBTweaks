package vai.hbtweaks.context.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Developer override for the permission check. When the <code>.hbtweaks_debug</code> file
 * exists, its content ("true" or "false") replaces the real game master detection, so both
 * the player and the game master views can be tested without changing game mode.
 *
 * @see Util#hasPerm()
 */
public final class DebugPerm {

    private static final Path FILE = Path.of(".hbtweaks_debug");

    /** Whether the override is active, decided once at startup by the presence of the file. */
    public static final boolean ENABLED = Files.exists(FILE);

    private static boolean perm = false;

    private DebugPerm() {}

    /**
     * Reads the override value from the file. An empty file is initialised to "false".
     * Does nothing when the override is disabled.
     */
    public static void load() {
        if (!ENABLED) return;
        try {
            String s = Files.readString(FILE).trim();
            if (s.isEmpty()) {
                perm = false;
                save();
            } else {
                perm = Boolean.parseBoolean(s);
            }
        } catch (IOException e) {
            perm = false;
        }
    }

    public static boolean get() {
        return perm;
    }

    /**
     * Changes the override value and persists it to the file.
     *
     * @param value the new permission value
     */
    public static void set(boolean value) {
        perm = value;
        save();
    }

    private static void save() {
        try {
            Files.writeString(FILE, Boolean.toString(perm));
        } catch (IOException ignored) {
        }
    }
}
