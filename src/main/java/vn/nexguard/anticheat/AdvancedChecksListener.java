
package vn.nexguard.anticheat;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AdvancedChecksListener implements Listener {

    private final NexGuard plugin;
    private final Map<UUID, PlayerData> data = new HashMap<>();

    public AdvancedChecksListener(NexGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        PlayerData state = data.computeIfAbsent(
                player.getUniqueId(), ignored -> new PlayerData()
        );

        long now = System.currentTimeMillis();

        // FastPlace: kiểm tra tốc độ đặt block.
        if (plugin.getConfig().getBoolean(
                "checks.fastplace.enabled", false)) {

            synchronized (state) {
                state.placements.addLast(now);

                while (!state.placements.isEmpty()
                        && now - state.placements.peekFirst() > 1000L) {
                    state.placements.removeFirst();
                }

                int limit = Math.max(8, plugin.getConfig()
                        .getInt("checks.fastplace.max-per-second", 20));

                if (state.placements.size() > limit
                        && now - state.lastFastPlaceAlert >= 3000L) {
                    state.lastFastPlaceAlert = now;
                    report(player, "FastPlace");
                }
            }
        }

        // Scaffold: kiểm tra mẫu đặt block dưới chân khi di chuyển.
        if (plugin.getConfig().getBoolean(
                "checks.scaffold.enabled", false)) {

            Location playerLoc = player.getLocation();
            Location placedLoc = event.getBlockPlaced().getLocation();

            boolean belowFeet =
                    placedLoc.getBlockY() < playerLoc.getBlockY();

            boolean lookingDown =
                    playerLoc.getPitch() >= plugin.getConfig()
                            .getDouble("checks.scaffold.min-pitch", 58.0);

            boolean moving =
                    state.horizontalMovement >= plugin.getConfig()
                            .getDouble(
                                    "checks.scaffold.min-horizontal-movement",
                                    0.10
                            );

            synchronized (state) {
                if (belowFeet && lookingDown && moving) {
                    state.scaffoldSignals.addLast(now);
                }

                while (!state.scaffoldSignals.isEmpty()
                        && now - state.scaffoldSignals.peekFirst() > 4000L) {
                    state.scaffoldSignals.removeFirst();
                }

                int required = Math.max(3, plugin.getConfig()
                        .getInt("checks.scaffold.required-signals", 6));

                if (state.scaffoldSignals.size() >= required
                        && now - state.lastScaffoldAlert >= 5000L) {
                    state.lastScaffoldAlert = now;
                    state.scaffoldSignals.clear();
                    report(player, "Scaffold");
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null || from.getWorld() != to.getWorld()) {
            return;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        PlayerData state = data.computeIfAbsent(
                event.getPlayer().getUniqueId(),
                ignored -> new PlayerData()
        );

        state.horizontalMovement = Math.sqrt(dx * dx + dz * dz);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removePlayer(event.getPlayer().getUniqueId());
    }

    private void report(Player player, String check) {
        if (!player.isOnline() || !plugin.getConfig()
                .getBoolean("violations.enabled", true)) {
            return;
        }

        var result = plugin.getViolationManager().addViolation(
                player.getUniqueId(), check, 1.0
        );

        plugin.getAlertManager().sendAlert(
                player, check, result.violations(), result.vl()
        );
    }

    public void removePlayer(UUID uuid) {
        if (uuid != null) {
            data.remove(uuid);
        }
    }

    public void clear() {
        data.clear();
    }

    private static final class PlayerData {
        private final Deque<Long> placements = new ArrayDeque<>();
        private final Deque<Long> scaffoldSignals = new ArrayDeque<>();

        private long lastFastPlaceAlert;
        private long lastScaffoldAlert;
        private double horizontalMovement;
    }
}
