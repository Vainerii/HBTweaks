package vai.hbtweaks.context.client;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;

import java.util.Set;

public final class WorldTint {

    // Not sure if everything matters
    private static final Set<EnvironmentAttribute<?>> NEUTRALISED = Set.of(
            EnvironmentAttributes.FOG_COLOR,
            EnvironmentAttributes.FOG_START_DISTANCE,
            EnvironmentAttributes.FOG_END_DISTANCE,
            EnvironmentAttributes.SKY_FOG_END_DISTANCE,
            EnvironmentAttributes.CLOUD_FOG_END_DISTANCE,
            EnvironmentAttributes.SKY_COLOR,
            EnvironmentAttributes.SKY_LIGHT_FACTOR,
            EnvironmentAttributes.SKY_LIGHT_COLOR,
            EnvironmentAttributes.BLOCK_LIGHT_TINT,
            EnvironmentAttributes.AMBIENT_LIGHT_COLOR
    );

    private static boolean neutral = false;

    private WorldTint() {}

    public static boolean isNeutral() {
        return neutral;
    }

    public static void setNeutral(boolean value) {
        neutral = value;
    }

    public static boolean overrides(EnvironmentAttribute<?> attribute) {
        return neutral && NEUTRALISED.contains(attribute);
    }
}
