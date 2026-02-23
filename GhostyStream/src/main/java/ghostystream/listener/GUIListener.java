package ghostystream.listener;

import ghostystream.GhostyStream;
import ghostystream.gui.ContentGUI;
import ghostystream.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles GUI click events.
 */
public class GUIListener implements Listener {

    private final GhostyStream plugin;
    private final NamespacedKey guiTypeKey;
    private final NamespacedKey streamLinkKey;

    public GUIListener(GhostyStream plugin) {
        this.plugin = plugin;
        this.guiTypeKey = new NamespacedKey(plugin, "gui_type");
        this.streamLinkKey = new NamespacedKey(plugin, "stream_link");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Use Adventure title() instead of deprecated getTitle()
        String title = LegacyComponentSerializer.legacySection()
                .serialize(event.getView().title());
        String expectedTitle = plugin.getLang().get("gui-title");
        if (!title.equals(expectedTitle)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        // Check if this is a creator item
        String guiType = meta.getPersistentDataContainer().get(guiTypeKey, PersistentDataType.STRING);
        if (!ContentGUI.GUI_IDENTIFIER.equals(guiType)) return;

        // Check if it has a stream link (live creator)
        String link = meta.getPersistentDataContainer().get(streamLinkKey, PersistentDataType.STRING);
        if (link != null && !link.isEmpty()) {
            sendClickableLink(player, link);
            player.sendMessage(plugin.getLang().get("gui-link-copied"));
        }
    }

    private void sendClickableLink(Player player, String link) {
        // Use Adventure API instead of deprecated BungeeCord chat API
        Component message = LegacyComponentSerializer.legacySection()
                .deserialize(ColorUtil.colorize("&#00BFFF▶ " + link))
                .clickEvent(ClickEvent.openUrl(link))
                .hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacySection()
                                .deserialize(ColorUtil.colorize("&eKlicken zum Öffnen des Streams!"))));
        player.sendMessage(message);
    }
}
