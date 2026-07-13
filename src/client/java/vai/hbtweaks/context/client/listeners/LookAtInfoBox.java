package vai.hbtweaks.context.client.listeners;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import vai.hbtweaks.context.client.Util;
import vai.hbtweaks.context.client.config.HBConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class LookAtInfoBox implements ClientTickEvents.EndTick {

    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("hb-tweaks-context", "look_at_info");
    private static final int MARGIN = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int BG_COLOR = 0xD0000000;

    private static volatile List<Component> lines = null;
    private static volatile int width = 0;
    private static volatile Player lastTarget = null;
    private static UUID lastUuid = null;

    public void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, LookAtInfoBox::render);
    }

    @Override
    public void onEndTick(Minecraft client) {
        try {
            updateTarget(client);
        } catch (Exception ignored) { }
    }

    private static void updateTarget(Minecraft mc) {
        Entity camera = mc.getCameraEntity();
        if (camera == null || mc.player == null) {
            clear();
            return;
        }
        Player target = getTargetedPlayer(camera, Util.hasPerm());
        if (target == null || mc.player.connection.getPlayerInfo(target.getUUID()) == null) {
            clear();
            return;
        }
        if (target.getUUID().equals(lastUuid))
            return;
        List<Component> built = buildLines(target);
        if (built.isEmpty()) {
            clear();
            return;
        }
        Font font = mc.font;
        int w = 0;
        for (Component c : built)
            w = Math.max(w, font.width(c.getString()));
        lastUuid = target.getUUID();
        lastTarget = target;
        width = w;
        lines = built;
    }

    private static void clear() {
        lastUuid = null;
        lastTarget = null;
        lines = null;
        width = 0;
    }

    private static List<Component> buildLines(Player player) {
        List<Component> out = new ArrayList<>();
        Component rp = Util.getRpName(player);
        if (rp != null)
            out.add(rp);
        String mc = Util.getMCName(player);
        if (mc != null)
            out.add(Component.literal(mc).withStyle(ChatFormatting.DARK_GRAY));
        // GMs also see the player's fake Minecraft name.
        if (Util.hasPerm() && Util.hasFakeName(player))
            out.add(Component.literal(Util.getFakeName(player)).withStyle(ChatFormatting.YELLOW));
        return out;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker tickDelta) {
        try {
            renderBox(graphics);
        } catch (Exception ignored) { }
    }

    private static void renderBox(GuiGraphicsExtractor graphics) {
        List<Component> cached = lines;
        if (cached == null || cached.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ChatScreen)
            return;

        Font font = mc.font;
        // Rebuild each frame bc of animation & indicators
        List<Component> current = new ArrayList<>(cached);
        Player target = lastTarget;
        int w = width;
        if (target != null && mc.player != null) {
            Component line = Util.distanceIndicator(mc.player.position().distanceTo(target.position()))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.WHITE))
                    .append(Util.writingIndicator(target));
            current.add(line);
            w = Math.max(w, font.width(line.getString()));
        }

        int n = current.size();
        int boxW = w + 4;
        int boxH = n * LINE_HEIGHT + 2;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int boxX = switch (HBConfig.get().boxPosition) {
            case TOP_LEFT -> MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - boxW - MARGIN;
        };
        int boxY = switch (HBConfig.get().boxPosition) {
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            case BOTTOM_RIGHT -> screenH - boxH - MARGIN;
        };

        int textX = boxX + 2;
        int textY = boxY + 2;
        graphics.fill(textX - 2, textY - 2, textX + w + 2, textY + n * LINE_HEIGHT, BG_COLOR);
        for (int i = 0; i < n; i++) {
            graphics.text(font, current.get(i), textX, textY + i * LINE_HEIGHT, -1, true);
        }
    }

    private static Player getTargetedPlayer(Entity e, boolean seeThroughWall) {
        try {
            Predicate<Entity> isVisible =
                    entity -> !entity.isSpectator() && entity.isPickable() && !entity.isInvisible();
            Vec3 ep = e.getEyePosition();
            Vec3 vv = e.getViewVector(1.0f);
            Vec3 ray = ep.add(vv.multiply(100f, 100f, 100f));
            AABB searchBox = e.getBoundingBox().expandTowards(vv.scale(100f)).inflate(1.0D, 1.0D, 1.0D);
            EntityHitResult result = ProjectileUtil.getEntityHitResult(e, ep, ray, searchBox, isVisible, 10000f);
            if (result == null || !(result.getEntity() instanceof Player))
                return null;
            if (!seeThroughWall) {
                HitResult hit = e.pick(100, 0, false);
                if (hit.distanceTo(e) < result.distanceTo(e))
                    return null;
            }
            return (Player) result.getEntity();
        } catch (Exception exception) {
            return null;
        }
    }
}
