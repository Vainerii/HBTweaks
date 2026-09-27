package vai.hbtweaks.context.client.network;

import fr.herobrine.network.AbstractPacket;
import fr.herobrine.network.PacketInfo;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Helpers bridging the Herobrine networking library's PacketInfo definitions to Fabric's
 * payload registry.
 */
public final class Packets {

    private Packets() {}

    /**
     * @param info the packet definition
     * @return the Fabric payload type matching the packet's identifier
     */
    public static <T extends AbstractPacket> CustomPacketPayload.Type<T> type(PacketInfo<T> info) {
        return new CustomPacketPayload.Type<>(info.identifier());
    }

    /**
     * Registers a client to server packet.
     *
     * @param info the packet definition
     */
    public static <T extends AbstractPacket> void registerC2S(PacketInfo<T> info) {
        PayloadTypeRegistry.serverboundPlay().register(type(info), info.streamCodec());
    }

    /**
     * Registers a server to client packet. A receiver must still be registered separately.
     *
     * @param info the packet definition
     */
    public static <T extends AbstractPacket> void registerS2C(PacketInfo<T> info) {
        PayloadTypeRegistry.clientboundPlay().register(type(info), info.streamCodec());
    }
}
