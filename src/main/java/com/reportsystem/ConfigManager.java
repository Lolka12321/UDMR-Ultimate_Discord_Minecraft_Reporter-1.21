package com.reportsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ConfigManager {

    private final ReportSystem plugin;
    private FileConfiguration config;

    public ConfigManager(ReportSystem plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // Discord настройки
    public String getWebhookUrl() {
        return config.getString("discord.webhook-url", "");
    }

    public boolean useBot() {
        return config.getBoolean("discord.use-bot", false);
    }

    public String getBotToken() {
        return config.getString("discord.bot-token", "");
    }

    public String getChannelId() {
        return config.getString("discord.channel-id", "");
    }

    // Настройки жалоб
    public int getMaxActiveReports() {
        return config.getInt("reports.max-active-reports", 5);
    }

    public int getFormTimeout() {
        return config.getInt("reports.form-timeout", 300);
    }

    public int getHistoryLimit() {
        return config.getInt("reports.history-limit", 20);
    }

    // Сообщения
    public Component getMessage(String path) {
        String prefix = config.getString("messages.prefix", "&8[&6Report&8]&r");
        String message = config.getString("messages." + path, "Message not found: " + path);
        return colorize(prefix + " " + message);
    }

    public Component getMessageWithoutPrefix(String path) {
        String message = config.getString("messages." + path, "Message not found: " + path);
        return colorize(message);
    }

    public Component getMessage(String path, String... replacements) {
        String message = config.getString("messages." + path, "Message not found: " + path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        String prefix = config.getString("messages.prefix", "&8[&6Report&8]&r");
        return colorize(prefix + " " + message);
    }

    public Component getMessageRaw(String path, String... replacements) {
        String message = config.getString("messages." + path, "Message not found: " + path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return colorize(message);
    }

    // Формат даты
    public String formatDate(Date date) {
        String format = config.getString("date-format", "dd.MM.yyyy HH:mm");
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    // Утилита для цвета
    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}