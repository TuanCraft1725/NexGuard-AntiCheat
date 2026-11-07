
package vn.nexguard.anticheat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NexGuard extends JavaPlugin implements Listener {

    private final Map<UUID, Deque<Long>> attacks = new ConcurrentHashMap<>();
    private final Set<UUID> pendingKick = ConcurrentHashMap.newKeySet();

    private volatile boolean autoClickerEnabled;
    private volatile int maxCps;
    private volatile long windowMs;
    private volatile boolean pingEnabled;
    private volatile int maxPing;

    private CombatListener combatListener;
    private BukkitTask pingTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        if (Bukkit.getPluginManager().getPlugin("packetevents") == null) {
            getLogger().severe("Khong tim thay PacketEvents!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        combatListener = new CombatListener();
        PacketEvents.getAPI().getEventManager()
                .registerListener(combatListener);

        pingTask = Bukkit.getScheduler().runTaskTimer(
                this, this::checkPing, 20L, 20L
        );

        getLogger().info("NexGuard da khoi dong!");
    }

    private void loadSettings() {
        reloadConfig();

        autoClickerEnabled = getConfig().getBoolean(
                "autoclicker.enabled", true
        );

        maxCps = Math.max(1, getConfig().getInt(
                "autoclicker.max-cps", 24
        ));

        windowMs = Math.max(250L, getConfig().getLong(
                "autoclicker.measurement-window-ms", 1000L
        ));

        pingEnabled = getConfig().getBoolean(
                "ping-protection.enabled", true
        );

        maxPing = Math.max(1, getConfig().getInt(
                "ping-protection.max-ping", 5000
        ));
    }

    private final class CombatListener extends PacketListenerAbstract {

        private CombatListener() {
            super(PacketListenerPriority.NORMAL);
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (!autoClickerEnabled) return;

            if (event.getPacketType()
                    != PacketType.Play.Client.INTERACT_ENTITY) {
                return;
            }

            WrapperPlayClientInteractEntity packet =
                    new WrapperPlayClientInteractEntity(event);

            if (packet.getAction()
                    != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                return;
            }

            UUID uuid = event.getUser().getUUID();

            if (pendingKick.contains(uuid)) return;

            long now = System.nanoTime() / 1_000_000L;

            Deque<Long> times = attacks.computeIfAbsent(
                    uuid, key -> new ArrayDeque<>()
            );

            boolean exceeded;

            synchronized (times) {
                while (!times.isEmpty()
                        && now - times.peekFirst() >= windowMs) {
                    times.removeFirst();
                }

                times.addLast(now);

                // max-cps = 24 means kick from 25 attacks.
                exceeded = times.size() > maxCps;
            }

            if (exceeded && pendingKick.add(uuid)) {
                Bukkit.getScheduler().runTask(
                        NexGuard.this,
                        () -> kickForAutoClicker(uuid, times)
                );
            }
        }
    }

    private void kickForAutoClicker(
            UUID uuid, Deque<Long> times
    ) {
        Player player = Bukkit.getPlayer(uuid);

        if (player != null && player.isOnline()) {
            int count;

            synchronized (times) {
                count = times.size();
            }

            List<String> lines = getConfig().getStringList(
                    "autoclicker.kick-message"
            );

            String message;

            if (lines.isEmpty()) {
                message = "&c&lNEXGUARD\n"
                        + "&7Bạn click quá nhanh!\n"
                        + "&7Vui lòng không sử dụng AutoClicker.";
            } else {
                message = String.join("\n", lines);
            }

            getLogger().warning(
                    "Da kick " + player.getName()
                            + " - " + count + " attack packets."
            );

            player.kickPlayer(color(message));
        }

        attacks.remove(uuid);
        pendingKick.remove(uuid);
    }

    private void checkPing() {
        if (!pingEnabled) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            int ping = player.getPing();

            if (ping > maxPing) {
                getLogger().warning(
                        "Da kick " + player.getName()
                                + " do ping " + ping + "ms."
                );

                String message = getConfig().getString(
                        "ping-protection.kick-message",
                        "Ping của bạn quá cao, vào lại để chơi tiếp nhé!"
                );

                player.kickPlayer(color(message));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        attacks.remove(uuid);
        pendingKick.remove(uuid);
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                message == null ? "" : message.replace("\\n", "\n")
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
            sender.sendMessage(color("&cBạn không có quyền!"));
            return true;
        }

        if (args.length > 0
                && args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            sender.sendMessage(color("&aĐã tải lại cấu hình NexGuard!"));
            return true;
        }

        sender.sendMessage(color("&bNexGuard AntiCheat"));
        sender.sendMessage(color("&7Trạng thái: &aĐang hoạt động"));
        sender.sendMessage(color("&7Phiên bản: " + getDescription().getVersion()));
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
        }

        attacks.clear();
        pendingKick.clear();

        getLogger().info("NexGuard da tat.");
    }
}
