
package vn.nexguard.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AlertManager {

    private final NexGuard plugin;

    private final Map<UUID, Long> cooldowns =
            new ConcurrentHashMap<>();

    public AlertManager(NexGuard plugin) {
        this.plugin = plugin;
    }

    public void sendAlert(
            Player target,
            String checkName,
            int violations,
            double vl
    ) {
        if (!plugin.getConfig().getBoolean(
                "alerts.enabled", true)) {
            return;
        }

        if (target == null || !target.isOnline()) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldown = Math.max(0,
                plugin.getConfig().getLong(
                        "alerts.cooldown-seconds", 2
                )) * 1000L;

        Long last = cooldowns.get(target.getUniqueId());

        if (last != null && now - last < cooldown) {
            return;
        }

        cooldowns.put(target.getUniqueId(), now);

        int ping = target.getPing();

        String prefix = plugin.getConfig().getString(
                "general.prefix", "&b&lNEXGUARD &8» "
        );

        StringBuilder message = new StringBuilder(prefix);

        if (plugin.getConfig().getBoolean(
                "alerts.show.player", true)) {
            message.append("&f")
                    .append(target.getName())
                    .append(" ");
        }

        if (plugin.getConfig().getBoolean(
                "alerts.show.check", true)) {
            message.append("&7[")
                    .append(checkName)
                    .append("] ");
        }

        if (plugin.getConfig().getBoolean(
                "alerts.show.violations", true)) {
            message.append("&cViolations: ")
                    .append(violations)
                    .append(" ");
        }

        if (plugin.getConfig().getBoolean(
                "alerts.show.vl", true)) {
            message.append("&eVL: ")
                    .append(String.format("%.1f", vl))
                    .append(" ");
        }

        if (plugin.getConfig().getBoolean(
                "alerts.show.ping", true)) {
            message.append("&bPing: ")
                    .append(ping)
                    .append("ms");
        }

        String formatted = ChatColor.translateAlternateColorCodes(
                '&', message.toString()
        );

        String permission = plugin.getConfig().getString(
                "alerts.permission", "nexguard.alerts"
        );

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission)) {
                staff.sendMessage(formatted);
            }
        }

        if (plugin.getConfig().getBoolean(
                "logging.console", true)) {
            plugin.getLogger().info(
                    ChatColor.stripColor(formatted)
            );
        }
    }

    public void clear(UUID uuid) {
        if (uuid != null) {
            cooldowns.remove(uuid);
        }
    }

    public void clearAll() {
        cooldowns.clear();
    }
}
