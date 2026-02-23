package ghostystream;

import ghostystream.commands.*;
import ghostystream.gui.ContentGUI;
import ghostystream.listener.GUIListener;
import ghostystream.manager.CreatorCodeManager;
import ghostystream.manager.LangManager;
import ghostystream.manager.StreamManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GhostyStream – Main Plugin Class
 * Author: Ger_Gh0stface
 * Version: 1.0.0
 * Supports: Paper / Spigot 1.21 – 1.21.11
 */
public class GhostyStream extends JavaPlugin {

    private static GhostyStream instance;

    private LangManager langManager;
    private StreamManager streamManager;
    private CreatorCodeManager creatorCodeManager;
    private ContentGUI contentGUI;

    private Economy economy = null;

    @Override
    public void onEnable() {
        instance = this;

        // ── Config ────────────────────────────────────────────
        saveDefaultConfig();

        // ── Managers ──────────────────────────────────────────
        this.langManager = new LangManager(this);
        langManager.load();

        this.streamManager = new StreamManager(this);

        this.creatorCodeManager = new CreatorCodeManager(this);
        creatorCodeManager.load();

        this.contentGUI = new ContentGUI(this);

        // ── Vault ─────────────────────────────────────────────
        if (!setupEconomy()) {
            getLogger().warning("Vault / Economy not found! Streamer rewards will not work.");
        } else {
            getLogger().info("Vault Economy hooked: " + economy.getName());
        }

        // ── Commands ──────────────────────────────────────────
        registerCommands();

        // ── Listeners ─────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // ── Done ──────────────────────────────────────────────
        String lang = langManager.getLanguage();
        getLogger().info("GhostyStream v" + getPluginMeta().getVersion() + " enabled! [Lang: " + lang + "]");
    }

    @Override
    public void onDisable() {
        if (streamManager != null) {
            streamManager.stopAll();
        }
        if (creatorCodeManager != null) {
            creatorCodeManager.save();
        }
        getLogger().info("GhostyStream disabled.");
    }

    // ─── Reload ─────────────────────────────────────────────

    public void reloadPlugin() {
        // Stop all active streams
        if (streamManager != null) streamManager.stopAll();

        // Save data
        if (creatorCodeManager != null) creatorCodeManager.save();

        // Reload config
        reloadConfig();

        // Reload lang
        langManager.load();

        // Reload data
        creatorCodeManager.load();

        // Re-setup economy in case Vault changed
        setupEconomy();

        getLogger().info("GhostyStream reloaded.");
    }

    // ─── Internal Setup ─────────────────────────────────────

    private void registerCommands() {
        var streamCmd = getCommand("stream");
        if (streamCmd != null) {
            StreamCommand executor = new StreamCommand(this);
            streamCmd.setExecutor(executor);
            streamCmd.setTabCompleter(executor);
        }

        var contentCmd = getCommand("content");
        if (contentCmd != null) {
            ContentCommand executor = new ContentCommand(this);
            contentCmd.setExecutor(executor);
            contentCmd.setTabCompleter(executor);
        }

        var creatorCodeCmd = getCommand("creatorcode");
        if (creatorCodeCmd != null) {
            CreatorCodeCommand executor = new CreatorCodeCommand(this);
            creatorCodeCmd.setExecutor(executor);
            creatorCodeCmd.setTabCompleter(executor);
        }

        var gsCmd = getCommand("ghostystream");
        if (gsCmd != null) {
            GhostyStreamCommand executor = new GhostyStreamCommand(this);
            gsCmd.setExecutor(executor);
            gsCmd.setTabCompleter(executor);
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return true;
    }

    // ─── Getters ────────────────────────────────────────────

    public static GhostyStream getInstance() {
        return instance;
    }

    public LangManager getLang() {
        return langManager;
    }

    public StreamManager getStreamManager() {
        return streamManager;
    }

    public CreatorCodeManager getCreatorCodeManager() {
        return creatorCodeManager;
    }

    public ContentGUI getContentGUI() {
        return contentGUI;
    }

    public Economy getEconomy() {
        return economy;
    }
}
