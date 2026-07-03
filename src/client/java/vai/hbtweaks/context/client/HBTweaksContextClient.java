package vai.hbtweaks.context.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.screens.ChatScreen;
import vai.hbtweaks.context.HBTweaksContext;
import vai.hbtweaks.context.client.config.HBConfig;
import vai.hbtweaks.context.client.listeners.ContextMenuTrigger;
import vai.hbtweaks.context.client.listeners.LookAtInfoBox;
import vai.hbtweaks.context.client.listeners.SendMessageTrigger;
import vai.hbtweaks.context.client.mouse.MouseTracker;
import vai.hbtweaks.context.client.mouse.MouseTrackerEntityClickUpCallback;

import java.io.File;

public class HBTweaksContextClient implements ClientModInitializer {

	private static final ContextMenuTrigger cmt = new ContextMenuTrigger();
	private static final SendMessageTrigger smt = new SendMessageTrigger();

	public static final boolean DEBUG_MODE = new File(".hbtweaks_debug").exists();

	private static final LookAtInfoBox lookAtInfoBox = new LookAtInfoBox();

	@Override
	public void onInitializeClient() {
		//new HerobrinePlayerListener().register();
		HBConfig.HANDLER.load();

		ClientTickEvents.END_CLIENT_TICK.register(lookAtInfoBox);
		lookAtInfoBox.register();

		MouseTracker.register();
		MouseTrackerEntityClickUpCallback.EVENT.register(cmt);
		MouseTrackerEntityClickUpCallback.EVENT.register(smt);
		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof ChatScreen) {
				ScreenMouseEvents.afterMouseClick(screen).register(cmt);
				ScreenKeyboardEvents.afterKeyPress(screen).register((s, keyEvent) -> {
					if (keyEvent.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE)
						ContextMenuTrigger.handleDelete();
				});
				ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
					cmt.renderOnScreen(graphics, DeltaTracker.ZERO);
				});
				ScreenEvents.remove(screen).register(s -> ContextMenuTrigger.dispose());
			}
		});

		HBTweaksContext.LOGGER.info("HBTweaks - Context : initialized");

	}
}