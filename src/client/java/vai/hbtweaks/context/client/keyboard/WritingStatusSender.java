package vai.hbtweaks.context.client.keyboard;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.ChatScreen;
import fr.herobrine.network.speech.ServerboundStartTypingPacket;
import fr.herobrine.network.speech.ServerboundStopTypingPacket;
import vai.hbtweaks.context.client.config.HBConfig;

/**
 * Sends the local "is writing" status to the server. A start packet is sent when the player
 * starts writing and then repeated at most every 5 seconds as a keep-alive, while a stop
 * packet is sent as soon as the text is no longer eligible, the edits pause, or the chat
 * closes. Start packets are only sent when HBConfig.shareTyping is enabled.
 *
 * @see WritingObserver
 * @see WritersBank
 */
public class WritingStatusSender {

    /** Minimum delay between two start packets, in milliseconds. Must stay below WritersBank's timeout. */
    private static final long TYPING_INTERVAL = 5000;

    /** Whether the server was last told that the local player is writing. */
    private static boolean writing = false;
    private static long lastTypingSent = 0;

    /** Registers the tick handler and the chat close hook. */
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(_ -> tick());
        ScreenEvents.AFTER_INIT.register((_, screen, _, _) -> {
            if (screen instanceof ChatScreen)
                ScreenEvents.remove(screen).register(s -> stopWriting());
        });
    }

    /** Starts or stops writing immediately, depending on whether the new text is eligible. */
    static void onTextChanged() {
        if (WritingObserver.isEligible()) {
            if (!writing) startWriting();
        } else {
            stopWriting();
        }
    }

    /** Every 5 seconds while writing, either renews the start packet or sends a stop one. */
    private static void tick() {
        if (!writing) return;
        if (System.currentTimeMillis() - lastTypingSent < TYPING_INTERVAL) return;
        if (WritingObserver.isWriting())
            startWriting();
        else
            stopWriting();
    }

    private static void startWriting() {
        writing = true;
        lastTypingSent = System.currentTimeMillis();
        sendTyping();
    }

    public static void stopWriting() {
        if (!writing) return;
        writing = false;
        sendStopTyping();
    }

    /** Sends a start packet carrying the first 5 characters of the text. */
    private static void sendTyping() {
        if (!HBConfig.get().shareTyping) return;
        ClientPlayNetworking.send(new ServerboundStartTypingPacket(WritingObserver.getText().substring(0, 5)));
    }

    private static void sendStopTyping() {
        ClientPlayNetworking.send(ServerboundStopTypingPacket.INSTANCE);
    }
}
