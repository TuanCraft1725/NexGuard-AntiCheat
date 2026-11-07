
package vn.nexguard.anticheat;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatListener extends PacketListenerAbstract {

    private final NexGuard plugin;

    private final Map<UUID, AttackData> players =
            new ConcurrentHashMap<>();

    public CombatListener(NexGuard plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType()
                != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        if (!plugin.getConfig().getBoolean(
                "checks.autoclicker.enabled", true)) {
            return;
        }

        WrapperPlayClientInteractEntity packet =
                new WrapperPlayClientInteractEntity(event);

        if (packet.getAction()
                != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }

        UUID uuid = event.getUser().getUUID();
        long now = System.nanoTime() / 1_000_000L;

        AttackData data = players.computeIfAbsent(
                uuid, key -> new AttackData()
        );

        boolean detected = false;

        int maxCps = Math.max(1,
                plugin.getConfig().getInt(
                        "autoclicker.max-cps", 24
                ));

        long window = Math.max(250L,
                plugin.getConfig().getLong(
                        "autoclicker.measurement-window-ms", 1000L
                ));

        synchronized (data) {
            while (!data.attacks.isEmpty()
                    && now - data.attacks.peekFirst() >= window) {
                data.attacks.removeFirst();
            }

            data.attacks.addLast(now);

            // Chi ghi nhan mot lan moi 2 giay.
            if (data.attacks.size() > maxCps
                    && now - data.lastDetection >= 2000L) {
                data.lastDetection = now;
                detected = true;
            }
        }

        if (detected) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);

                if (player == null || !player.isOnline()) {
                    return;
                }

                ViolationManager.ViolationSnapshot result =
                        plugin.getViolationManager().addViolation(
                                uuid, "AutoClicker", 1.0
                        );

                plugin.getAlertManager().sendAlert(
                        player,
                        "AutoClicker",
                        result.violations(),
                        result.vl()
                );
            });
        }
    }

    public void removePlayer(UUID uuid) {
        if (uuid != null) {
            players.remove(uuid);
        }
    }

    public void clear() {
        players.clear();
    }

    private static final class AttackData {
        private final Deque<Long> attacks = new ArrayDeque<>();
        private long lastDetection;
    }
}
