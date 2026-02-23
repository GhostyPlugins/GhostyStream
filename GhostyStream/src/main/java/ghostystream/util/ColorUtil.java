package ghostystream.util;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for translating color codes including & codes and &#RRGGBB hex codes.
 */
public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Translates both & color codes and &#RRGGBB hex codes into Minecraft color codes.
     *
     * @param text the text to colorize
     * @return the colorized text
     */
    public static String colorize(String text) {
        if (text == null) return "";

        // First handle hex codes &#RRGGBB
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder(ChatColor.COLOR_CHAR + "x");
            for (char c : hex.toCharArray()) {
                replacement.append(ChatColor.COLOR_CHAR).append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        // Then handle standard & codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Strips all color codes (& and hex) from a string.
     *
     * @param text the text to strip
     * @return plain text
     */
    public static String strip(String text) {
        if (text == null) return "";
        text = HEX_PATTERN.matcher(text).replaceAll("");
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }
}
