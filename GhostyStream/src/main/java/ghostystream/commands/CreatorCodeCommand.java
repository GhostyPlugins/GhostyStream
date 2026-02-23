package ghostystream.commands;

import ghostystream.GhostyStream;
import ghostystream.manager.CreatorCodeManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /creatorcode <create|redeem> [...]
 */
public class CreatorCodeCommand implements CommandExecutor, TabCompleter {

    private final GhostyStream plugin;

    public CreatorCodeCommand(GhostyStream plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ghostystream.creatorcode")) {
            sender.sendMessage(plugin.getLang().get("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getLang().get("creatorcode-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "redeem" -> handleRedeem(sender, args);
            case "delete" -> handleDelete(sender, args);
            default -> sender.sendMessage(plugin.getLang().get("creatorcode-usage"));
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghostystream.creatorcode.create")) {
            sender.sendMessage(plugin.getLang().get("no-permission"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getLang().get("creatorcode-create-usage"));
            return;
        }

        String targetName = args[1];
        String code = args[2];

        // Find player (online or offline)
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        // Check if they are a registered content creator
        if (!plugin.getCreatorCodeManager().isCreator(targetUUID)) {
            sender.sendMessage(plugin.getLang().get("creatorcode-not-creator",
                    "player", targetName));
            return;
        }

        if (!plugin.getCreatorCodeManager().createCode(code, targetUUID)) {
            sender.sendMessage(plugin.getLang().get("creatorcode-already-exists"));
            return;
        }

        sender.sendMessage(plugin.getLang().get("creatorcode-created",
                "code", code,
                "streamer", targetName));
    }

    private void handleRedeem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().get("player-only"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLang().get("creatorcode-redeem-usage"));
            return;
        }

        String code = args[1];

        CreatorCodeManager.RedeemResult result = plugin.getCreatorCodeManager().redeem(player, code);

        switch (result) {
            case CODE_NOT_FOUND -> player.sendMessage(plugin.getLang().get("creatorcode-not-found"));
            case ALREADY_REDEEMED -> player.sendMessage(plugin.getLang().get("creatorcode-already-redeemed"));
            case SUCCESS -> {
                UUID streamerUUID = plugin.getCreatorCodeManager().getStreamerForCode(code).orElse(null);
                String streamerName = "Unknown";
                if (streamerUUID != null) {
                    OfflinePlayer streamer = Bukkit.getOfflinePlayer(streamerUUID);
                    streamerName = streamer.getName() != null ? streamer.getName() : "Unknown";

                    // Warn if streamer is offline
                    if (!streamer.isOnline()) {
                        player.sendMessage(plugin.getLang().get("creatorcode-streamer-offline",
                                "streamer", streamerName));
                    }
                }

                List<String> lines = plugin.getLang().getLines("creatorcode-redeemed-player");
                for (String line : lines) {
                    player.sendMessage(line
                            .replace("{code}", code)
                            .replace("{streamer}", streamerName));
                }
            }
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghostystream.creatorcode.create")) {
            sender.sendMessage(plugin.getLang().get("no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("&cNutzung: /creatorcode delete <code>");
            return;
        }

        String code = args[1];
        if (!plugin.getCreatorCodeManager().deleteCode(code)) {
            sender.sendMessage(plugin.getLang().get("creatorcode-not-found"));
            return;
        }

        sender.sendMessage(plugin.getLang().get("creatorcode-deleted", "code", code));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("redeem"));
            if (sender.hasPermission("ghostystream.creatorcode.create")) {
                subs.add("create");
                subs.add("delete");
            }
            return subs;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("create") && sender.hasPermission("ghostystream.creatorcode.create")) {
            // Suggest online content creators
            List<String> creators = new ArrayList<>();
            plugin.getCreatorCodeManager().getContentCreators().forEach(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) creators.add(p.getName());
            });
            return creators;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("delete") && sender.hasPermission("ghostystream.creatorcode.create")) {
            return new ArrayList<>(plugin.getCreatorCodeManager().getCodes().keySet());
        }

        return List.of();
    }
}
