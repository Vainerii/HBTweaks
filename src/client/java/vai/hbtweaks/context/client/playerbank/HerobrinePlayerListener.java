package vai.hbtweaks.context.client.playerbank;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.world.entity.player.Player;
import vai.hbtweaks.context.HBTweaksContext;
import vai.hbtweaks.context.client.Util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Left for now, I will handle this later
public class HerobrinePlayerListener {

    private final Queue<Player> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.level == null || client.player == null) return;
            double renderDist = client.options.renderDistance().get() * 16.0;
            for (Player player : client.level.players()) {
                if (player == client.player) continue;
                if (!Util.isReal(player)) {
                    HBTweaksContext.LOGGER.info("Foud on join player {} is not real", player.getName().getString());
                    continue;
                }
                if (player.distanceTo(client.player) <= renderDist) {
                    HBTweaksContext.LOGGER.info("Queuing player on join: {}", player.getName().getString());
                    queue.add(player);
                }
            }
        });

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Player player) {
                if (Util.isReal(player)) {
                    HBTweaksContext.LOGGER.info("Queuing player: {}", player.getName().getString());
                    queue.add(player);
                } else {
                    HBTweaksContext.LOGGER.info("Foud player {} is not real", player.getName().getString());
                }
            }
        });

        scheduler.scheduleAtFixedRate(() -> {
            Player player = queue.poll();
            if (player != null) {
                HBTweaksContext.LOGGER.info("Processing player: {} ({} remaining in queue)", player.getName().getString(), queue.size());
                try {
                    HerobrinePlayer.processPlayer(player);
                    HBTweaksContext.LOGGER.info("Done processing player: {}", player.getName().getString());
                } catch (Exception e) {
                    HBTweaksContext.LOGGER.error("Failed to process player: {}", player.getName().getString(), e);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}