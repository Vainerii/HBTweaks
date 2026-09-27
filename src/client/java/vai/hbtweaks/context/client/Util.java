package vai.hbtweaks.context.client;

import com.mojang.authlib.properties.Property;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import vai.hbtweaks.context.client.keyboard.WritersBank;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared helpers about players: names (Minecraft, roleplay, fake), heads, permission checks,
 * NPC detection, and the small indicator components shown next to names.
 */
public class Util {

    /**
     * Maximum raycast distance used by the entity trackers: the chunk render distance in
     * blocks, capped at 64.
     *
     * @return the ray length in blocks
     */
    public static int rayLength() {
        // If entities are further than the player view settings, no need to check
        return Math.min(64, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16);
    }

    /**
     * Short coloured symbol summarising a distance, from "[#]" (very close) to "[:]"
     * (over 100 blocks).
     *
     * @param dist the distance in blocks
     * @return the indicator component
     */
    public static MutableComponent distanceIndicator(double dist) {
        if (dist > 100f) return Component.literal("[:]").withStyle(ChatFormatting.WHITE);
        if (dist > 50f)  return Component.literal("[!]").withStyle(ChatFormatting.RED);
        if (dist > 20f)  return Component.literal("[+]").withStyle(ChatFormatting.YELLOW);
        if (dist > 10f)  return Component.literal("[ ]").withStyle(ChatFormatting.GREEN);
        if (dist > 3f)   return Component.literal("[-]").withStyle(ChatFormatting.DARK_GREEN);
        return Component.literal("[#]").withStyle(ChatFormatting.DARK_AQUA);
    }

    /**
     * Three dots showing whether a player is typing. While typing, each dot brightens in turn
     * over a 32 tick cycle; otherwise they stay dark grey. A trailing "?" means the player has
     * never been seen typing, so they may not have the mod at all.
     *
     * @param target the player to check
     * @return the indicator component, meant to be rebuilt every tick for the animation
     */
    public static MutableComponent writingIndicator(Player target) {
        MutableComponent out = Component.empty();
        boolean writing = WritersBank.isWriting(target);
        int subtick = Minecraft.getInstance().gui.getGuiTicks() % 32;
        for (int i = 1; i <= 3; i++) {
            int v = 0x30;
            if (writing) {
                int level = Math.max(0, 8 - Math.abs(subtick - i * 8)); // evolution over 8 ticks, peak at i*8
                v = 0x30 + level * (0xFF - 0x30) / 8;
            }
            out.append(Component.literal("•").withColor(0xFF000000 | (v << 16) | (v << 8) | v));
        }
        if (!writing && !WritersBank.alreadyWrote(target))
            out.append(Component.literal("?").withColor(0xFF303030));
        return out;
    }

    /**
     * Real Minecraft account name of a player, read from the tab list profile. Must not be
     * shown as is to players without permission.
     *
     * @param player the player
     * @return the account name, or null if the player is not in the tab list
     * @see #getVisibleMCName(Player)
     */
    public static String getMCName(Player player) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return null;
        PlayerInfo pi = me.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return null;
        return pi.getProfile().name();
    }

    /**
     * Roleplay name of a player, which the server puts in the tab list display name.
     *
     * @param player the player
     * @return the roleplay name, null if the player is not in the tab list or has none
     */
    public static Component getRpName(Player player) {
        try {
            LocalPlayer me = Minecraft.getInstance().player;
            if (me == null) return null;
            PlayerInfo pi = me.connection.getPlayerInfo(player.getUUID());
            if (pi == null) return null;
            return pi.getTabListDisplayName();
        } catch (Exception ignored) {
            return Component.empty();
        }
    }

    /** Cached head component, tied to the PlayerInfo it was built from. */
    private record Head(PlayerInfo info, Component component) { }

    /**
     * Head components by player UUID. An entry is rebuilt when the player's PlayerInfo instance
     * changes (e.g. after a relog or skin change), and the whole cache is cleared on disconnect.
     */
    private static final Map<UUID, Head> HEADS = new HashMap<>();

    /**
     * Inline sprite of the player's head followed by a space, meant to prefix a name.
     *
     * @param player the player
     * @return the head component, or an empty component if unavailable
     */
    public static Component getHead(Player player) {
        try {
            LocalPlayer me = Minecraft.getInstance().player;
            if (me == null) return Component.empty();
            PlayerInfo pi = me.connection.getPlayerInfo(player.getUUID());
            if (pi == null) return Component.empty();
            Head cached = HEADS.get(player.getUUID());
            if (cached != null && cached.info() == pi)
                return cached.component();
            ResolvableProfile rp = ResolvableProfile.createResolved(pi.getProfile());
            Component head = Component.object(new PlayerSprite(rp, true)).append(Component.literal(" "));
            HEADS.put(player.getUUID(), new Head(pi, head));
            return head;
        } catch (Exception ignored) {
            return Component.empty();
        }
    }

    public static void clearCaches() {
        HEADS.clear();
    }

    /**
     * Fake Minecraft name of a player, carried by the "minecraft_name" property of its profile.
     * It hides the real account name from players without permission.
     *
     * @param player the player
     * @return the fake name, or null if the player has none
     */
    public static String getFakeName(Player player) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return null;
        PlayerInfo pi = me.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return null;
        for (Property property : pi.getProfile().properties().get("minecraft_name"))
            return property.value();
        return null;
    }

    public static boolean hasFakeName(Player player) {
        return getFakeName(player) != null;
    }

    // Minecraft name to display without perms
    /**
     * Minecraft name that the local player is allowed to see: the fake name if the target has
     * one and the local player has no permission, the real name otherwise. This is a privacy
     * feature, the real name must never leak to regular players.
     *
     * @param player the player
     * @return the name to display, or null if unavailable
     */
    public static String getVisibleMCName(Player player) {
        String fake = getFakeName(player);
        if (!hasPerm() && fake != null)
            return fake;
        return getMCName(player);
    }

    // It's working. but NPC are not simply easy to differentiate from players, so, lets stay vigilant.
    /**
     * Best-effort test telling a real player apart from the server's NPCs, which are fake
     * players. A real player has gravity and a non-empty tab list display name.
     *
     * @param player the player to test
     * @return true if the player looks like a real one
     */
    public static boolean isReal(Player player) {
        try {
            if (player.isNoGravity()) return false;
            LocalPlayer me = Minecraft.getInstance().player;
            if (me == null) return false;
            PlayerInfo pi = me.connection.getPlayerInfo(player.getUUID());
            if (pi == null) return false;
            return pi.getTabListDisplayName() != null && !pi.getTabListDisplayName().getString().isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean hasDev() {
        return HBTweaksContextClient.DEBUG_MODE;
    }

    /**
     * Whether the local player counts as a game master. Approximated by creative or spectator
     * mode, unless the DebugPerm override is active. Gates game master menu entries, real name
     * visibility and targeting through walls.
     *
     * @return true if the local player has game master permissions
     */
    public static boolean hasPerm() {
        if (DebugPerm.ENABLED)
            return DebugPerm.get();
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return false;
        // Replace when api cat tell if user is GM
        return me.isCreative() || me.isSpectator();
    }
}
