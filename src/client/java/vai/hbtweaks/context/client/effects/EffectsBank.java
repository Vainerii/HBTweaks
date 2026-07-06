package vai.hbtweaks.context.client.effects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EffectsBank {

    private static final Map<UUID, List<MobEffectInstance>> INSTANCE = new HashMap<>();

    public static void put(UUID uuid, List<MobEffectInstance> effects) {
        INSTANCE.put(uuid, effects);
    }

    public static List<MobEffectInstance> get(Player player) {
        return get(player.getUUID());
    }

    public static List<MobEffectInstance> get(UUID uuid) {
        return INSTANCE.getOrDefault(uuid, List.of());
    }

}
