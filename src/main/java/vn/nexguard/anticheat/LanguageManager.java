
package vn.nexguard.anticheat;

import org.bukkit.ChatColor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

public final class LanguageManager {

    private final NexGuard plugin;

    private Properties messages = new Properties();
    private Properties english = new Properties();

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

        language = language.trim().toLowerCase(Locale.ROOT);

        if (!language.equals("vi") && !language.equals("en")) {
            plugin.getLogger().warning(
                    "Ngôn ngữ không hợp lệ, chuyển sang English."
            );
            language = "en";
        }

        english = readLanguage("lang/en.lang");

        if (language.equals("en")) {
            messages = english;
        } else {
            messages = readLanguage("lang/vi.lang");
        }
    }

    private Properties readLanguage(String path) {
        Properties result = new Properties();

        try (InputStream input = plugin.getResource(path)) {
            if (input == null) {
                plugin.getLogger().severe(
                        "Không tìm thấy tệp ngôn ngữ: " + path
                );
                return result;
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 input,
                                 StandardCharsets.UTF_8
                         )) {
                result.load(reader);
            }

        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "Không thể tải ngôn ngữ: " + path
            );
        }

        return result;
    }

    public String get(String key) {
        return get(key, new String[0]);
    }

    public String get(String key, String... placeholders) {
        String message = messages.getProperty(key);

        if (message == null) {
            message = english.getProperty(key, key);
        }

        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace(
                    "{" + placeholders[i] + "}",
                    placeholders[i + 1]
            );
        }

        return ChatColor.translateAlternateColorCodes(
                '&',
                message.replace("\\n", "\n")
        );
    }
}
