package vai.hbtweaks.context.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vai.hbtweaks.context.client.PlayerOutline;

@Mixin(Minecraft.class)
public class MinecraftGlowingMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void hbtweaks$outlinePlayers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (PlayerOutline.shouldOutline(entity))
            cir.setReturnValue(true);
    }
}
