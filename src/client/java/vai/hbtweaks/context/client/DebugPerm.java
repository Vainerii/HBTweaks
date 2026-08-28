package vai.hbtweaks.context.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DebugPerm {

    private static final Path FILE = Path.of(".hbtweaks_debug");

    public static final boolean ENABLED = Files.exists(FILE);

    private static boolean perm = false;

    private DebugPerm() {}

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
