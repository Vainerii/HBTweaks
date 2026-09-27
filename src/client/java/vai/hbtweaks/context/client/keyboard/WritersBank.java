package vai.hbtweaks.context.client.keyboard;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Remembers which other players are currently writing, from the typing packets relayed by the
 * server. A player stops counting as writing on a stop packet, or when no start packet was
 * received for 7 seconds.
 *
 * @see WritingStatusSender
 */
public class WritersBank {

    /** Milliseconds after the last start packet before a player is considered done writing. */
    private static final long WRITING_TIMEOUT = 7000;

    /** Time of the last start packet, by player UUID. */
    private static final Map<UUID, Long> writing = new HashMap<>();
    /** Every player who has ever been seen writing, i.e. who has the mod. */
    private static final Set<UUID> seen = new HashSet<>();

    public static boolean isWriting(Player player) {
        return isWriting(player.getUUID());
    }

    /**
     * Whether a player is writing. Also removes the entry if it has timed out.
     *
     * @param uuid the player's UUID
     * @return true if a start packet was received less than 7 seconds ago
     */
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

    /**
     * @param uuid the player's UUID
     * @return true if the player has been seen writing at least once
     */
    public static boolean alreadyWrote(UUID uuid) {
        return seen.contains(uuid);
    }

    /** Milliseconds since the last typing packet */
    public static long sinceLastWrite(UUID uuid) {
        Long t = writing.get(uuid);
        return t == null ? -1 : System.currentTimeMillis() - t;
    }

    public static long timeout() {
        return WRITING_TIMEOUT;
    }

    public static int writingCount() {
        return writing.size();
    }

    public static int seenCount() {
        return seen.size();
    }

    public static void clear() {
        writing.clear();
        seen.clear();
    }
}
