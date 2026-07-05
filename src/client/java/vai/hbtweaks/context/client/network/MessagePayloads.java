package vai.hbtweaks.context.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import vai.hbtweaks.context.client.keyboard.WritersBank;

import java.util.UUID;

public final class MessagePayloads {

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(HandshakePayload.ID, HandshakePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StartTypingPayload.ID, StartTypingPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StopTypingPayload.ID, StopTypingPayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(PlayerStartTypingPayload.ID, PlayerStartTypingPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlayerStopTypingPayload.ID, PlayerStopTypingPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(PlayerStartTypingPayload.ID, (payload, _) ->
                WritersBank.startWriting(payload.player()));
        ClientPlayNetworking.registerGlobalReceiver(PlayerStopTypingPayload.ID, (payload, _) ->
                WritersBank.stopWriting(payload.player()));

        ClientPlayConnectionEvents.JOIN.register((_, _, _) -> {
            if (ClientPlayNetworking.canSend(HandshakePayload.ID)) ClientPlayNetworking.send(new HandshakePayload());
        });
    }

    // === Handshake (C->S)
    public record HandshakePayload() implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<HandshakePayload> ID =
                new CustomPacketPayload.Type<>(Identifier.parse("herobrine:hbtweaks/handshake"));

        public static final StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> CODEC =
                StreamCodec.unit(new HandshakePayload());

        @Override public @NonNull Type<HandshakePayload> type() { return ID; }
    }

    // === Start typing (C->S) [str text]
    public record StartTypingPayload(String text) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<StartTypingPayload> ID =
                new CustomPacketPayload.Type<>(Identifier.parse("herobrine:speech/start_typing"));

        public static final StreamCodec<RegistryFriendlyByteBuf, StartTypingPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, StartTypingPayload::text,
                        StartTypingPayload::new
                );

        @Override public @NonNull Type<StartTypingPayload> type() { return ID; }
    }

    // === Stop typing (C->S)
    public record StopTypingPayload() implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<StopTypingPayload> ID =
                new CustomPacketPayload.Type<>(Identifier.parse("herobrine:speech/stop_typing"));

        public static final StreamCodec<RegistryFriendlyByteBuf, StopTypingPayload> CODEC =
                StreamCodec.unit(new StopTypingPayload());

        @Override public @NonNull Type<StopTypingPayload> type() { return ID; }
    }

    // === Player start typing (S->C) : [uuid player]
    public record PlayerStartTypingPayload(UUID player) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<PlayerStartTypingPayload> ID =
                new CustomPacketPayload.Type<>(Identifier.parse("herobrine:speech/player_start_typing"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerStartTypingPayload> CODEC =
                StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC, PlayerStartTypingPayload::player,
                        PlayerStartTypingPayload::new
                );

        @Override public @NonNull Type<PlayerStartTypingPayload> type() { return ID; }
    }

    // === Player stop typing (S->C) : [uuid player]
    public record PlayerStopTypingPayload(UUID player) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<PlayerStopTypingPayload> ID =
                new CustomPacketPayload.Type<>(Identifier.parse("herobrine:speech/player_stop_typing"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerStopTypingPayload> CODEC =
                StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC, PlayerStopTypingPayload::player,
                        PlayerStopTypingPayload::new
                );

        @Override public @NonNull Type<PlayerStopTypingPayload> type() { return ID; }
    }

}
