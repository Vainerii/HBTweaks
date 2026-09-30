package vai.hbtweaks.context.client.network;

import fr.herobrine.network.inventory.ServerboundGiveItemToPlayerPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.UUID;

public final class GiveItemToPlayerPayloads {

    private GiveItemToPlayerPayloads() {}

    public static void init() {
        Packets.registerC2S(ServerboundGiveItemToPlayerPacket.PACKET_INFO);
    }

    public static boolean canGive() {
        return ClientPlayNetworking.canSend(Packets.type(ServerboundGiveItemToPlayerPacket.PACKET_INFO));
    }

    public static void giveToPlayer(UUID player) {
        if (canGive())
            ClientPlayNetworking.send(new ServerboundGiveItemToPlayerPacket(player));
    }
}
