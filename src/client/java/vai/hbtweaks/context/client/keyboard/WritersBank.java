package vai.hbtweaks.context.client.keyboard;

import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.UUID;

public class WritersBank extends HashSet<UUID> {

    private final HashSet<UUID> seen = new HashSet<>();

    private static final WritersBank INSTANCE = new WritersBank();

    public static boolean isWriting(Player player) {
        return INSTANCE.contains(player.getUUID());
    }

    public static void startWriting(Player player) {
        startWriting(player.getUUID());
    }

    public static void startWriting(UUID uuid) {
        INSTANCE.add(uuid);
        INSTANCE.seen.add(uuid);
    }

    public static void stopWriting(Player player) {
        stopWriting(player.getUUID());
    }

    public static void stopWriting(UUID uuid) {
        INSTANCE.remove(uuid);
    }

    public static boolean alreadyWrote(Player player) {
        return alreadyWrote(player.getUUID());
    }

    public static boolean alreadyWrote(UUID uuid) {
        return INSTANCE.seen.contains(uuid);
    }
}
