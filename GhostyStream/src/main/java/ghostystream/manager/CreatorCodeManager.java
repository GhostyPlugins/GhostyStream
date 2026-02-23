package ghostystream.manager;

import ghostystream.GhostyStream;
import ghostystream.util.ColorUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages creator codes: creation, redemption, persistence.
 */
public class CreatorCodeManager {

    private final GhostyStream plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    // code -> streamer UUID
    private final Map<String, UUID> codes = new HashMap<>();
    // player UUID -> redeemed code
    private final Map<UUID, String> redeemedCodes = new HashMap<>();
    // registered content creators (UUID set)
    private final Set<UUID> contentCreators = new HashSet<>();

    public CreatorCodeManager(GhostyStream plugin) {
        this.plugin = plugin;
    }

    // ─── Load / Save ────────────────────────────────────────

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        codes.clear();
        redeemedCodes.clear();
        contentCreators.clear();

        // Load codes
        ConfigurationSection codesSection = dataConfig.getConfigurationSection("codes");
        if (codesSection != null) {
            for (String code : codesSection.getKeys(false)) {
                String uuidStr = codesSection.getString(code);
                if (uuidStr != null) {
                    try {
                        codes.put(code, UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        // Load redeemed
        ConfigurationSection redeemedSection = dataConfig.getConfigurationSection("redeemed");
        if (redeemedSection != null) {
            for (String uuidStr : redeemedSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String code = redeemedSection.getString(uuidStr);
                    redeemedCodes.put(uuid, code);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Load content creators
        List<String> creatorList = dataConfig.getStringList("content-creators");
        for (String uuidStr : creatorList) {
            try {
                contentCreators.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        if (dataConfig == null) return;

        // Save codes
        dataConfig.set("codes", null);
        codes.forEach((code, uuid) -> dataConfig.set("codes." + code, uuid.toString()));

        // Save redeemed
        dataConfig.set("redeemed", null);
        redeemedCodes.forEach((uuid, code) -> dataConfig.set("redeemed." + uuid.toString(), code));

        // Save creators
        List<String> creatorList = new ArrayList<>();
        contentCreators.forEach(uuid -> creatorList.add(uuid.toString()));
        dataConfig.set("content-creators", creatorList);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    // ─── Creator Management ─────────────────────────────────

    public boolean addCreator(UUID uuid) {
        if (contentCreators.contains(uuid)) return false;
        contentCreators.add(uuid);
        save();
        return true;
    }

    public boolean removeCreator(UUID uuid) {
        if (!contentCreators.contains(uuid)) return false;
        contentCreators.remove(uuid);
        save();
        return true;
    }

    public boolean isCreator(UUID uuid) {
        return contentCreators.contains(uuid);
    }

    public Set<UUID> getContentCreators() {
        return Collections.unmodifiableSet(contentCreators);
    }

    // ─── Code Management ────────────────────────────────────

    public boolean createCode(String code, UUID streamerUUID) {
        if (codes.containsKey(code)) return false;
        codes.put(code, streamerUUID);
        save();
        return true;
    }

    public boolean deleteCode(String code) {
        if (!codes.containsKey(code)) return false;
        codes.remove(code);
        save();
        return true;
    }

    public boolean codeExists(String code) {
        return codes.containsKey(code);
    }

    public Optional<UUID> getStreamerForCode(String code) {
        UUID uuid = codes.get(code);
        return Optional.ofNullable(uuid);
    }

    // ─── Redemption ─────────────────────────────────────────

    public boolean hasRedeemed(UUID playerUUID) {
        return redeemedCodes.containsKey(playerUUID);
    }

    /**
     * Redeems a creator code for a player.
     * Gives rewards to the player and credits the streamer.
     *
     * @param player the player redeeming
     * @param code   the code to redeem
     * @return result enum
     */
    public RedeemResult redeem(Player player, String code) {
        if (!codes.containsKey(code)) {
            return RedeemResult.CODE_NOT_FOUND;
        }
        if (redeemedCodes.containsKey(player.getUniqueId())) {
            return RedeemResult.ALREADY_REDEEMED;
        }

        UUID streamerUUID = codes.get(code);
        OfflinePlayer streamer = Bukkit.getOfflinePlayer(streamerUUID);

        // Give rewards to player
        givePlayerRewards(player, streamer);

        // Give currency to streamer via Vault
        double reward = plugin.getConfig().getDouble("streamer-reward", 2000.0);
        if (plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(streamer, reward);
        }

        // Mark as redeemed
        redeemedCodes.put(player.getUniqueId(), code);
        save();

        // Notify streamer if online
        Player onlineStreamer = streamer.getPlayer();
        if (onlineStreamer != null && onlineStreamer.isOnline()) {
            String currency = plugin.getEconomy() != null ? plugin.getEconomy().currencyNamePlural() : "Coins";
            onlineStreamer.sendMessage(plugin.getLang().get("creatorcode-redeemed-streamer",
                    "player", player.getName(),
                    "code", code,
                    "reward", String.valueOf((int) reward),
                    "currency", currency));
        }

        return RedeemResult.SUCCESS;
    }

    private void givePlayerRewards(Player player, OfflinePlayer streamer) {
        List<ItemStack> rewards = new ArrayList<>();

        // Iron Armor
        rewards.add(new ItemStack(Material.IRON_HELMET));
        rewards.add(new ItemStack(Material.IRON_CHESTPLATE));
        rewards.add(new ItemStack(Material.IRON_LEGGINGS));
        rewards.add(new ItemStack(Material.IRON_BOOTS));

        // Iron Tools
        rewards.add(new ItemStack(Material.IRON_SWORD));
        rewards.add(new ItemStack(Material.IRON_PICKAXE));
        rewards.add(new ItemStack(Material.IRON_AXE));
        rewards.add(new ItemStack(Material.IRON_SHOVEL));
        rewards.add(new ItemStack(Material.IRON_HOE));

        // 2x Enchanted Golden Apple
        ItemStack enchApple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2);
        rewards.add(enchApple);

        // Player head of the streamer
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(streamer);
            // Use Adventure displayName instead of deprecated setDisplayName
            skullMeta.displayName(LegacyComponentSerializer.legacySection().deserialize(
                    ColorUtil.colorize("&6Kopf von &e" + streamer.getName())));
            head.setItemMeta(skullMeta);
        }
        rewards.add(head);

        // Add to inventory, drop overflow
        for (ItemStack item : rewards) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            overflow.values().forEach(dropped ->
                    player.getWorld().dropItemNaturally(player.getLocation(), dropped));
        }
    }

    public Map<String, UUID> getCodes() {
        return Collections.unmodifiableMap(codes);
    }

    // ─── Result Enum ────────────────────────────────────────

    public enum RedeemResult {
        SUCCESS,
        CODE_NOT_FOUND,
        ALREADY_REDEEMED
    }
}
