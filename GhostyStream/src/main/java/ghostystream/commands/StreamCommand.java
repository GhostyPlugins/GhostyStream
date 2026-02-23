package ghostystream.commands;

import ghostystream.GhostyStream;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /stream <link|stop>
 * Allows content creators to activate or deactivate their live status.
 */
public class StreamCommand implements CommandExecutor, TabCompleter {

    private final GhostyStream plugin;

    public StreamCommand(GhostyStream plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().get("player-only"));
            return true;
        }

        if (!player.hasPermission("ghostystream.stream")) {
            player.sendMessage(plugin.getLang().get("no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getLang().get("stream-usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            if (!plugin.getStreamManager().isStreaming(player.getUniqueId())) {
                player.sendMessage(plugin.getLang().get("stream-not-active"));
                return true;
            }
            plugin.getStreamManager().stopStream(player.getUniqueId(), false);
            return true;
        }

        // Treat argument as stream link
        String link = args[0];

        // Basic URL validation
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            player.sendMessage(plugin.getLang().get("stream-invalid-link"));
            return true;
        }

        if (!plugin.getStreamManager().startStream(player, link)) {
            player.sendMessage(plugin.getLang().get("stream-already-live"));
            return true;
        }

        int interval = plugin.getConfig().getInt("stream-broadcast-interval-minutes", 20);
        int duration = plugin.getConfig().getInt("stream-auto-stop-minutes", 60);
        player.sendMessage(plugin.getLang().get("stream-started",
                "interval", String.valueOf(interval),
                "duration", String.valueOf(duration)));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            if (plugin.getStreamManager().isStreaming(
                    sender instanceof Player p ? p.getUniqueId() : null)) {
                return List.of("stop");
            }
            return List.of("https://twitch.tv/", "https://youtube.com/", "stop");
        }
        return List.of();
    }
}
