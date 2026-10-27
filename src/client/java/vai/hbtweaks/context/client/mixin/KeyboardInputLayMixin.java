package vai.hbtweaks.context.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vai.hbtweaks.context.client.LayAssist;

/**
 * Blocks movement (and jump, sneak, sprint) while lying: moving breaks the body orientation
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputLayMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void hbtweaks$blockMovementWhileLying(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (LayAssist.isActive() && player != null && player.input == this) {
            this.keyPresses = Input.EMPTY;
            this.moveVector = Vec2.ZERO;
        }
    }
}
