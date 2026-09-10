package vai.hbtweaks.context.client.mouse;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;
import vai.hbtweaks.context.client.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MouseTracker implements ClientTickEvents.EndTick {

    private static volatile List<Entity> hoveredEntities = List.of();

    public static List<Entity> getHoveredEntities() {
        return hoveredEntities;
    }

    private record Hit(double distSqr, Vec3 point, Entity entity) { }

    private final List<Hit> hits = new ArrayList<>();

    private boolean leftDown = false;
    private boolean middleDown = false;
    private boolean rightDown = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(new MouseTracker());
    }

    private List<Entity> getRayCastedEntities(Vec3 rayDirection) {
        Minecraft mc = Minecraft.getInstance();
        Entity ce = mc.getCameraEntity();
        if (ce == null) return List.of();

        Vec3 origin = mc.gameRenderer.getMainCamera().position();
        Vec3 end = origin.add(rayDirection.normalize().scale(Util.rayLength()));
        AABB searchBox = new AABB(origin, end).inflate(1.0D);

        this.hits.clear();
        for (Entity e : ce.level().getEntities(ce, searchBox,
                en -> Util.hasDev() || (en instanceof Player p && Util.isReal(p)))) {
            AABB ebb = e.getBoundingBox().inflate(e.getPickRadius());
            if (ebb.contains(origin)) {
                this.hits.add(new Hit(0D, origin, e));
                continue;
            }
            Optional<Vec3> optional = ebb.clip(origin, end);
            if (optional.isPresent())
                this.hits.add(new Hit(origin.distanceToSqr(optional.get()), optional.get(), e));
        }
        if (this.hits.isEmpty()) return List.of();
        this.hits.sort(Comparator.comparingDouble(Hit::distSqr));

        int from = 0;
        if (!Util.hasPerm()) {
            while (from < this.hits.size() && isOccluded(ce, origin, this.hits.get(from).point()))
                from++;
            if (from == this.hits.size()) return List.of();
        }

        List<Entity> out = new ArrayList<>(this.hits.size() - from);
        for (int i = from; i < this.hits.size(); i++)
            out.add(this.hits.get(i).entity());
        return List.copyOf(out);
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
        Camera camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized())
            return null;

        int screenWidth = mc.getWindow().getWidth();
        int screenHeight = mc.getWindow().getHeight();
        double fov = Math.toRadians(camera.getFov());
        double aspect = (double) screenWidth / screenHeight;
        double tanHalfFov = Math.tan(fov / 2.0);
        double height = 2.0 * tanHalfFov;
        double width = height * aspect;

        // Cursor is in screen points; framebuffer is in pixels. On HiDPI (macOS Retina)
        // they differ by the DPI scale, so convert the cursor to framebuffer pixels.
        double sx = (double) screenWidth / mc.getWindow().getScreenWidth();
        double sy = (double) screenHeight / mc.getWindow().getScreenHeight();
        double xm = mc.mouseHandler.xpos() * sx;
        double ym = mc.mouseHandler.ypos() * sy;

        double x_ndc = (2.0 * xm / screenWidth) - 1.0;
        double y_ndc = 1.0 - (2.0 * ym / screenHeight);

        double x_cam = x_ndc * (width / 2.0);
        double y_cam = y_ndc * (height / 2.0);

        Vector3fc fwd = camera.forwardVector();
        Vector3fc upv = camera.upVector();
        Vector3fc leftv = camera.leftVector();

        Vec3 forward = new Vec3(fwd.x(), fwd.y(), fwd.z());
        Vec3 right = new Vec3(-leftv.x(), -leftv.y(), -leftv.z());
        Vec3 up = new Vec3(upv.x(), upv.y(), upv.z());

        return right.scale(x_cam)
                .add(up.scale(y_cam))
                .add(forward)
                .normalize();
    }

    @Override
    public void onEndTick(Minecraft minecraft) {
        MouseHandler mh = minecraft.mouseHandler;

        if (mh.isMouseGrabbed()) {
            hoveredEntities = List.of();
            this.leftDown = false;
            this.middleDown = false;
            this.rightDown = false;
            return;
        }

        ScreenType screenType = ScreenType.fromScreen(minecraft.screen);

        if (Math.floorMod(minecraft.gui.getGuiTicks(), 2) == 1) {
            Vec3 pixelRay = this.pixelRayCast();
            hoveredEntities = pixelRay == null ? List.of() : this.getRayCastedEntities(pixelRay);
        }
        List<Entity> detectedEntities = hoveredEntities;

        long windowHandle = minecraft.getWindow().handle();
        boolean left = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean middle = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (this.leftDown && !left)
            MouseTrackerEntityClickUpCallback.EVENT.invoker()
                    .onClickUp(detectedEntities, ClickType.LEFT_CLICK, screenType);
        if (this.middleDown && !middle)
            MouseTrackerEntityClickUpCallback.EVENT.invoker()
                    .onClickUp(detectedEntities, ClickType.MIDDLE_CLICK, screenType);
        if (this.rightDown && !right)
            MouseTrackerEntityClickUpCallback.EVENT.invoker()
                    .onClickUp(detectedEntities, ClickType.RIGHT_CLICK, screenType);

        this.leftDown = left;
        this.middleDown = middle;
        this.rightDown = right;
    }
}
