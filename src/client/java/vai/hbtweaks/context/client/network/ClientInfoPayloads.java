package vai.hbtweaks.context.client.network;

import fr.herobrine.network.client.packets.ClientboundClientInformationPacket;
import fr.herobrine.network.client.packets.ServerboundRequestClientInformationPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import vai.hbtweaks.context.client.clientinfo.ClientInfoBank;

import java.util.UUID;

public final class ClientInfoPayloads {

    private ClientInfoPayloads() {}

    public static void init() {
        Packets.registerC2S(ServerboundRequestClientInformationPacket.PACKET_INFO);
        Packets.registerS2C(ClientboundClientInformationPacket.PACKET_INFO);

        ClientPlayNetworking.registerGlobalReceiver(Packets.type(ClientboundClientInformationPacket.PACKET_INFO),
                (packet, _) -> ClientInfoBank.put(packet.getUuid(), packet.getClientInformation()));
    }

    public static boolean canRequest() {
        return ClientPlayNetworking.canSend(Packets.type(ServerboundRequestClientInformationPacket.PACKET_INFO));
    }

    public static void request(UUID player) {
        if (canRequest())
            ClientPlayNetworking.send(new ServerboundRequestClientInformationPacket(player));
    }
}
