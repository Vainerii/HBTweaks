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

public final class ScriptRunner {

    private ScriptRunner() {}

    private record Step(String line, Player player) {}

    private static final Deque<Step> QUEUE = new ArrayDeque<>();
    private static int delay = 0;

    private static final Pattern WAIT = Pattern.compile("^\\[wait:(\\d+)([st])]$", Pattern.CASE_INSENSITIVE);

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(_ -> tick());
    }

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
