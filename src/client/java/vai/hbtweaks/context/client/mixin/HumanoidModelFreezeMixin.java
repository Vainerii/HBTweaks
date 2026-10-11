// DELETEME

// package vai.hbtweaks.context.client.mixin;
// 
// import net.minecraft.client.model.HumanoidModel;
// import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
// import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// import vai.hbtweaks.context.client.render.BackItems;
//
// @Mixin(HumanoidModel.class)
// public class HumanoidModelFreezeMixin {
// 
//     @Inject(method = "setupAnim", at = @At("TAIL"))
//     private void hbtweaks$freezeArms(HumanoidRenderState state, CallbackInfo ci) {
//         if (!BackItems.freezeArms) return;
//         HumanoidModel<?> self = (HumanoidModel<?>) (Object) this;
//         self.rightArm.xRot = 0.0F;
//         self.rightArm.yRot = 0.0F;
//         self.rightArm.zRot = 0.0F;
//         self.leftArm.xRot = 0.0F;
//         self.leftArm.yRot = 0.0F;
//         self.leftArm.zRot = 0.0F;
//     }
// }
