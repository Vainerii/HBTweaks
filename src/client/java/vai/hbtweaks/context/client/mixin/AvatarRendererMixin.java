package vai.hbtweaks.context.client.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vai.hbtweaks.context.client.render.BackItems;

/**
 * Lets the arm hang for a back item. Vanilla returns EMPTY for an empty hand
 * and ITEM for an item. So we return EMPTY is back item.
 */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"), cancellable = true)
    private static void hbtweaks$emptyArmForBackItem(Avatar avatar, ItemStack itemInHand, InteractionHand hand,
                                                     CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? avatar.getMainArm()
                : avatar.getMainArm().getOpposite();
        if (BackItems.isBackMounted(arm, itemInHand))
            cir.setReturnValue(HumanoidModel.ArmPose.EMPTY);
    }
}
