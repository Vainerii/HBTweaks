package vai.hbtweaks.context.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vai.hbtweaks.context.client.LayAssist;

/** While lying, the camera uses the rotation of LayAssist instead of the player's */
@Mixin(Camera.class)
public class CameraLayMixin {

    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float hbtweaks$layCameraYaw(Entity entity, float partialTicks, Operation<Float> original) {
        return LayAssist.controlsCamera() && entity == Minecraft.getInstance().player
                ? LayAssist.cameraYaw()
                : original.call(entity, partialTicks);
    }

    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float hbtweaks$layCameraPitch(Entity entity, float partialTicks, Operation<Float> original) {
        return LayAssist.controlsCamera() && entity == Minecraft.getInstance().player
                ? LayAssist.cameraPitch()
                : original.call(entity, partialTicks);
    }
}
