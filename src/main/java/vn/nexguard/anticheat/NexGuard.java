
package vn.nexguard.anticheat;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class NexGuard extends JavaPlugin implements Listener {

    private LanguageManager languageManager;
    private ViolationManager violationManager;
    private AlertManager alertManager;
    private CombatListener combatListener;
    private MovementListener movementListener;

    private BukkitTask pingTask;

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ViolationManager getViolationManager() {
        return violationManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        languageManager = new LanguageManager(this);

        if (Bukkit.getPluginManager()
                .getPlugin("packetevents") == null) {
            getLogger().severe(
                    ChatColor.stripColor(
                            languageManager.get(
                                    "error.packet_events_missing"
                            )
                    )
            );

            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        violationManager = new ViolationManager(this);
        alertManager = new AlertManager(this);

        combatListener = new CombatListener(this);
        movementListener = new MovementListener(this);

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(
                movementListener, this
        );

        PacketEvents.getAPI().getEventManager()
                .registerListener(combatListener);

        pingTask = Bukkit.getScheduler().runTaskTimer(
                this,
                this::checkPing,
                20L,
                20L
        );

        getLogger().info(
                ChatColor.stripColor(
                        languageManager.get("plugin.enabled")
                )
        );
    }

    private void checkPing() {
        if (!getConfig().getBoolean(
                "ping-protection.enabled", true)) {
            return;
        }

        int maxPing = Math.max(
                1,
                getConfig().getInt(
                        "ping-protection.max-ping", 5000
                )
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            int ping = player.getPing();

            if (ping > maxPing) {
                String message = languageManager.get(
                        "ping.kick_message"
                );

                getLogger().warning(
                        ChatColor.stripColor(
                                languageManager.get(
                                        "ping.kick_log",
                                        "player", player.getName(),
                                        "ping", String.valueOf(ping)
                                )
                        )
                );

                player.kickPlayer(message);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (combatListener != null) {
            combatListener.removePlayer(
                    event.getPlayer().getUniqueId()
            );
        }

        if (movementListener != null) {
            movementListener.reset(
                    event.getPlayer().getUniqueId()
            );
        }

        if (alertManager != null) {
            alertManager.clear(
                    event.getPlayer().getUniqueId()
            );
        }
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!command.getName().equalsIgnoreCase("nexguard")) {
            return false;
        }

        if (!sender.hasPermission("nexguard.admin")) {
            sender.sendMessage(
                    languageManager.get("error.no_permission")
            );
            return true;
        }

        if (args.length > 0
                && args[0].equalsIgnoreCase("reload")) {

            reloadConfig();
            languageManager.load();

            sender.sendMessage(
                    languageManager.get("plugin.reload")
            );
            return true;
        }

        sender.sendMessage(
                languageManager.get("plugin.status.header")
        );
        sender.sendMessage(
                languageManager.get("plugin.status.active")
        );
        sender.sendMessage(
                languageManager.get(
                        "plugin.status.version",
                        "version",
                        getDescription().getVersion()
                )
        );
        sender.sendMessage(
                languageManager.get("plugin.status.command")
        );

        return true;
    }

    @Override
    public void onDisable() {
        if (pingTask != null) {
            pingTask.cancel();
        }

        if (combatListener != null) {
            PacketEvents.getAPI().getEventManager()
                    .unregisterListener(combatListener);
            combatListener.clear();
        }

        if (movementListener != null) {
            movementListener.clear();
        }

        if (alertManager != null) {
            alertManager.clearAll();
        }

        if (languageManager != null) {
            getLogger().info(
                    ChatColor.stripColor(
                            languageManager.get("plugin.disabled")
                    )
            );
        }
    }
}
