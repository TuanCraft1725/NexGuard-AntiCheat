
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
    private AdvancedChecksListener advancedChecksListener;

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

        if (Bukkit.getPluginManager().getPlugin("packetevents") == null) {
            getLogger().severe("Không tìm thấy PacketEvents!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        violationManager = new ViolationManager(this);
        alertManager = new AlertManager(this);

        combatListener = new CombatListener(this);
        movementListener = new MovementListener(this);
        advancedChecksListener = new AdvancedChecksListener(this);

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(movementListener, this);
        Bukkit.getPluginManager().registerEvents(
                advancedChecksListener, this
        );

        PacketEvents.getAPI().getEventManager()
                .registerListener(combatListener);

        pingTask = Bukkit.getScheduler().runTaskTimer(
                this, this::checkPing, 20L, 20L
        );

        getLogger().info("NexGuard AntiCheat đã khởi động!");
    }

    private void checkPing() {
        if (!getConfig().getBoolean("ping-protection.enabled", true)) {
            return;
        }

        int maxPing = Math.max(
                1,
                getConfig().getInt("ping-protection.max-ping", 5000)
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            int ping = player.getPing();

            if (ping > maxPing) {
                player.kickPlayer(
                        languageManager.get("ping.kick_message")
                );

                getLogger().warning(
                        "Đã ngắt kết nối " + player.getName()
                                + " do ping " + ping + "ms."
                );
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();

        if (combatListener != null) {
            combatListener.removePlayer(uuid);
        }

        if (movementListener != null) {
            movementListener.reset(uuid);
        }

        if (advancedChecksListener != null) {
            advancedChecksListener.removePlayer(uuid);
        }

        if (alertManager != null) {
            alertManager.clear(uuid);
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

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            languageManager.load();
            sender.sendMessage(languageManager.get("plugin.reload"));
            return true;
        }

        sender.sendMessage(languageManager.get("plugin.status.header"));
        sender.sendMessage(languageManager.get("plugin.status.active"));
        sender.sendMessage(
                languageManager.get(
                        "plugin.status.version",
                        "version",
                        getDescription().getVersion()
                )
        );
        sender.sendMessage(languageManager.get("plugin.status.command"));

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

        if (advancedChecksListener != null) {
            advancedChecksListener.clear();
        }

        if (alertManager != null) {
            alertManager.clearAll();
        }

        getLogger().info("NexGuard AntiCheat đã tắt.");
    }
}
