package vai.hbtweaks.context.client.clientinfo;

import fr.herobrine.network.client.ClientInformation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientInfoBank {

    private static final Map<UUID, ClientInformation> INSTANCE = new ConcurrentHashMap<>();

    private ClientInfoBank() {}

    public static void put(UUID uuid, ClientInformation info) {
        INSTANCE.put(uuid, info);
    }

    public static ClientInformation get(UUID uuid) {
        return INSTANCE.get(uuid);
    }

    public static boolean has(UUID uuid) {
        return INSTANCE.containsKey(uuid);
    }

    public static int size() {
        return INSTANCE.size();
    }

    public static void clear() {
        INSTANCE.clear();
    }
}
