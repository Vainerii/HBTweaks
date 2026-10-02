package vai.hbtweaks.context.client.render;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Items the resource pack draws as worn on the back while they are actually held in hand.
 *
 * Placement happens in model space, at the torso pivot, before the orientation vanilla
 * applies to a held item. That frame is: +X right, +Y down, +Z backward, in blocks.
 */
public final class BackItems {

    private static final CompoundTag BACK_ITEM_MARKER = marker();

    private static CompoundTag marker() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("back_item", true);
        return tag;
    }

    /**
     * Transformations empirically found in game to keep the object in the same place as before
     */
    public static float offsetX = 0.33F;
    public static float offsetY = 0.03F;
    public static float offsetZ = -0.23F;

    public static float pitchDegrees = 22.5F;
    public static float yawDegrees = 0.0F;
    public static float rollDegrees = 0.0F;

    // public static boolean enabled = true;
    // public static boolean freezeArms = false;

    private BackItems() {}

    /** check minecraft:custom_data back_item=true, and if held in  left hand. */
    public static boolean isBackMounted(HumanoidArm arm, ItemStack stack) {
        // if (!enabled || arm != HumanoidArm.LEFT || stack.isEmpty()) return false;
        if (arm != HumanoidArm.LEFT || stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.matchedBy(BACK_ITEM_MARKER);
    }
}
