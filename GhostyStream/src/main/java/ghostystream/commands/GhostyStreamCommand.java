package ghostystream.commands;

import ghostystream.GhostyStream;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /ghostystream <reload|addcreator|removecreator|help>
 * Main admin command.
 */
public class GhostyStreamCommand implements CommandExecutor, TabCompleter {

    private final GhostyStream plugin;

    public GhostyStreamCommand(GhostyStream plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ghostystream.admin")) {
            sender.sendMessage(plugin.getLang().get("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "addcreator" -> handleAddCreator(sender, args);
            case "removecreator" -> handleRemoveCreator(sender, args);
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage(plugin.getLang().get("unknown-subcommand"));
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadPlugin();
        sender.sendMessage(plugin.getLang().get("reload-success"));
    }

    private void handleAddCreator(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLang().get("addcreator-usage"));
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = target.getUniqueId();

        if (!plugin.getCreatorCodeManager().addCreator(uuid)) {
            sender.sendMessage(plugin.getLang().get("addcreator-already",
                    "player", targetName));
            return;
        }

        sender.sendMessage(plugin.getLang().get("addcreator-success",
                "player", targetName));
    }

    private void handleRemoveCreator(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLang().get("removecreator-usage"));
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = target.getUniqueId();

        if (!plugin.getCreatorCodeManager().removeCreator(uuid)) {
            sender.sendMessage(plugin.getLang().get("removecreator-not-creator",
                    "player", targetName));
            return;
        }

        sender.sendMessage(plugin.getLang().get("removecreator-success",
                "player", targetName));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLang().get("help-header"));
        sender.sendMessage(plugin.getLang().get("help-stream"));
        sender.sendMessage(plugin.getLang().get("help-content"));
        sender.sendMessage(plugin.getLang().get("help-creatorcode-redeem"));
        if (sender.hasPermission("ghostystream.creatorcode.create")) {
            sender.sendMessage(plugin.getLang().get("help-creatorcode-create"));
        }
        sender.sendMessage(plugin.getLang().get("help-ghostystream"));
        sender.sendMessage(plugin.getLang().get("help-footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ghostystream.admin")) return List.of();

        if (args.length == 1) {
            return List.of("reload", "addcreator", "removecreator", "help");
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("addcreator")) {
                List<String> names = new ArrayList<>();
                Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
                return names;
            }
            if (args[0].equalsIgnoreCase("removecreator")) {
                List<String> creators = new ArrayList<>();
                plugin.getCreatorCodeManager().getContentCreators().forEach(uuid -> {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                    if (p.getName() != null) creators.add(p.getName());
                });
                return creators;
            }
        }

        return List.of();
    }
}
