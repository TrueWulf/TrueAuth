package ru.truwlf.trueauth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Set;
import java.util.Map;

final class LocaleManager {
    private final YamlConfiguration locale;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("de_DE", "en_US", "es_ES", "fr_FR", "it_IT", "pt_BR", "ru_RU");

    LocaleManager(File dataFolder, String language) {
        String safeLanguage = SUPPORTED_LANGUAGES.contains(language) ? language : "en_US";
        File file = new File(dataFolder, "lang/" + safeLanguage + ".yml");
        locale = YamlConfiguration.loadConfiguration(file.isFile() ? file : new File(dataFolder, "lang/en_US.yml"));
    }

    Component message(String key) { return message(key, Map.of()); }
    Component message(String key, Map<String, String> values) {
        String text = locale.getString(key, "<red>Missing locale key: " + key);
        for (Map.Entry<String, String> value : values.entrySet()) text = text.replace("<" + value.getKey() + ">", miniMessage.escapeTags(value.getValue()));
        return miniMessage.deserialize(text);
    }
}
