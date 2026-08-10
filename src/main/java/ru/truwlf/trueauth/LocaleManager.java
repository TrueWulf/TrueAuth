package ru.truwlf.trueauth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

final class LocaleManager {
    private final YamlConfiguration locale;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    LocaleManager(File dataFolder, String language) {
        File file = new File(dataFolder, "lang/" + language + ".yml");
        locale = YamlConfiguration.loadConfiguration(file.isFile() ? file : new File(dataFolder, "lang/en_US.yml"));
    }

    Component message(String key) { return message(key, Map.of()); }
    Component message(String key, Map<String, String> values) {
        String text = locale.getString(key, "<red>Missing locale key: " + key);
        for (Map.Entry<String, String> value : values.entrySet()) text = text.replace("<" + value.getKey() + ">", value.getValue());
        return miniMessage.deserialize(text);
    }
}
