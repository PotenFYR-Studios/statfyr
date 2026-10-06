package in.potenfyr.statfyr.listeners;

import in.potenfyr.statfyr.Statfyr;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feeds lifecycle and activity events into the analytics engine.
 *
 * <p>Handlers are deliberately cheap; any disk work is delegated to the
 * analytics engine's async executor.
 */
public final class PlayerListener implements Listener {

    private final Statfyr plugin;
    private final ConcurrentHashMap<UUID, Long> lastTouch =
            new ConcurrentHashMap<>();

    public PlayerListener(Statfyr plugin) {

        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {

        if (plugin.getAnalytics() == null) {
            return;
        }

        plugin.getAnalytics().onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {

        if (plugin.getAnalytics() == null) {
            return;
        }

        lastTouch.remove(event.getPlayer().getUniqueId());
        plugin.getAnalytics().onQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {

        if (plugin.getAnalytics() == null) {
            return;
        }

        // Only count real movement between blocks, and throttle to once a
        // second per player so the hot path stays cheap.
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }

        long now =
                System.currentTimeMillis();

        Long previous =
                lastTouch.get(event.getPlayer().getUniqueId());

        if (previous != null && now - previous < 1000L) {
            return;
        }

        lastTouch.put(event.getPlayer().getUniqueId(), now);

        plugin.getAnalytics().touch(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {

        if (plugin.getAnalytics() != null) {
            plugin.getAnalytics().touch(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {

        if (plugin.getAnalytics() != null) {
            // Deaths are aggregated into the session summary on quit rather
            // than written per event, to keep storage writes minimal.
            plugin.getAnalytics().touch(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {

        if (plugin.getAnalytics() == null) {
            return;
        }

        Player killer =
                event.getEntity().getKiller();

        if (killer != null) {
            // Same here: kills are summarised per session, not per kill.
            plugin.getAnalytics().touch(killer);
        }
    }
}
