package vai.hbtweaks.context.client.effects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Last known potion effects of other players, by UUID, as received from the server through
 * EffectPayloads.
 */
public class EffectsBank {

    private static final Map<UUID, List<MobEffectInstance>> INSTANCE = new HashMap<>();

    public static void put(UUID uuid, List<MobEffectInstance> effects) {
        INSTANCE.put(uuid, effects);
    }

    public static List<MobEffectInstance> get(Player player) {
        return get(player.getUUID());
    }

    /**
     * @param uuid the player's UUID
     * @return the player's effects, or an empty list if none were received
     */
    public static List<MobEffectInstance> get(UUID uuid) {
        return INSTANCE.getOrDefault(uuid, List.of());
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
