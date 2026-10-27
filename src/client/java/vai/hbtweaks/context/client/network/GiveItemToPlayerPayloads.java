package vai.hbtweaks.context.client.network;

import fr.herobrine.network.inventory.ServerboundGiveItemToPlayerPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class GiveItemToPlayerPayloads {

    private GiveItemToPlayerPayloads() {}

    public static void init() {
        Packets.registerC2S(ServerboundGiveItemToPlayerPacket.PACKET_INFO);
    }

    public static boolean canGive() {
        return ClientPlayNetworking.canSend(Packets.type(ServerboundGiveItemToPlayerPacket.PACKET_INFO));
    }

    /** Get max distance. */
    public static boolean inRange(Player target) {
        LocalPlayer me = Minecraft.getInstance().player;
        return me != null && me.distanceTo(target) <= ServerboundGiveItemToPlayerPacket.MAX_DISTANCE;
    }

    public static void giveToPlayer(UUID player) {
        if (!canGive()) return;
        // We double-check the range.
        Minecraft mc = Minecraft.getInstance();
        Player target = mc.level == null ? null : mc.level.getPlayerByUUID(player);
        if (target == null || !inRange(target)) {
            mc.gui.getChat().addClientSystemMessage(Component.literal(
                    "Trop loin pour donner l'objet.")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        ClientPlayNetworking.send(new ServerboundGiveItemToPlayerPacket(player));
    }
}
