package vai.hbtweaks.context.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import vai.hbtweaks.context.client.config.HBConfig;
import vai.hbtweaks.context.client.config.HBConfig.LayAssistMode;

public final class LayAssist {

    // Stuff stuff stuff
    // It works but I'm not 100% sure why
    private static final float MAX_HEAD_TURN = 50.0F;
    private static final float ALIGNED = 2.0F;

    private static boolean active = false;
    private static boolean frozen = false;
    private static boolean aligning = false;
    private static float cameraYaw;
    private static float cameraPitch;
    private static float sentYaw;
    private static float sentPitch;
    private static float overshoot;
    private static @Nullable CameraType previousCamera;

    private LayAssist() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(LayAssist::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> exit(Minecraft.getInstance()));
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean controlsCamera() {
        Minecraft mc = Minecraft.getInstance();
        return active && mc.player != null && mc.getCameraEntity() == mc.player;
    }

    public static float cameraYaw() {
        return cameraYaw;
    }

    public static float cameraPitch() {
        return cameraPitch;
    }

    /** Mouse movement while lying */
    public static void turn(LocalPlayer me, double xo, double yo) {
        cameraYaw += (float) xo * 0.15F;
        cameraPitch = Mth.clamp(cameraPitch + (float) yo * 0.15F, -90.0F, 90.0F);
        if (frozen) return;
        followCamera();
        sendRotation(me);
    }

    private static void tick(Minecraft mc) {
        LayAssistMode mode = HBConfig.get().layAssist;
        LocalPlayer me = mc.player;
        Player body = me == null || mode == LayAssistMode.OFF ? null : ownBody(me);
        if (body == null) {
            exit(mc);
            return;
        }
        if (!active)
            enter(mc, me, mode == LayAssistMode.SNEAK_HOLD);
        if (mode == LayAssistMode.SNEAK_HOLD) {
            while (mc.options.keyShift.consumeClick()) { }
            boolean held = mc.options.keyShift.isDown();
            if (frozen == held)
                setFrozen(mc, !held);
        } else {
            while (mc.options.keyShift.consumeClick())
                setFrozen(mc, !frozen);
        }

        if (aligning) {
            float diff = Mth.wrapDegrees(sentYaw - body.yBodyRot);
            aligning = Math.abs(diff) > ALIGNED;
            overshoot = aligning ? Math.signum(diff) * MAX_HEAD_TURN : 0.0F;
        }
        sendRotation(me);
    }

    private static @Nullable Player ownBody(LocalPlayer me) {
        for (Player p : me.level().players()) {
            if (LyingBody.isBodyOf(p, me))
                return p;
        }
        return null;
    }

    private static void enter(Minecraft mc, LocalPlayer me, boolean startFrozen) {
        active = true;
        frozen = startFrozen;
        aligning = true;
        cameraYaw = me.getYRot();
        cameraPitch = me.getXRot();
        followCamera();
        previousCamera = mc.options.getCameraType();
        setCamera(mc, frozen ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
        while (mc.options.keyShift.consumeClick()) { }
    }

    private static void exit(Minecraft mc) {
        if (!active) return;
        active = false;
        frozen = false;
        aligning = false;
        if (mc.player != null)
            setRotation(mc.player, cameraYaw, cameraPitch);
        if (previousCamera != null)
            setCamera(mc, previousCamera);
        previousCamera = null;
    }

    private static void setFrozen(Minecraft mc, boolean value) {
        frozen = value;
        if (frozen) {
            setCamera(mc, CameraType.FIRST_PERSON);
        } else {
            cameraYaw = -90.0F - sentYaw;
            cameraPitch = sentPitch;
            setCamera(mc, CameraType.THIRD_PERSON_BACK);
        }
    }

    private static void followCamera() {
        sentYaw = -90.0F - cameraYaw;
        sentPitch = cameraPitch;
    }

    private static void sendRotation(LocalPlayer me) {
        setRotation(me, sentYaw + (aligning ? overshoot : 0.0F), sentPitch);
    }

    private static void setRotation(LocalPlayer me, float yaw, float pitch) {
        me.setYRot(yaw);
        me.setXRot(pitch);
        me.yRotO = yaw;
        me.xRotO = pitch;
    }

    /** Same as the vanilla perspective key. */
    private static void setCamera(Minecraft mc, CameraType type) {
        CameraType previous = mc.options.getCameraType();
        mc.options.setCameraType(type);
        if (previous.isFirstPerson() != type.isFirstPerson())
            mc.gameRenderer.checkEntityPostEffect(type.isFirstPerson() ? mc.getCameraEntity() : null);
        mc.levelRenderer.needsUpdate();
    }
}
