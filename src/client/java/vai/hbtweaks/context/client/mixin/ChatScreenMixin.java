package vai.hbtweaks.context.client.mixin;

import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vai.hbtweaks.context.client.keyboard.WritingObserver;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "onEdited", at = @At("TAIL"))
    private void hbtweaks$onEdited(String value, CallbackInfo ci) {
        WritingObserver.textChanged(value);
    }
}
