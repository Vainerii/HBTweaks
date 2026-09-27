package vai.hbtweaks.context.client.script;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import vai.hbtweaks.context.client.contextmenu.ContextMenu;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the multi-line scripts of context menu entries, one line per tick. A line starting with
 * "/" is sent as a command, any other line as a chat message, after placeholder replacement.
 * A line <code>[wait:Ns]</code> or <code>[wait:Nt]</code> pauses the queue for N seconds or
 * N ticks. The queue is dropped when the player leaves the world.
 */
public final class ScriptRunner {

    private ScriptRunner() {}

    /** One script line, with the player targeted by the menu it came from. */
    private record Step(String line, Player player) {}

    private static final Deque<Step> QUEUE = new ArrayDeque<>();
    /** Remaining ticks to wait before running the next line. */
    private static int delay = 0;

    /** Matches a wait line: a number followed by "s" (seconds) or "t" (ticks). */
    private static final Pattern WAIT = Pattern.compile("^\\[wait:(\\d+)([st])]$", Pattern.CASE_INSENSITIVE);

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(_ -> tick());
    }

    /**
     * Appends script lines to the queue, after any script already running.
     *
     * @param lines the script lines
     * @param player the player the placeholders refer to
     */
    public static void enqueue(List<String> lines, Player player) {
        for (String line : lines) QUEUE.add(new Step(line, player));
    }

    public static int queueSize() {
        return QUEUE.size();
    }

    public static int delayTicks() {
        return delay;
    }

    public static void clearQueue() {
        QUEUE.clear();
        delay = 0;
    }

    private static void tick() {
        if (Minecraft.getInstance().player == null) {
            QUEUE.clear();
            delay = 0;
            return;
        }
        if (delay > 0) {
            delay--;
            return;
        }
        while (!QUEUE.isEmpty()) {
            if (runStep(QUEUE.poll())) return;
        }
    }

    /**
     * Runs one script line.
     *
     * @param step the line to run
     * @return true if the line consumed the tick (message sent or wait started), false if it
     * was blank and the next line can run right away
     */
    private static boolean runStep(Step step) {
        String raw = step.line().trim();
        if (raw.isEmpty()) return false;

        Matcher m = WAIT.matcher(raw);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            delay = m.group(2).equalsIgnoreCase("s") ? n * 20 : n;
            return true;
        }
        String out = ContextMenu.replaceString(raw, step.player());
        Minecraft mc = Minecraft.getInstance();
        if (out.startsWith("/"))
            mc.player.connection.sendCommand(out.substring(1));
        else
            mc.player.connection.sendChat(out);
        return true;
    }
}
