package vai.hbtweaks.context.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vai.hbtweaks.context.client.LayAssist;

/**
 * While lying, the mouse turns the camera of LayAssist instead the one of the player
 */
@Mixin(MouseHandler.class)
public class MouseHandlerLayMixin {

    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void hbtweaks$turnLayCamera(LocalPlayer player, double xo, double yo, Operation<Void> original) {
        if (LayAssist.controlsCamera())
            LayAssist.turn(player, xo, yo);
        else
            original.call(player, xo, yo);
    }
}
