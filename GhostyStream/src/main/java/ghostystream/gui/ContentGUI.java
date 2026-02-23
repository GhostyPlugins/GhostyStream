package ghostystream.gui;

import ghostystream.GhostyStream;
import ghostystream.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds and opens the Content Creator GUI.
 */
public class ContentGUI {

    private final GhostyStream plugin;

    // Tag to identify this GUI in click events
    public static final String GUI_IDENTIFIER = "GhostyStream_ContentGUI";

    public ContentGUI(GhostyStream plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the Content Creator overview GUI for a player.
     *
     * @param viewer the player to open the GUI for
     */
    public void open(Player viewer) {
        String title = plugin.getLang().get("gui-title");
        int size = plugin.getConfig().getInt("gui.size", 54);
        // Ensure size is multiple of 9 and within bounds
        size = Math.min(54, Math.max(9, ((size + 8) / 9) * 9));

        Inventory gui = Bukkit.createInventory(null, size,
                LegacyComponentSerializer.legacySection().deserialize(title));

        // Fill borders with glass panes
        ItemStack border = createGlassPane();
        for (int i = 0; i < 9; i++) gui.setItem(i, border);
        for (int i = size - 9; i < size; i++) gui.setItem(i, border);
        for (int i = 9; i < size - 9; i += 9) gui.setItem(i, border);
        for (int i = 17; i < size - 9; i += 9) gui.setItem(i, border);

        // Populate with content creators
        List<UUID> creators = new ArrayList<>(plugin.getCreatorCodeManager().getContentCreators());

        if (creators.isEmpty()) {
            // Show empty placeholder in the center
            gui.setItem(size / 2, createEmptyItem());
        } else {
            // Fill slots (interior only, skipping border)
            int slot = 10;
            for (UUID creatorUUID : creators) {
                if (slot >= size - 10) break; // GUI full
                // Skip border right column
                if ((slot % 9) == 8) slot += 2;

                OfflinePlayer creator = Bukkit.getOfflinePlayer(creatorUUID);
                ItemStack creatorItem = buildCreatorItem(creator);
                gui.setItem(slot, creatorItem);
                slot++;
            }
        }

        viewer.openInventory(gui);
    }

    private ItemStack buildCreatorItem(OfflinePlayer creator) {
        boolean online = creator.isOnline();
        boolean streaming = online && plugin.getStreamManager().isStreaming(creator.getUniqueId());
        Optional<String> link = plugin.getStreamManager().getStreamLink(creator.getUniqueId());

        String displayName;
        List<String> lore;

        if (streaming) {
            displayName = plugin.getLang().get("gui-creator-live-name")
                    .replace("{player}", creator.getName() != null ? creator.getName() : "Unknown");
            lore = new ArrayList<>(plugin.getLang().getList("gui-creator-live-lore"));
            lore.replaceAll(line -> line.replace("{link}", link.orElse("N/A")));
        } else if (online) {
            displayName = plugin.getLang().get("gui-creator-online-name")
                    .replace("{player}", creator.getName() != null ? creator.getName() : "Unknown");
            lore = new ArrayList<>(plugin.getLang().getList("gui-creator-online-lore"));
        } else {
            displayName = plugin.getLang().get("gui-creator-offline-name")
                    .replace("{player}", creator.getName() != null ? creator.getName() : "Unknown");
            lore = new ArrayList<>(plugin.getLang().getList("gui-creator-offline-lore"));
        }

        // Build player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(creator);
            // Adventure-based display name and lore (non-deprecated)
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(displayName));
            meta.lore(lore.stream()
                    .map(l -> LegacyComponentSerializer.legacySection().deserialize(l))
                    .collect(Collectors.toList()));

            // Store stream link in persistent data for click handling
            if (streaming && link.isPresent()) {
                meta.getPersistentDataContainer().set(
                        new org.bukkit.NamespacedKey(plugin, "stream_link"),
                        org.bukkit.persistence.PersistentDataType.STRING,
                        link.get()
                );
            }
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "gui_type"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    GUI_IDENTIFIER
            );

            head.setItemMeta(meta);
        }

        return head;
    }

    private ItemStack createGlassPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createEmptyItem() {
        String materialName = plugin.getConfig().getString("gui.empty-item.material", "GRAY_STAINED_GLASS_PANE");
        Material mat;
        try {
            mat = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(
                    ColorUtil.colorize(
                            plugin.getConfig().getString("gui.empty-item.name", "&7Keine Creator aktiv"))));
            item.setItemMeta(meta);
        }
        return item;
    }
}
