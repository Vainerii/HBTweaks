package vai.hbtweaks.context.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PlayerOutline {

    private static boolean enabled = false;

    private PlayerOutline() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean shouldOutline(Entity entity) {
        if (!enabled) return false;
        if (!(entity instanceof Player player)) return false;
        if (player == Minecraft.getInstance().player) return false;
        return Util.isReal(player);
    }
}
