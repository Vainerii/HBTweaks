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

public class Util {

    public static int rayLength() {
        // If entities are further than the player view settings, no need to check
        return Math.min(64, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16);
    }

    public static MutableComponent distanceIndicator(double dist) {
        if (dist > 100f) return Component.literal("[:]").withStyle(ChatFormatting.WHITE);
        if (dist > 50f)  return Component.literal("[!]").withStyle(ChatFormatting.RED);
        if (dist > 20f)  return Component.literal("[+]").withStyle(ChatFormatting.YELLOW);
        if (dist > 10f)  return Component.literal("[ ]").withStyle(ChatFormatting.GREEN);
        if (dist > 3f)   return Component.literal("[-]").withStyle(ChatFormatting.DARK_GREEN);
        return Component.literal("[#]").withStyle(ChatFormatting.DARK_AQUA);
    }

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

    public static String getMCName(Player player) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return null;
        PlayerInfo pi = me.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return null;
        return pi.getProfile().name();
    }

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

    private record Head(PlayerInfo info, Component component) { }

    private static final Map<UUID, Head> HEADS = new HashMap<>();

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
    public static String getVisibleMCName(Player player) {
        String fake = getFakeName(player);
        if (!hasPerm() && fake != null)
            return fake;
        return getMCName(player);
    }

    // It's working. but NPC are not simply easy to differentiate from players, so, lets stay vigilant.
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

    public static boolean hasPerm() {
        if (DebugPerm.ENABLED)
            return DebugPerm.get();
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return false;
        // Replace when api cat tell if user is GM
        return me.isCreative() || me.isSpectator();
    }
}
