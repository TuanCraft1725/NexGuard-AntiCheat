
package vn.nexguard.anticheat;

import org.bukkit.ChatColor;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class LanguageManager {

    private final NexGuard plugin;
    private Properties messages = new Properties();

    public LanguageManager(NexGuard plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        String language = plugin.getConfig()
                .getString("general.language", "vi");

        if (language == null) {
            language = "vi";
        }

        language = language.trim().toLowerCase();

        if (!language.equals("vi") && !language.equals("en")) {
            language = "en";
        }

        String path = "lang/" + language + ".lang";
        File file = new File(plugin.getDataFolder(), path);

        if (!file.exists()) {
            plugin.saveResource(path, false);
        }

        Properties loaded = new Properties();

        try (InputStream input = plugin.getResource(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing language resource: " + path
                );
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 input,
                                 StandardCharsets.UTF_8
                         )) {
                loaded.load(reader);
            }
        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "Cannot load language: " + path
            );

            if (!language.equals("en")) {
                language = "en";
                loadEnglishFallback(loaded);
            }
        }

        messages = loaded;
    }

    private void loadEnglishFallback(Properties loaded) {
        try (InputStream input =
                     plugin.getResource("lang/en.lang")) {

            if (input == null) return;

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 input,
                                 StandardCharsets.UTF_8
                         )) {
                loaded.load(reader);
            }
        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "Cannot load English fallback language."
            );
        }
    }

    public String get(String key) {
        return get(key, new String[0]);
    }

    public String get(String key, String... placeholders) {
        String message = messages.getProperty(key);

        if (message == null) {
            message = key;
        }

        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace(
                    "{" + placeholders[i] + "}",
                    placeholders[i + 1]
            );
        }

        return ChatColor.translateAlternateColorCodes(
                '&', message.replace("\\n", "\n")
        );
    }
}
