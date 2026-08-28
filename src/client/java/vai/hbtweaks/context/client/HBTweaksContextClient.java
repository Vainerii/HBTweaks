package vai.hbtweaks.context.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import vai.hbtweaks.context.client.screen.CursorScreen;
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
import vai.hbtweaks.context.client.keyboard.WritingStatusSender;
import vai.hbtweaks.context.client.network.EffectPayloads;
import vai.hbtweaks.context.client.network.MessagePayloads;
import vai.hbtweaks.context.client.mouse.MouseTracker;
import vai.hbtweaks.context.client.mouse.MouseTrackerEntityClickUpCallback;
import vai.hbtweaks.context.client.script.ScriptRunner;

import java.io.File;

public class HBTweaksContextClient implements ClientModInitializer {

	private static final ContextMenuTrigger cmt = new ContextMenuTrigger();
	private static final SendMessageTrigger smt = new SendMessageTrigger();

	public static final boolean DEBUG_MODE = new File(".hbtweaks_debug").exists();

	private static final LookAtInfoBox lookAtInfoBox = new LookAtInfoBox();

	public static final KeyMapping CURSOR_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.hb-tweaks-context.cursor", GLFW.GLFW_KEY_R, KeyMapping.Category.MISC));

	@Override
	public void onInitializeClient() {
		//new HerobrinePlayerListener().register();
		HBConfig.HANDLER.load();
		DebugPerm.load();
		if (DebugPerm.ENABLED)
			DebugCommand.register();

		ClientTickEvents.END_CLIENT_TICK.register(lookAtInfoBox);
		lookAtInfoBox.register();

		MessagePayloads.init();
		EffectPayloads.init();
		WritingStatusSender.init();
		ScriptRunner.init();

		MouseTracker.register();
		MouseTrackerEntityClickUpCallback.EVENT.register(cmt);
		MouseTrackerEntityClickUpCallback.EVENT.register(smt);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (CURSOR_KEY.consumeClick()) {
				if (client.screen == null && client.player != null)
					client.setScreen(new CursorScreen());
			}
		});

		ScreenEvents.BEFORE_INIT.register((_, screen, _, _) -> {
			if (screen instanceof ChatScreen || screen instanceof CursorScreen) {
				ScreenMouseEvents.afterMouseClick(screen).register(cmt);
				ScreenKeyboardEvents.afterKeyPress(screen).register((_, keyEvent) -> {
					if (keyEvent.key() == GLFW.GLFW_KEY_DELETE)
						ContextMenuTrigger.handleDelete();
				});
				ScreenEvents.afterExtract(screen).register((_, graphics, _, _, _) -> {
					cmt.renderOnScreen(graphics, DeltaTracker.ZERO);
				});
				ScreenEvents.remove(screen).register(_ -> ContextMenuTrigger.dispose());
			}
		});

		HBTweaksContext.LOGGER.info("HBTweaks - Context : initialized");

	}
}