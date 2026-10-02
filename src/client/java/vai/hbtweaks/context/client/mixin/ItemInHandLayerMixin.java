package vai.hbtweaks.context.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vai.hbtweaks.context.client.render.BackItems;

/**
 * Draws back items on the torso instead of the hand with transformations so it looks fine
 */
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void hbtweaks$drawOnBack(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack itemStack,
                                     HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                     int lightCoords, CallbackInfo ci) {
        if (item.isEmpty()) return;
        if (state.entityType != EntityType.PLAYER) return;
        if (!BackItems.isBackMounted(arm, itemStack)) return;
        if (!(((RenderLayer<?, ?>) (Object) this).getParentModel() instanceof HumanoidModel<?> humanoid)) return;

        poseStack.pushPose();
        humanoid.root().translateAndRotate(poseStack);
        humanoid.body.translateAndRotate(poseStack);

        poseStack.translate(BackItems.offsetX, BackItems.offsetY, BackItems.offsetZ);

        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(-1.0F / 16.0F, 2.0F / 16.0F, -10.0F / 16.0F);

        if (BackItems.pitchDegrees != 0.0F)
            poseStack.mulPose(Axis.XP.rotationDegrees(BackItems.pitchDegrees));
        if (BackItems.yawDegrees != 0.0F)
            poseStack.mulPose(Axis.YP.rotationDegrees(BackItems.yawDegrees));
        if (BackItems.rollDegrees != 0.0F)
            poseStack.mulPose(Axis.ZP.rotationDegrees(BackItems.rollDegrees));

        item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        ci.cancel();
    }
}
