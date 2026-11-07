
package vn.nexguard.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class NexGuard extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info("NexGuard da khoi dong!");
        getLogger().info("He thong dang duoc phat trien.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NexGuard da tat.");
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
            sender.sendMessage(ChatColor.RED + "Ban khong co quyen!");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "NexGuard AntiCheat");
        sender.sendMessage(ChatColor.GRAY + "Phien ban: " + getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Trang thai: Dang hoat dong");

        return true;
    }
}
