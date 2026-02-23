package ghostystream.manager;

import ghostystream.GhostyStream;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active streams, broadcast tasks, and auto-stop logic.
 */
public class StreamManager {

    private final GhostyStream plugin;

    // UUID -> StreamSession
    private final Map<UUID, StreamSession> activeSessions = new ConcurrentHashMap<>();

    public StreamManager(GhostyStream plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts a live session for a player.
     *
     * @param player    the player starting the stream
     * @param streamLink the stream URL
     * @return true if started successfully, false if already active
     */
    public boolean startStream(Player player, String streamLink) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            return false;
        }

        int intervalMinutes = plugin.getConfig().getInt("stream-broadcast-interval-minutes", 20);
        int durationMinutes = plugin.getConfig().getInt("stream-auto-stop-minutes", 60);

        long intervalTicks = (long) intervalMinutes * 60 * 20;
        long durationTicks = (long) durationMinutes * 60 * 20;

        // Broadcast task (repeating every intervalMinutes)
        BukkitTask broadcastTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopStream(player.getUniqueId(), true);
                return;
            }
            broadcastStream(player, streamLink);
        }, intervalTicks, intervalTicks);

        // Auto-stop task (after durationMinutes)
        BukkitTask stopTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeSessions.containsKey(player.getUniqueId())) {
                stopStream(player.getUniqueId(), false);
                if (player.isOnline()) {
                    player.sendMessage(plugin.getLang().get("stream-auto-stopped",
                            "duration", String.valueOf(durationMinutes)));
                }
            }
        }, durationTicks);

        activeSessions.put(player.getUniqueId(), new StreamSession(
                player.getUniqueId(),
                player.getName(),
                streamLink,
                broadcastTask,
                stopTask,
                System.currentTimeMillis()
        ));

        // Immediate first broadcast
        broadcastStream(player, streamLink);

        return true;
    }

    /**
     * Stops an active stream session.
     *
     * @param uuid     the player's UUID
     * @param silent   if true, does not broadcast a stop message
     */
    public void stopStream(UUID uuid, boolean silent) {
        StreamSession session = activeSessions.remove(uuid);
        if (session == null) return;

        session.broadcastTask().cancel();
        session.stopTask().cancel();

        if (!silent) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(plugin.getLang().get("stream-stopped"));
            }
        }
    }

    private void broadcastStream(Player player, String link) {
        List<String> lines = plugin.getLang().getLines("stream-broadcast");
        for (String line : lines) {
            String formatted = line
                    .replace("{player}", player.getName())
                    .replace("{link}", link);
            Bukkit.getServer().broadcast(
                    LegacyComponentSerializer.legacySection().deserialize(formatted));
        }
    }

    /**
     * Checks whether a player has an active stream.
     */
    public boolean isStreaming(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    /**
     * Gets the stream link for a player if active.
     */
    public Optional<String> getStreamLink(UUID uuid) {
        StreamSession session = activeSessions.get(uuid);
        return session == null ? Optional.empty() : Optional.of(session.streamLink());
    }

    /**
     * Returns all active sessions (unmodifiable view).
     */
    public Map<UUID, StreamSession> getActiveSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }

    /**
     * Stops all active streams (used on plugin disable).
     */
    public void stopAll() {
        new HashSet<>(activeSessions.keySet()).forEach(uuid -> stopStream(uuid, true));
    }

    // ─── Inner record ───────────────────────────────────────

    public record StreamSession(
            UUID uuid,
            String playerName,
            String streamLink,
            BukkitTask broadcastTask,
            BukkitTask stopTask,
            long startTime
    ) {}
}
