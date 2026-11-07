
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

    private ViolationManager violationManager;
    private AlertManager alertManager;
    private CombatListener combatListener;
    private MovementListener movementListener;

    private BukkitTask pingTask;

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

        if (Bukkit.getPluginManager()
                .getPlugin("packetevents") == null) {
            getLogger().severe(
                    "Khong tim thay PacketEvents! NexGuard se tat."
            );
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        violationManager = new ViolationManager(this);
        alertManager = new AlertManager(this);

        combatListener = new CombatListener(this);
        movementListener = new MovementListener(this);

        Bukkit.getPluginManager().registerEvents(
                this, this
        );

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
                "NexGuard AntiCheat da khoi dong!"
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
            if (player.getPing() > maxPing) {
                String message = getConfig().getString(
                        "ping-protection.kick-message",
                        "Ping của bạn quá cao, vào lại để chơi tiếp nhé!"
                );

                getLogger().warning(
                        "Da ngat ket noi " + player.getName()
                                + " do ping " + player.getPing() + "ms."
                );

                player.kickPlayer(color(message));
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

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                message == null ? "" :
                        message.replace("\\n", "\n")
        );
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
                    color("&cBạn không có quyền sử dụng lệnh này!")
            );
            return true;
        }

        if (args.length > 0
                && args[0].equalsIgnoreCase("reload")) {

            reloadConfig();

            sender.sendMessage(
                    color("&aĐã tải lại cấu hình NexGuard!")
            );
            return true;
        }

        sender.sendMessage(color("&b&lNEXGUARD ANTICHEAT"));
        sender.sendMessage(color("&7Trạng thái: &aĐang hoạt động"));
        sender.sendMessage(color("&7Phiên bản: "
                + getDescription().getVersion()));
        sender.sendMessage(color("&7Lệnh: &f/nexguard reload"));

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

        getLogger().info("NexGuard AntiCheat da tat.");
    }
}
