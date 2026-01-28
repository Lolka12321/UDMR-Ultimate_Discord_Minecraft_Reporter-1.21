package com.reportsystem;

import com.reportsystem.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class LocaleManager {

    private final ReportSystem plugin;
    private final Map<String, FileConfiguration> locales;
    private String currentLocale;

    public LocaleManager(ReportSystem plugin) {
        this.plugin = plugin;
        this.locales = new HashMap<>();
        this.currentLocale = plugin.getConfig().getString("language", "en-EN");
        loadLocales();
    }

    private void loadLocales() {
        File localesFolder = new File(plugin.getDataFolder(), "locales");
        if (!localesFolder.exists()) {
            localesFolder.mkdirs();
        }

        saveDefaultLocale("en-EN.yml");
        saveDefaultLocale("ru-RU.yml");

        File[] localeFiles = localesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (localeFiles != null) {
            for (File localeFile : localeFiles) {
                String localeName = localeFile.getName().replace(".yml", "");
                FileConfiguration locale = YamlConfiguration.loadConfiguration(localeFile);
                locales.put(localeName, locale);
                plugin.getLogger().info("Loaded locale: " + localeName);
            }
        }

        if (!locales.containsKey(currentLocale)) {
            plugin.getLogger().warning("Locale " + currentLocale + " not found, falling back to en-EN");
            currentLocale = "en-EN";
        }
    }

    private void saveDefaultLocale(String fileName) {
        File localeFile = new File(plugin.getDataFolder(), "locales/" + fileName);
        if (!localeFile.exists()) {
            try {
                InputStream inputStream = plugin.getResource("locales/" + fileName);
                if (inputStream != null) {
                    java.nio.file.Files.copy(inputStream, localeFile.toPath());
                    plugin.getLogger().info("Created default locale file: " + fileName);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save default locale " + fileName + ": " + e.getMessage());
            }
        }
    }

    public void reload() {
        locales.clear();
        this.currentLocale = plugin.getConfig().getString("language", "en-EN");
        loadLocales();
    }

    public String getCurrentLocale() {
        return currentLocale;
    }

    public void setLocale(String locale) {
        if (locales.containsKey(locale)) {
            this.currentLocale = locale;
            plugin.getConfig().set("language", locale);
            plugin.saveConfig();
            plugin.getLogger().info("Language changed to: " + locale);
        } else {
            plugin.getLogger().warning("Locale " + locale + " not found!");
        }
    }

    public String getRaw(String path) {
        FileConfiguration locale = locales.get(currentLocale);
        if (locale == null || !locale.contains(path)) {
            locale = locales.get("en-EN");
            if (locale == null || !locale.contains(path)) {
                return "Missing translation: " + path;
            }
        }
        return locale.getString(path, "Missing translation: " + path);
    }

    public Component getMessage(String path) {
        String prefix = getRaw("prefix");
        String message = getRaw(path);
        return colorize(prefix + " " + message);
    }

    public Component getMessageWithoutPrefix(String path) {
        String message = getRaw(path);
        return colorize(message);
    }

    public Component getMessage(String path, String... replacements) {
        String message = getRaw(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        String prefix = getRaw("prefix");
        return colorize(prefix + " " + message);
    }

    public Component getMessageRaw(String path, String... replacements) {
        String message = getRaw(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return colorize(message);
    }

    private Component colorize(String text) {
        // Используем ColorUtil для поддержки градиентов
        return ColorUtil.parseColors(text);
    }
}