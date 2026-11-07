
package vn.nexguard.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MovementListener implements Listener {

    private final NexGuard plugin;

    private final Map<UUID, MovementData> data =
            new ConcurrentHashMap<>();

    public MovementListener(NexGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.isOnline()
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || player.isFlying()
                || player.isInsideVehicle()
                || player.isGliding()
                || player.isSwimming()
                || player.isRiptiding()) {
            reset(player.getUniqueId());
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null || from.getWorld() != to.getWorld()) {
            reset(player.getUniqueId());
            return;
        }

        if (from.distanceSquared(to) > 16) {
            reset(player.getUniqueId());
            return;
        }

        if (!plugin.getConfig().getBoolean(
                "checks.speed.enabled", true)) {
            reset(player.getUniqueId());
            return;
        }

        long now = System.nanoTime();

        MovementData movement = data.computeIfAbsent(
                player.getUniqueId(),
                key -> new MovementData()
        );

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        double distance = Math.sqrt(dx * dx + dz * dz);

        synchronized (movement) {
            if (movement.lastTime == 0) {
                movement.lastTime = now;
                return;
            }

            double elapsed =
                    (now - movement.lastTime) / 1_000_000_000.0;

            movement.lastTime = now;

            if (elapsed <= 0 || elapsed > 0.25) {
                movement.suspiciousSamples = 0;
                return;
            }

            double speed = distance / elapsed;

            double allowedSpeed = 9.0;

            var speedEffect = player.getPotionEffect(
                    PotionEffectType.SPEED
            );

            if (speedEffect != null) {
                allowedSpeed +=
                        (speedEffect.getAmplifier() + 1) * 1.5;
            }

            if (player.isInWater()
                    || player.isInLava()
                    || player.getLocation().getBlock().isLiquid()) {
                movement.suspiciousSamples = 0;
                return;
            }

            if (speed > allowedSpeed) {
                movement.suspiciousSamples++;
            } else {
                movement.suspiciousSamples = Math.max(
                        0, movement.suspiciousSamples - 1
                );
            }

            if (movement.suspiciousSamples >= 8
                    && now - movement.lastAlert > 3_000_000_000L) {

                movement.lastAlert = now;
                movement.suspiciousSamples = 0;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;

                    var result = plugin.getViolationManager()
                            .addViolation(
                                    player.getUniqueId(),
                                    "Speed",
                                    1.0
                            );

                    plugin.getAlertManager().sendAlert(
                            player,
                            "Speed",
                            result.violations(),
                            result.vl()
                    );
                });
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        reset(event.getPlayer().getUniqueId());
    }

    public void reset(UUID uuid) {
        if (uuid != null) {
            data.remove(uuid);
        }
    }

    public void clear() {
        data.clear();
    }

    private static final class MovementData {
        private long lastTime;
        private long lastAlert;
        private int suspiciousSamples;
    }
}
