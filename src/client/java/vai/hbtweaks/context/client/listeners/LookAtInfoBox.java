package vai.hbtweaks.context.client.listeners;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import vai.hbtweaks.context.client.Util;
import vai.hbtweaks.context.client.config.HBConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static vai.hbtweaks.context.client.Util.isReal;

public class LookAtInfoBox implements ClientTickEvents.EndTick {

    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("hb-tweaks-context", "look_at_info");
    private static final int MARGIN = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int BG_COLOR = 0xD0000000;

    private record Box(List<Component> lines, int width) { }

    private static volatile Box box = null;

    private static List<Component> staticLines = null;
    private static int staticWidth = 0;
    private static UUID lastUuid = null;

    public void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, LookAtInfoBox::render);
    }

    @Override
    public void onEndTick(Minecraft client) {
        // Both intervals are even, and MouseTracker only runs on odd gui ticks, so the
        // two raycasts never land on the same tick. Slower rate is enough when cursor doesnt move.
        int interval = client.mouseHandler.isMouseGrabbed() ? 2 : 6;
        if (Math.floorMod(client.gui.getGuiTicks(), interval) != 0)
            return;
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

        Font font = mc.font;
        if (!target.getUUID().equals(lastUuid)) {
            List<Component> built = buildLines(target);
            if (built.isEmpty()) {
                clear();
                return;
            }
            int w = 0;
            for (Component c : built)
                w = Math.max(w, font.width(c));
            lastUuid = target.getUUID();
            staticLines = built;
            staticWidth = w;
        }

        Component live = Util.distanceIndicator(mc.player.position().distanceTo(target.position()))
                .append(Component.literal(" - ").withStyle(ChatFormatting.WHITE))
                .append(Util.writingIndicator(target));

        List<Component> built = new ArrayList<>(staticLines.size() + 1);
        built.addAll(staticLines);
        built.add(live);

        box = new Box(List.copyOf(built), Math.max(staticWidth, font.width(live)));
    }

    private static void clear() {
        lastUuid = null;
        staticLines = null;
        staticWidth = 0;
        box = null;
    }

    private static List<Component> buildLines(Player player) {
        List<Component> out = new ArrayList<>();
        Component rp = Util.getRpName(player);
        rp = Util.getHead(player).copy().append(rp);
        if (!rp.getString().isEmpty())
            out.add(rp);
        String mc = Util.getVisibleMCName(player);
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
        Box current = box;
        if (current == null || current.lines().isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();

        int w = current.width();
        int n = current.lines().size();
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
            graphics.text(mc.font, current.lines().get(i), textX, textY + i * LINE_HEIGHT, -1, true);
        }
    }

    private static Player getTargetedPlayer(Entity camera, boolean seeThroughWall) {
        try {
            Minecraft mc = Minecraft.getInstance();
            double range = Util.rayLength();
            Vec3 eye = camera.getEyePosition();
            Vec3 end = eye.add(camera.getViewVector(1.0f).scale(range));

            Player best = null;
            double bestDist = Double.MAX_VALUE;
            for (Player p : camera.level().players()) {
                if (p == camera || p == mc.player) continue;
                if (!isReal(p)) continue;
                if (p.isSpectator() || !p.isPickable() || p.isInvisible()) continue;
                AABB box = p.getBoundingBox().inflate(p.getPickRadius());
                Optional<Vec3> hit = box.clip(eye, end);
                if (hit.isEmpty()) continue;
                double d = eye.distanceToSqr(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = p;
                }
            }
            if (best == null)
                return null;

            // Blocks still hide the box, but only the winner is worth testing.
            if (!seeThroughWall) {
                HitResult blocks = camera.pick(range, 0, false);
                if (blocks.getType() != HitResult.Type.MISS
                        && blocks.getLocation().distanceToSqr(eye) < bestDist)
                    return null;
            }
            return best;
        } catch (Exception exception) {
            return null;
        }
    }
}
