package vai.hbtweaks.context.client.chatpatches.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vai.hbtweaks.context.client.chatpatches.HoverStyleFinder;
import vai.hbtweaks.context.client.listeners.ContextMenuTrigger;

/**
 * Before ChatPatches context menu
 * on a right-click that lands on a hoverable chat component
 */
@Mixin(ChatScreen.class)
public abstract class ChatMessageMenuMixin {

    @Shadow
    private ChatComponent.DisplayMode displayMode;

    @Shadow
    protected EditBox input;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void hbtweaks$onRightClickMessage(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        int button = event.button();
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            if (ContextMenuTrigger.isMenuOver(event.x(), event.y()))
                cir.setReturnValue(true);
            return;
        }
        if (button != InputConstants.MOUSE_BUTTON_RIGHT && button != InputConstants.MOUSE_BUTTON_MIDDLE)
            return;

        Style hovered = hbtweaks$hoverStyleAt(event.x(), event.y());
        if (hovered == null) return;
        if (!(hovered.getHoverEvent() instanceof HoverEvent.ShowText(Component value))) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String firstLine = value.getString().split("\n", 2)[0].trim();
        PlayerInfo pi = mc.player.connection.getPlayerInfo(firstLine);
        if (pi == null) return;
        String mcName = pi.getProfile().name();

        if (button == InputConstants.MOUSE_BUTTON_MIDDLE) {
            this.input.setValue("> " + mcName);
            cir.setReturnValue(true);
            return;
        }

        // Right-click: open that player's context menu.
        if (mc.level == null) return;
        int x = (int) event.x();
        int y = (int) event.y();
        Player target = mc.level.getPlayerByUUID(pi.getProfile().id());
        // mc.execute for not multiclicking
        if (target != null)
            mc.execute(() -> ContextMenuTrigger.openMenuFor(target, x, y));
        else
            mc.execute(() -> ContextMenuTrigger.openMenuForRemote(pi.getProfile(), x, y)); // not loaded
        cir.setReturnValue(true);
    }

    @Unique
    private Style hbtweaks$hoverStyleAt(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        ChatComponent chat = mc.gui.hud.getChat();
        HoverStyleFinder finder = new HoverStyleFinder(mc.font, (int) mouseX, (int) mouseY);
        chat.captureClickableText(finder, mc.getWindow().getGuiScaledHeight(), mc.gui.hud.getGuiTicks(), this.displayMode);
        return finder.result();
    }
}
