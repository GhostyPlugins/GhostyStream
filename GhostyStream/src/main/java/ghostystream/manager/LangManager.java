package ghostystream.manager;

import ghostystream.GhostyStream;
import ghostystream.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages language files and message retrieval.
 */
public class LangManager {

    private final GhostyStream plugin;
    private FileConfiguration langConfig;
    private String language;

    public LangManager(GhostyStream plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.language = plugin.getConfig().getString("language", "de");
        // Always extract ALL bundled lang files so they appear in the lang folder
        extractAllLangFiles();
        langConfig = loadLangFile(language);
    }

    /** Extracts every bundled lang file to plugins/GhostyStream/lang/ on first run. */
    private void extractAllLangFiles() {
        String[] bundled = {"de", "en"};
        for (String lang : bundled) {
            File target = new File(new File(plugin.getDataFolder(), "lang"), lang + ".yml");
            if (!target.exists()) {
                copyResource("lang/" + lang + ".yml", target);
            }
        }
    }

    private void copyResource(String resourcePath, File target) {
        target.getParentFile().mkdirs();
        try (InputStream is = plugin.getResource(resourcePath)) {
            if (is == null) return;
            try (OutputStream os = new FileOutputStream(target)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not copy " + resourcePath + ": " + e.getMessage());
        }
    }

    private FileConfiguration loadLangFile(String lang) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        File langFile = new File(langFolder, lang + ".yml");

        // Copy from resources if still not present (e.g. custom language)
        if (!langFile.exists()) {
            copyResource("lang/" + lang + ".yml", langFile);
            if (!langFile.exists()) {
                plugin.getLogger().warning("Language file '" + lang + ".yml' not found! Falling back to 'de'.");
                if (!lang.equals("de")) {
                    return loadLangFile("de");
                }
            }
        }

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file still missing, using empty config.");
            return new YamlConfiguration();
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load language file: " + e.getMessage());
            return new YamlConfiguration();
        }
    }

    /**
     * Gets a message from the language file and applies color codes.
     * Replaces {prefix} with the configured prefix.
     *
     * @param key the message key
     * @return the colorized message
     */
    public String get(String key) {
        String raw = langConfig.getString(key, "&c[GhostyStream] Missing key: " + key);
        String prefix = langConfig.getString("prefix", "&#FF6A00[GhostyStream] ");
        raw = raw.replace("{prefix}", prefix);
        return ColorUtil.colorize(raw);
    }

    /**
     * Gets a message with placeholder replacements.
     *
     * @param key          the message key
     * @param replacements alternating key-value pairs
     * @return the colorized message with replacements
     */
    public String get(String key, String... replacements) {
        String msg = get(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return msg;
    }

    /**
     * Gets a multiline message (YAML block scalar) as a list of strings.
     *
     * @param key the message key
     * @return list of colorized lines
     */
    public List<String> getLines(String key) {
        String raw = langConfig.getString(key, "");
        if (raw.isEmpty()) {
            return langConfig.getStringList(key).stream()
                    .map(line -> {
                        String prefix = langConfig.getString("prefix", "");
                        line = line.replace("{prefix}", prefix);
                        return ColorUtil.colorize(line);
                    })
                    .collect(Collectors.toList());
        }
        String prefix = langConfig.getString("prefix", "");
        String[] lines = raw.split("\n");
        List<String> result = new java.util.ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) {
                line = line.replace("{prefix}", prefix);
                result.add(ColorUtil.colorize(line));
            }
        }
        return result;
    }

    /**
     * Gets a string list from the language file, colorized.
     *
     * @param key the message key
     * @return list of colorized strings
     */
    public List<String> getList(String key) {
        List<String> list = langConfig.getStringList(key);
        return list.stream()
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());
    }

    public FileConfiguration getConfig() {
        return langConfig;
    }

    public String getLanguage() {
        return language;
    }
}
