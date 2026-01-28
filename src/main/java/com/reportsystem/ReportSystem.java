package com.reportsystem;

import com.reportsystem.commands.ReportCommand;
import com.reportsystem.commands.UnfreezeCommand;
import com.reportsystem.data.DataManager;
import com.reportsystem.discord.DiscordBot;
import com.reportsystem.gui.ReportsGUI;
import com.reportsystem.listeners.ChatListener;
import com.reportsystem.listeners.GUIListener;
import com.reportsystem.managers.ReportManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public class ReportSystem extends JavaPlugin {

    private static ReportSystem instance;
    private LocaleManager localeManager;
    private DataManager dataManager;
    private ReportManager reportManager;
    private DiscordBot discordBot;
    private ReportsGUI reportsGUI;

    @Override
    public void onEnable() {
        instance = this;

        // Загрузка конфигурации
        saveDefaultConfig();

        // Инициализация локализации
        localeManager = new LocaleManager(this);

        // Инициализация менеджеров
        dataManager = new DataManager(this);
        reportManager = new ReportManager(this);
        reportsGUI = new ReportsGUI(this);

        // Регистрация команд
        registerCommands();

        // Инициализация Discord бота
        if (getConfig().getBoolean("discord.use-bot", false)) {
            String botToken = getConfig().getString("discord.bot-token", "");
            String channelId = getConfig().getString("discord.channel-id", "");

            if (botToken.isEmpty() || botToken.contains("YOUR_BOT_TOKEN")) {
                getLogger().warning("Discord bot token is not configured. Set it in config.yml");
            } else if (channelId.isEmpty() || channelId.contains("YOUR_CHANNEL_ID")) {
                getLogger().warning("Discord channel ID is not configured. Set it in config.yml");
            } else {
                discordBot = new DiscordBot(this);
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    discordBot.start();
                });
            }
        }

        // Регистрация слушателей
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        getLogger().info("ReportSystem v1.0.0 enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Сохранение данных
        if (dataManager != null) {
            dataManager.saveAllData();
        }

        // Остановка Discord бота
        if (discordBot != null) {
            discordBot.shutdown();
        }

        getLogger().info("ReportSystem disabled!");
    }

    /**
     * Регистрация команд
     */
    private void registerCommands() {
        ReportCommand reportCommand = new ReportCommand(this);
        UnfreezeCommand unfreezeCommand = new UnfreezeCommand(this);

        // Пробуем стандартный способ
        PluginCommand reportCmd = getCommand("report");
        PluginCommand reportsCmd = getCommand("reports");
        PluginCommand unfreezeCmd = getCommand("unfreeze");

        if (reportCmd != null) {
            // Стандартная регистрация работает
            reportCmd.setExecutor(reportCommand);
            reportCmd.setTabCompleter(reportCommand);
        } else {
            // Используем ручную регистрацию для TabExecutor
            registerTabCommandManually("report", reportCommand, "rep");
        }

        if (reportsCmd != null) {
            // Регистрируем /reports как отдельную команду
            reportsCmd.setExecutor(reportCommand);
            reportsCmd.setTabCompleter(reportCommand);
        } else {
            // Используем ручную регистрацию для TabExecutor
            registerTabCommandManually("reports", reportCommand);
        }

        if (unfreezeCmd != null) {
            unfreezeCmd.setExecutor(unfreezeCommand);
        } else {
            // Используем ручную регистрацию для CommandExecutor
            registerCommandManually("unfreeze", unfreezeCommand);
        }
    }

    /**
     * Ручная регистрация команды через CommandMap
     */
    private void registerCommandManually(String name, org.bukkit.command.CommandExecutor executor, String... aliases) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            org.bukkit.command.defaults.BukkitCommand command = new org.bukkit.command.defaults.BukkitCommand(name) {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                    return executor.onCommand(sender, this, label, args);
                }
            };

            if (aliases.length > 0) {
                command.setAliases(java.util.Arrays.asList(aliases));
            }

            commandMap.register(getName(), command);

        } catch (Exception e) {
            getLogger().severe("Failed to register command: " + name);
        }
    }

    /**
     * Ручная регистрация TabExecutor команды через CommandMap
     */
    private void registerTabCommandManually(String name, org.bukkit.command.TabExecutor executor, String... aliases) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            org.bukkit.command.defaults.BukkitCommand command = new org.bukkit.command.defaults.BukkitCommand(name) {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                    return executor.onCommand(sender, this, label, args);
                }

                @Override
                public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                    return executor.onTabComplete(sender, this, alias, args);
                }
            };

            if (aliases.length > 0) {
                command.setAliases(java.util.Arrays.asList(aliases));
            }

            commandMap.register(getName(), command);

        } catch (Exception e) {
            getLogger().severe("Failed to register command: " + name);
        }
    }

    public static ReportSystem getInstance() {
        return instance;
    }

    public LocaleManager getLocaleManager() {
        return localeManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public ReportsGUI getReportsGUI() {
        return reportsGUI;
    }
}