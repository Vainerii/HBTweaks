package vai.hbtweaks.context.client.mouse;

import com.google.common.base.Predicates;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;
import vai.hbtweaks.context.client.Util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MouseTracker implements ClientTickEvents.EndTick {

    private List<Entity> previouslyDetectedEntities = new ArrayList<>();

    private static volatile List<Entity> hoveredEntities = List.of();

    public static List<Entity> getHoveredEntities() {
        return hoveredEntities;
    }

    private Map<ClickType, Boolean> previousClicksStatus = new EnumMap<>(Map.of(
            ClickType.LEFT_CLICK, false,
            ClickType.RIGHT_CLICK, false,
            ClickType.MIDDLE_CLICK, false
    ));

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(new MouseTracker());
    }

    private List<Entity> getRayCastedEntities(Vec3 rayDirection) {
        Entity ce = Minecraft.getInstance().getCameraEntity();
        if (ce == null) return List.of();

        Vec3 origin = ce.getEyePosition();
        Vec3 end = origin.add(rayDirection.normalize().scale(256.0));
        AABB searchBox = new AABB(origin, end).inflate(1.0D);
        ArrayList<Map.Entry<Double, Entity>> resultMap = new ArrayList<>();

        boolean checkOcclusion = !Util.hasPerm();

        for (Entity e : ce.level().getEntities(ce, searchBox, Predicates.alwaysTrue())) {
            AABB ebb = e.getBoundingBox().inflate(e.getPickRadius());
            Optional<Vec3> optional = ebb.clip(origin, end);
            if (ebb.contains(origin)) {
                resultMap.add(Map.entry(0D, e));
            } else if (optional.isPresent()) {
                if (checkOcclusion && isOccluded(ce, origin, optional.get())) continue;
                double dist = origin.distanceToSqr(optional.get());
                resultMap.add(Map.entry(dist, e));
            }
        }
        resultMap.sort(Map.Entry.comparingByKey());
        return resultMap.stream().map(Map.Entry::getValue).toList();
    }

    private boolean isOccluded(Entity ce, Vec3 origin, Vec3 target) {
        Level level = ce.level();
        Boolean hit = BlockGetter.traverseBlocks(origin, target, null,
                (ctx, pos) -> {
                    BlockState state = level.getBlockState(pos);
                    // Transparent blocks (glass, leaves, flowers...) never occlude.
                    if (!state.canOcclude()) return null;
                    VoxelShape shape = state.getShape(level, pos);
                    return shape.clip(origin, target, pos) != null ? Boolean.TRUE : null;
                },
                ctx -> null);
        return Boolean.TRUE.equals(hit);
    }

    private Vec3 pixelRayCast() {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        if (camera == null)
            return null;

        int screenWidth = mc.getWindow().getWidth();
        int screenHeight = mc.getWindow().getHeight();
        double fov = Math.toRadians(mc.gameRenderer.getMainCamera().getFov());
        double aspect = (double) screenWidth / screenHeight;
        double tanHalfFov = Math.tan(fov / 2.0);
        double height = 2.0 * tanHalfFov;
        double width = height * aspect;

        double xm = mc.mouseHandler.xpos();
        double ym = mc.mouseHandler.ypos();

        double x_ndc = (2.0 * xm / screenWidth) - 1.0;
        double y_ndc = 1.0 - (2.0 * ym / screenHeight);

        double x_cam = x_ndc * (width / 2.0);
        double y_cam = y_ndc * (height / 2.0);

        float yaw = (float) Math.toRadians(camera.getYRot());
        float pitch = (float) Math.toRadians(camera.getXRot());

        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);

        Vec3 forward = new Vec3(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
        Vec3 right = new Vec3(-cosYaw, 0, -sinYaw);
        Vec3 up = right.cross(forward).normalize();

        return right.scale(x_cam)
                .add(up.scale(y_cam))
                .add(forward.scale(1.0D))
                .normalize();
    }

    @Override
    public void onEndTick(Minecraft minecraft) {
        MouseHandler mh = minecraft.mouseHandler;

        if (mh.isMouseGrabbed()) {
            hoveredEntities = List.of();
            return;
        }

        ScreenType screenType = ScreenType.fromScreen(minecraft.screen);

        Vec3 pixelRay = this.pixelRayCast();
        if (pixelRay == null) {
            hoveredEntities = List.of();
            return;
        }

        List<Entity> detectedEntities = this.getRayCastedEntities(pixelRay);
        hoveredEntities = detectedEntities;

        long windowHandle = minecraft.getWindow().handle();
        Map<ClickType, Boolean> clicksStatus = new EnumMap<>(Map.of(
                ClickType.LEFT_CLICK, GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS,
                ClickType.MIDDLE_CLICK, GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS,
                ClickType.RIGHT_CLICK, GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS
        ));

        for (var clickEntry : clicksStatus.entrySet()) {
            boolean wasPressed = this.previousClicksStatus.get(clickEntry.getKey());
            boolean isPressed = clickEntry.getValue();
            if (wasPressed && !isPressed) {
                MouseTrackerEntityClickUpCallback.EVENT.invoker()
                        .onClickUp(detectedEntities, clickEntry.getKey(), screenType);
            }
        }

        this.previouslyDetectedEntities = detectedEntities;
        this.previousClicksStatus = clicksStatus;
    }
}
