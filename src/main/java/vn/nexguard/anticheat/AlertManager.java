
package vn.nexguard.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Locale;
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
        if (!plugin.getConfig().getBoolean("alerts.enabled", true)
                || target == null || !target.isOnline()) {
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

        LanguageManager lang = plugin.getLanguageManager();

        String prefix = plugin.getConfig().getString(
                "general.prefix", "&b&lNEXGUARD &8» "
        );

        String checkKey = "check." + checkName
                .toLowerCase(Locale.ROOT);

        String translatedCheck = lang.get(checkKey);

        StringBuilder message = new StringBuilder(prefix);

        if (plugin.getConfig().getBoolean(
                "alerts.show.player", true)) {
            message.append(lang.get(
                    "alert.player", "player", target.getName()
            )).append(" ");
        }

        if (plugin.getConfig().getBoolean(
                "alerts.show.check", true)) {
            message.append(lang.get(
                    "alert.check", "check", translatedCheck
            )).append(" ");
        }

        if (plugin.getConfig().getBoolean(
                "alerts.show.violations", true)) {
            message.append(lang.get(
                    "alert.violations",
                    "violations",
                    String.valueOf(violations)
            )).append(" ");
        }

        if (plugin.getConfig().getBoolean(
                "alerts.show.vl", true)) {
            message.append(lang.get(
                    "alert.vl",
                    "vl",
                    String.format(Locale.US, "%.1f", vl)
            )).append(" ");
        }

        if (plugin.getConfig().getBoolean(
                "alerts.show.ping", true)) {
            message.append(lang.get(
                    "alert.ping",
                    "ping",
                    String.valueOf(target.getPing())
            ));
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
