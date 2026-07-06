package vai.hbtweaks.context.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import org.jspecify.annotations.NonNull;
import vai.hbtweaks.context.client.effects.EffectsBank;

import java.util.List;
import java.util.UUID;

public final class EffectPayloads {

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(RequestEffectsPayload.ID, RequestEffectsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlayerEffectsPayload.ID, PlayerEffectsPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(PlayerEffectsPayload.ID, (payload, _) ->
                EffectsBank.put(payload.player(), payload.effects()));
    }

    public static void requestEffects(UUID player) {
        if (ClientPlayNetworking.canSend(RequestEffectsPayload.ID))
            ClientPlayNetworking.send(new RequestEffectsPayload(player));
    }

    // === Request effects (C->S) : [uuid player]
    public record RequestEffectsPayload(UUID player) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<RequestEffectsPayload> ID =
                new CustomPacketPayload.Type<>(Identifier.parse("herobrine:hbtweaks/request_effects"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RequestEffectsPayload> CODEC =
                StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC, RequestEffectsPayload::player,
                        RequestEffectsPayload::new
                );

        @Override public @NonNull Type<RequestEffectsPayload> type() { return ID; }
    }

    // === Player effects (S->C) : [uuid player][list<MobEffectInstance> effects]
    public record PlayerEffectsPayload(UUID player, List<MobEffectInstance> effects) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<PlayerEffectsPayload> ID =
                new CustomPacketPayload.Type<>(Identifier.parse("herobrine:hbtweaks/player_effects"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerEffectsPayload> CODEC =
                StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC, PlayerEffectsPayload::player,
                        MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()), PlayerEffectsPayload::effects,
                        PlayerEffectsPayload::new
                );

        @Override public @NonNull Type<PlayerEffectsPayload> type() { return ID; }
    }
}
