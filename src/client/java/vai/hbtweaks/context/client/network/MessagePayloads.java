package vai.hbtweaks.context.client.network;

import fr.herobrine.network.mods.ServerboundHerobrineTweaksHandshakePacket;
import fr.herobrine.network.speech.ClientboundStartTypingPacket;
import fr.herobrine.network.speech.ClientboundStopTypingPayload;
import fr.herobrine.network.speech.ServerboundStartTypingPacket;
import fr.herobrine.network.speech.ServerboundStopTypingPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import vai.hbtweaks.context.client.keyboard.WritersBank;

/**
 * Registers the handshake and typing packets. The handshake tells the server this client has
 * the mod; it is sent on join only if the server accepts that channel.
 */
public final class MessagePayloads {

    private MessagePayloads() {}

    /** Registers the packets, the typing receivers feeding WritersBank, and the join handshake. */
    public static void init() {
        Packets.registerC2S(ServerboundHerobrineTweaksHandshakePacket.PACKET_INFO);
        Packets.registerC2S(ServerboundStartTypingPacket.PACKET_INFO);
        Packets.registerC2S(ServerboundStopTypingPacket.PACKET_INFO);

        Packets.registerS2C(ClientboundStartTypingPacket.PACKET_INFO);
        ClientPlayNetworking.registerGlobalReceiver(Packets.type(ClientboundStartTypingPacket.PACKET_INFO),
                (packet, _) -> WritersBank.startWriting(packet.getPlayer()));

        Packets.registerS2C(ClientboundStopTypingPayload.PACKET_INFO);
        ClientPlayNetworking.registerGlobalReceiver(Packets.type(ClientboundStopTypingPayload.PACKET_INFO),
                (packet, _) -> WritersBank.stopWriting(packet.getPlayer()));

        ClientPlayConnectionEvents.JOIN.register((_, _, _) -> {
            if (ClientPlayNetworking.canSend(Packets.type(ServerboundHerobrineTweaksHandshakePacket.PACKET_INFO)))
                ClientPlayNetworking.send(ServerboundHerobrineTweaksHandshakePacket.INSTANCE);
        });
    }
}
