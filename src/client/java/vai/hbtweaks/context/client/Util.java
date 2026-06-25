package vai.hbtweaks.context.client;

import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class Util {

    public static String getMCName(Player player) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return null;
        PlayerInfo pi = me.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return null;
        return pi.getProfile().name();
    }

    public static Component getRpName(Player player) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return null;
        PlayerInfo pi = me.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return null;
        return pi.getTabListDisplayName();
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

    public static boolean isReal(Player player) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return false;
        PlayerInfo pi = me.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return false;
        return pi.getTabListDisplayName() != null && !pi.getTabListDisplayName().getString().isEmpty();
    }

    public static boolean hasDev() {
        return HBTweaksContextClient.DEBUG_MODE;
    }

    public static boolean hasPerm() {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return false;
        return me.isCreative() || me.isSpectator() || hasDev();
    }
}
