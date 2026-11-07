
package vn.nexguard.anticheat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quan ly so lan vi pham va Violation Level (VL).
 */
public final class ViolationManager {

    private final NexGuard plugin;

    private final Map<UUID, PlayerData> violations =
            new ConcurrentHashMap<>();

    public ViolationManager(NexGuard plugin) {
        this.plugin = plugin;
    }

    /**
     * Cong them mot vi pham cho nguoi choi.
     */
    public ViolationSnapshot addViolation(
            UUID uuid,
            String checkName,
            double vl
    ) {
        if (!plugin.getConfig().getBoolean(
                "violations.enabled", true)) {
            return getSnapshot(uuid);
        }

        if (uuid == null || checkName == null
                || checkName.isBlank()
                || !Double.isFinite(vl) || vl <= 0) {
            return getSnapshot(uuid);
        }

        int maxVL = Math.max(1,
                plugin.getConfig().getInt(
                        "violations.max-vl", 100
                ));

        PlayerData data = violations.computeIfAbsent(
                uuid, key -> new PlayerData()
        );

        synchronized (data) {
            data.violationCount++;

            data.vl = Math.min(
                    maxVL,
                    data.vl + vl
            );

            data.lastCheck = checkName;
            data.lastViolationTime = System.currentTimeMillis();

            return new ViolationSnapshot(
                    data.violationCount,
                    data.vl,
                    data.lastCheck
            );
        }
    }

    /**
     * Lay thong tin vi pham cua nguoi choi.
     */
    public ViolationSnapshot getSnapshot(UUID uuid) {
        if (uuid == null) {
            return new ViolationSnapshot(0, 0, "");
        }

        PlayerData data = violations.get(uuid);

        if (data == null) {
            return new ViolationSnapshot(0, 0, "");
        }

        synchronized (data) {
            return new ViolationSnapshot(
                    data.violationCount,
                    data.vl,
                    data.lastCheck
            );
        }
    }

    /**
     * Lay tong so lan vi pham.
     */
    public int getViolationCount(UUID uuid) {
        return getSnapshot(uuid).violations();
    }

    /**
     * Lay diem VL.
     */
    public double getVL(UUID uuid) {
        return getSnapshot(uuid).vl();
    }

    /**
     * Giam VL, khong lam giam so lan vi pham.
     */
    public void reduceVL(UUID uuid, double amount) {
        if (uuid == null || !Double.isFinite(amount)
                || amount <= 0) {
            return;
        }

        PlayerData data = violations.get(uuid);

        if (data == null) return;

        synchronized (data) {
            data.vl = Math.max(0, data.vl - amount);
        }
    }

    /**
     * Xoa du lieu cua mot nguoi choi.
     */
    public void clear(UUID uuid) {
        if (uuid != null) {
            violations.remove(uuid);
        }
    }

    /**
     * Xoa toan bo du lieu.
     */
    public void clearAll() {
        violations.clear();
    }

    private static final class PlayerData {
        private int violationCount;
        private double vl;
        private String lastCheck = "";
        private long lastViolationTime;
    }

    public record ViolationSnapshot(
            int violations,
            double vl,
            String lastCheck
    ) {}
}
