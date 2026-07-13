package vai.hbtweaks.context.client.keyboard;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WritersBank {

    private static final long WRITING_TIMEOUT = 7000;

    private static final Map<UUID, Long> writing = new HashMap<>();
    private static final Set<UUID> seen = new HashSet<>();

    public static boolean isWriting(Player player) {
        return isWriting(player.getUUID());
    }

    public static boolean isWriting(UUID uuid) {
        Long t = writing.get(uuid);
        if (t == null) return false;
        if (System.currentTimeMillis() - t > WRITING_TIMEOUT) {
            writing.remove(uuid);
            return false;
        }
        return true;
    }

    public static void startWriting(Player player) {
        startWriting(player.getUUID());
    }

    public static void startWriting(UUID uuid) {
        writing.put(uuid, System.currentTimeMillis());
        seen.add(uuid);
    }

    public static void stopWriting(Player player) {
        stopWriting(player.getUUID());
    }

    public static void stopWriting(UUID uuid) {
        writing.remove(uuid);
    }

    public static boolean alreadyWrote(Player player) {
        return alreadyWrote(player.getUUID());
    }

    public static boolean alreadyWrote(UUID uuid) {
        return seen.contains(uuid);
    }
}
