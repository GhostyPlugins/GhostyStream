package ghostystream.commands;

import ghostystream.GhostyStream;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /content
 * Opens the Content Creator GUI.
 */
public class ContentCommand implements CommandExecutor, TabCompleter {

    private final GhostyStream plugin;

    public ContentCommand(GhostyStream plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().get("player-only"));
            return true;
        }

        if (!player.hasPermission("ghostystream.content")) {
            player.sendMessage(plugin.getLang().get("no-permission"));
            return true;
        }

        if (plugin.getCreatorCodeManager().getContentCreators().isEmpty()) {
            player.sendMessage(plugin.getLang().get("gui-no-creators"));
            return true;
        }

        plugin.getContentGUI().open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return List.of();
    }
}
