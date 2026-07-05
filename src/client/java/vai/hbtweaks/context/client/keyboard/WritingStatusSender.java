package vai.hbtweaks.context.client.keyboard;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.ChatScreen;
import vai.hbtweaks.context.client.network.MessagePayloads;

public class WritingStatusSender {

    private static final long TYPING_INTERVAL = 5000;

    private static boolean writing = false;
    private static long lastTypingSent = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(_ -> tick());
        ScreenEvents.AFTER_INIT.register((_, screen, _, _) -> {
            if (screen instanceof ChatScreen)
                ScreenEvents.remove(screen).register(s -> stopWriting());
        });
    }

    static void onTextChanged() {
        if (WritingObserver.isEligible()) {
            if (!writing) startWriting();
        } else {
            stopWriting();
        }
    }

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

    private static void stopWriting() {
        if (!writing) return;
        writing = false;
        sendStopTyping();
    }

    private static void sendTyping() {
        ClientPlayNetworking.send(new MessagePayloads.StartTypingPayload(WritingObserver.getText().substring(0, 5)));
        //HBTweaksContext.LOGGER.info("[STARTED TYPING]");
        //WritersBank.startWriting(Minecraft.getInstance().player.getUUID());
    }

    private static void sendStopTyping() {
        ClientPlayNetworking.send(new MessagePayloads.StopTypingPayload());
        //HBTweaksContext.LOGGER.info("[ENDED TYPING]");
        //WritersBank.stopWriting(Minecraft.getInstance().player.getUUID());
    }
}
