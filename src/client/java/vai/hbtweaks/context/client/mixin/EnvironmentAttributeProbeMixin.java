package vai.hbtweaks.context.client.mixin;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vai.hbtweaks.context.client.WorldTint;

@Mixin(EnvironmentAttributeProbe.class)
public class EnvironmentAttributeProbeMixin {

    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private <Value> void hbtweaks$neutralValue(EnvironmentAttribute<Value> attribute, float partialTicks,
                                               CallbackInfoReturnable<Value> cir) {
        if (WorldTint.overrides(attribute))
            cir.setReturnValue(attribute.defaultValue());
    }
}
