package vai.hbtweaks.context.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class PlayerOutline {

    private static boolean enabled = false;

    private static UUID highlighted = null;
    private static long highlightUntil = 0;

    private PlayerOutline() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void highlight(UUID player, long untilMillis) {
        highlighted = player;
        highlightUntil = untilMillis;
    }

    public static boolean shouldOutline(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        if (highlighted != null && isHighlighted(player)) return true;
        if (!enabled) return false;
        if (player == Minecraft.getInstance().player) return false;
        return Util.isReal(player);
    }

    private static boolean isHighlighted(Player player) {
        if (System.currentTimeMillis() > highlightUntil) {
            highlighted = null;
            return false;
        }
        if (highlighted.equals(player.getUUID())) return true;
        // A lying player is invisible, outline their laying body instead
        if (!LyingBody.isLying(player)) return false;
        Player owner = LyingBody.owner(player);
        return owner != null && highlighted.equals(owner.getUUID());
    }
}
