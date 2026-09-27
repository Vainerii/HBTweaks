package vai.hbtweaks.context.client.network;

import fr.herobrine.network.effects.ClientboundPlayerEffectsPacket;
import fr.herobrine.network.effects.ServerboundRequestEffectsPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import vai.hbtweaks.context.client.effects.EffectsBank;

import java.util.UUID;

/**
 * Registers the potion effects request and response packets. Responses are stored in
 * EffectsBank.
 */
public final class EffectPayloads {

    private EffectPayloads() {}

    /** Registers both packets and the response receiver. */
    public static void init() {
        Packets.registerC2S(ServerboundRequestEffectsPacket.PACKET_INFO);
        Packets.registerS2C(ClientboundPlayerEffectsPacket.PACKET_INFO);

        ClientPlayNetworking.registerGlobalReceiver(Packets.type(ClientboundPlayerEffectsPacket.PACKET_INFO),
                (packet, _) -> EffectsBank.put(packet.getPlayer(), packet.getEffects()));
    }

    /**
     * Asks the server for a player's active effects. Silently does nothing if the server does
     * not accept the request channel.
     *
     * @param player the UUID of the player whose effects are requested
     */
    public static void requestEffects(UUID player) {
        if (ClientPlayNetworking.canSend(Packets.type(ServerboundRequestEffectsPacket.PACKET_INFO)))
            ClientPlayNetworking.send(new ServerboundRequestEffectsPacket(player));
    }
}
